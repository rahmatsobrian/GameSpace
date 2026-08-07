package com.siroha.gamespace.feature.blocker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.siroha.gamespace.R

@Composable
fun BlockerScreen(viewModel: BlockerViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPermissions()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
        Text(stringResource(R.string.blocker_title), style = MaterialTheme.typography.titleLarge)

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(top = 16.dp)
        ) {
            item {
                ToggleCard(
                    label = stringResource(R.string.blocker_notification_label),
                    desc = stringResource(R.string.blocker_notification_desc),
                    checked = state.notificationBlockingActive,
                    onToggle = viewModel::onNotificationBlockingToggle,
                    permissionMessage = if (!state.hasNotificationAccess) {
                        stringResource(R.string.blocker_notification_permission_message)
                    } else null,
                    onRequestPermission = viewModel::requestNotificationAccess
                )
            }
            item {
                if (!state.callScreeningAvailable) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(stringResource(R.string.blocker_call_label), style = MaterialTheme.typography.titleMedium)
                            Text(
                                stringResource(R.string.blocker_call_unavailable_message),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                } else {
                    ToggleCard(
                        label = stringResource(R.string.blocker_call_label),
                        desc = stringResource(R.string.blocker_call_desc),
                        checked = state.callBlockingActive,
                        onToggle = viewModel::onCallBlockingToggle,
                        permissionMessage = if (!state.hasCallScreeningRole) {
                            stringResource(R.string.blocker_call_permission_message)
                        } else null,
                        onRequestPermission = viewModel::requestCallScreeningRole
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleCard(
    label: String,
    desc: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    permissionMessage: String?,
    onRequestPermission: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.titleMedium)
                    Text(
                        desc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Switch(checked = checked, onCheckedChange = onToggle)
            }
            if (permissionMessage != null) {
                Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(permissionMessage, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = onRequestPermission) {
                        Text(stringResource(R.string.blocker_permission_action))
                    }
                }
            }
        }
    }
}
