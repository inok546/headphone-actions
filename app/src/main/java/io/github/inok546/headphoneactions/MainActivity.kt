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
import io.github.inok546.headphoneactions.device.DeviceAccess
import io.github.inok546.headphoneactions.device.DeviceResult
import io.github.inok546.headphoneactions.device.HeadphoneModel
import io.github.inok546.headphoneactions.device.RegisteredDevice
import io.github.inok546.headphoneactions.device.SourceDevice
import io.github.inok546.headphoneactions.device.SourceDevicesResult
import io.github.inok546.headphoneactions.device.SupportedAction
import io.github.inok546.headphoneactions.device.findModel
import io.github.inok546.headphoneactions.routines.ShortcutPublisher
import io.github.inok546.headphoneactions.widget.HeadphoneActionsWidgets
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
                    onRunAction = ::runAction,
                    onReadSourceDevices = ::readSourceDevices,
                    onChooseThisPhone = ::chooseThisPhone,
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
        refreshWidgets()
        refresh()
    }

    private fun testConnection() = runDeviceOperation(getString(R.string.test_connection)) { model, device ->
        model.testConnection(applicationContext, device)
    }

    private fun runAction(action: SupportedAction) = runDeviceOperation(action.label) { model, device ->
        model.execute(applicationContext, device, action)
    }

    private fun readSourceDevices() = runDeviceOperation(getString(R.string.read_source_devices)) { model, device ->
        when (val result = model.readSourceDevices(applicationContext, device)) {
            is SourceDevicesResult.Success -> {
                uiState = uiState.copy(sourceDevices = result.devices)
                DeviceResult.Success("${result.devices.size} devices; pick this phone below")
            }
            is SourceDevicesResult.Failure -> DeviceResult.Failure(result.reason)
        }
    }

    private fun chooseThisPhone(source: SourceDevice) {
        val device = preferences.registeredDevice ?: return
        preferences.registeredDevice = device.copy(phoneAddress = source.address, phoneName = source.name)
        Log.i(LOG_TAG, "This phone on the headphones: ${source.name} [${source.address}]")
        uiState = uiState.copy(sourceDevices = null, deviceOperation = null)
        refresh()
    }

    /** Explicit user actions only: the app never connects to the headphones on its own. */
    private fun runDeviceOperation(
        title: String,
        operation: suspend (HeadphoneModel, RegisteredDevice) -> DeviceResult,
    ) {
        val device = preferences.registeredDevice ?: return
        val model = findModel(device.modelId) ?: return
        uiState = uiState.copy(deviceOperation = DeviceOperation(title))
        lifecycleScope.launch {
            Log.i(LOG_TAG, "$title: starting for $device")
            val result = DeviceAccess.exclusive { operation(model, device) }
            Log.i(LOG_TAG, "$title: $result")
            uiState = uiState.copy(deviceOperation = DeviceOperation(title, result))
        }
    }

    private fun removeRegistration() {
        preferences.registeredDevice = null
        Log.i(LOG_TAG, "Registration removed")
        ShortcutPublisher.removeAll(this)
        refreshWidgets()
        uiState = uiState.copy(deviceOperation = null, sourceDevices = null)
        refresh()
    }

    /** Home screen widgets follow explicit registration changes only. */
    private fun refreshWidgets() {
        lifecycleScope.launch { HeadphoneActionsWidgets.refreshAll(applicationContext) }
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
