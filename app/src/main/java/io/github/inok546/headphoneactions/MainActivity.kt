// SPDX-License-Identifier: AGPL-3.0-only

package io.github.inok546.headphoneactions

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import io.github.inok546.headphoneactions.bluetooth.PairedDevice
import io.github.inok546.headphoneactions.bluetooth.readPairedDevices
import io.github.inok546.headphoneactions.device.HeadphoneModel
import io.github.inok546.headphoneactions.device.RegisteredDevice
import io.github.inok546.headphoneactions.device.findModel
import io.github.inok546.headphoneactions.routines.ShortcutPublisher
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var preferences: AppPreferences
    private var uiState by mutableStateOf(MainUiState())

    private val bluetoothPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Log.i(LOG_TAG, "Bluetooth permission granted: $granted")
            refresh()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        preferences = AppPreferences(this)
        setContent {
            AppTheme {
                MainScreen(
                    state = uiState,
                    onRequestBluetoothPermission = ::requestBluetoothPermission,
                    onOpenAppSettings = ::openAppSettings,
                    onRegister = ::register,
                    onTestConnection = ::testConnection,
                    onRemoveRegistration = ::removeRegistration,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Picks up routine invocations recorded while we were in the background,
        // and devices the user may have just paired in the system settings.
        refresh()
    }

    private fun requestBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bluetoothPermissionRequest.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }

    private fun openAppSettings() {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)),
        )
    }

    private fun register(device: PairedDevice, model: HeadphoneModel) {
        val registered = RegisteredDevice(device.address, device.name ?: model.displayName, model.id)
        preferences.registeredDevice = registered
        Log.i(LOG_TAG, "Registered device: $registered")
        ShortcutPublisher.publish(this, model)
        refresh()
    }

    /** Explicit user action only: the app never connects to the headphones on its own. */
    private fun testConnection() {
        val device = preferences.registeredDevice ?: return
        val model = findModel(device.modelId) ?: return
        uiState = uiState.copy(connectionTestRunning = true, connectionTestResult = null)
        lifecycleScope.launch {
            Log.i(LOG_TAG, "Testing connection to $device")
            val result = model.testConnection(applicationContext, device)
            Log.i(LOG_TAG, "Connection test result: $result")
            uiState = uiState.copy(connectionTestRunning = false, connectionTestResult = result)
        }
    }

    private fun removeRegistration() {
        preferences.registeredDevice = null
        Log.i(LOG_TAG, "Registration removed")
        ShortcutPublisher.removeAll(this)
        uiState = uiState.copy(connectionTestResult = null)
        refresh()
    }

    private fun refresh() {
        val registeredDevice = preferences.registeredDevice
        uiState = uiState.copy(
            registeredDevice = registeredDevice,
            registeredModel = registeredDevice?.let { findModel(it.modelId) },
            // Paired devices are only needed to pick a device, so they are not read while
            // one is registered.
            pairedDevices = if (registeredDevice == null) readPairedDevices(this) else null,
            publishedActions = ShortcutPublisher.publishedActions(this),
            lastRoutineAction = preferences.lastRoutineAction,
        )
    }
}
