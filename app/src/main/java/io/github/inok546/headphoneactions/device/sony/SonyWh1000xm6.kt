// SPDX-License-Identifier: AGPL-3.0-only

package io.github.inok546.headphoneactions.device.sony

import android.content.Context
import io.github.inok546.headphoneactions.device.DeviceResult
import io.github.inok546.headphoneactions.device.HeadphoneModel
import io.github.inok546.headphoneactions.device.RegisteredDevice
import io.github.inok546.headphoneactions.device.SupportedAction
import io.github.inok546.headphoneactions.device.sony.mdr.SonyMdrConnection
import io.github.inok546.headphoneactions.device.sony.mdr.SonyNoiseControl
import io.github.inok546.headphoneactions.device.sony.mdr.toHex

/**
 * Sony WH-1000XM6. Only Noise Cancelling is executed so far; the other actions are declared
 * but not implemented yet. The action IDs are published and must not change.
 */
object SonyWh1000xm6 : HeadphoneModel {

    private val noiseCancelling = SupportedAction("sony.wh1000xm6.noise_control.anc", "Noise Cancelling")

    override val id = "sony.wh1000xm6"
    override val displayName = "Sony WH-1000XM6"
    override val actions = listOf(
        noiseCancelling,
        SupportedAction("sony.wh1000xm6.noise_control.ambient", "Ambient Sound"),
        SupportedAction("sony.wh1000xm6.noise_control.off", "Noise Control Off"),
        SupportedAction("sony.wh1000xm6.speak_to_chat.on", "Speak-to-Chat On"),
        SupportedAction("sony.wh1000xm6.speak_to_chat.off", "Speak-to-Chat Off"),
        SupportedAction("sony.wh1000xm6.dsee.on", "DSEE Extreme On"),
        SupportedAction("sony.wh1000xm6.dsee.off", "DSEE Extreme Off"),
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

    override suspend fun execute(context: Context, device: RegisteredDevice, action: SupportedAction): DeviceResult =
        when (action.id) {
            noiseCancelling.id -> SonyMdrConnection.open(context, device.address) { session ->
                SonyNoiseControl.setMode(session, SonyNoiseControl.Mode.NOISE_CANCELLING)
            }
            else -> DeviceResult.Failure("${action.label} is not implemented yet")
        }
}
