package com.siroha.gamespace.feature.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.siroha.gamespace.core.theme.DataTextStyles
import com.siroha.gamespace.core.theme.GameSpaceTheme
import com.siroha.gamespace.core.theme.MetricBattery
import com.siroha.gamespace.core.theme.MetricCpu
import com.siroha.gamespace.core.theme.MetricRam
import com.siroha.gamespace.core.theme.ThermalHot
import com.siroha.gamespace.core.theme.ThermalNormal
import com.siroha.gamespace.core.theme.ThermalWarm
import com.siroha.gamespace.data.monitoring.CpuSnapshot
import com.siroha.gamespace.data.monitoring.DeviceSnapshot
import com.siroha.gamespace.data.monitoring.ThermalStatus
import com.siroha.gamespace.data.quicktoggle.QuickToggleRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class OverlayPage { MONITOR, TOOLS, TIMER_STOPWATCH }

// Labels in this file are hardcoded Indonesian strings, not
// stringResource() calls, unlike every other screen in this app —
// stringResource() would actually work fine here too (a Service is a
// valid Context for resource resolution), this just wasn't extracted to
// strings.xml yet. Same category of simplification as HomeScreen's
// formatLastPlayed/playtimeSuffix — worth doing once the Settings phase
// adds a Language option that would actually need it.

/**
 * Wraps itself in GameSpaceTheme(amoled = true) rather than inheriting a
 * theme — an overlay window isn't inside this app's Activity, so none of
 * the normal composition-local theme setup from MainActivity reaches it.
 *
 * Page state (which of Monitor/Tools/Timer is showing) lives here, at the
 * root, so it survives collapse/expand. The Timer/Stopwatch's own running
 * state does NOT survive navigating away from that page and back — it's
 * `remember`ed inside TimerStopwatchPage itself, not hoisted this far up.
 * Known, deliberate simplification for a first pass at these tools; worth
 * hoisting further if that turns out to matter in practice.
 */
@Composable
fun OverlayBubbleContent(
    snapshot: DeviceSnapshot?,
    quickToggleRepository: QuickToggleRepository,
    onDrag: (dxPx: Float, dyPx: Float) -> Unit,
    onClose: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf(OverlayPage.MONITOR) }
    var rotationLocked by remember { mutableStateOf(quickToggleRepository.isRotationLocked()) }
    val scope = rememberCoroutineScope()

    GameSpaceTheme(darkTheme = true, dynamicColor = false, amoled = true) {
        val dragModifier = Modifier.pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                onDrag(dragAmount.x, dragAmount.y)
            }
        }

        if (!expanded) {
            CollapsedBubble(modifier = dragModifier, onTap = { expanded = true })
        } else {
            ExpandedPanel(
                page = page,
                snapshot = snapshot,
                isRotationLocked = rotationLocked,
                headerDragModifier = dragModifier,
                onPageChange = { page = it },
                onToggleWifi = { scope.launch { quickToggleRepository.toggleWifi() } },
                onToggleBluetooth = { scope.launch { quickToggleRepository.toggleBluetooth() } },
                onToggleRotation = {
                    quickToggleRepository.toggleRotationLock()
                    rotationLocked = quickToggleRepository.isRotationLocked()
                },
                onCollapse = { expanded = false; page = OverlayPage.MONITOR },
                onClose = onClose
            )
        }
    }
}

@Composable
private fun CollapsedBubble(modifier: Modifier, onTap: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.size(52.dp).clickable(onClick = onTap)
    ) {
        Column(
            modifier = Modifier.size(52.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("GS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ExpandedPanel(
    page: OverlayPage,
    snapshot: DeviceSnapshot?,
    isRotationLocked: Boolean,
    headerDragModifier: Modifier,
    onPageChange: (OverlayPage) -> Unit,
    onToggleWifi: () -> Unit,
    onToggleBluetooth: () -> Unit,
    onToggleRotation: () -> Unit,
    onCollapse: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.width(200.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Drag handle — deliberately only this row, not the whole
            // panel. The panel body is full of its own clickable rows now
            // (tools, tabs, timer buttons); a drag detector wrapping all
            // of that risks swallowing their taps instead of the intended
            // tap-vs-drag disambiguation happening cleanly. A dedicated
            // header drag handle sidesteps the question entirely instead
            // of gambling on how that interaction resolves.
            //
            // This row still has both the drag detector AND a tap-to-
            // collapse click on the title text sharing the same space —
            // that specific combination (parent pointerInput drag +
            // child clickable) is the one gesture interaction in this
            // file not confirmed to disambiguate cleanly without a real
            // device to test on. Worst case if it doesn't: tapping the
            // title to collapse feels unreliable while dragging still
            // works — the close button is unaffected either way, since
            // it's a plain IconButton rather than sharing the drag row.
            Row(verticalAlignment = Alignment.CenterVertically, modifier = headerDragModifier) {
                Text(
                    "GameSpace",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f).clickable(onClick = onCollapse)
                )
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (page != OverlayPage.TIMER_STOPWATCH) {
                Row(modifier = Modifier.padding(top = 4.dp)) {
                    PageTab("Monitor", page == OverlayPage.MONITOR) { onPageChange(OverlayPage.MONITOR) }
                    PageTab("Tools", page == OverlayPage.TOOLS) { onPageChange(OverlayPage.TOOLS) }
                }
            }

            when (page) {
                OverlayPage.MONITOR -> MonitorPage(snapshot)
                OverlayPage.TOOLS -> ToolsPage(
                    isRotationLocked = isRotationLocked,
                    onToggleWifi = onToggleWifi,
                    onToggleBluetooth = onToggleBluetooth,
                    onToggleRotation = onToggleRotation,
                    onOpenTimerStopwatch = { onPageChange(OverlayPage.TIMER_STOPWATCH) }
                )
                OverlayPage.TIMER_STOPWATCH -> TimerStopwatchPage(onBack = { onPageChange(OverlayPage.TOOLS) })
            }
        }
    }
}

@Composable
private fun PageTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = Modifier.clickable(onClick = onClick).padding(end = 12.dp, top = 4.dp)
    )
}

