// SPDX-License-Identifier: AGPL-3.0-only

package io.github.inok546.headphoneactions.routines

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import io.github.inok546.headphoneactions.AppPreferences
import io.github.inok546.headphoneactions.LOG_TAG
import io.github.inok546.headphoneactions.R
import io.github.inok546.headphoneactions.device.DeviceResult

/**
 * Target of every published shortcut, i.e. what Samsung Modes and Routines launches.
 *
 * Its theme is Theme.NoDisplay, so it must finish before onCreate returns: it records the
 * invocation and hands the action to [ActionExecutionService]. Starting a foreground service
 * is allowed here because the app is in a user-visible state while this activity starts.
 */
class RoutineActionActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handle(intent.getStringExtra(EXTRA_ACTION_ID))
        finish()
    }

    private fun handle(actionId: String?) {
        if (actionId == null) {
            Log.w(LOG_TAG, "Routine action ignored: no action ID in $intent")
            return
        }
        val invokedAtMillis = System.currentTimeMillis()
        val preferences = AppPreferences(this)
        preferences.recordRoutineAction(actionId, invokedAtMillis)
        Log.i(LOG_TAG, "Routine action invoked: $actionId")

        val failure = if (!hasBluetoothConnectPermission()) {
            // A connectedDevice foreground service may not start without it (Android 14+).
            "Nearby devices permission is not granted"
        } else {
            try {
                ContextCompat.startForegroundService(this, ActionExecutionService.intentFor(this, actionId, invokedAtMillis))
                null
            } catch (e: IllegalStateException) {
                // ForegroundServiceStartNotAllowedException on Android 12+.
                Log.w(LOG_TAG, "Could not start the action service", e)
                "Android did not allow the action to start: ${e.message}"
            }
        }
        if (failure != null) {
            Log.w(LOG_TAG, "Routine action $actionId failed: $failure")
            preferences.recordRoutineResult(invokedAtMillis, DeviceResult.Failure(failure))
            Toast.makeText(this, getString(R.string.routine_action_failed, actionId, failure), Toast.LENGTH_LONG).show()
        }
    }

    private fun hasBluetoothConnectPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED

    companion object {
        private const val ACTION_RUN = "io.github.inok546.headphoneactions.action.RUN_HEADPHONE_ACTION"
        private const val EXTRA_ACTION_ID = "io.github.inok546.headphoneactions.extra.ACTION_ID"

        fun intentFor(context: Context, actionId: String): Intent =
            Intent(ACTION_RUN)
                .setClass(context, RoutineActionActivity::class.java)
                .putExtra(EXTRA_ACTION_ID, actionId)
    }
}
