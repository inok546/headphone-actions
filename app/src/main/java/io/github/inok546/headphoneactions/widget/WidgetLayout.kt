// SPDX-License-Identifier: AGPL-3.0-only

package io.github.inok546.headphoneactions.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.action.actionStartService
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.components.FilledButton
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import io.github.inok546.headphoneactions.MainActivity
import io.github.inok546.headphoneactions.R
import io.github.inok546.headphoneactions.device.ActionIcon
import io.github.inok546.headphoneactions.device.SupportedAction
import io.github.inok546.headphoneactions.routines.ActionExecutionService

/** Icon buttons need a 48 dp touch target; a 1-row widget shows as many as fit, in the user's order. */
private val ICON_BUTTON_SIZE = 48.dp

/** Tiles with a label are at least this wide; they are used from about 2 cells of height on. */
private val TILE_MIN_WIDTH = 72.dp
private val TILES_MIN_HEIGHT = 140.dp

/**
 * Size breakpoints for [androidx.glance.appwidget.SizeMode.Responsive]: widths from 2 to 4+ cells
 * (130 dp and 276 dp per the official 73n − 16 formula), each as a single row of icons and as
 * tiles. The launcher picks the largest breakpoint that fits.
 */
internal val responsiveSizes: Set<DpSize> =
    listOf(110.dp, 180.dp, 250.dp, 320.dp)
        .flatMap { width -> listOf(DpSize(width, 40.dp), DpSize(width, TILES_MIN_HEIGHT)) }
        .toSet()

@Composable
internal fun WidgetLayout(content: WidgetContent, appWidgetId: Int) {
    val context = LocalContext.current
    Scaffold(horizontalPadding = 4.dp) {
        when (content) {
            WidgetContent.NoDevice -> Message(
                title = R.string.widget_no_device,
                detail = null,
                button = R.string.widget_open_app,
                onClick = actionStartActivity(Intent(context, MainActivity::class.java)),
            )
            WidgetContent.ReconfigurationRequired -> Message(
                title = R.string.widget_headphones_changed,
                detail = R.string.widget_configure_again,
                button = R.string.widget_configure,
                onClick = actionStartActivity(configureIntent(context, appWidgetId)),
            )
            is WidgetContent.Buttons ->
                if (LocalSize.current.height >= TILES_MIN_HEIGHT) Tiles(content.actions) else IconRow(content.actions)
        }
    }
}

@Composable
private fun IconRow(actions: List<SupportedAction>) {
    val context = LocalContext.current
    val fitting = ((LocalSize.current.width - 8.dp) / ICON_BUTTON_SIZE).toInt().coerceAtLeast(1)
    Row(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        actions.take(fitting).forEach { action ->
            Box(modifier = GlanceModifier.defaultWeight(), contentAlignment = Alignment.Center) {
                CircleIconButton(
                    imageProvider = ImageProvider(action.icon.drawable()),
                    contentDescription = action.label,
                    onClick = executeAction(context, action),
                    backgroundColor = GlanceTheme.colors.secondaryContainer,
                    contentColor = GlanceTheme.colors.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun Tiles(actions: List<SupportedAction>) {
    val columns = ((LocalSize.current.width - 8.dp) / TILE_MIN_WIDTH).toInt().coerceAtLeast(1)
    val rows = if (actions.size > columns) 2 else 1
    Column(modifier = GlanceModifier.fillMaxSize().padding(vertical = 4.dp)) {
        actions.take(columns * rows).chunked(columns).forEach { rowActions ->
            Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                rowActions.forEach { Tile(it, GlanceModifier.defaultWeight()) }
                // Keeps tiles of a shorter last row as wide as the ones above.
                repeat(columns - rowActions.size) { Spacer(GlanceModifier.defaultWeight()) }
            }
        }
    }
}

@Composable
private fun Tile(action: SupportedAction, modifier: GlanceModifier) {
    val context = LocalContext.current
    Box(modifier = modifier.fillMaxHeight().padding(3.dp)) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(16.dp)
                .background(GlanceTheme.colors.secondaryContainer)
                .clickable(executeAction(context, action)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                provider = ImageProvider(action.icon.drawable()),
                contentDescription = action.label,
                modifier = GlanceModifier.size(24.dp),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onSecondaryContainer),
            )
            Text(
                text = action.shortLabel,
                modifier = GlanceModifier.padding(top = 4.dp, start = 4.dp, end = 4.dp),
                style = TextStyle(
                    color = GlanceTheme.colors.onSecondaryContainer,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun Message(@StringRes title: Int, @StringRes detail: Int?, @StringRes button: Int, onClick: Action) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = context.getString(title),
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
        )
        if (detail != null && LocalSize.current.height >= TILES_MIN_HEIGHT) {
            Text(
                text = context.getString(detail),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp, textAlign = TextAlign.Center),
                maxLines = 2,
            )
        }
        Spacer(GlanceModifier.height(4.dp))
        FilledButton(text = context.getString(button), onClick = onClick, maxLines = 1)
    }
}

/** Starts the connectedDevice foreground service directly; a widget click may start one. */
private fun executeAction(context: Context, action: SupportedAction): Action =
    actionStartService(ActionExecutionService.intentFor(context, action.id), isForegroundService = true)

private fun configureIntent(context: Context, appWidgetId: Int): Intent =
    Intent(context, WidgetConfigActivity::class.java).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

@DrawableRes
internal fun ActionIcon.drawable(): Int = when (this) {
    ActionIcon.GENERIC -> R.drawable.ic_action_generic
    ActionIcon.NOISE_CANCELLING -> R.drawable.ic_action_noise_cancelling
    ActionIcon.AMBIENT_SOUND -> R.drawable.ic_action_ambient_sound
    ActionIcon.NOISE_CONTROL_OFF -> R.drawable.ic_action_noise_control_off
    ActionIcon.SPEAK_TO_CHAT_ON -> R.drawable.ic_action_speak_to_chat_on
    ActionIcon.SPEAK_TO_CHAT_OFF -> R.drawable.ic_action_speak_to_chat_off
    ActionIcon.UPSCALING_ON -> R.drawable.ic_action_upscaling_on
    ActionIcon.UPSCALING_OFF -> R.drawable.ic_action_upscaling_off
    ActionIcon.PLAYBACK_THIS_PHONE -> R.drawable.ic_action_playback_this_phone
    ActionIcon.PLAYBACK_OTHER_DEVICE -> R.drawable.ic_action_playback_other_device
    ActionIcon.PLAYBACK_LOCK -> R.drawable.ic_action_playback_lock
    ActionIcon.PLAYBACK_AUTO -> R.drawable.ic_action_playback_auto
}