@Composable
private fun MonitorPage(snapshot: DeviceSnapshot?) {
    if (snapshot == null) {
        Text("…", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
    } else {
        Column(modifier = Modifier.padding(top = 4.dp)) {
            MetricRow("RAM", "${snapshot.ram.usedPercent}%", MetricRam)
            snapshot.battery?.let { MetricRow("BAT", "${it.percent}%", MetricBattery) }
            MetricRow("CPU", cpuLabel(snapshot.cpu), MetricCpu)
            MetricRow("TEMP", thermalLabel(snapshot.thermal.status), thermalColor(snapshot.thermal.status))
        }
    }
}

@Composable
private fun ToolsPage(
    isRotationLocked: Boolean,
    onToggleWifi: () -> Unit,
    onToggleBluetooth: () -> Unit,
    onToggleRotation: () -> Unit,
    onOpenTimerStopwatch: () -> Unit
) {
    Column(modifier = Modifier.padding(top = 4.dp)) {
        ToolRow("WiFi", onClick = onToggleWifi)
        ToolRow("Bluetooth", onClick = onToggleBluetooth)
        ToolRow(if (isRotationLocked) "Rotasi: Terkunci" else "Rotasi: Otomatis", onClick = onToggleRotation)
        ToolRow("Timer / Stopwatch", onClick = onOpenTimerStopwatch)
    }
}

@Composable
private fun ToolRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun TimerStopwatchPage(onBack: () -> Unit) {
    var showStopwatch by remember { mutableStateOf(true) }
    Column(modifier = Modifier.padding(top = 4.dp)) {
        Text(
            "< Kembali",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onBack).padding(bottom = 6.dp)
        )
        Row {
            PageTab("Stopwatch", showStopwatch) { showStopwatch = true }
            PageTab("Timer", !showStopwatch) { showStopwatch = false }
        }
        if (showStopwatch) StopwatchTool() else CountdownTimerTool()
    }
}

@Composable
private fun StopwatchTool() {
    var elapsedMillis by remember { mutableStateOf(0L) }
    var running by remember { mutableStateOf(false) }

    LaunchedEffect(running) {
        while (running) {
            delay(100)
            elapsedMillis += 100
        }
    }

    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(formatDuration(elapsedMillis), style = DataTextStyles.readoutMedium)
        Row(modifier = Modifier.padding(top = 4.dp)) {
            TextButton(onClick = { running = !running }) { Text(if (running) "Jeda" else "Mulai") }
            TextButton(onClick = { running = false; elapsedMillis = 0L }) { Text("Reset") }
        }
    }
}

@Composable
private fun CountdownTimerTool() {
    var totalSeconds by remember { mutableStateOf(300) }
    var remainingMillis by remember { mutableStateOf(totalSeconds * 1000L) }
    var running by remember { mutableStateOf(false) }

    LaunchedEffect(running) {
        while (running && remainingMillis > 0) {
            delay(100)
            remainingMillis = (remainingMillis - 100).coerceAtLeast(0)
        }
        if (remainingMillis <= 0) running = false
    }

    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(formatDuration(remainingMillis), style = DataTextStyles.readoutMedium)
        Row(modifier = Modifier.padding(top = 4.dp)) {
            TextButton(
                onClick = {
                    totalSeconds = (totalSeconds - 60).coerceAtLeast(60)
                    if (!running) remainingMillis = totalSeconds * 1000L
                }
            ) { Text("-1m") }
            TextButton(
                onClick = {
                    totalSeconds += 60
                    if (!running) remainingMillis = totalSeconds * 1000L
                }
            ) { Text("+1m") }
        }
        Row(modifier = Modifier.padding(top = 4.dp)) {
            TextButton(onClick = { running = !running }) { Text(if (running) "Jeda" else "Mulai") }
            TextButton(onClick = { running = false; remainingMillis = totalSeconds * 1000L }) { Text("Reset") }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

@Composable
private fun MetricRow(label: String, value: String, accent: Color) {
    Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
        Text(value, style = DataTextStyles.readoutSmall, color = accent)
    }
}

private fun cpuLabel(cpu: CpuSnapshot): String = when (cpu) {
    is CpuSnapshot.Percent -> "${cpu.value}%"
    CpuSnapshot.Unavailable -> "—"
}

private fun thermalLabel(status: ThermalStatus): String = when (status) {
    ThermalStatus.NONE, ThermalStatus.LIGHT -> "Normal"
    ThermalStatus.MODERATE -> "Sedang"
    ThermalStatus.SEVERE, ThermalStatus.CRITICAL -> "Tinggi"
    ThermalStatus.EMERGENCY, ThermalStatus.SHUTDOWN -> "Darurat"
    ThermalStatus.UNKNOWN -> "?"
}

private fun thermalColor(status: ThermalStatus): Color = when (status) {
    ThermalStatus.NONE, ThermalStatus.LIGHT -> ThermalNormal
    ThermalStatus.MODERATE -> ThermalWarm
    else -> ThermalHot
}
