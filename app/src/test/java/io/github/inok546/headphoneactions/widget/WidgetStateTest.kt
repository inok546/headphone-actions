// SPDX-License-Identifier: AGPL-3.0-only

package io.github.inok546.headphoneactions.widget

import androidx.datastore.preferences.core.mutablePreferencesOf
import io.github.inok546.headphoneactions.device.sony.SonyWh1000xm6
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetStateTest {

    private val xm6 = SonyWh1000xm6
    private val anc = "sony.wh1000xm6.noise_control.anc"
    private val ambient = "sony.wh1000xm6.noise_control.ambient"
    private val stcOff = "sony.wh1000xm6.speak_to_chat.off"
    private val dseeOn = "sony.wh1000xm6.dsee.on"

    @Test
    fun `new widgets for the WH-1000XM6 get noise cancelling, ambient sound and speak-to-chat off`() {
        assertEquals(WidgetConfig(xm6.id, listOf(anc, ambient, stcOff)), defaultWidgetConfig(xm6))
    }

    @Test
    fun `the chosen order is stored and shown`() {
        val config = WidgetConfig(xm6.id, listOf(dseeOn, anc, stcOff))
        val state = mutablePreferencesOf().also { config.writeTo(it) }

        assertEquals(config, WidgetConfig.readFrom(state))
        assertEquals(listOf(dseeOn, anc, stcOff), buttonIds(resolveWidgetContent(xm6, config)))
    }

    @Test
    fun `unsupported action IDs are left out`() {
        val config = WidgetConfig(xm6.id, listOf("sony.wh1000xm6.unknown", anc))

        assertEquals(listOf(anc), buttonIds(resolveWidgetContent(xm6, config)))
        assertEquals(
            WidgetContent.ReconfigurationRequired,
            resolveWidgetContent(xm6, WidgetConfig(xm6.id, listOf("sony.wh1000xm6.unknown"))),
        )
    }

    @Test
    fun `buttons chosen for another model require reconfiguration`() {
        val config = WidgetConfig("other.model", listOf("other.model.noise_control.anc"))

        assertEquals(WidgetContent.ReconfigurationRequired, resolveWidgetContent(xm6, config))
    }

    @Test
    fun `without a registered device the widget shows no buttons`() {
        assertEquals(WidgetContent.NoDevice, resolveWidgetContent(null, defaultWidgetConfig(xm6)))
        assertEquals(WidgetContent.NoDevice, resolveWidgetContent(null, null))
    }

    @Test
    fun `a widget first seeing a registered model stores that model's defaults`() {
        val state = mutablePreferencesOf()

        ensureConfig(state, null)
        assertNull(WidgetConfig.readFrom(state))

        ensureConfig(state, xm6)
        assertEquals(defaultWidgetConfig(xm6), WidgetConfig.readFrom(state))
    }

    @Test
    fun `removing the registration keeps the buttons for when the same model returns`() {
        val config = WidgetConfig(xm6.id, listOf(stcOff))
        val state = mutablePreferencesOf().also { config.writeTo(it) }

        ensureConfig(state, null)
        assertEquals(WidgetContent.NoDevice, resolveWidgetContent(null, WidgetConfig.readFrom(state)))

        ensureConfig(state, xm6)
        assertEquals(listOf(stcOff), buttonIds(resolveWidgetContent(xm6, WidgetConfig.readFrom(state))))
    }

    @Test
    fun `buttons of another model are not replaced when registering`() {
        val other = WidgetConfig("other.model", listOf("other.model.noise_control.anc"))
        val state = mutablePreferencesOf().also { other.writeTo(it) }

        ensureConfig(state, xm6)

        assertEquals(other, WidgetConfig.readFrom(state))
        assertEquals(WidgetContent.ReconfigurationRequired, resolveWidgetContent(xm6, WidgetConfig.readFrom(state)))
    }

    @Test
    fun `each widget keeps its own buttons`() {
        // Glance gives every widget ID its own state store; configuring one leaves the other alone.
        val widgetA = mutablePreferencesOf()
        val widgetB = mutablePreferencesOf()
        ensureConfig(widgetA, xm6)
        ensureConfig(widgetB, xm6)

        WidgetConfig(xm6.id, listOf(anc, ambient)).writeTo(widgetA)

        assertEquals(listOf(anc, ambient), WidgetConfig.readFrom(widgetA)?.actionIds)
        assertEquals(defaultWidgetConfig(xm6), WidgetConfig.readFrom(widgetB))
    }

    @Test
    fun `the configuration screen lists chosen actions first and saves them in order`() {
        val rows = configRows(xm6, WidgetConfig(xm6.id, listOf(stcOff, anc)))

        assertEquals(listOf(stcOff, anc), rows.filter { it.selected }.map { it.action.id })
        assertEquals(listOf(stcOff, anc), rows.take(2).map { it.action.id })
        assertEquals(xm6.actions.size, rows.size)

        val reordered = rows.moved(index = 1, offset = -1)
        assertEquals(WidgetConfig(xm6.id, listOf(anc, stcOff)), reordered.toConfig(xm6))
        assertEquals(rows, rows.moved(index = 0, offset = -1))
    }

    @Test
    fun `the configuration screen starts from the defaults for another model's buttons`() {
        val rows = configRows(xm6, WidgetConfig("other.model", listOf("other.model.noise_control.anc")))

        assertEquals(defaultWidgetConfig(xm6), rows.toConfig(xm6))
    }

    private fun buttonIds(content: WidgetContent): List<String>? =
        (content as? WidgetContent.Buttons)?.actions?.map { it.id }
}
