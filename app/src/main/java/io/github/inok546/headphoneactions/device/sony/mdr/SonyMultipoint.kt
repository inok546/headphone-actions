// SPDX-License-Identifier: AGPL-3.0-only
//
// Multipoint control lives in the second command table (message type COMMAND_2). The opcodes
// and payload layouts are protocol facts taken from Gadgetbridge pull request #6517
// (https://codeberg.org/Freeyourgadget/Gadgetbridge/pulls/6517, multipoint for the WH-1000XM5)
// and from BudsLink (https://github.com/maniacx/BudsLink, GPL-3.0, sonySocketV2.js), which
// enables it for the WH-1000XM6. See NOTICE.md.

package io.github.inok546.headphoneactions.device.sony.mdr

import android.util.Log
import io.github.inok546.headphoneactions.LOG_TAG
import io.github.inok546.headphoneactions.device.DeviceResult

/**
 * Switching between the two source devices connected to the headphones and locking playback
 * to one of them, so that playback started on the other device does not take over.
 */
object SonyMultipoint {

    private const val DEVICES_GET: Byte = 0x36
    private const val DEVICES_RET: Byte = 0x37
    private const val PARAMETER_SET: Byte = 0x38
    private const val DEVICES_NOTIFY: Byte = 0x39
    private const val CONNECTION_SET: Byte = 0x3c
    private const val CONNECTION_NOTIFY: Byte = 0x3d

    /** Paired-device list inquiry types: Gadgetbridge uses 0x00, BudsLink 0x02 on some models. */
    private val DEVICE_LIST_TYPES = listOf<Byte>(0x00, 0x02)

    /** Inquiry type of the active (playing) device and of its lock. */
    private const val ACTIVE_DEVICE: Byte = 0x01

    private const val ADDRESS_LENGTH = 17
    private val ADDRESS_PATTERN = Regex("([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}")
    private const val COMMAND_2 = SonyMdrMessage.TYPE_COMMAND_2

    /** A source device the headphones know, by the address they report; [slot] is 0 while disconnected. */
    data class Source(val address: String, val name: String, val slot: Int)

    /** [playingSlot] is null if the headphones do not report which source is playing. */
    data class DeviceList(val sources: List<Source>, val playingSlot: Int?) {
        val connected: List<Source> get() = sources.filter { it.slot != 0 }
        val playing: Source? get() = playingSlot?.let { slot -> sources.find { it.slot == slot } }
    }

    sealed interface DeviceListResult {
        data class Read(val list: DeviceList) : DeviceListResult
        data class Failed(val reason: String) : DeviceListResult
    }

    fun deviceListRequest(type: Byte) = byteArrayOf(DEVICES_GET, type)

    fun switchRequest(address: String): ByteArray =
        byteArrayOf(CONNECTION_SET, ACTIVE_DEVICE) + address.toByteArray(Charsets.US_ASCII)

    /** The lock flag is inverted: 0x00 locks the playback device, 0x01 allows automatic switching. */
    fun lockRequest(locked: Boolean) = byteArrayOf(PARAMETER_SET, ACTIVE_DEVICE, if (locked) 0x00 else 0x01)

    /**
     * Parses `[0x37|0x39, type, count, {address, status…, nameLength, name}×count, playingSlot?]`.
     * The status takes 4 bytes (BudsLink) or 1 byte (one of Gadgetbridge's variants).
     */
    fun parseDeviceList(payload: ByteArray): DeviceList? {
        if (payload.size < 3) return null
        val count = payload[2].toInt() and 0xff
        return parseDeviceList(payload, count, statusLength = 4) ?: parseDeviceList(payload, count, statusLength = 1)
    }

    private fun parseDeviceList(payload: ByteArray, count: Int, statusLength: Int): DeviceList? {
        var i = 3
        val sources = mutableListOf<Source>()
        repeat(count) {
            if (i + ADDRESS_LENGTH + statusLength + 1 > payload.size) return null
            val address = String(payload, i, ADDRESS_LENGTH, Charsets.US_ASCII)
            if (!ADDRESS_PATTERN.matches(address)) return null
            i += ADDRESS_LENGTH
            val slot = payload[i].toInt() and 0xff
            i += statusLength
            val nameLength = payload[i++].toInt() and 0xff
            if (i + nameLength > payload.size) return null
            sources += Source(address, String(payload, i, nameLength, Charsets.UTF_8), slot)
            i += nameLength
        }
        val playingSlot = when (payload.size - i) {
            0 -> null
            1 -> payload[i].toInt() and 0xff
            else -> return null
        }
        return DeviceList(sources, playingSlot)
    }

