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

    /**
     * Sends [payload] and waits for the ACK. False if none arrives in time. [messageType] is
     * COMMAND_1 for most settings and COMMAND_2 for the second table (e.g. multipoint).
     */
    suspend fun command(payload: ByteArray, messageType: Byte = SonyMdrMessage.TYPE_COMMAND_1): Boolean =
        withTimeoutOrNull(REPLY_TIMEOUT_MS) {
            link.send(payload, messageType)
            link.awaitAck()
        } != null

    /**
     * Sends a `[code, type, …]` request and returns the `[replyCode, type, …]` reply; null if
     * none arrives in time.
     */
    suspend fun query(
        request: ByteArray,
        replyCode: Byte,
        messageType: Byte = SonyMdrMessage.TYPE_COMMAND_1,
    ): SonyMdrMessage? {
        val type = request[1]
        discard(replyCode, type)
        return withTimeoutOrNull(REPLY_TIMEOUT_MS) {
            link.send(request, messageType)
            link.awaitAck()
            link.awaitCommand { it.matches(replyCode, type) }
        }
    }

    /** Waits for a `[code, type, …]` message from the headphones; null if none arrives in time. */
    suspend fun await(code: Byte, type: Byte): SonyMdrMessage? = awaitAny(setOf(code), type)

    /** Waits for a `[code, type, …]` message with any of [codes]; null if none arrives in time. */
    suspend fun awaitAny(codes: Set<Byte>, type: Byte): SonyMdrMessage? =
        withTimeoutOrNull(REPLY_TIMEOUT_MS) { link.awaitCommand { it.matches(codes, type) } }

    /**
     * Forgets already received `[code, type, …]` messages. The WH-1000XM6 may repeat a reply,
     * and a stale repeat must not be taken for the answer to a later request.
     */
    fun discard(code: Byte, type: Byte) = link.discardReceived { it.matches(setOf(code), type) }

    // The type byte tells apart settings that share an opcode, e.g. the SYSTEM parameters.
    private fun SonyMdrMessage.matches(code: Byte, type: Byte) = matches(setOf(code), type)

    private fun SonyMdrMessage.matches(codes: Set<Byte>, type: Byte) =
        payload.size >= 2 && payload[0] in codes && payload[1] == type

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
    fun send(payload: ByteArray, messageType: Byte = SonyMdrMessage.TYPE_COMMAND_1) {
        write(SonyMdrMessage(messageType, sequence, payload))
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
