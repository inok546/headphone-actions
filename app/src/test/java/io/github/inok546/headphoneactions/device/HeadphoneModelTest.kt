// SPDX-License-Identifier: AGPL-3.0-only

package io.github.inok546.headphoneactions.device

import io.github.inok546.headphoneactions.device.sony.sonyWh1000xm6
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HeadphoneModelTest {

    @Test
    fun `registered model ID resolves to its model`() {
        assertEquals(sonyWh1000xm6, findModel("sony.wh1000xm6"))
        assertNull(findModel("sony.unknown"))
    }

    @Test
    fun `paired device name resolves to a supported model`() {
        assertEquals(sonyWh1000xm6, findModelForDeviceName("WH-1000XM6"))
        assertNull(findModelForDeviceName("Galaxy Buds3 Pro"))
        assertNull(findModelForDeviceName(null))
    }

    @Test
    fun `only actions of the model itself are found`() {
        assertEquals(
            "Noise Cancelling",
            sonyWh1000xm6.findAction("sony.wh1000xm6.noise_control.anc")?.label,
        )
        assertNull(sonyWh1000xm6.findAction("other.model.noise_control.anc"))
    }
}