    /** The address in an active device notification; BudsLink reads it at offset 2, Gadgetbridge at 3. */
    fun parseSwitchNotification(payload: ByteArray): String? =
        listOf(2, 3).firstNotNullOfOrNull { offset ->
            if (payload.size < offset + ADDRESS_LENGTH) return@firstNotNullOfOrNull null
            String(payload, offset, ADDRESS_LENGTH, Charsets.US_ASCII).takeIf { ADDRESS_PATTERN.matches(it) }
        }

    /** Whether a `[0x37|0x39, 0x01, flag, …]` payload reports the playback device locked. */
    fun parseLock(payload: ByteArray): Boolean? {
        if (payload.size < 3 || payload[1] != ACTIVE_DEVICE) return null
        return when (payload[2].toInt()) {
            0 -> true
            1 -> false
            else -> null
        }
    }

    suspend fun readDevices(session: SonyMdrSession): DeviceListResult {
        if (session.protocolVersion != SonyMdrProtocolVersion.V2) {
            return DeviceListResult.Failed("Multipoint is implemented for Sony protocol v2 only, not ${session.protocolVersion}")
        }
        for (type in DEVICE_LIST_TYPES) {
            val reply = session.query(deviceListRequest(type), DEVICES_RET, COMMAND_2) ?: continue
            val list = parseDeviceList(reply.payload)
                ?: return DeviceListResult.Failed("Unexpected device list ${reply.payload.toHex()}")
            Log.i(LOG_TAG, "Multipoint devices: $list")
            return DeviceListResult.Read(list)
        }
        return DeviceListResult.Failed("No reply to the device list query")
    }

    /** Locks playback to whichever source is playing now. */
    suspend fun lockCurrent(session: SonyMdrSession): DeviceResult = withDeviceList(session) { list ->
        setLock(session, locked = true, description = "Locked to ${list.playing?.name ?: "the current device"}")
    }

    /** Lets the headphones switch to whichever source starts playing. */
    suspend fun unlock(session: SonyMdrSession): DeviceResult = withDeviceList(session) {
        setLock(session, locked = false, description = "Automatic switching on")
    }

    /**
     * Switches playback to the connected source chosen by [pick], then locks it there.
     * [missing] explains why nothing was picked.
     */
    suspend fun lockTo(
        session: SonyMdrSession,
        missing: String,
        pick: (DeviceList) -> Source?,
    ): DeviceResult = withDeviceList(session) { list ->
        val target = pick(list) ?: return@withDeviceList DeviceResult.Failure(missing)
        if (list.playing?.address?.equals(target.address, ignoreCase = true) != true) {
            switchTo(session, target)?.let { return@withDeviceList it }
        }
        setLock(session, locked = true, description = "Locked to ${target.name}")
    }

    private suspend fun withDeviceList(
        session: SonyMdrSession,
        operation: suspend (DeviceList) -> DeviceResult,
    ): DeviceResult = when (val result = readDevices(session)) {
        is DeviceListResult.Read -> operation(result.list)
        is DeviceListResult.Failed -> DeviceResult.Failure(result.reason)
    }

    /** Returns a failure, or null once the headphones have accepted the switch. */
    private suspend fun switchTo(session: SonyMdrSession, target: Source): DeviceResult? {
        session.discard(CONNECTION_NOTIFY, ACTIVE_DEVICE)
        if (!session.command(switchRequest(target.address), COMMAND_2)) {
            return DeviceResult.Failure("The headphones did not acknowledge switching to ${target.name}")
        }
        val switchedTo = session.await(CONNECTION_NOTIFY, ACTIVE_DEVICE)?.let { parseSwitchNotification(it.payload) }
        Log.i(LOG_TAG, "Multipoint switch to ${target.address}: notified ${switchedTo ?: "nothing"}")
        if (switchedTo != null && !switchedTo.equals(target.address, ignoreCase = true)) {
            return DeviceResult.Failure("The headphones switched to $switchedTo instead of ${target.name}")
        }
        return null
    }

    private suspend fun setLock(session: SonyMdrSession, locked: Boolean, description: String): DeviceResult {
        session.discard(DEVICES_RET, ACTIVE_DEVICE)
        session.discard(DEVICES_NOTIFY, ACTIVE_DEVICE)
        if (!session.command(lockRequest(locked), COMMAND_2)) {
            return DeviceResult.Failure("The headphones did not acknowledge the change")
        }
        val report = session.awaitAny(setOf(DEVICES_RET, DEVICES_NOTIFY), ACTIVE_DEVICE)
        val reportedLocked = report?.let { parseLock(it.payload) }
        Log.i(LOG_TAG, "Multipoint lock ${if (locked) "on" else "off"}: reported ${reportedLocked ?: "nothing"}")
        return when (reportedLocked) {
            null -> DeviceResult.Success("$description (not confirmed by the headphones)")
            locked -> DeviceResult.Success(description)
            else -> DeviceResult.Failure("The headphones report automatic switching ${if (reportedLocked == true) "off" else "on"} after the change")
        }
    }
}
