// SPDX-License-Identifier: AGPL-3.0-only

package io.github.inok546.headphoneactions.widget

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import io.github.inok546.headphoneactions.AppPreferences
import io.github.inok546.headphoneactions.device.findModel

/**
 * Home screen widget with buttons for actions of the registered headphones. Each instance keeps
 * its buttons ([WidgetConfig]) in its own Glance state; [HeadphoneActionsWidgets] updates the
 * instances after explicit changes.
 */
class HeadphoneActionsWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(responsiveSizes)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        HeadphoneActionsWidgets.sync(context, id)
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        provideContent {
            // Glance reassigns the state on every update(), which recomposes this block, so the
            // registration read below is current as well. The state may predate sync() on the
            // very first composition; resolveWidgetContent() falls back to the same defaults.
            val config = WidgetConfig.readFrom(currentState<Preferences>())
            val device = AppPreferences(context).registeredDevice
            val model = device?.let { findModel(it.modelId) }
            GlanceTheme { WidgetLayout(resolveWidgetContent(model, config), device?.name, appWidgetId) }
        }
    }
}
