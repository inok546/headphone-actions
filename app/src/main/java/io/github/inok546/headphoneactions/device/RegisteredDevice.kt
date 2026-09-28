// SPDX-License-Identifier: AGPL-3.0-only

package io.github.inok546.headphoneactions.device

/**
 * The headphones the user explicitly registered; the only device the app manages.
 * It stays registered whether or not it is currently connected.
 *
 * [phoneAddress] and [phoneName] identify this phone among the source devices the headphones
 * know (multipoint). The user picks them in the app: Android does not tell apps the phone's
 * own Bluetooth address.
 */
data class RegisteredDevice(
    val address: String,
    val name: String,
    val modelId: String,
    val phoneAddress: String? = null,
    val phoneName: String? = null,
)
