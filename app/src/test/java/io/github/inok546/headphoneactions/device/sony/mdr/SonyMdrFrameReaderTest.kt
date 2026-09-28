// SPDX-License-Identifier: AGPL-3.0-only

package io.github.inok546.headphoneactions.device.sony.mdr

import org.junit.Assert.assertEquals
import org.junit.Test

class SonyMdrFrameReaderTest {

    private val ack = SonyMdrMessage(SonyMdrMessage.TYPE_ACK, 1, ByteArray(0)).encode()
    private val reply = SonyMdrMessage(SonyMdrMessage.TYPE_COMMAND_1, 0, byteArrayOf(0x01, 0x00, 0x40, 0x10)).encode()

    @Test
    fun `splits several frames received at once`() {
        val frames = SonyMdrFrameReader().feed(ack + reply)

        assertEquals(listOf(ack.toHex(), reply.toHex()), frames.map { it.toHex() })
    }

    @Test
    fun `joins a frame split across reads`() {
        val reader = SonyMdrFrameReader()

        assertEquals(0, reader.feed(reply.copyOfRange(0, 5)).size)
        assertEquals(listOf(reply.toHex()), reader.feed(reply.copyOfRange(5, reply.size)).map { it.toHex() })
    }

    @Test
    fun `drops bytes outside frames`() {
        val frames = SonyMdrFrameReader().feed(byteArrayOf(0x00, 0x12) + ack)

        assertEquals(listOf(ack.toHex()), frames.map { it.toHex() })
    }

    @Test
    fun `uses only the given number of bytes`() {
        val buffer = ack + byteArrayOf(0x3e, 0x0c)

        assertEquals(listOf(ack.toHex()), SonyMdrFrameReader().feed(buffer, ack.size).map { it.toHex() })
    }
}
