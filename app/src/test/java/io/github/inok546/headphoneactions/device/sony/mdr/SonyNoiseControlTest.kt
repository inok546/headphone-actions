// SPDX-License-Identifier: AGPL-3.0-only

package io.github.inok546.headphoneactions.device.sony.mdr

import io.github.inok546.headphoneactions.device.sony.mdr.SonyNoiseControl.Mode
import io.github.inok546.headphoneactions.device.sony.mdr.SonyNoiseControl.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SonyNoiseControlTest {

    private val ambientState = State(
        enabled = true,
        ambientSelected = true,
        focusOnVoice = true,
        ambientLevel = 12,
        autoAmbientSound = true,
        autoAmbientSensitivity = 0x02,
    )

    @Test
    fun `get request asks for the WH-1000XM6 state`() {
        assertEquals("66:19", SonyNoiseControl.getRequest.toHex())
    }

    @Test
    fun `each mode keeps the other settings`() {
        assertEquals("68:19:01:01:00:01:0c:01:02", SonyNoiseControl.setRequest(ambientState.withMode(Mode.NOISE_CANCELLING)).toHex())
        assertEquals("68:19:01:01:01:01:0c:01:02", SonyNoiseControl.setRequest(ambientState.withMode(Mode.AMBIENT_SOUND)).toHex())
        // Off keeps the ambient selection too, so switching on again returns to ambient sound.
        assertEquals("68:19:01:00:01:01:0c:01:02", SonyNoiseControl.setRequest(ambientState.withMode(Mode.OFF)).toHex())
    }

    @Test
    fun `switching off and on again restores the previous mode`() {
        val off = ambientState.withMode(Mode.OFF)

        assertEquals(Mode.OFF, off.mode)
        assertEquals(Mode.AMBIENT_SOUND, off.copy(enabled = true).mode)
    }

    @Test
    fun `parses returned and notified states`() {
        assertEquals(
            State(
                enabled = true,
                ambientSelected = true,
                focusOnVoice = false,
                ambientLevel = 20,
                autoAmbientSound = true,
                autoAmbientSensitivity = 0x01,
            ),
            SonyNoiseControl.parse(bytes(0x67, 0x19, 0x01, 0x01, 0x01, 0x00, 0x14, 0x01, 0x01)),
        )
        assertEquals(
            Mode.NOISE_CANCELLING,
            SonyNoiseControl.parse(bytes(0x69, 0x19, 0x01, 0x01, 0x00, 0x01, 0x0c, 0x01, 0x00))?.mode,
        )
        assertEquals(
            Mode.OFF,
            SonyNoiseControl.parse(bytes(0x67, 0x19, 0x01, 0x00, 0x01, 0x00, 0x14, 0x00, 0x00))?.mode,
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
