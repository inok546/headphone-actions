// SPDX-License-Identifier: AGPL-3.0-only

package io.github.inok546.headphoneactions.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import io.github.inok546.headphoneactions.AppPreferences
import io.github.inok546.headphoneactions.AppTheme
import io.github.inok546.headphoneactions.LOG_TAG
import io.github.inok546.headphoneactions.MainActivity
import io.github.inok546.headphoneactions.device.HeadphoneModel
import io.github.inok546.headphoneactions.device.findModel
import kotlinx.coroutines.launch

/**
 * Chooses the buttons of one widget instance. Opened by the launcher (long press → Settings, and
 * when placing the widget on Android 11 and lower) and by the widget's own "Configure" button.
 */
class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var rows by mutableStateOf<List<ConfigRow>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        // Leaving without saving keeps the widget as it was (and cancels placing it on Android 11 and lower).
        setResult(RESULT_CANCELED, resultIntent())
        if (!isOwnWidget(appWidgetId)) {
            Log.w(LOG_TAG, "Widget configuration ignored: $appWidgetId is not a Headphone Actions widget")
            finish()
            return
        }
        val glanceId = GlanceAppWidgetManager(this).getGlanceIdBy(appWidgetId)
        val device = AppPreferences(this).registeredDevice
        val model = device?.let { findModel(it.modelId) }

        if (model != null) {
            lifecycleScope.launch {
                val stored = WidgetConfig.readFrom(getAppWidgetState(this@WidgetConfigActivity, PreferencesGlanceStateDefinition, glanceId))
                rows = configRows(model, stored)
            }
        }
        setContent {
            AppTheme {
                WidgetConfigScreen(
                    deviceName = device?.name,
                    rows = if (model != null) rows else null,
                    onRowsChange = { rows = it },
                    onSave = { model?.let { save(glanceId, it) } },
                    onOpenApp = ::openApp,
                )
            }
        }
    }

    private fun save(glanceId: GlanceId, model: HeadphoneModel) {
        val config = rows?.toConfig(model)?.takeIf { it.actionIds.isNotEmpty() } ?: return
        lifecycleScope.launch {
            updateAppWidgetState(applicationContext, glanceId) { state -> config.writeTo(state) }
            HeadphoneActionsWidget().update(applicationContext, glanceId)
            Log.i(LOG_TAG, "Widget $appWidgetId configured: $config")
            setResult(RESULT_OK, resultIntent())
            finish()
        }
    }

    private fun openApp() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun resultIntent() = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

    /** The activity is exported for launchers, so only IDs of this app's own widgets are accepted. */
    private fun isOwnWidget(id: Int): Boolean {
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return false
        val provider = AppWidgetManager.getInstance(this).getAppWidgetInfo(id)?.provider
        return provider == ComponentName(this, HeadphoneActionsWidgetReceiver::class.java)
    }
}
