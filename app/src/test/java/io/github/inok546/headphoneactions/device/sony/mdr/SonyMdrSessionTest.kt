// SPDX-License-Identifier: AGPL-3.0-only

package io.github.inok546.headphoneactions.device.sony.mdr

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream

class SonyMdrSessionTest {

    private val incoming = Channel<SonyMdrMessage>(Channel.UNLIMITED)
    private val session = SonyMdrSession(SonyMdrLink(ByteArrayOutputStream(), incoming), ByteArray(8))

    @Test
    fun `a query returns the reply for its own type`() = runBlocking {
        incoming.send(ack(sequence = 1))
        incoming.send(command(0xf7, 0x0d, 0x01)) // Another SYSTEM parameter.
        incoming.send(command(0xf7, 0x0c, 0x00, 0x01))

        val reply = session.query(bytes(0xf6, 0x0c), 0xf7.toByte())

        assertEquals("f7:0c:00:01", reply?.payload?.toHex())
    }

    @Test
    fun `waiting for a notification skips other types`() = runBlocking {
        incoming.send(command(0xe9, 0x02, 0x01))
        incoming.send(command(0xe9, 0x01, 0x01))

        assertEquals("e9:01:01", session.await(0xe9.toByte(), 0x01)?.payload?.toHex())
    }

    private fun ack(sequence: Int) = SonyMdrMessage(SonyMdrMessage.TYPE_ACK, sequence.toByte(), ByteArray(0))

    private fun command(vararg payload: Int) = SonyMdrMessage(SonyMdrMessage.TYPE_COMMAND_1, 0, bytes(*payload))

    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }
}
