// SPDX-License-Identifier: AGPL-3.0-only

package app.headphoneactions.device.sony

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SonyWh1000xm6MockTest {

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
            sonyWh1000xm6Mock.actions.map { it.id },
        )
    }

    @Test
    fun `action IDs are scoped to the model`() {
        sonyWh1000xm6Mock.actions.forEach {
            assertTrue(it.id, it.id.startsWith("${sonyWh1000xm6Mock.id}."))
        }
    }
}
