// SPDX-License-Identifier: AGPL-3.0-only

package app.headphoneactions.device

import app.headphoneactions.device.sony.sonyWh1000xm6Mock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HeadphoneModelTest {

    @Test
    fun `registered model ID resolves to its model`() {
        assertEquals(sonyWh1000xm6Mock, findModel("sony.wh1000xm6"))
        assertNull(findModel("sony.unknown"))
    }

    @Test
    fun `only actions of the model itself are found`() {
        assertEquals(
            "Noise Cancelling",
            sonyWh1000xm6Mock.findAction("sony.wh1000xm6.noise_control.anc")?.label,
        )
        assertNull(sonyWh1000xm6Mock.findAction("other.model.noise_control.anc"))
    }
}
