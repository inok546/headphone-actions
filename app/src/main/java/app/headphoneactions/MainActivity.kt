// SPDX-License-Identifier: AGPL-3.0-only

package app.headphoneactions

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.headphoneactions.device.HeadphoneModel
import app.headphoneactions.device.findModel
import app.headphoneactions.routines.ShortcutPublisher

class MainActivity : ComponentActivity() {

    private lateinit var preferences: AppPreferences
    private var uiState by mutableStateOf(MainUiState())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        preferences = AppPreferences(this)
        setContent {
            AppTheme {
                MainScreen(
                    state = uiState,
                    onRegister = ::register,
                    onRemoveRegistration = ::removeRegistration,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Picks up invocations RoutineActionActivity recorded while we were in the background.
        refresh()
    }

    private fun register(model: HeadphoneModel) {
        preferences.registeredModelId = model.id
        Log.i(LOG_TAG, "Registered device: ${model.id}")
        ShortcutPublisher.publish(this, model)
        refresh()
    }

    private fun removeRegistration() {
        preferences.registeredModelId = null
        Log.i(LOG_TAG, "Registration removed")
        ShortcutPublisher.removeAll(this)
        refresh()
    }

    private fun refresh() {
        uiState = MainUiState(
            registeredModel = preferences.registeredModelId?.let(::findModel),
            publishedActions = ShortcutPublisher.publishedActions(this),
            lastRoutineAction = preferences.lastRoutineAction,
        )
    }
}
