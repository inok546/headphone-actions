// SPDX-License-Identifier: AGPL-3.0-only

package io.github.inok546.headphoneactions.device.sony.mdr

import io.github.inok546.headphoneactions.device.sony.mdr.SonyMultipoint.Source
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SonyMultipointTest {

    private val phone = "AA:BB:CC:DD:EE:01"
    private val laptop = "AA:BB:CC:DD:EE:02"

    @Test
    fun `parses the device list with 4 status bytes and the playing slot`() {
        val payload = bytes(0x37, 0x00, 0x02) +
            entry(phone, slot = 1, statusLength = 4, name = "Phone A") +
            entry(laptop, slot = 2, statusLength = 4, name = "Laptop") +
            bytes(0x02)

        val list = SonyMultipoint.parseDeviceList(payload)!!

        assertEquals(listOf(Source(phone, "Phone A", 1), Source(laptop, "Laptop", 2)), list.sources)
        assertEquals(laptop, list.playing?.address)
    }

    @Test
    fun `parses the device list with 1 status byte and no playing slot`() {
        val payload = bytes(0x37, 0x00, 0x02) +
            entry(phone, slot = 1, statusLength = 1, name = "Phone A") +
            entry(laptop, slot = 0, statusLength = 1, name = "Laptop")

        val list = SonyMultipoint.parseDeviceList(payload)!!

        assertEquals(listOf(phone), list.connected.map { it.address })
        assertNull(list.playing)
    }

    @Test
    fun `rejects a malformed device list`() {
        assertNull(SonyMultipoint.parseDeviceList(bytes(0x37, 0x00, 0x01) + "not an address!!!".toByteArray()))
        assertNull(SonyMultipoint.parseDeviceList(bytes(0x37, 0x00)))
    }

    @Test
    fun `requests`() {
        assertEquals("36:00", SonyMultipoint.deviceListRequest(0x00).toHex())
        assertEquals("3c:01:" + phone.toByteArray().toHex(), SonyMultipoint.switchRequest(phone).toHex())
        assertEquals("38:01:00", SonyMultipoint.lockRequest(locked = true).toHex())
        assertEquals("38:01:01", SonyMultipoint.lockRequest(locked = false).toHex())
    }

    @Test
    fun `finds the address in switch notifications of both layouts`() {
        assertEquals(phone, SonyMultipoint.parseSwitchNotification(bytes(0x3d, 0x01) + phone.toByteArray()))
        assertEquals(phone, SonyMultipoint.parseSwitchNotification(bytes(0x3d, 0x01, 0x00) + phone.toByteArray()))
        assertNull(SonyMultipoint.parseSwitchNotification(bytes(0x3d, 0x01, 0x00)))
    }

    @Test
    fun `parses the lock state`() {
        assertEquals(true, SonyMultipoint.parseLock(bytes(0x37, 0x01, 0x00, 0x00)))
        assertEquals(false, SonyMultipoint.parseLock(bytes(0x39, 0x01, 0x01)))
        assertNull(SonyMultipoint.parseLock(bytes(0x37, 0x00, 0x00)))
        assertNull(SonyMultipoint.parseLock(bytes(0x37, 0x01, 0x02)))
    }

    private fun entry(address: String, slot: Int, statusLength: Int, name: String): ByteArray =
        address.toByteArray() + bytes(slot) + ByteArray(statusLength - 1) + bytes(name.length) + name.toByteArray()

    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }
}
