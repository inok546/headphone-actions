// SPDX-License-Identifier: AGPL-3.0-only
//
// The Speak-to-Chat and audio upsampling (DSEE) payload layouts follow Gadgetbridge
// (https://codeberg.org/Freeyourgadget/Gadgetbridge), files
// service/devices/sony/headphones/protocol/impl/v2/SonyProtocolImplV2.java:
//   Copyright (C) 2022-2024 José Rebelo
// service/devices/sony/headphones/protocol/impl/v1/PayloadTypeV1.java:
//   Copyright (C) 2022-2024 José Rebelo
// Gadgetbridge is licensed under the GNU AGPL version 3 or later; used here under version 3.
//
// BudsLink (https://github.com/maniacx/BudsLink, GPL-3.0) uses the same layouts and enables
// both settings for the WH-1000XM6.

package io.github.inok546.headphoneactions.device.sony.mdr

import android.util.Log
import io.github.inok546.headphoneactions.LOG_TAG
import io.github.inok546.headphoneactions.device.DeviceResult

/**
 * An on/off setting read with `[get, type]`, changed with `[set, type, …]` and confirmed by the
 * `[notify, type, …]` message the headphones send after a change (or by reading it again).
 */
class SonyMdrSwitch(
    private val name: String,
    private val getCode: Byte,
    private val type: Byte,
    private val parseEnabled: (ByteArray) -> Boolean?,
    private val setPayload: (Boolean) -> ByteArray,
) {
    // As for every Sony MDR parameter, RET, SET and NOTIFY follow GET.
    private val retCode = (getCode + 1).toByte()
    private val setCode = (getCode + 2).toByte()
    private val notifyCode = (getCode + 3).toByte()

    val getRequest = byteArrayOf(getCode, type)

    fun setRequest(enabled: Boolean): ByteArray = byteArrayOf(setCode, type) + setPayload(enabled)

    /** Whether a RET or NOTIFY payload reports the setting on; null if it is not in the expected layout. */
    fun parse(payload: ByteArray): Boolean? =
        if (payload.size >= 2 && payload[1] == type) parseEnabled(payload) else null

    suspend fun set(session: SonyMdrSession, enabled: Boolean): DeviceResult {
        if (session.protocolVersion != SonyMdrProtocolVersion.V2) {
            return DeviceResult.Failure("$name is implemented for Sony protocol v2 only, not ${session.protocolVersion}")
        }
        val before = session.query(getRequest, retCode)
            ?: return DeviceResult.Failure("No reply to the $name query")
        val current = parse(before.payload)
            ?: return DeviceResult.Failure("Unexpected $name state ${before.payload.toHex()}")
        Log.i(LOG_TAG, "$name before: ${state(current)}")
        if (current == enabled) return DeviceResult.Success("Already: $name ${state(enabled)}")

        session.discard(notifyCode, type)
        if (!session.command(setRequest(enabled))) {
            return DeviceResult.Failure("The headphones did not acknowledge the change")
        }

        val confirmation = session.await(notifyCode, type) ?: session.query(getRequest, retCode)
            ?: return DeviceResult.Failure("No confirmation of the change")
        val updated = parse(confirmation.payload)
            ?: return DeviceResult.Failure("Unexpected $name state ${confirmation.payload.toHex()}")
        Log.i(LOG_TAG, "$name after: ${state(updated)}")
        return if (updated == enabled) {
            DeviceResult.Success("Now: $name ${state(enabled)} (was: ${state(current)})")
        } else {
            DeviceResult.Failure("The headphones report $name ${state(updated)} after the change")
        }
    }

    private fun state(enabled: Boolean) = if (enabled) "on" else "off"

    companion object {

        /** `[0xF6/0xF7/0xF8/0xF9, 0x0C, disabled, 0x01]`: the flag is inverted, 0x00 means on. */
        val speakToChat = SonyMdrSwitch(
            name = "Speak-to-Chat",
            getCode = 0xf6.toByte(),
            type = 0x0c,
            parseEnabled = { payload -> if (payload.size == 4) flag(payload[2])?.not() else null },
            setPayload = { enabled -> byteArrayOf(if (enabled) 0x00 else 0x01, 0x01) },
        )

        /** `[0xE6/0xE7/0xE8/0xE9, 0x01, enabled]`. */
        val dseeExtreme = SonyMdrSwitch(
            name = "DSEE Extreme",
            getCode = 0xe6.toByte(),
            type = 0x01,
            parseEnabled = { payload -> if (payload.size == 3) flag(payload[2]) else null },
            setPayload = { enabled -> byteArrayOf(if (enabled) 0x01 else 0x00) },
        )

        private fun flag(value: Byte): Boolean? = when (value.toInt()) {
            0 -> false
            1 -> true
            else -> null
        }
    }
}
