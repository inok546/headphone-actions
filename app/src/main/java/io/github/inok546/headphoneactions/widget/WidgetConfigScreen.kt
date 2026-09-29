// SPDX-License-Identifier: AGPL-3.0-only

package io.github.inok546.headphoneactions.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.inok546.headphoneactions.R

/**
 * [rows] is null when no headphones are registered (and while the stored buttons load). The
 * order of the checked rows is the order of the widget's buttons.
 */
@Composable
fun WidgetConfigScreen(
    deviceName: String?,
    rows: List<ConfigRow>?,
    onRowsChange: (List<ConfigRow>) -> Unit,
    onSave: () -> Unit,
    onOpenApp: () -> Unit,
) {
    Scaffold(
        bottomBar = {
            if (rows != null) {
                Button(
                    onClick = onSave,
                    enabled = rows.any { it.selected },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) {
                    Text(stringResource(R.string.widget_config_save))
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.widget_config_title), style = MaterialTheme.typography.headlineSmall)
            if (deviceName == null) {
                Text(stringResource(R.string.widget_config_no_device))
                Button(onClick = onOpenApp) { Text(stringResource(R.string.widget_open_app)) }
                return@Column
            }
            Text(stringResource(R.string.widget_config_hint, deviceName))
            rows?.forEachIndexed { index, row ->
                ConfigRowItem(
                    row = row,
                    canMoveUp = index > 0,
                    canMoveDown = index < rows.lastIndex,
                    onSelectedChange = { selected ->
                        onRowsChange(rows.toMutableList().also { it[index] = row.copy(selected = selected) })
                    },
                    onMove = { offset -> onRowsChange(rows.moved(index, offset)) },
                )
            }
        }
    }
}

@Composable
private fun ConfigRowItem(
    row: ConfigRow,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    onMove: (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = row.selected, onCheckedChange = onSelectedChange)
        Icon(painterResource(row.action.icon.drawable()), contentDescription = null)
        Text(
            text = row.action.label,
            modifier = Modifier.weight(1f).padding(start = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
        IconButton(onClick = { onMove(-1) }, enabled = canMoveUp) {
            Icon(painterResource(R.drawable.ic_move_up), contentDescription = stringResource(R.string.widget_config_move_up))
        }
        IconButton(onClick = { onMove(1) }, enabled = canMoveDown) {
            Icon(painterResource(R.drawable.ic_move_down), contentDescription = stringResource(R.string.widget_config_move_down))
        }
    }
}
