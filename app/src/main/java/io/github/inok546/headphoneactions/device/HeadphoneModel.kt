// SPDX-License-Identifier: AGPL-3.0-only

package io.github.inok546.headphoneactions.device

import io.github.inok546.headphoneactions.device.sony.sonyWh1000xm6

/**
 * One action a specific headphone model supports.
 *
 * [id] becomes the Android shortcut ID that Samsung Modes and Routines persists,
 * so it must never change once released. [label] may change freely.
 */
data class SupportedAction(val id: String, val label: String)

/** A headphone model, how to recognize it among paired devices, and the actions it supports. */
data class HeadphoneModel(
    val id: String,
    val displayName: String,
    /** Must match the paired device's whole Bluetooth name. */
    val bluetoothNamePattern: Regex,
    val actions: List<SupportedAction>,
) {
    fun matchesDeviceName(name: String): Boolean = bluetoothNamePattern.matches(name)

    fun findAction(actionId: String): SupportedAction? = actions.find { it.id == actionId }
}

val supportedModels: List<HeadphoneModel> = listOf(sonyWh1000xm6)

fun findModel(modelId: String): HeadphoneModel? = supportedModels.find { it.id == modelId }

/** The supported model a paired device is, judged by its Bluetooth name. */
fun findModelForDeviceName(name: String?): HeadphoneModel? =
    name?.let { supportedModels.find { model -> model.matchesDeviceName(it) } }
