package com.siroha.gamespace.feature.booster

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
fun BoosterScreen(viewModel: BoosterViewModel = hiltViewModel()) {
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
        Text(stringResource(R.string.booster_title), style = MaterialTheme.typography.titleLarge)

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(top = 16.dp)
        ) {
            item {
                BrightnessCard(
                    percent = state.brightnessPercent,
                    hasPermission = state.hasBrightnessPermission,
                    onChange = viewModel::onBrightnessChange,
                    onRequestPermission = viewModel::requestBrightnessPermission
                )
            }
            item {
                DndCard(
                    enabled = state.dndEnabled,
                    hasPermission = state.hasDndPermission,
                    onToggle = viewModel::onDndToggle
                )
            }
            item {
                AnimationCard(
                    reduced = state.animationsReduced,
                    hasElevatedAccess = state.hasElevatedAccess,
                    onToggle = viewModel::onAnimationsToggle
                )
            }
            item {
                RefreshRateCard(
                    selected = state.selectedRefreshRate,
                    hasElevatedAccess = state.hasElevatedAccess,
                    onSelect = viewModel::onRefreshRateSelect
                )
            }
        }
    }
}

@Composable
private fun BrightnessCard(
    percent: Int,
    hasPermission: Boolean,
    onChange: (Int) -> Unit,
    onRequestPermission: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.booster_brightness_label), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text("$percent%", style = MaterialTheme.typography.titleMedium)
            }
            Slider(
                value = percent.toFloat(),
                onValueChange = { onChange(it.toInt()) },
                valueRange = 0f..100f
            )
            if (!hasPermission) {
                PermissionRow(stringResource(R.string.booster_brightness_permission_message), onRequestPermission)
            }
        }
    }
}

@Composable
private fun DndCard(enabled: Boolean, hasPermission: Boolean, onToggle: (Boolean) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.booster_dnd_label), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.booster_dnd_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Switch(checked = enabled, onCheckedChange = onToggle)
            }
            if (!hasPermission) {
                PermissionRow(stringResource(R.string.booster_dnd_permission_message)) { onToggle(true) }
            }
        }
    }
}

@Composable
private fun AnimationCard(reduced: Boolean, hasElevatedAccess: Boolean, onToggle: (Boolean) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.booster_animation_label), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.booster_animation_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Switch(checked = reduced, onCheckedChange = onToggle, enabled = hasElevatedAccess)
            }
            if (!hasElevatedAccess) {
                Text(
                    stringResource(R.string.booster_elevated_required_message),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun RefreshRateCard(selected: Int?, hasElevatedAccess: Boolean, onSelect: (Int?) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.booster_refresh_rate_label), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.booster_refresh_rate_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
            ) {
                listOf(null, 60, 90, 120).forEach { option ->
                    FilterChip(
                        selected = selected == option,
                        onClick = { onSelect(option) },
                        label = { Text(option?.let { "${it}Hz" } ?: "Auto") },
                        enabled = hasElevatedAccess
                    )
                }
            }
            if (!hasElevatedAccess) {
                Text(
                    stringResource(R.string.booster_elevated_required_message),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(message: String, onRequest: () -> Unit) {
    Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        TextButton(onClick = onRequest) { Text(stringResource(R.string.booster_permission_action)) }
    }
}
