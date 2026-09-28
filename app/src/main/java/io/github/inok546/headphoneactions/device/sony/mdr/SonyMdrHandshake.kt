// SPDX-License-Identifier: AGPL-3.0-only
//
// The RFCOMM service UUIDs, the init request with its retries, protocol version detection
// and the ACK / sequence number rules follow Gadgetbridge
// (https://codeberg.org/Freeyourgadget/Gadgetbridge), files
// service/devices/sony/headphones/SonyHeadphonesSupport.java:
//   Copyright (C) 2021-2024 Arjan Schrijver, José Rebelo
// service/devices/sony/headphones/SonyHeadphonesProtocol.java:
//   Copyright (C) 2021-2026 José Rebelo
// Gadgetbridge is licensed under the GNU AGPL version 3 or later; used here under version 3.

package io.github.inok546.headphoneactions.device.sony.mdr

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import io.github.inok546.headphoneactions.LOG_TAG
import io.github.inok546.headphoneactions.device.ConnectionTestResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Opens the Sony MDR RFCOMM channel, performs the init handshake and closes the channel.
 * This proves the transport; commands come later.
 */
object SonyMdrHandshake {

    private val SERVICE_UUID_V1 = UUID.fromString("96CC203E-5068-46ad-B32D-E316F5E069BA")
    private val SERVICE_UUID_V2 = UUID.fromString("956C7B26-D49A-4BA8-B03F-B17D393CB6E2")

    private val INIT_REQUEST = byteArrayOf(0x00, 0x00)
    private const val INIT_REPLY: Byte = 0x01

    // The headphones sometimes ignore the first init request.
    private const val INIT_ATTEMPTS = 3
    private const val INIT_REPLY_TIMEOUT_MS = 1250L
    private const val CONNECT_TIMEOUT_MS = 15_000L

