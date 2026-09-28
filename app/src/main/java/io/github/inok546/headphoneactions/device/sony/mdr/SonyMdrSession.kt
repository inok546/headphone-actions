// SPDX-License-Identifier: AGPL-3.0-only
//
// The ACK and sequence number rules follow Gadgetbridge
// (https://codeberg.org/Freeyourgadget/Gadgetbridge), file
// service/devices/sony/headphones/SonyHeadphonesProtocol.java:
//   Copyright (C) 2021-2026 José Rebelo
// Gadgetbridge is licensed under the GNU AGPL version 3 or later; used here under version 3.

package io.github.inok546.headphoneactions.device.sony.mdr

import android.util.Log
import io.github.inok546.headphoneactions.LOG_TAG
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.withTimeoutOrNull
import java.io.OutputStream

enum class SonyMdrProtocolVersion { V1, V2, UNKNOWN }

/** An open MDR channel on which the init handshake has completed. */
class SonyMdrSession(private val link: SonyMdrLink, val initReply: ByteArray) {

    // Init reply payload lengths seen by Gadgetbridge: 4 bytes on v1 devices, 8 bytes on v2 devices.
    val protocolVersion: SonyMdrProtocolVersion = when (initReply.size) {
        4 -> SonyMdrProtocolVersion.V1
        8 -> SonyMdrProtocolVersion.V2
        else -> SonyMdrProtocolVersion.UNKNOWN
    }

    /** Sends [payload] and waits for the ACK. False if none arrives in time. */
    suspend fun command(payload: ByteArray): Boolean =
        withTimeoutOrNull(REPLY_TIMEOUT_MS) {
            link.send(payload)
            link.awaitAck()
        } != null

    /** Sends [payload] and returns the reply whose payload starts with [replyCode]; null if none arrives in time. */
    suspend fun query(payload: ByteArray, replyCode: Byte): SonyMdrMessage? {
        discard(replyCode)
        return withTimeoutOrNull(REPLY_TIMEOUT_MS) {
            link.send(payload)
            link.awaitAck()
            awaitCode(replyCode)
        }
    }

    /** Waits for a message from the headphones whose payload starts with [code]; null if none arrives in time. */
    suspend fun await(code: Byte): SonyMdrMessage? = withTimeoutOrNull(REPLY_TIMEOUT_MS) { awaitCode(code) }

    /**
     * Forgets already received messages starting with [code]. The WH-1000XM6 may repeat a
     * reply, and a stale repeat must not be taken for the answer to a later request.
     */
    fun discard(code: Byte) = link.discardReceived { it.payload.firstOrNull() == code }

    private suspend fun awaitCode(code: Byte) = link.awaitCommand { it.payload.firstOrNull() == code }

    private companion object {
        const val REPLY_TIMEOUT_MS = 2000L
    }
}

/**
 * Message exchange with the headphones. Every command they send is acknowledged; every ACK
 * they send carries the sequence number to use for our next message. The headphones silently
 * drop a message with the wrong sequence number, so a new message must not be sent before the
 * previous one has been acknowledged.
 */
class SonyMdrLink(private val output: OutputStream, private val incoming: ReceiveChannel<SonyMdrMessage>) {

    private var sequence: Byte = 0
    private var ackPending = false

    /** Commands already received and acknowledged but not yet asked for. */
    private val received = ArrayDeque<SonyMdrMessage>()

    /** Sends a command. Resending before the ACK (as the init retries do) abandons the previous one. */
    fun send(payload: ByteArray) {
        write(SonyMdrMessage(SonyMdrMessage.TYPE_COMMAND_1, sequence, payload))
        ackPending = true
    }

    /** Waits until the last sent command is acknowledged; returns at once if it already is. */
    suspend fun awaitAck() {
        while (ackPending) receive()
    }

    /** Returns the first command from the headphones matching [predicate], including ones received earlier. */
    suspend fun awaitCommand(predicate: (SonyMdrMessage) -> Boolean): SonyMdrMessage {
        while (true) {
            received.firstOrNull(predicate)?.let {
                received.remove(it)
                return it
            }
            receive()
        }
    }

    fun discardReceived(predicate: (SonyMdrMessage) -> Boolean) {
        received.removeAll(predicate)
    }

    private suspend fun receive() {
        val message = incoming.receive()
        if (message.type == SonyMdrMessage.TYPE_ACK) {
            sequence = message.sequence
            ackPending = false
        } else {
            write(SonyMdrMessage(SonyMdrMessage.TYPE_ACK, (1 - message.sequence).toByte(), ByteArray(0)))
            received.addLast(message)
        }
    }

    private fun write(message: SonyMdrMessage) {
        Log.d(LOG_TAG, "Sony TX $message")
        output.write(message.encode())
        output.flush()
    }
}
