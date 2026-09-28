// SPDX-License-Identifier: AGPL-3.0-only
//
// Sony MDR message framing, ported to Kotlin from Gadgetbridge
// (https://codeberg.org/Freeyourgadget/Gadgetbridge), files
// service/devices/sony/headphones/protocol/Message.java and MessageType.java:
//   Copyright (C) 2021-2024 José Rebelo
// Gadgetbridge is licensed under the GNU AGPL version 3 or later; used here under version 3.

package io.github.inok546.headphoneactions.device.sony.mdr

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * One Sony MDR protocol message.
 *
 * On the wire: HEADER, type, sequence number, 4-byte big-endian payload length, payload,
 * a 1-byte checksum (sum of the bytes between header and checksum), TRAILER. Everything
 * between HEADER and TRAILER is escaped, so neither marker occurs inside a frame.
 */
class SonyMdrMessage(val type: Byte, val sequence: Byte, val payload: ByteArray) {

    fun encode(): ByteArray {
        val content = ByteBuffer.allocate(6 + payload.size)
            .put(type)
            .put(sequence)
            .putInt(payload.size)
            .put(payload)
            .array()
        val out = ByteArrayOutputStream(content.size + 4)
        out.write(HEADER.toInt())
        writeEscaped(out, content)
        writeEscaped(out, byteArrayOf(checksum(content)))
        out.write(TRAILER.toInt())
        return out.toByteArray()
    }

    override fun toString(): String = "type=%02x seq=%d payload=[%s]".format(type, sequence, payload.toHex())

    companion object {
        const val TYPE_ACK: Byte = 0x01
        const val TYPE_COMMAND_1: Byte = 0x0c
        const val TYPE_COMMAND_2: Byte = 0x0e

        const val HEADER: Byte = 0x3e
        const val TRAILER: Byte = 0x3c
        private const val ESCAPE: Byte = 0x3d
        private const val ESCAPE_MASK = 0b11101111

        /** Decodes one complete HEADER…TRAILER frame; null if it is malformed. */
        fun decode(frame: ByteArray): SonyMdrMessage? {
            if (frame.size < 2 || frame.first() != HEADER || frame.last() != TRAILER) return null
            val body = unescape(frame.copyOfRange(1, frame.size - 1)) ?: return null
            if (body.size < 7) return null
            val content = body.copyOfRange(0, body.size - 1)
            if (checksum(content) != body.last()) return null
            val payloadLength = ByteBuffer.wrap(content, 2, 4).int
            if (payloadLength != content.size - 6) return null
            return SonyMdrMessage(content[0], content[1], content.copyOfRange(6, content.size))
        }

        private fun checksum(bytes: ByteArray): Byte = bytes.sumOf { it.toInt() and 0xff }.toByte()

        private fun writeEscaped(out: ByteArrayOutputStream, bytes: ByteArray) {
            for (b in bytes) {
                if (b == HEADER || b == TRAILER || b == ESCAPE) {
                    out.write(ESCAPE.toInt())
                    out.write(b.toInt() and ESCAPE_MASK)
                } else {
                    out.write(b.toInt())
                }
            }
        }

        private fun unescape(bytes: ByteArray): ByteArray? {
            val out = ByteArrayOutputStream(bytes.size)
            var i = 0
            while (i < bytes.size) {
                if (bytes[i] == ESCAPE) {
                    if (++i == bytes.size) return null
                    out.write(bytes[i].toInt() or ESCAPE_MASK.inv())
                } else {
                    out.write(bytes[i].toInt())
                }
                i++
            }
            return out.toByteArray()
        }
    }
}

/** Splits the incoming RFCOMM byte stream into HEADER…TRAILER frames. */
class SonyMdrFrameReader {

    private var pending = ByteArray(0)

    /** Adds [length] received bytes and returns the frames completed by them. */
    fun feed(data: ByteArray, length: Int = data.size): List<ByteArray> {
        pending += data.copyOf(length)
        val frames = mutableListOf<ByteArray>()
        var start = pending.indexOf(SonyMdrMessage.HEADER)
        while (start >= 0) {
            val end = (start + 1 until pending.size).firstOrNull { pending[it] == SonyMdrMessage.TRAILER } ?: break
            frames += pending.copyOfRange(start, end + 1)
            start = (end + 1 until pending.size).firstOrNull { pending[it] == SonyMdrMessage.HEADER } ?: -1
        }
        // Keep an unfinished frame; bytes outside any frame are noise and dropped.
        pending = if (start >= 0) pending.copyOfRange(start, pending.size) else ByteArray(0)
        return frames
    }
}

fun ByteArray.toHex(): String = joinToString(":") { "%02x".format(it) }
