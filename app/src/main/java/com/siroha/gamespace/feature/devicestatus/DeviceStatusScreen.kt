package com.siroha.gamespace.feature.devicestatus

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.siroha.gamespace.R
import com.siroha.gamespace.core.privilege.PrivilegeTier
import com.siroha.gamespace.core.theme.DataTextStyles
import com.siroha.gamespace.core.theme.MetricBattery
import com.siroha.gamespace.core.theme.MetricCpu
import com.siroha.gamespace.core.theme.MetricRam
import com.siroha.gamespace.core.theme.StatusNeutral
import com.siroha.gamespace.core.theme.ThermalHot
import com.siroha.gamespace.core.theme.ThermalNormal
import com.siroha.gamespace.core.theme.ThermalWarm
import com.siroha.gamespace.data.monitoring.ChargePlug
import com.siroha.gamespace.data.monitoring.CpuSnapshot
import com.siroha.gamespace.data.monitoring.DeviceSnapshot
import com.siroha.gamespace.data.monitoring.ThermalSnapshot
import com.siroha.gamespace.data.monitoring.ThermalStatus

@Composable
fun DeviceStatusScreen(
    onOpenSystemAccess: () -> Unit,
    onOpenBooster: () -> Unit,
    onOpenBlocker: () -> Unit,
    viewModel: DeviceStatusViewModel = hiltViewModel()
) {
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val activeTier by viewModel.activeTier.collectAsStateWithLifecycle()
    val hasOverlayPermission by viewModel.hasOverlayPermission.collectAsStateWithLifecycle()
    val overlayActive by viewModel.overlayActive.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshOverlayPermissionStatus()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
        Text(stringResource(R.string.device_status_title), style = MaterialTheme.typography.titleLarge)
        Row(modifier = Modifier.padding(top = 4.dp)) {
            TextButton(onClick = onOpenBooster) { Text(stringResource(R.string.booster_title)) }
            TextButton(onClick = onOpenBlocker) { Text(stringResource(R.string.blocker_title)) }
        }

        val current = snapshot
        if (current == null) {
            // First tick hasn't landed yet — same "measuring" language as
            // the CPU card's own not-ready state, not a separate loading UI.
            Text(
                text = stringResource(R.string.cpu_measuring),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 24.dp)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                item {
                    OverlayToggleCard(
                        hasPermission = hasOverlayPermission,
                        isActive = overlayActive,
                        onToggle = viewModel::toggleOverlay,
                        onRequestPermission = viewModel::requestOverlayPermission
                    )
                }
                item {
                    BatteryCard(current)
                }
                item {
                    MetricCard(
                        label = stringResource(R.string.label_ram),
                        value = "${current.ram.usedPercent}%",
                        accent = MetricRam,
                        detail = formatBytes(current.ram.availableBytes) + " tersisa dari " + formatBytes(current.ram.totalBytes)
                    )
                }
                item {
                    ThermalCard(current.thermal)
                }
                item {
                    CpuCard(cpu = current.cpu, activeTier = activeTier, onOpenSystemAccess = onOpenSystemAccess)
                }
            }
        }
    }
}

@Composable
private fun OverlayToggleCard(
    hasPermission: Boolean,
    isActive: Boolean,
    onToggle: () -> Unit,
    onRequestPermission: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.overlay_toggle_label), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.overlay_toggle_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Switch(checked = isActive, onCheckedChange = { onToggle() })
            }
            if (!hasPermission) {
                Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.overlay_permission_message),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onRequestPermission) {
                        Text(stringResource(R.string.overlay_permission_action))
                    }
                }
            }
        }
    }
}

@Composable
private fun BatteryCard(snapshot: DeviceSnapshot) {
    val battery = snapshot.battery
    if (battery == null) {
        MetricCard(label = stringResource(R.string.label_battery), value = "—", accent = MetricBattery, detail = null)
        return
    }
    val plugLabel = when (battery.chargePlug) {
        ChargePlug.AC -> "Charger AC"
        ChargePlug.USB -> "USB"
        ChargePlug.WIRELESS -> "Nirkabel"
        ChargePlug.NONE -> if (battery.isCharging) "Mengisi" else "Baterai"
    }
    MetricCard(
        label = stringResource(R.string.label_battery),
        value = "${battery.percent}%",
        accent = MetricBattery,
        detail = "$plugLabel · ${battery.temperatureCelsius}°C"
    )
}

@Composable
private fun ThermalCard(thermal: ThermalSnapshot) {
    val (label, color) = when (thermal.status) {
        ThermalStatus.NONE, ThermalStatus.LIGHT -> "Normal" to ThermalNormal
        ThermalStatus.MODERATE -> "Sedang" to ThermalWarm
        ThermalStatus.SEVERE, ThermalStatus.CRITICAL -> "Tinggi" to ThermalHot
        ThermalStatus.EMERGENCY, ThermalStatus.SHUTDOWN -> "Darurat" to ThermalHot
        ThermalStatus.UNKNOWN -> "?" to StatusNeutral
    }
    val detail = thermal.headroom?.let { "Headroom %.2f".format(it) }
    MetricCard(label = stringResource(R.string.label_thermal), value = label, accent = color, detail = detail)
}

@Composable
private fun CpuCard(cpu: CpuSnapshot, activeTier: PrivilegeTier, onOpenSystemAccess: () -> Unit) {
    when (cpu) {
        is CpuSnapshot.Percent -> MetricCard(
            label = stringResource(R.string.label_cpu),
            value = "${cpu.value}%",
            accent = MetricCpu,
            detail = null
        )
        CpuSnapshot.Unavailable -> {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.label_cpu), style = MaterialTheme.typography.titleMedium)
                    if (activeTier == PrivilegeTier.PUBLIC_ONLY) {
                        Text(
                            text = stringResource(R.string.cpu_locked_message),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        TextButton(onClick = onOpenSystemAccess, modifier = Modifier.padding(top = 4.dp)) {
                            Text(stringResource(R.string.cpu_locked_action))
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.cpu_measuring),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, accent: Color, detail: String?) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                if (detail != null) {
                    Text(
                        detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            Text(text = value, style = DataTextStyles.readoutLarge, color = accent)
        }
    }
}

private fun formatBytes(bytes: Long): String {
    val gb = bytes / 1_073_741_824.0
    return if (gb >= 1.0) "%.1f GB".format(gb) else "%d MB".format(bytes / 1_048_576)
}
