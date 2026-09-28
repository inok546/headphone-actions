// SPDX-License-Identifier: AGPL-3.0-only

package io.github.inok546.headphoneactions

import android.content.Context
import androidx.core.content.edit
import io.github.inok546.headphoneactions.device.DeviceResult
import io.github.inok546.headphoneactions.device.RegisteredDevice

/** [result] is null until the execution of the action has finished. */
data class LastRoutineAction(val actionId: String, val invokedAtMillis: Long, val result: DeviceResult?)

/** Locally persisted state: the registered device and the last routine invocation. */
class AppPreferences(context: Context) {

    private val prefs = context.getSharedPreferences("headphone_actions", Context.MODE_PRIVATE)

    /** The explicitly registered headphones, or null if none are registered. */
    var registeredDevice: RegisteredDevice?
        get() {
            val address = prefs.getString(KEY_DEVICE_ADDRESS, null) ?: return null
            val name = prefs.getString(KEY_DEVICE_NAME, null) ?: return null
            val modelId = prefs.getString(KEY_DEVICE_MODEL_ID, null) ?: return null
            return RegisteredDevice(
                address,
                name,
                modelId,
                phoneAddress = prefs.getString(KEY_DEVICE_PHONE_ADDRESS, null),
                phoneName = prefs.getString(KEY_DEVICE_PHONE_NAME, null),
            )
        }
        set(value) = prefs.edit {
            putString(KEY_DEVICE_ADDRESS, value?.address)
            putString(KEY_DEVICE_NAME, value?.name)
            putString(KEY_DEVICE_MODEL_ID, value?.modelId)
            putString(KEY_DEVICE_PHONE_ADDRESS, value?.phoneAddress)
            putString(KEY_DEVICE_PHONE_NAME, value?.phoneName)
        }

    val lastRoutineAction: LastRoutineAction?
        get() {
            val actionId = prefs.getString(KEY_LAST_ACTION_ID, null) ?: return null
            val message = prefs.getString(KEY_LAST_ACTION_RESULT, null)
            val result = when {
                message == null -> null
                prefs.getBoolean(KEY_LAST_ACTION_SUCCEEDED, false) -> DeviceResult.Success(message)
                else -> DeviceResult.Failure(message)
            }
            return LastRoutineAction(actionId, prefs.getLong(KEY_LAST_ACTION_AT, 0), result)
        }

    fun recordRoutineAction(actionId: String, invokedAtMillis: Long) {
        prefs.edit {
            putString(KEY_LAST_ACTION_ID, actionId)
            putLong(KEY_LAST_ACTION_AT, invokedAtMillis)
            remove(KEY_LAST_ACTION_SUCCEEDED)
            remove(KEY_LAST_ACTION_RESULT)
        }
    }

    /** Stores the result of the invocation made at [invokedAtMillis], unless a newer one was recorded since. */
    fun recordRoutineResult(invokedAtMillis: Long, result: DeviceResult) {
        if (prefs.getLong(KEY_LAST_ACTION_AT, 0) != invokedAtMillis) return
        prefs.edit {
            putBoolean(KEY_LAST_ACTION_SUCCEEDED, result is DeviceResult.Success)
            putString(
                KEY_LAST_ACTION_RESULT,
                when (result) {
                    is DeviceResult.Success -> result.details
                    is DeviceResult.Failure -> result.reason
                },
            )
        }
    }

    private companion object {
        const val KEY_DEVICE_ADDRESS = "registered_device_address"
        const val KEY_DEVICE_NAME = "registered_device_name"
        const val KEY_DEVICE_MODEL_ID = "registered_device_model_id"
        const val KEY_DEVICE_PHONE_ADDRESS = "registered_device_phone_address"
        const val KEY_DEVICE_PHONE_NAME = "registered_device_phone_name"
        const val KEY_LAST_ACTION_ID = "last_routine_action_id"
        const val KEY_LAST_ACTION_AT = "last_routine_action_at"
        const val KEY_LAST_ACTION_SUCCEEDED = "last_routine_action_succeeded"
        const val KEY_LAST_ACTION_RESULT = "last_routine_action_result"
    }
}
