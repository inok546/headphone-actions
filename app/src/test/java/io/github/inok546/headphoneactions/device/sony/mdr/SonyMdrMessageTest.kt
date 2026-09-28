// SPDX-License-Identifier: AGPL-3.0-only

package io.github.inok546.headphoneactions.device.sony.mdr

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SonyMdrMessageTest {

    @Test
    fun `init request encodes to the known frame`() {
        val frame = SonyMdrMessage(SonyMdrMessage.TYPE_COMMAND_1, 0, bytes(0x00, 0x00)).encode()
        assertEquals("3e:0c:00:00:00:00:02:00:00:0e:3c", frame.toHex())
    }

    @Test
    fun `ACK encodes without payload`() {
        val frame = SonyMdrMessage(SonyMdrMessage.TYPE_ACK, 1, ByteArray(0)).encode()
        assertEquals("3e:01:01:00:00:00:00:02:3c", frame.toHex())
    }

    @Test
    fun `frame markers inside the message are escaped and restored`() {
        val payload = bytes(0x3e, 0x3c, 0x3d, 0x01)
        val frame = SonyMdrMessage(SonyMdrMessage.TYPE_COMMAND_1, 1, payload).encode()

        assertEquals("3e:0c:01:00:00:00:04:3d:2e:3d:2c:3d:2d:01:c9:3c", frame.toHex())
        val decoded = SonyMdrMessage.decode(frame)!!
        assertEquals(SonyMdrMessage.TYPE_COMMAND_1, decoded.type)
        assertEquals(1.toByte(), decoded.sequence)
        assertArrayEquals(payload, decoded.payload)
    }

    @Test
    fun `decodes an init reply`() {
        val payload = bytes(0x01, 0x00, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00)
        val frame = SonyMdrMessage(SonyMdrMessage.TYPE_COMMAND_1, 0, payload).encode()

        assertArrayEquals(payload, SonyMdrMessage.decode(frame)!!.payload)
    }

    @Test
    fun `rejects a frame with a wrong checksum`() {
        val frame = SonyMdrMessage(SonyMdrMessage.TYPE_COMMAND_1, 0, bytes(0x00, 0x00)).encode()
        frame[frame.size - 2] = 0x0f

        assertNull(SonyMdrMessage.decode(frame))
    }

    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }
}
