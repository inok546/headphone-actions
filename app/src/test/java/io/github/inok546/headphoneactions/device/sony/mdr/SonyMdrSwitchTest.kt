// SPDX-License-Identifier: AGPL-3.0-only

package io.github.inok546.headphoneactions.device.sony.mdr

import io.github.inok546.headphoneactions.device.sony.mdr.SonyMdrSwitch.Companion.dseeExtreme
import io.github.inok546.headphoneactions.device.sony.mdr.SonyMdrSwitch.Companion.speakToChat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SonyMdrSwitchTest {

    @Test
    fun `speak-to-chat requests use the inverted flag`() {
        assertEquals("f6:0c", speakToChat.getRequest.toHex())
        assertEquals("f8:0c:00:01", speakToChat.setRequest(enabled = true).toHex())
        assertEquals("f8:0c:01:01", speakToChat.setRequest(enabled = false).toHex())
    }

    @Test
    fun `speak-to-chat states are parsed`() {
        assertEquals(true, speakToChat.parse(bytes(0xf7, 0x0c, 0x00, 0x01)))
        assertEquals(false, speakToChat.parse(bytes(0xf9, 0x0c, 0x01, 0x01)))
        assertNull(speakToChat.parse(bytes(0xf7, 0x0d, 0x00, 0x01)))
        assertNull(speakToChat.parse(bytes(0xf7, 0x0c, 0x02, 0x01)))
        assertNull(speakToChat.parse(bytes(0xf7, 0x0c, 0x00)))
    }

    @Test
    fun `dsee requests`() {
        assertEquals("e6:01", dseeExtreme.getRequest.toHex())
        assertEquals("e8:01:01", dseeExtreme.setRequest(enabled = true).toHex())
        assertEquals("e8:01:00", dseeExtreme.setRequest(enabled = false).toHex())
    }

    @Test
    fun `dsee states are parsed`() {
        assertEquals(true, dseeExtreme.parse(bytes(0xe7, 0x01, 0x01)))
        assertEquals(false, dseeExtreme.parse(bytes(0xe9, 0x01, 0x00)))
        assertNull(dseeExtreme.parse(bytes(0xe7, 0x02, 0x01)))
        assertNull(dseeExtreme.parse(bytes(0xe7, 0x01, 0x01, 0x00)))
    }

    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }
}
