// SPDX-License-Identifier: AGPL-3.0-only

package io.github.inok546.headphoneactions.device.sony

import android.content.Context
import io.github.inok546.headphoneactions.device.ActionIcon
import io.github.inok546.headphoneactions.device.DeviceResult
import io.github.inok546.headphoneactions.device.HeadphoneModel
import io.github.inok546.headphoneactions.device.RegisteredDevice
import io.github.inok546.headphoneactions.device.SourceDevice
import io.github.inok546.headphoneactions.device.SourceDevicesResult
import io.github.inok546.headphoneactions.device.SupportedAction
import io.github.inok546.headphoneactions.device.sony.mdr.SonyMdrConnection
import io.github.inok546.headphoneactions.device.sony.mdr.SonyMdrSession
import io.github.inok546.headphoneactions.device.sony.mdr.SonyMdrSwitch
import io.github.inok546.headphoneactions.device.sony.mdr.SonyMultipoint
import io.github.inok546.headphoneactions.device.sony.mdr.SonyNoiseControl
import io.github.inok546.headphoneactions.device.sony.mdr.toHex

/** Sony WH-1000XM6. The action IDs are published and must not change. */
object SonyWh1000xm6 : HeadphoneModel {

    private val noiseCancelling = SupportedAction(
        "sony.wh1000xm6.noise_control.anc", "Noise Cancelling", "ANC", ActionIcon.NOISE_CANCELLING,
    )
    private val ambientSound = SupportedAction(
        "sony.wh1000xm6.noise_control.ambient", "Ambient Sound", "Ambient", ActionIcon.AMBIENT_SOUND,
    )
    private val noiseControlOff = SupportedAction(
        "sony.wh1000xm6.noise_control.off", "Noise Control Off", "NC off", ActionIcon.NOISE_CONTROL_OFF,
    )
    private val speakToChatOn = SupportedAction(
        "sony.wh1000xm6.speak_to_chat.on", "Speak-to-Chat On", "STC on", ActionIcon.SPEAK_TO_CHAT_ON,
    )
    private val speakToChatOff = SupportedAction(
        "sony.wh1000xm6.speak_to_chat.off", "Speak-to-Chat Off", "STC off", ActionIcon.SPEAK_TO_CHAT_OFF,
    )
    private val dseeOn = SupportedAction("sony.wh1000xm6.dsee.on", "DSEE Extreme On", "DSEE on", ActionIcon.UPSCALING_ON)
    private val dseeOff = SupportedAction(
        "sony.wh1000xm6.dsee.off", "DSEE Extreme Off", "DSEE off", ActionIcon.UPSCALING_OFF,
    )
    private val lockThisPhone = SupportedAction(
        "sony.wh1000xm6.playback.lock_this_phone", "Play on This Phone", "This phone", ActionIcon.PLAYBACK_THIS_PHONE,
    )
    private val lockOtherDevice = SupportedAction(
        "sony.wh1000xm6.playback.lock_other_device", "Play on Other Device", "Other device", ActionIcon.PLAYBACK_OTHER_DEVICE,
    )
    private val lockCurrent = SupportedAction(
        "sony.wh1000xm6.playback.lock_current", "Lock Playback Device", "Lock device", ActionIcon.PLAYBACK_LOCK,
    )
    private val autoSwitch = SupportedAction(
        "sony.wh1000xm6.playback.auto_switch", "Auto Playback Switching", "Auto switch", ActionIcon.PLAYBACK_AUTO,
    )

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
        lockThisPhone,
        lockOtherDevice,
        lockCurrent,
        autoSwitch,
    )

    override val quickActionIds = listOf(noiseCancelling.id, ambientSound.id, speakToChatOff.id)

    // Gadgetbridge's SonyWH1000XM6Coordinator matches ".*WH-1000XM6.*". "LE_"-prefixed
    // entries are excluded: that is the LE Audio side of the headphones, while the Sony
    // protocol needs the Bluetooth Classic device.
    private val bluetoothNamePattern = Regex("(?!LE_).*WH-1000XM6.*")

    override fun matchesDeviceName(name: String): Boolean = bluetoothNamePattern.matches(name)

    override suspend fun testConnection(context: Context, device: RegisteredDevice): DeviceResult =
        SonyMdrConnection.open(context, device.address) { session ->
            DeviceResult.Success("Sony MDR protocol ${session.protocolVersion.name.lowercase()}, init reply ${session.initReply.toHex()}")
        }

    private val operations: Map<String, suspend (SonyMdrSession, RegisteredDevice) -> DeviceResult> = mapOf(
        noiseCancelling.id to { session, _ -> SonyNoiseControl.setMode(session, SonyNoiseControl.Mode.NOISE_CANCELLING) },
        ambientSound.id to { session, _ -> SonyNoiseControl.setMode(session, SonyNoiseControl.Mode.AMBIENT_SOUND) },
        noiseControlOff.id to { session, _ -> SonyNoiseControl.setMode(session, SonyNoiseControl.Mode.OFF) },
        speakToChatOn.id to { session, _ -> SonyMdrSwitch.speakToChat.set(session, enabled = true) },
        speakToChatOff.id to { session, _ -> SonyMdrSwitch.speakToChat.set(session, enabled = false) },
        dseeOn.id to { session, _ -> SonyMdrSwitch.dseeExtreme.set(session, enabled = true) },
        dseeOff.id to { session, _ -> SonyMdrSwitch.dseeExtreme.set(session, enabled = false) },
        lockThisPhone.id to { session, device ->
            SonyMultipoint.lockTo(session, missing = "This phone is not connected to the headphones") { list ->
                list.connected.find { it.address.equals(device.phoneAddress, ignoreCase = true) }
            }
        },
        lockOtherDevice.id to { session, device ->
            SonyMultipoint.lockTo(session, missing = "No other device is connected to the headphones") { list ->
                list.connected.find { !it.address.equals(device.phoneAddress, ignoreCase = true) }
            }
        },
        lockCurrent.id to { session, _ -> SonyMultipoint.lockCurrent(session) },
        autoSwitch.id to { session, _ -> SonyMultipoint.unlock(session) },
    )

    /** Actions that tell this phone and the other source device apart. */
    private val needsPhone = setOf(lockThisPhone.id, lockOtherDevice.id)

    override suspend fun execute(context: Context, device: RegisteredDevice, action: SupportedAction): DeviceResult {
        val operation = operations[action.id]
            ?: return DeviceResult.Failure("${action.label} is not supported by ${device.name}")
        if (action.id in needsPhone && device.phoneAddress == null) {
            return DeviceResult.Failure("Choose this phone in Headphone Actions first")
        }
        return SonyMdrConnection.open(context, device.address) { session -> operation(session, device) }
    }

    override val supportsMultipoint = true

    override suspend fun readSourceDevices(context: Context, device: RegisteredDevice): SourceDevicesResult =
        SonyMdrConnection.open(context, device.address, { SourceDevicesResult.Failure(it) }) { session ->
            when (val read = SonyMultipoint.readDevices(session)) {
                is SonyMultipoint.DeviceListResult.Read -> SourceDevicesResult.Success(
                    read.list.sources.map { SourceDevice(it.address, it.name, it.slot != 0, it == read.list.playing) },
                )
                is SonyMultipoint.DeviceListResult.Failed -> SourceDevicesResult.Failure(read.reason)
            }
        }
}
