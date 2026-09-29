// SPDX-License-Identifier: AGPL-3.0-only

package io.github.inok546.headphoneactions.device

import android.content.Context
import io.github.inok546.headphoneactions.device.sony.SonyWh1000xm6

/**
 * One action a specific headphone model supports.
 *
 * [id] becomes the Android shortcut ID that Samsung Modes and Routines persists,
 * so it must never change once released. [label], [shortLabel] (where space is tight, such as
 * widget tiles) and [icon] may change freely.
 */
data class SupportedAction(
    val id: String,
    val label: String,
    val shortLabel: String = label,
    val icon: ActionIcon = ActionIcon.GENERIC,
)

/** What an action's icon depicts; the UI maps it to a drawable. Not a cross-vendor action model. */
enum class ActionIcon {
    GENERIC,
    NOISE_CANCELLING,
    AMBIENT_SOUND,
    NOISE_CONTROL_OFF,
    SPEAK_TO_CHAT_ON,
    SPEAK_TO_CHAT_OFF,
    UPSCALING_ON,
    UPSCALING_OFF,
    PLAYBACK_THIS_PHONE,
    PLAYBACK_OTHER_DEVICE,
    PLAYBACK_LOCK,
    PLAYBACK_AUTO,
}

/** Outcome of talking to the headphones; the texts are shown to the user. */
sealed interface DeviceResult {
    data class Success(val details: String) : DeviceResult
    data class Failure(val reason: String) : DeviceResult
}

/** A source device the headphones know (multipoint), identified by the address they report. */
data class SourceDevice(val address: String, val name: String, val connected: Boolean, val playing: Boolean)

sealed interface SourceDevicesResult {
    data class Success(val devices: List<SourceDevice>) : SourceDevicesResult
    data class Failure(val reason: String) : SourceDevicesResult
}

/**
 * Support for one headphone model: how to recognize it among paired devices, which actions
 * it exposes and how to talk to it. Vendor protocol details stay behind this interface.
 */
interface HeadphoneModel {
    val id: String
    val displayName: String
    val actions: List<SupportedAction>

    /** Action IDs offered first where only a few fit, such as on a new home screen widget. */
    val quickActionIds: List<String>

    /** Whether a paired device with this whole Bluetooth name is this model. */
    fun matchesDeviceName(name: String): Boolean

    /** Connects to [device], performs the protocol handshake and disconnects. */
    suspend fun testConnection(context: Context, device: RegisteredDevice): DeviceResult

    /** Connects to [device], performs [action] and disconnects. */
    suspend fun execute(context: Context, device: RegisteredDevice, action: SupportedAction): DeviceResult

    /**
     * Whether the headphones can play from one of two connected source devices. Some of their
     * actions then need to know which source is this phone ([RegisteredDevice.phoneAddress]).
     */
    val supportsMultipoint: Boolean

    /** Connects to [device], reads the source devices it knows and disconnects. */
    suspend fun readSourceDevices(context: Context, device: RegisteredDevice): SourceDevicesResult

    fun findAction(actionId: String): SupportedAction? = actions.find { it.id == actionId }
}

val supportedModels: List<HeadphoneModel> = listOf(SonyWh1000xm6)

fun findModel(modelId: String): HeadphoneModel? = supportedModels.find { it.id == modelId }

/** The supported model a paired device is, judged by its Bluetooth name. */
fun findModelForDeviceName(name: String?): HeadphoneModel? =
    name?.let { supportedModels.find { model -> model.matchesDeviceName(it) } }
