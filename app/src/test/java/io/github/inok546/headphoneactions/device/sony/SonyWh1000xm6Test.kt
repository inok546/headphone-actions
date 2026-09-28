// SPDX-License-Identifier: AGPL-3.0-only

package io.github.inok546.headphoneactions.device.sony

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SonyWh1000xm6Test {

    @Test
    fun `action IDs are stable`() {
        // Samsung Routines persists these as shortcut IDs; renaming one breaks existing routines.
        assertEquals(
            listOf(
                "sony.wh1000xm6.noise_control.anc",
                "sony.wh1000xm6.noise_control.ambient",
                "sony.wh1000xm6.noise_control.off",
                "sony.wh1000xm6.speak_to_chat.on",
                "sony.wh1000xm6.speak_to_chat.off",
                "sony.wh1000xm6.dsee.on",
                "sony.wh1000xm6.dsee.off",
            ),
            sonyWh1000xm6.actions.map { it.id },
        )
    }

    @Test
    fun `action IDs are scoped to the model`() {
        sonyWh1000xm6.actions.forEach {
            assertTrue(it.id, it.id.startsWith("${sonyWh1000xm6.id}."))
        }
    }

    @Test
    fun `matches the Bluetooth Classic device name`() {
        assertTrue(sonyWh1000xm6.matchesDeviceName("WH-1000XM6"))
        assertTrue(sonyWh1000xm6.matchesDeviceName("Sony WH-1000XM6"))
    }

    @Test
    fun `does not match the LE Audio entry or other models`() {
        assertFalse(sonyWh1000xm6.matchesDeviceName("LE_WH-1000XM6"))
        assertFalse(sonyWh1000xm6.matchesDeviceName("WH-1000XM5"))
        assertFalse(sonyWh1000xm6.matchesDeviceName("WF-1000XM6"))
    }
}
