// SPDX-License-Identifier: AGPL-3.0-only

package io.github.inok546.headphoneactions.device

/**
 * The headphones the user explicitly registered; the only device the app manages.
 * It stays registered whether or not it is currently connected.
 */
data class RegisteredDevice(val address: String, val name: String, val modelId: String)
