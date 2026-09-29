// SPDX-License-Identifier: AGPL-3.0-only

package io.github.inok546.headphoneactions.widget

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.inok546.headphoneactions.device.HeadphoneModel
import io.github.inok546.headphoneactions.device.SupportedAction

/**
 * The buttons of one widget instance: the model they were chosen for and the chosen action IDs
 * in the user's order. Stored in the instance's own Glance state (one DataStore per widget ID,
 * deleted by Glance together with the widget).
 */
data class WidgetConfig(val modelId: String, val actionIds: List<String>) {

    fun writeTo(state: MutablePreferences) {
        state[MODEL_ID] = modelId
        state[ACTION_IDS] = actionIds.joinToString(SEPARATOR)
    }

    companion object {
        private val MODEL_ID = stringPreferencesKey("config_model_id")
        private val ACTION_IDS = stringPreferencesKey("config_action_ids")

        // Action IDs are dotted identifiers and never contain a comma.
        private const val SEPARATOR = ","

        fun readFrom(state: Preferences): WidgetConfig? {
            val modelId = state[MODEL_ID] ?: return null
            val actionIds = state[ACTION_IDS]?.split(SEPARATOR)?.filter { it.isNotEmpty() }.orEmpty()
            return WidgetConfig(modelId, actionIds)
        }
    }
}

/** The config a new widget gets for [model]: its quick actions, as far as it supports them. */
fun defaultWidgetConfig(model: HeadphoneModel): WidgetConfig =
    WidgetConfig(model.id, model.quickActionIds.filter { model.findAction(it) != null })

/** What a widget instance shows. */
sealed interface WidgetContent {
    data object NoDevice : WidgetContent

    /** The widget was set up for other headphones, or none of its buttons exist any more. */
    data object ReconfigurationRequired : WidgetContent

    data class Buttons(val actions: List<SupportedAction>) : WidgetContent
}

/**
 * Decides what a widget shows for the registered [model] (null when none is registered) and
 * its stored [config]. A config made for another model is never mapped onto the new one.
 */
fun resolveWidgetContent(model: HeadphoneModel?, config: WidgetConfig?): WidgetContent {
    if (model == null) return WidgetContent.NoDevice
    val chosen = config ?: defaultWidgetConfig(model)
    if (chosen.modelId != model.id) return WidgetContent.ReconfigurationRequired
    val actions = chosen.actionIds.mapNotNull { model.findAction(it) }
    return if (actions.isEmpty()) WidgetContent.ReconfigurationRequired else WidgetContent.Buttons(actions)
}

/**
 * Stores the defaults of the registered [model] for a widget that has no buttons yet, so that
 * the widget remembers which model they were chosen for. Buttons chosen for another model are
 * left alone: the widget then asks to be configured again.
 */
fun ensureConfig(state: MutablePreferences, model: HeadphoneModel?) {
    if (model != null && WidgetConfig.readFrom(state) == null) defaultWidgetConfig(model).writeTo(state)
}

/** One row of the configuration screen. */
data class ConfigRow(val action: SupportedAction, val selected: Boolean)

/**
 * The configuration screen's rows for [model]: the chosen actions first, in their order, then
 * the others in the model's order. A [config] for another model is ignored in favour of the defaults.
 */
fun configRows(model: HeadphoneModel, config: WidgetConfig?): List<ConfigRow> {
    val chosen = config?.takeIf { it.modelId == model.id } ?: defaultWidgetConfig(model)
    val selected = chosen.actionIds.mapNotNull { model.findAction(it) }.distinct()
    return selected.map { ConfigRow(it, selected = true) } +
        model.actions.filter { it !in selected }.map { ConfigRow(it, selected = false) }
}

/** Moves the row at [index] by [offset] positions, if that stays within the list. */
fun List<ConfigRow>.moved(index: Int, offset: Int): List<ConfigRow> {
    val target = index + offset
    if (index !in indices || target !in indices) return this
    return toMutableList().apply { add(target, removeAt(index)) }
}

/** The config the rows describe, in their order. */
fun List<ConfigRow>.toConfig(model: HeadphoneModel): WidgetConfig =
    WidgetConfig(model.id, filter { it.selected }.map { it.action.id })
