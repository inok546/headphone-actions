// SPDX-License-Identifier: AGPL-3.0-only

package io.github.inok546.headphoneactions.routines

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import io.github.inok546.headphoneactions.AppPreferences
import io.github.inok546.headphoneactions.LOG_TAG
import io.github.inok546.headphoneactions.R
import io.github.inok546.headphoneactions.device.DeviceAccess
import io.github.inok546.headphoneactions.device.DeviceResult
import io.github.inok546.headphoneactions.device.findModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Executes one action triggered through a published shortcut. RoutineActionActivity closes
 * at once, so this short-lived foreground service (type connectedDevice) keeps the process
 * running for the few seconds the Bluetooth exchange takes, then stops itself. Android 12+
 * defers its notification by up to 10 seconds, so normally none is shown.
 */
class ActionExecutionService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startInForeground()
        val actionId = intent?.getStringExtra(EXTRA_ACTION_ID)
        val invokedAtMillis = intent?.getLongExtra(EXTRA_INVOKED_AT, 0) ?: 0
        scope.launch {
            if (actionId != null) execute(actionId, invokedAtMillis)
            // Stops only once the latest start request has been handled.
            stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun execute(actionId: String, invokedAtMillis: Long) {
        val preferences = AppPreferences(this)
        val device = preferences.registeredDevice
        val model = device?.let { findModel(it.modelId) }
        val action = model?.findAction(actionId)
        val result = if (device == null || action == null) {
            DeviceResult.Failure("Not supported by the registered device (${device ?: "none registered"})")
        } else {
            DeviceAccess.exclusive { model.execute(applicationContext, device, action) }
        }
        Log.i(LOG_TAG, "Routine action $actionId: $result")
        preferences.recordRoutineResult(invokedAtMillis, result)
        if (result is DeviceResult.Failure) {
            val label = action?.label ?: actionId
            Toast.makeText(this, getString(R.string.routine_action_failed, label, result.reason), Toast.LENGTH_LONG)
                .show()
        }
    }

    private fun startInForeground() {
        val notifications = NotificationManagerCompat.from(this)
        notifications.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(getString(R.string.notification_channel_actions))
                .build(),
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_headphones)
            .setContentTitle(getString(R.string.routine_action_running))
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_DEFERRED)
            .build()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    companion object {
        private const val CHANNEL_ID = "headphone_actions"
        private const val NOTIFICATION_ID = 1
        private const val EXTRA_ACTION_ID = "io.github.inok546.headphoneactions.extra.ACTION_ID"
        private const val EXTRA_INVOKED_AT = "io.github.inok546.headphoneactions.extra.INVOKED_AT"

        fun intentFor(context: Context, actionId: String, invokedAtMillis: Long): Intent =
            Intent(context, ActionExecutionService::class.java)
                .putExtra(EXTRA_ACTION_ID, actionId)
                .putExtra(EXTRA_INVOKED_AT, invokedAtMillis)
    }
}
