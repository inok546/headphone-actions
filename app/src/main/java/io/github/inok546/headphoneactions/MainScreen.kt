// SPDX-License-Identifier: AGPL-3.0-only

package io.github.inok546.headphoneactions

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.inok546.headphoneactions.bluetooth.PairedDevice
import io.github.inok546.headphoneactions.bluetooth.PairedDevices
import io.github.inok546.headphoneactions.device.DeviceResult
import io.github.inok546.headphoneactions.device.HeadphoneModel
import io.github.inok546.headphoneactions.device.RegisteredDevice
import io.github.inok546.headphoneactions.device.SupportedAction
import io.github.inok546.headphoneactions.device.findModelForDeviceName
import java.text.DateFormat
import java.util.Date

data class MainUiState(
    val registeredDevice: RegisteredDevice? = null,
    val registeredModel: HeadphoneModel? = null,
    /** Null while a device is registered: the list is only used to pick one. */
    val pairedDevices: PairedDevices? = null,
    val publishedActions: List<SupportedAction> = emptyList(),
    val lastRoutineAction: LastRoutineAction? = null,
    val deviceOperation: DeviceOperation? = null,
) {
    val deviceBusy: Boolean get() = deviceOperation != null && deviceOperation.result == null
}

/** The last explicit operation on the headphones; [result] is null while it runs. */
data class DeviceOperation(val title: String, val result: DeviceResult? = null)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(),
        content = content,
    )
}

@Composable
fun MainScreen(
    state: MainUiState,
    onRequestBluetoothPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onRegister: (PairedDevice, HeadphoneModel) -> Unit,
    onTestConnection: () -> Unit,
    onRunAction: (SupportedAction) -> Unit,
    onRemoveRegistration: () -> Unit,
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
            RegistrationSection(state.registeredDevice, state.registeredModel, onRemoveRegistration)
            if (state.registeredDevice != null) {
                ControlSection(state.deviceOperation, state.deviceBusy, onTestConnection)
            }
            state.pairedDevices?.let {
                PairedDevicesSection(it, onRequestBluetoothPermission, onOpenAppSettings, onRegister)
            }
            PublishedActionsSection(
                state.publishedActions,
                runEnabled = state.registeredDevice != null && !state.deviceBusy,
                onRunAction = onRunAction,
            )
            LastRoutineActionSection(state.lastRoutineAction, state.registeredModel)
        }
    }
}

@Composable
private fun RegistrationSection(
    registeredDevice: RegisteredDevice?,
    registeredModel: HeadphoneModel?,
    onRemoveRegistration: () -> Unit,
) {
    Section(stringResource(R.string.registered_device_title)) {
        if (registeredDevice == null) {
            Text(stringResource(R.string.registered_device_none))
            return@Section
        }
        Column {
            Text(registeredDevice.name, style = MaterialTheme.typography.bodyLarge)
            registeredModel?.let { Text(stringResource(R.string.registered_device_model, it.displayName)) }
            Monospace(registeredDevice.address)
        }
        OutlinedButton(onClick = onRemoveRegistration) {
            Text(stringResource(R.string.remove_registration))
        }
    }
}

@Composable
private fun ControlSection(operation: DeviceOperation?, busy: Boolean, onTestConnection: () -> Unit) {
    Section(stringResource(R.string.control_title)) {
        Button(onClick = onTestConnection, enabled = !busy) {
            Text(stringResource(R.string.test_connection))
        }
        when (val result = operation?.result) {
            null -> if (operation != null) Text(stringResource(R.string.operation_running, operation.title))
            is DeviceResult.Success -> Text(stringResource(R.string.operation_success, operation.title, result.details))
            is DeviceResult.Failure -> Text(
                stringResource(R.string.operation_failure, operation.title, result.reason),
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun PairedDevicesSection(
    pairedDevices: PairedDevices,
    onRequestBluetoothPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onRegister: (PairedDevice, HeadphoneModel) -> Unit,
) {
    Section(stringResource(R.string.paired_devices_title)) {
        when (pairedDevices) {
            PairedDevices.PermissionRequired -> {
                Text(stringResource(R.string.paired_devices_permission_rationale))
                Button(onClick = onRequestBluetoothPermission) {
                    Text(stringResource(R.string.paired_devices_allow))
                }
                TextButton(onClick = onOpenAppSettings) {
                    Text(stringResource(R.string.paired_devices_open_settings))
                }
            }
            PairedDevices.BluetoothUnavailable -> Text(stringResource(R.string.paired_devices_bluetooth_unavailable))
            PairedDevices.BluetoothOff -> Text(stringResource(R.string.paired_devices_bluetooth_off))
            is PairedDevices.Available -> {
                if (pairedDevices.devices.isEmpty()) {
                    Text(stringResource(R.string.paired_devices_none))
                }
                pairedDevices.devices.forEach { PairedDeviceItem(it, onRegister) }
            }
        }
    }
}

@Composable
private fun PairedDeviceItem(device: PairedDevice, onRegister: (PairedDevice, HeadphoneModel) -> Unit) {
    val model = findModelForDeviceName(device.name)
    Column {
        Text(device.name ?: stringResource(R.string.paired_device_unnamed), style = MaterialTheme.typography.bodyLarge)
        Monospace(device.address)
        if (model != null) {
            Button(onClick = { onRegister(device, model) }) {
                Text(stringResource(R.string.paired_device_register, model.displayName))
            }
        } else {
            Text(
                stringResource(R.string.paired_device_unsupported),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PublishedActionsSection(
    actions: List<SupportedAction>,
    runEnabled: Boolean,
    onRunAction: (SupportedAction) -> Unit,
) {
    Section(stringResource(R.string.published_actions_title)) {
        if (actions.isEmpty()) {
            Text(stringResource(R.string.published_actions_none))
        }
        actions.forEach { action ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(action.label, style = MaterialTheme.typography.bodyLarge)
                    Monospace(action.id)
                }
                TextButton(onClick = { onRunAction(action) }, enabled = runEnabled) {
                    Text(stringResource(R.string.run_action))
                }
            }
        }
    }
}

@Composable
private fun LastRoutineActionSection(lastAction: LastRoutineAction?, registeredModel: HeadphoneModel?) {
    Section(stringResource(R.string.last_routine_action_title)) {
        if (lastAction == null) {
            Text(stringResource(R.string.last_routine_action_none))
            return@Section
        }
        registeredModel?.findAction(lastAction.actionId)?.let {
            Text(it.label, style = MaterialTheme.typography.bodyLarge)
        }
        Monospace(lastAction.actionId)
        val invokedAt = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM)
            .format(Date(lastAction.invokedAtMillis))
        Text(stringResource(R.string.last_routine_action_time, invokedAt))
        when (val result = lastAction.result) {
            null -> Text(stringResource(R.string.last_routine_action_pending))
            is DeviceResult.Success -> Text(stringResource(R.string.last_routine_action_success, result.details))
            is DeviceResult.Failure -> Text(
                stringResource(R.string.last_routine_action_failure, result.reason),
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        content()
    }
}

@Composable
private fun Monospace(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
