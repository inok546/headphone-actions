// SPDX-License-Identifier: AGPL-3.0-only

package app.headphoneactions.routines

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import app.headphoneactions.AppPreferences
import app.headphoneactions.LOG_TAG
import app.headphoneactions.device.findModel

/**
 * Target of every published shortcut, i.e. what Samsung Modes and Routines launches.
 *
 * For now it only records the invocation; nothing is sent to the headphones.
 * Its theme is Theme.NoDisplay, so it must finish before onCreate returns.
 */
class RoutineActionActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handle(intent.getStringExtra(EXTRA_ACTION_ID))
        finish()
    }

    private fun handle(actionId: String?) {
        if (actionId == null) {
            Log.w(LOG_TAG, "Routine action ignored: no action ID in $intent")
            return
        }
        val preferences = AppPreferences(this)
        val model = preferences.registeredModelId?.let(::findModel)
        val action = model?.findAction(actionId)
        if (action == null) {
            Log.w(
                LOG_TAG,
                "Routine action ignored: $actionId is not supported by the registered device " +
                    "(${model?.id ?: "none registered"})",
            )
            return
        }
        preferences.recordRoutineAction(action.id, System.currentTimeMillis())
        Log.i(LOG_TAG, "Routine action invoked: ${action.id} (${action.label}) for ${model.displayName}")
    }

    companion object {
        private const val ACTION_RUN = "app.headphoneactions.action.RUN_HEADPHONE_ACTION"
        private const val EXTRA_ACTION_ID = "app.headphoneactions.extra.ACTION_ID"

        fun intentFor(context: Context, actionId: String): Intent =
            Intent(ACTION_RUN)
                .setClass(context, RoutineActionActivity::class.java)
                .putExtra(EXTRA_ACTION_ID, actionId)
    }
}
