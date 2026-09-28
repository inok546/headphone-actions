// SPDX-License-Identifier: AGPL-3.0-only
//
// The ambient sound control opcodes (0x66-0x69) and the field order follow Gadgetbridge
// (https://codeberg.org/Freeyourgadget/Gadgetbridge), files
// service/devices/sony/headphones/protocol/impl/v2/SonyProtocolImplV2.java:
//   Copyright (C) 2022-2024 José Rebelo
// service/devices/sony/headphones/protocol/impl/v1/PayloadTypeV1.java:
//   Copyright (C) 2022-2024 José Rebelo
// Gadgetbridge is licensed under the GNU AGPL version 3 or later; used here under version 3.
//
// The WH-1000XM6 layout (inquired type 0x19, 9-byte payload, auto ambient sound flag) is
// documented in sonyctl's docs/MDR_PROBE_RESULTS.md (https://github.com/sevsev9/sonyctl, MIT).

package io.github.inok546.headphoneactions.device.sony.mdr

import android.util.Log
import io.github.inok546.headphoneactions.LOG_TAG
import io.github.inok546.headphoneactions.device.DeviceResult

/**
 * Noise cancelling / ambient sound control in the WH-1000XM6 layout: payloads
 * `code, 0x19, commit, enabled, ambient, focusOnVoice, ambientLevel, autoAmbient, 0x00`.
 * The WH-1000XM6 ignores the 0x15 layout Gadgetbridge uses for it.
 */
object SonyNoiseControl {

    private const val GET: Byte = 0x66
    private const val RET: Byte = 0x67
    private const val SET: Byte = 0x68
    private const val NOTIFY: Byte = 0x69
    private const val INQUIRED_TYPE: Byte = 0x19

    enum class Mode(val description: String) {
        OFF("noise control off"),
        NOISE_CANCELLING("noise cancelling on"),
        AMBIENT_SOUND("ambient sound on"),
    }

    /**
     * The fields as the headphones report them. While [enabled] is false the others keep the
     * last settings, including whether ambient sound was selected, and are sent back unchanged.
     */
    data class State(
        val enabled: Boolean,
        val ambientSelected: Boolean,
        val focusOnVoice: Boolean,
        val ambientLevel: Int,
        val autoAmbientSound: Boolean,
    ) {
        val mode: Mode
            get() = when {
                !enabled -> Mode.OFF
                ambientSelected -> Mode.AMBIENT_SOUND
                else -> Mode.NOISE_CANCELLING
            }

        fun withMode(mode: Mode): State = when (mode) {
            Mode.OFF -> copy(enabled = false)
            Mode.NOISE_CANCELLING -> copy(enabled = true, ambientSelected = false)
            Mode.AMBIENT_SOUND -> copy(enabled = true, ambientSelected = true)
        }
    }

    val getRequest = byteArrayOf(GET, INQUIRED_TYPE)

    fun setRequest(state: State): ByteArray = byteArrayOf(
        SET,
        INQUIRED_TYPE,
        0x01, // Commit; the Sony app sends 0x00 for preview frames while a slider is dragged.
        if (state.enabled) 0x01 else 0x00,
        if (state.ambientSelected) 0x01 else 0x00,
        if (state.focusOnVoice) 0x01 else 0x00,
        state.ambientLevel.toByte(),
        if (state.autoAmbientSound) 0x01 else 0x00,
        0x00,
    )

    /** Parses a RET or NOTIFY payload; null if it is not in the expected layout. */
    fun parse(payload: ByteArray): State? {
        if (payload.size != 9 || payload[1] != INQUIRED_TYPE) return null
        val enabled = flag(payload[3]) ?: return null
        val ambientSelected = flag(payload[4]) ?: return null
        val focusOnVoice = flag(payload[5]) ?: return null
        val ambientLevel = payload[6].toInt()
        if (ambientLevel !in 0..20) return null
        val autoAmbientSound = flag(payload[7]) ?: return null
        return State(enabled, ambientSelected, focusOnVoice, ambientLevel, autoAmbientSound)
    }

    private fun flag(value: Byte): Boolean? = when (value.toInt()) {
        0 -> false
        1 -> true
        else -> null
    }

    /**
     * Switches to [mode], keeping the other settings as they are, and confirms the change from
     * the notification the headphones send after it (or, failing that, by reading the state).
     */
    suspend fun setMode(session: SonyMdrSession, mode: Mode): DeviceResult {
        if (session.protocolVersion != SonyMdrProtocolVersion.V2) {
            return DeviceResult.Failure("Noise control is implemented for Sony protocol v2 only, not ${session.protocolVersion}")
        }
        val before = session.query(getRequest, RET)
            ?: return DeviceResult.Failure("No reply to the noise control query")
        val current = parse(before.payload)
            ?: return DeviceResult.Failure("Unexpected noise control state ${before.payload.toHex()}")
        Log.i(LOG_TAG, "Noise control before: $current")
        if (current.mode == mode) return DeviceResult.Success("Already: ${mode.description}")

        session.discard(NOTIFY)
        if (!session.command(setRequest(current.withMode(mode)))) {
            return DeviceResult.Failure("The headphones did not acknowledge the change")
        }

        val confirmation = session.await(NOTIFY) ?: session.query(getRequest, RET)
            ?: return DeviceResult.Failure("No confirmation of the change")
        val updated = parse(confirmation.payload)
            ?: return DeviceResult.Failure("Unexpected noise control state ${confirmation.payload.toHex()}")
        Log.i(LOG_TAG, "Noise control after: $updated")
        return if (updated.mode == mode) {
            DeviceResult.Success("Now: ${mode.description} (was: ${current.mode.description})")
        } else {
            DeviceResult.Failure("The headphones report ${updated.mode.description} after the change")
        }
    }
}
