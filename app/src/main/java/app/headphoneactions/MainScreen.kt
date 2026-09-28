// SPDX-License-Identifier: AGPL-3.0-only

package app.headphoneactions

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import app.headphoneactions.device.HeadphoneModel
import app.headphoneactions.device.SupportedAction
import app.headphoneactions.device.registrableModels
import java.text.DateFormat
import java.util.Date

data class MainUiState(
    val registeredModel: HeadphoneModel? = null,
    val publishedActions: List<SupportedAction> = emptyList(),
    val lastRoutineAction: LastRoutineAction? = null,
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(),
        content = content,
    )
}

@Composable
fun MainScreen(
    state: MainUiState,
    onRegister: (HeadphoneModel) -> Unit,
    onRemoveRegistration: () -> Unit,
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
            RegistrationSection(state.registeredModel, onRegister, onRemoveRegistration)
            PublishedActionsSection(state.publishedActions)
            LastRoutineActionSection(state.lastRoutineAction, state.registeredModel)
        }
    }
}

@Composable
private fun RegistrationSection(
    registeredModel: HeadphoneModel?,
    onRegister: (HeadphoneModel) -> Unit,
    onRemoveRegistration: () -> Unit,
) {
    Section(stringResource(R.string.registered_device_title)) {
        if (registeredModel == null) {
            Text(stringResource(R.string.registered_device_none))
            registrableModels.forEach { model ->
                Button(onClick = { onRegister(model) }) {
                    Text(stringResource(R.string.register_model, model.displayName))
                }
            }
        } else {
            Text(registeredModel.displayName, style = MaterialTheme.typography.bodyLarge)
            OutlinedButton(onClick = onRemoveRegistration) {
                Text(stringResource(R.string.remove_registration))
            }
        }
    }
}

@Composable
private fun PublishedActionsSection(actions: List<SupportedAction>) {
    Section(stringResource(R.string.published_actions_title)) {
        if (actions.isEmpty()) {
            Text(stringResource(R.string.published_actions_none))
        }
        actions.forEach { action ->
            Column {
                Text(action.label, style = MaterialTheme.typography.bodyLarge)
                ActionId(action.id)
            }
        }
    }
}

@Composable
private fun LastRoutineActionSection(lastAction: LastRoutineAction?, registeredModel: HeadphoneModel?) {
    Section(stringResource(R.string.last_routine_action_title)) {
        if (lastAction == null) {
            Text(stringResource(R.string.last_routine_action_none))
            return@Section
        }
        registeredModel?.findAction(lastAction.actionId)?.let {
            Text(it.label, style = MaterialTheme.typography.bodyLarge)
        }
        ActionId(lastAction.actionId)
        val invokedAt = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM)
            .format(Date(lastAction.invokedAtMillis))
        Text(stringResource(R.string.last_routine_action_time, invokedAt))
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        content()
    }
}

@Composable
private fun ActionId(id: String) {
    Text(
        id,
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
