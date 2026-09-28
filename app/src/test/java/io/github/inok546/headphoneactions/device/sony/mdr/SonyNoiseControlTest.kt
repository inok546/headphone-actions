// SPDX-License-Identifier: AGPL-3.0-only

package io.github.inok546.headphoneactions.device.sony.mdr

import io.github.inok546.headphoneactions.device.sony.mdr.SonyNoiseControl.Mode
import io.github.inok546.headphoneactions.device.sony.mdr.SonyNoiseControl.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SonyNoiseControlTest {

    @Test
    fun `get request asks for the WH-1000XM6 state`() {
        assertEquals("66:19", SonyNoiseControl.getRequest.toHex())
    }

    @Test
    fun `set request keeps the other settings`() {
        assertEquals(
            "68:19:01:01:00:01:0c:00:00",
            SonyNoiseControl.setRequest(State(Mode.NOISE_CANCELLING, true, 12, false)).toHex(),
        )
        assertEquals(
            "68:19:01:01:01:00:14:01:00",
            SonyNoiseControl.setRequest(State(Mode.AMBIENT_SOUND, false, 20, true)).toHex(),
        )
        assertEquals(
            "68:19:01:00:00:00:05:00:00",
            SonyNoiseControl.setRequest(State(Mode.OFF, false, 5, false)).toHex(),
        )
    }

    @Test
    fun `parses returned and notified states`() {
        assertEquals(
            State(Mode.AMBIENT_SOUND, false, 20, false),
            SonyNoiseControl.parse(bytes(0x67, 0x19, 0x01, 0x01, 0x01, 0x00, 0x14, 0x00, 0x00)),
        )
        assertEquals(
            State(Mode.NOISE_CANCELLING, true, 12, true),
            SonyNoiseControl.parse(bytes(0x69, 0x19, 0x01, 0x01, 0x00, 0x01, 0x0c, 0x01, 0x00)),
        )
        // While off, the remaining fields keep the last settings.
        assertEquals(
            State(Mode.OFF, false, 20, false),
            SonyNoiseControl.parse(bytes(0x67, 0x19, 0x01, 0x00, 0x01, 0x00, 0x14, 0x00, 0x00)),
        )
    }

    @Test
    fun `rejects other layouts and out-of-range values`() {
        // The 0x15 layout Gadgetbridge uses for older models.
        assertNull(SonyNoiseControl.parse(bytes(0x67, 0x15, 0x01, 0x01, 0x00, 0x00, 0x0c)))
        assertNull(SonyNoiseControl.parse(bytes(0x67, 0x19, 0x01, 0x01, 0x02, 0x00, 0x0c, 0x00, 0x00)))
        assertNull(SonyNoiseControl.parse(bytes(0x67, 0x19, 0x01, 0x01, 0x00, 0x00, 0x15, 0x00, 0x00)))
    }

    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }
}