    suspend fun run(context: Context, address: String): ConnectionTestResult = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return@withContext ConnectionTestResult.Failure("Nearby devices permission is not granted")
        }
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
            ?: return@withContext ConnectionTestResult.Failure("Bluetooth is not available")
        if (!adapter.isEnabled) return@withContext ConnectionTestResult.Failure("Bluetooth is off")

        val device = adapter.getRemoteDevice(address)
        var socket: BluetoothSocket? = null
        try {
            socket = device.createRfcommSocketToServiceRecord(chooseServiceUuid(device))
            connect(socket)
            Log.i(LOG_TAG, "Sony RFCOMM connected to $address")
            handshake(socket)
        } catch (e: IOException) {
            Log.w(LOG_TAG, "Sony RFCOMM connection to $address failed", e)
            ConnectionTestResult.Failure("RFCOMM connection failed: ${e.message}")
        } catch (e: SecurityException) {
            Log.w(LOG_TAG, "Sony RFCOMM connection to $address not permitted", e)
            ConnectionTestResult.Failure("Bluetooth access denied: ${e.message}")
        } finally {
            socket?.close()
            Log.i(LOG_TAG, "Sony RFCOMM closed")
        }
    }

    @SuppressLint("MissingPermission") // Checked in run().
    private fun chooseServiceUuid(device: BluetoothDevice): UUID {
        val advertised = device.uuids?.map { it.uuid }.orEmpty()
        Log.d(LOG_TAG, "Device service UUIDs: $advertised")
        // Prefer V2, as Gadgetbridge does; fall back to it when the SDP cache has neither.
        val uuid = when {
            SERVICE_UUID_V2 in advertised -> SERVICE_UUID_V2
            SERVICE_UUID_V1 in advertised -> SERVICE_UUID_V1
            else -> SERVICE_UUID_V2
        }
        Log.i(LOG_TAG, "Using Sony service UUID ${if (uuid == SERVICE_UUID_V2) "V2" else "V1"} $uuid")
        return uuid
    }

    /** Blocks until connected; closing the socket from the watchdog aborts a hanging attempt. */
    @SuppressLint("MissingPermission") // Checked in run().
    private suspend fun connect(socket: BluetoothSocket) = coroutineScope {
        val watchdog = launch {
            delay(CONNECT_TIMEOUT_MS)
            Log.w(LOG_TAG, "Sony RFCOMM connect timed out")
            socket.close()
        }
        try {
            socket.connect()
        } finally {
            watchdog.cancel()
        }
    }

    private suspend fun handshake(socket: BluetoothSocket): ConnectionTestResult = coroutineScope {
        val incoming = Channel<SonyMdrMessage>(Channel.UNLIMITED)
        val reader = launch { readMessages(socket.inputStream, incoming) }
        try {
            val link = MdrLink(socket.outputStream, incoming)
            val reply = requestInit(link)
                ?: return@coroutineScope ConnectionTestResult.Failure("No reply to the init request")
            ConnectionTestResult.Success("Sony MDR protocol ${protocolVersion(reply)}, init reply ${reply.payload.toHex()}")
        } catch (e: ClosedReceiveChannelException) {
            ConnectionTestResult.Failure("The headphones closed the connection")
        } finally {
            socket.close() // Unblocks the reader.
            reader.cancel()
        }
    }

    private suspend fun requestInit(link: MdrLink): SonyMdrMessage? {
        repeat(INIT_ATTEMPTS) { attempt ->
            link.send(SonyMdrMessage.TYPE_COMMAND_1, INIT_REQUEST)
            val reply = withTimeoutOrNull(INIT_REPLY_TIMEOUT_MS) {
                var message: SonyMdrMessage
                do {
                    message = link.receiveCommand()
                } while (message.type != SonyMdrMessage.TYPE_COMMAND_1 || message.payload.firstOrNull() != INIT_REPLY)
                message
            }
            if (reply != null) return reply
            Log.w(LOG_TAG, "No init reply (attempt ${attempt + 1} of $INIT_ATTEMPTS)")
        }
        return null
    }

    // Init reply payload lengths seen by Gadgetbridge: 4 bytes on v1 devices, 8 bytes on v2 devices.
    private fun protocolVersion(initReply: SonyMdrMessage): String = when (initReply.payload.size) {
        4 -> "v1"
        8 -> "v2"
        else -> "unknown (${initReply.payload.size}-byte init reply)"
    }

    /** Runs on the IO dispatcher; the blocking read ends when the socket is closed. */
    private suspend fun readMessages(input: InputStream, incoming: Channel<SonyMdrMessage>) {
        val frames = SonyMdrFrameReader()
        val buffer = ByteArray(1024)
        try {
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                for (frame in frames.feed(buffer, count)) {
                    val message = SonyMdrMessage.decode(frame)
                    if (message == null) {
                        Log.w(LOG_TAG, "Sony RX undecodable frame ${frame.toHex()}")
                        continue
                    }
                    Log.d(LOG_TAG, "Sony RX $message")
                    incoming.send(message)
                }
            }
        } catch (e: IOException) {
            Log.d(LOG_TAG, "Sony RX stopped: ${e.message}")
        } finally {
            incoming.close()
        }
    }

    /** Sends messages and tracks the sequence number the headphones expect. */
    private class MdrLink(private val output: OutputStream, private val incoming: ReceiveChannel<SonyMdrMessage>) {

        private var sequence: Byte = 0

        fun send(type: Byte, payload: ByteArray) = write(SonyMdrMessage(type, sequence, payload))

        /**
         * Returns the next command from the headphones, acknowledging it. ACKs received on the
         * way carry the sequence number to use for our next message.
         */
        suspend fun receiveCommand(): SonyMdrMessage {
            while (true) {
                val message = incoming.receive()
                if (message.type == SonyMdrMessage.TYPE_ACK) {
                    sequence = message.sequence
                    continue
                }
                write(SonyMdrMessage(SonyMdrMessage.TYPE_ACK, (1 - message.sequence).toByte(), ByteArray(0)))
                return message
            }
        }

        private fun write(message: SonyMdrMessage) {
            Log.d(LOG_TAG, "Sony TX $message")
            output.write(message.encode())
            output.flush()
        }
    }
}
