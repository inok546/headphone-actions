// SPDX-License-Identifier: AGPL-3.0-only

package io.github.inok546.headphoneactions.widget

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import io.github.inok546.headphoneactions.AppPreferences
import io.github.inok546.headphoneactions.LOG_TAG
import io.github.inok546.headphoneactions.device.findModel

/**
 * Keeps the widgets in step with explicit changes: registering or removing headphones and
 * configuring a widget. Bluetooth connection changes are deliberately not a reason to update.
 */
object HeadphoneActionsWidgets {

    suspend fun refreshAll(context: Context) {
        val widget = HeadphoneActionsWidget()
        val ids = GlanceAppWidgetManager(context).getGlanceIds(HeadphoneActionsWidget::class.java)
        for (id in ids) {
            sync(context, id)
            widget.update(context, id)
        }
        Log.i(LOG_TAG, "Widgets refreshed: ${ids.size}")
    }

    /** Gives the widget [id] the defaults of the registered model if it has no buttons yet. */
    internal suspend fun sync(context: Context, id: GlanceId) {
        val model = AppPreferences(context).registeredDevice?.let { findModel(it.modelId) }
        updateAppWidgetState(context, id) { state -> ensureConfig(state, model) }
        Log.d(LOG_TAG, "Widget $id synced with registered model ${model?.id}")
    }
}
