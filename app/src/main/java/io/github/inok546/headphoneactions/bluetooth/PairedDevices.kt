// SPDX-License-Identifier: AGPL-3.0-only

package io.github.inok546.headphoneactions.bluetooth

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import io.github.inok546.headphoneactions.LOG_TAG

/** A Bluetooth device bonded with the phone. [name] is null if Android has not resolved it. */
data class PairedDevice(val address: String, val name: String?)

sealed interface PairedDevices {
    /** BLUETOOTH_CONNECT ("Nearby devices") is not granted; needed on Android 12+. */
    data object PermissionRequired : PairedDevices

    data object BluetoothUnavailable : PairedDevices

    /** Android does not report bonded devices while Bluetooth is off. */
    data object BluetoothOff : PairedDevices

    data class Available(val devices: List<PairedDevice>) : PairedDevices
}

/** Reads the devices bonded with the phone, for the user to pick from. */
fun readPairedDevices(context: Context): PairedDevices {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) !=
        PackageManager.PERMISSION_GRANTED
    ) {
        return PairedDevices.PermissionRequired
    }
    val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
        ?: return PairedDevices.BluetoothUnavailable
    if (!adapter.isEnabled) return PairedDevices.BluetoothOff

    val devices = adapter.bondedDevices.map { device ->
        Log.d(LOG_TAG, "Paired device: name=${device.name} address=${device.address} type=${device.type}")
        PairedDevice(device.address, device.name)
    }
    return PairedDevices.Available(devices.sortedBy { it.name })
}
