package com.android.axion.gamespace.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.axion.gamespace.R
import com.android.axion.gamespace.data.SessionHistory
import java.text.DateFormat
import java.util.Date

@Composable
private fun durationText(seconds: Long): String {
    val h = (seconds / 3600).toInt()
    val m = ((seconds % 3600) / 60).toInt()
    return if (h > 0) stringResource(R.string.stat_duration_hm, h, m)
    else stringResource(R.string.stat_duration_m, maxOf(m, if (seconds > 0) 1 else 0))
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** Kartu statistik main per-game, bergaya sama dengan kartu pengaturan lain. */
@Composable
fun PlayStatsCard(summary: SessionHistory.Summary, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceBright),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (summary.sessions == 0) {
                Text(
                    text = stringResource(R.string.play_stats_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.materialsymbols_ic_bar_chart_rounded_filled),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = durationText(summary.totalSec),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.stat_total_time),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                StatRow(stringResource(R.string.stat_sessions), summary.sessions.toString())
                if (summary.avgFps > 0) {
                    StatRow(stringResource(R.string.stat_avg_fps), summary.avgFps.toString())
                    StatRow(stringResource(R.string.stat_low1), summary.low1.toString())
                }
                if (summary.maxTemp > 0) {
                    StatRow(stringResource(R.string.stat_max_temp), "${summary.maxTemp}°C")
                }
                summary.last?.let { last ->
                    StatRow(
                        stringResource(R.string.stat_last),
                        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(last.startedAt))
                    )
                    StatRow(stringResource(R.string.stat_last_duration), durationText(last.durationSec.toLong()))
                    if (last.batteryDrop > 0) {
                        StatRow(stringResource(R.string.stat_battery_used), "${last.batteryDrop}%")
                    }
                }
            }
        }
    }
}
