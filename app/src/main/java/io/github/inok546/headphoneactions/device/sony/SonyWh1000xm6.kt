// SPDX-License-Identifier: AGPL-3.0-only

package io.github.inok546.headphoneactions.device.sony

import android.content.Context
import io.github.inok546.headphoneactions.device.DeviceResult
import io.github.inok546.headphoneactions.device.HeadphoneModel
import io.github.inok546.headphoneactions.device.RegisteredDevice
import io.github.inok546.headphoneactions.device.SupportedAction
import io.github.inok546.headphoneactions.device.sony.mdr.SonyMdrConnection
import io.github.inok546.headphoneactions.device.sony.mdr.SonyMdrSession
import io.github.inok546.headphoneactions.device.sony.mdr.SonyMdrSwitch
import io.github.inok546.headphoneactions.device.sony.mdr.SonyNoiseControl
import io.github.inok546.headphoneactions.device.sony.mdr.toHex

/** Sony WH-1000XM6. The action IDs are published and must not change. */
object SonyWh1000xm6 : HeadphoneModel {

    private val noiseCancelling = SupportedAction("sony.wh1000xm6.noise_control.anc", "Noise Cancelling")
    private val ambientSound = SupportedAction("sony.wh1000xm6.noise_control.ambient", "Ambient Sound")
    private val noiseControlOff = SupportedAction("sony.wh1000xm6.noise_control.off", "Noise Control Off")
    private val speakToChatOn = SupportedAction("sony.wh1000xm6.speak_to_chat.on", "Speak-to-Chat On")
    private val speakToChatOff = SupportedAction("sony.wh1000xm6.speak_to_chat.off", "Speak-to-Chat Off")
    private val dseeOn = SupportedAction("sony.wh1000xm6.dsee.on", "DSEE Extreme On")
    private val dseeOff = SupportedAction("sony.wh1000xm6.dsee.off", "DSEE Extreme Off")

    override val id = "sony.wh1000xm6"
    override val displayName = "Sony WH-1000XM6"
    override val actions = listOf(
        noiseCancelling,
        ambientSound,
        noiseControlOff,
        speakToChatOn,
        speakToChatOff,
        dseeOn,
        dseeOff,
    )

    // Gadgetbridge's SonyWH1000XM6Coordinator matches ".*WH-1000XM6.*". "LE_"-prefixed
    // entries are excluded: that is the LE Audio side of the headphones, while the Sony
    // protocol needs the Bluetooth Classic device.
    private val bluetoothNamePattern = Regex("(?!LE_).*WH-1000XM6.*")

    override fun matchesDeviceName(name: String): Boolean = bluetoothNamePattern.matches(name)

    override suspend fun testConnection(context: Context, device: RegisteredDevice): DeviceResult =
        SonyMdrConnection.open(context, device.address) { session ->
            DeviceResult.Success("Sony MDR protocol ${session.protocolVersion.name.lowercase()}, init reply ${session.initReply.toHex()}")
        }

    private val operations: Map<String, suspend (SonyMdrSession) -> DeviceResult> = mapOf(
        noiseCancelling.id to { SonyNoiseControl.setMode(it, SonyNoiseControl.Mode.NOISE_CANCELLING) },
        ambientSound.id to { SonyNoiseControl.setMode(it, SonyNoiseControl.Mode.AMBIENT_SOUND) },
        noiseControlOff.id to { SonyNoiseControl.setMode(it, SonyNoiseControl.Mode.OFF) },
        speakToChatOn.id to { SonyMdrSwitch.speakToChat.set(it, enabled = true) },
        speakToChatOff.id to { SonyMdrSwitch.speakToChat.set(it, enabled = false) },
        dseeOn.id to { SonyMdrSwitch.dseeExtreme.set(it, enabled = true) },
        dseeOff.id to { SonyMdrSwitch.dseeExtreme.set(it, enabled = false) },
    )

    override suspend fun execute(context: Context, device: RegisteredDevice, action: SupportedAction): DeviceResult {
        val operation = operations[action.id]
            ?: return DeviceResult.Failure("${action.label} is not supported by ${device.name}")
        return SonyMdrConnection.open(context, device.address, operation)
    }
}
