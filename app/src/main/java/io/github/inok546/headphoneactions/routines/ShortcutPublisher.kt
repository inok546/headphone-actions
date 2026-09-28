// SPDX-License-Identifier: AGPL-3.0-only

package io.github.inok546.headphoneactions.routines

import android.content.Context
import android.util.Log
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import io.github.inok546.headphoneactions.LOG_TAG
import io.github.inok546.headphoneactions.R
import io.github.inok546.headphoneactions.device.HeadphoneModel
import io.github.inok546.headphoneactions.device.SupportedAction

/**
 * Publishes the registered model's actions as app shortcuts, which Samsung Modes and
 * Routines offers under "Open an app or do an app action".
 *
 * Call only in response to an explicit registration change, never because of Bluetooth
 * connection state: routines persist shortcut IDs and must keep finding them.
 */
object ShortcutPublisher {

    fun publish(context: Context, model: HeadphoneModel) {
        val icon = IconCompat.createWithResource(context, R.mipmap.ic_launcher)
        val shortcuts = model.actions.mapIndexed { index, action ->
            ShortcutInfoCompat.Builder(context, action.id)
                .setShortLabel(action.label)
                .setIcon(icon)
                .setIntent(RoutineActionActivity.intentFor(context, action.id))
                .setRank(index)
                .build()
        }
        val accepted = ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
        Log.i(
            LOG_TAG,
            "Published ${shortcuts.size} shortcuts for ${model.id} (accepted=$accepted): " +
                shortcuts.joinToString { it.id },
        )
    }

    fun removeAll(context: Context) {
        ShortcutManagerCompat.removeAllDynamicShortcuts(context)
        Log.i(LOG_TAG, "Removed all published shortcuts")
    }

    /** Actions as currently published, read back from the system rather than from the model. */
    fun publishedActions(context: Context): List<SupportedAction> =
        ShortcutManagerCompat.getDynamicShortcuts(context)
            .sortedBy { it.rank }
            .map { SupportedAction(it.id, it.shortLabel.toString()) }
}
