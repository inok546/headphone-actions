// SPDX-License-Identifier: AGPL-3.0-only

package io.github.inok546.headphoneactions.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class HeadphoneActionsWidgetReceiver : GlanceAppWidgetReceiver() {
    // Glance deletes an instance's state when the instance is removed from the home screen.
    override val glanceAppWidget: GlanceAppWidget = HeadphoneActionsWidget()
}
