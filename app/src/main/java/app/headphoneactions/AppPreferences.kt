// SPDX-License-Identifier: AGPL-3.0-only

package app.headphoneactions

import android.content.Context
import androidx.core.content.edit

data class LastRoutineAction(val actionId: String, val invokedAtMillis: Long)

/** Locally persisted state: the registered device and the last routine invocation. */
class AppPreferences(context: Context) {

    private val prefs = context.getSharedPreferences("headphone_actions", Context.MODE_PRIVATE)

    /** ID of the explicitly registered model, or null if none is registered. */
    var registeredModelId: String?
        get() = prefs.getString(KEY_REGISTERED_MODEL_ID, null)
        set(value) = prefs.edit { putString(KEY_REGISTERED_MODEL_ID, value) }

    val lastRoutineAction: LastRoutineAction?
        get() {
            val actionId = prefs.getString(KEY_LAST_ACTION_ID, null) ?: return null
            return LastRoutineAction(actionId, prefs.getLong(KEY_LAST_ACTION_AT, 0))
        }

    fun recordRoutineAction(actionId: String, invokedAtMillis: Long) {
        prefs.edit {
            putString(KEY_LAST_ACTION_ID, actionId)
            putLong(KEY_LAST_ACTION_AT, invokedAtMillis)
        }
    }

    private companion object {
        const val KEY_REGISTERED_MODEL_ID = "registered_model_id"
        const val KEY_LAST_ACTION_ID = "last_routine_action_id"
        const val KEY_LAST_ACTION_AT = "last_routine_action_at"
    }
}
