// SPDX-License-Identifier: AGPL-3.0-only

package app.headphoneactions.device

import app.headphoneactions.device.sony.sonyWh1000xm6Mock

/**
 * One action a specific headphone model supports.
 *
 * [id] becomes the Android shortcut ID that Samsung Modes and Routines persists,
 * so it must never change once released. [label] may change freely.
 */
data class SupportedAction(val id: String, val label: String)

/** A headphone model and the actions it supports. */
data class HeadphoneModel(
    val id: String,
    val displayName: String,
    val actions: List<SupportedAction>,
) {
    fun findAction(actionId: String): SupportedAction? = actions.find { it.id == actionId }
}

/** Models the user can register. For now this is only the mock WH-1000XM6. */
val registrableModels: List<HeadphoneModel> = listOf(sonyWh1000xm6Mock)

fun findModel(modelId: String): HeadphoneModel? = registrableModels.find { it.id == modelId }
