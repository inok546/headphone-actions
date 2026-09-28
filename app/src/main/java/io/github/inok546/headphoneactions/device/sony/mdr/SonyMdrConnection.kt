// SPDX-License-Identifier: AGPL-3.0-only
//
// The RFCOMM service UUIDs and the init request with its retries follow Gadgetbridge
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
import io.github.inok546.headphoneactions.device.DeviceResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.io.InputStream
import java.util.UUID

/**
 * Short-lived Sony MDR connections: open the RFCOMM channel, perform the init handshake,
 * run one operation and close the channel again.
 */
object SonyMdrConnection {

    private val SERVICE_UUID_V1 = UUID.fromString("96CC203E-5068-46ad-B32D-E316F5E069BA")
    private val SERVICE_UUID_V2 = UUID.fromString("956C7B26-D49A-4BA8-B03F-B17D393CB6E2")

    private val INIT_REQUEST = byteArrayOf(0x00, 0x00)
    private const val INIT_REPLY: Byte = 0x01

    // The headphones sometimes ignore the first init request.
    private const val INIT_ATTEMPTS = 3
    private const val INIT_REPLY_TIMEOUT_MS = 1250L
    private const val CONNECT_TIMEOUT_MS = 15_000L

    suspend fun open(
        context: Context,
        address: String,
        operation: suspend (SonyMdrSession) -> DeviceResult,
    ): DeviceResult = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return@withContext DeviceResult.Failure("Nearby devices permission is not granted")
        }
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
            ?: return@withContext DeviceResult.Failure("Bluetooth is not available")
        if (!adapter.isEnabled) return@withContext DeviceResult.Failure("Bluetooth is off")

        val device = adapter.getRemoteDevice(address)
        var socket: BluetoothSocket? = null
        try {
            socket = device.createRfcommSocketToServiceRecord(chooseServiceUuid(device))
            connect(socket)
            Log.i(LOG_TAG, "Sony RFCOMM connected to $address")
            runSession(socket, operation)
        } catch (e: IOException) {
            Log.w(LOG_TAG, "Sony RFCOMM connection to $address failed", e)
            DeviceResult.Failure("RFCOMM connection failed: ${e.message}")
        } catch (e: SecurityException) {
            Log.w(LOG_TAG, "Sony RFCOMM connection to $address not permitted", e)
            DeviceResult.Failure("Bluetooth access denied: ${e.message}")
        } finally {
            socket?.close()
            Log.i(LOG_TAG, "Sony RFCOMM closed")
        }
    }

    @SuppressLint("MissingPermission") // Checked in open().
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
    @SuppressLint("MissingPermission") // Checked in open().
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

    private suspend fun runSession(
        socket: BluetoothSocket,
        operation: suspend (SonyMdrSession) -> DeviceResult,
    ): DeviceResult = coroutineScope {
        val incoming = Channel<SonyMdrMessage>(Channel.UNLIMITED)
        val reader = launch { readMessages(socket.inputStream, incoming) }
        try {
            val link = SonyMdrLink(socket.outputStream, incoming)
            val initReply = requestInit(link)
                ?: return@coroutineScope DeviceResult.Failure("No reply to the init request")
            operation(SonyMdrSession(link, initReply.payload))
        } catch (e: ClosedReceiveChannelException) {
            DeviceResult.Failure("The headphones closed the connection")
        } finally {
            socket.close() // Unblocks the reader.
            reader.cancel()
        }
    }

    private suspend fun requestInit(link: SonyMdrLink): SonyMdrMessage? {
        repeat(INIT_ATTEMPTS) { attempt ->
            link.send(INIT_REQUEST)
            val reply = withTimeoutOrNull(INIT_REPLY_TIMEOUT_MS) {
                link.awaitCommand {
                    it.type == SonyMdrMessage.TYPE_COMMAND_1 && it.payload.firstOrNull() == INIT_REPLY
                }.also { link.awaitAck() } // The ACK may arrive after the reply.
            }
            if (reply != null) return reply
            Log.w(LOG_TAG, "No init reply (attempt ${attempt + 1} of $INIT_ATTEMPTS)")
        }
        return null
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
}
