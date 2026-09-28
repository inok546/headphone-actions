// SPDX-License-Identifier: AGPL-3.0-only

package io.github.inok546.headphoneactions.device

import android.content.Context
import io.github.inok546.headphoneactions.device.sony.SonyWh1000xm6

/**
 * One action a specific headphone model supports.
 *
 * [id] becomes the Android shortcut ID that Samsung Modes and Routines persists,
 * so it must never change once released. [label] may change freely.
 */
data class SupportedAction(val id: String, val label: String)

sealed interface ConnectionTestResult {
    data class Success(val details: String) : ConnectionTestResult
    data class Failure(val reason: String) : ConnectionTestResult
}

/**
 * Support for one headphone model: how to recognize it among paired devices, which actions
 * it exposes and how to talk to it. Vendor protocol details stay behind this interface.
 */
interface HeadphoneModel {
    val id: String
    val displayName: String
    val actions: List<SupportedAction>

    /** Whether a paired device with this whole Bluetooth name is this model. */
    fun matchesDeviceName(name: String): Boolean

    /** Connects to [device], performs the protocol handshake and disconnects. */
    suspend fun testConnection(context: Context, device: RegisteredDevice): ConnectionTestResult

    fun findAction(actionId: String): SupportedAction? = actions.find { it.id == actionId }
}

val supportedModels: List<HeadphoneModel> = listOf(SonyWh1000xm6)

fun findModel(modelId: String): HeadphoneModel? = supportedModels.find { it.id == modelId }

/** The supported model a paired device is, judged by its Bluetooth name. */
fun findModelForDeviceName(name: String?): HeadphoneModel? =
    name?.let { supportedModels.find { model -> model.matchesDeviceName(it) } }
