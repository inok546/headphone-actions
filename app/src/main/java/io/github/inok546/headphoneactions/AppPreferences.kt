// SPDX-License-Identifier: AGPL-3.0-only

package io.github.inok546.headphoneactions

import android.content.Context
import androidx.core.content.edit
import io.github.inok546.headphoneactions.device.RegisteredDevice

data class LastRoutineAction(val actionId: String, val invokedAtMillis: Long)

/** Locally persisted state: the registered device and the last routine invocation. */
class AppPreferences(context: Context) {

    private val prefs = context.getSharedPreferences("headphone_actions", Context.MODE_PRIVATE)

    /** The explicitly registered headphones, or null if none are registered. */
    var registeredDevice: RegisteredDevice?
        get() {
            val address = prefs.getString(KEY_DEVICE_ADDRESS, null) ?: return null
            val name = prefs.getString(KEY_DEVICE_NAME, null) ?: return null
            val modelId = prefs.getString(KEY_DEVICE_MODEL_ID, null) ?: return null
            return RegisteredDevice(address, name, modelId)
        }
        set(value) = prefs.edit {
            putString(KEY_DEVICE_ADDRESS, value?.address)
            putString(KEY_DEVICE_NAME, value?.name)
            putString(KEY_DEVICE_MODEL_ID, value?.modelId)
        }

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
        const val KEY_DEVICE_ADDRESS = "registered_device_address"
        const val KEY_DEVICE_NAME = "registered_device_name"
        const val KEY_DEVICE_MODEL_ID = "registered_device_model_id"
        const val KEY_LAST_ACTION_ID = "last_routine_action_id"
        const val KEY_LAST_ACTION_AT = "last_routine_action_at"
    }
}
