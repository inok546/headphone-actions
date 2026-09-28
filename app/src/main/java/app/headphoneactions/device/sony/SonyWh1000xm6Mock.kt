// SPDX-License-Identifier: AGPL-3.0-only

package app.headphoneactions.device.sony

import app.headphoneactions.device.HeadphoneModel
import app.headphoneactions.device.SupportedAction

/**
 * Temporary stand-in for a real WH-1000XM6 driver: a fixed action list with no
 * Bluetooth behind it. The real driver must keep these action IDs.
 */
val sonyWh1000xm6Mock = HeadphoneModel(
    id = "sony.wh1000xm6",
    displayName = "Sony WH-1000XM6",
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
