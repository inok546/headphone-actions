// SPDX-License-Identifier: AGPL-3.0-only

package io.github.inok546.headphoneactions.device.sony.mdr

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream

class SonyMdrLinkTest {

    private val output = ByteArrayOutputStream()
    private val incoming = Channel<SonyMdrMessage>(Channel.UNLIMITED)
    private val link = SonyMdrLink(output, incoming)

    @Test
    fun `the ACK sets the sequence number of the next command`() = runBlocking {
        link.send(bytes(0x66, 0x15))
        incoming.send(ack(sequence = 1))
        link.awaitAck()
        link.send(bytes(0x68))

        assertEquals(listOf("type=0c seq=0 payload=[66:15]", "type=0c seq=1 payload=[68]"), sent())
    }

    @Test
    fun `commands from the headphones are acknowledged with the flipped sequence number`() = runBlocking {
        incoming.send(command(sequence = 0, 0x67, 0x15))

        val reply = link.awaitCommand { it.payload[0] == 0x67.toByte() }

        assertEquals("67:15", reply.payload.toHex())
        assertEquals(listOf("type=01 seq=1 payload=[]"), sent())
    }

    @Test
    fun `a reply that arrives before the ACK is kept`() = runBlocking {
        link.send(bytes(0x66, 0x15))
        incoming.send(command(sequence = 1, 0x67, 0x15))
        incoming.send(ack(sequence = 1))

        link.awaitAck()
        val reply = link.awaitCommand { it.payload[0] == 0x67.toByte() }

        assertEquals("67:15", reply.payload.toHex())
    }

    @Test
    fun `an ACK arriving after the reply is awaited before the next command`() = runBlocking {
        link.send(bytes(0x00, 0x00))
        incoming.send(command(sequence = 0, 0x01, 0x00))
        incoming.send(ack(sequence = 1))

        link.awaitCommand { it.payload[0] == 0x01.toByte() }
        link.awaitAck()
        link.send(bytes(0x66, 0x19))

        assertEquals(
            listOf("type=0c seq=0 payload=[00:00]", "type=01 seq=1 payload=[]", "type=0c seq=1 payload=[66:19]"),
            sent(),
        )
    }

    @Test
    fun `discarded repeats are not returned for a later request`() = runBlocking {
        incoming.send(command(sequence = 0, 0x67, 0x19, 0x00))
        incoming.send(command(sequence = 1, 0x69, 0x19))
        link.awaitCommand { it.payload[0] == 0x69.toByte() }

        link.discardReceived { it.payload[0] == 0x67.toByte() }
        incoming.send(command(sequence = 0, 0x67, 0x19, 0x01))

        assertEquals("67:19:01", link.awaitCommand { it.payload[0] == 0x67.toByte() }.payload.toHex())
    }

    private fun sent(): List<String> =
        SonyMdrFrameReader().feed(output.toByteArray()).map { SonyMdrMessage.decode(it).toString() }

    private fun ack(sequence: Int) = SonyMdrMessage(SonyMdrMessage.TYPE_ACK, sequence.toByte(), ByteArray(0))

    private fun command(sequence: Int, vararg payload: Int) =
        SonyMdrMessage(SonyMdrMessage.TYPE_COMMAND_1, sequence.toByte(), bytes(*payload))

    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }
}
