// SPDX-License-Identifier: AGPL-3.0-only

package io.github.inok546.headphoneactions.device.sony

import io.github.inok546.headphoneactions.device.HeadphoneModel
import io.github.inok546.headphoneactions.device.SupportedAction

/**
 * Sony WH-1000XM6. The actions are declared but not executed yet: the app has no
 * Sony protocol support so far. The action IDs are published and must not change.
 */
val sonyWh1000xm6 = HeadphoneModel(
    id = "sony.wh1000xm6",
    displayName = "Sony WH-1000XM6",
    // Gadgetbridge's SonyWH1000XM6Coordinator matches ".*WH-1000XM6.*". "LE_"-prefixed
    // entries are excluded: that is the LE Audio side of the headphones, while the Sony
    // protocol needs the Bluetooth Classic device.
    bluetoothNamePattern = Regex("(?!LE_).*WH-1000XM6.*"),
    actions = listOf(
        SupportedAction("sony.wh1000xm6.noise_control.anc", "Noise Cancelling"),
        SupportedAction("sony.wh1000xm6.noise_control.ambient", "Ambient Sound"),
        SupportedAction("sony.wh1000xm6.noise_control.off", "Noise Control Off"),
        SupportedAction("sony.wh1000xm6.speak_to_chat.on", "Speak-to-Chat On"),
        SupportedAction("sony.wh1000xm6.speak_to_chat.off", "Speak-to-Chat Off"),
        SupportedAction("sony.wh1000xm6.dsee.on", "DSEE Extreme On"),
        SupportedAction("sony.wh1000xm6.dsee.off", "DSEE Extreme Off"),
    ),
)
