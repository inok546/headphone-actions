// SPDX-License-Identifier: AGPL-3.0-only

package io.github.inok546.headphoneactions.device

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Runs operations on the headphones one at a time, whether they come from the app's buttons
 * or from Samsung Routines, so two Bluetooth sessions never overlap.
 */
object DeviceAccess {

    private val mutex = Mutex()

    suspend fun <T> exclusive(operation: suspend () -> T): T = mutex.withLock { operation() }
}
