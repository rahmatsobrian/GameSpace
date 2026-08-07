package com.siroha.gamespace.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.siroha.gamespace.BuildConfig
import com.siroha.gamespace.R
import com.siroha.gamespace.data.settings.DarkModePreference
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val backupMessage by viewModel.backupMessage.collectAsStateWithLifecycle()

    LaunchedEffect(backupMessage) {
        if (backupMessage != null) {
            delay(3_000)
            viewModel.clearBackupMessage()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(viewModel::exportLibrary) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::importLibrary) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleLarge)

        SectionCard(stringResource(R.string.settings_theme_section)) {
            Text(stringResource(R.string.settings_dark_mode_label), style = MaterialTheme.typography.bodyMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                FilterChip(
                    selected = settings.darkMode == DarkModePreference.LIGHT,
                    onClick = { viewModel.setDarkMode(DarkModePreference.LIGHT) },
                    label = { Text(stringResource(R.string.dark_mode_light)) }
                )
                FilterChip(
                    selected = settings.darkMode == DarkModePreference.DARK,
                    onClick = { viewModel.setDarkMode(DarkModePreference.DARK) },
                    label = { Text(stringResource(R.string.dark_mode_dark)) }
                )
                FilterChip(
                    selected = settings.darkMode == DarkModePreference.SYSTEM,
                    onClick = { viewModel.setDarkMode(DarkModePreference.SYSTEM) },
                    label = { Text(stringResource(R.string.dark_mode_system)) }
                )
            }

            SettingsSwitchRow(
                label = stringResource(R.string.settings_amoled_label),
                desc = stringResource(R.string.settings_amoled_desc),
                checked = settings.amoledEnabled,
                onCheckedChange = viewModel::setAmoledEnabled,
                enabled = settings.darkMode != DarkModePreference.LIGHT
            )
            SettingsSwitchRow(
                label = stringResource(R.string.settings_dynamic_color_label),
                desc = stringResource(R.string.settings_dynamic_color_desc),
                checked = settings.dynamicColorEnabled,
                onCheckedChange = viewModel::setDynamicColorEnabled
            )
        }

        SectionCard(stringResource(R.string.settings_backup_section)) {
            BackupRow(
                label = stringResource(R.string.settings_export_label),
                desc = stringResource(R.string.settings_export_desc),
                actionLabel = stringResource(R.string.settings_export_action),
                onClick = { exportLauncher.launch("gamespace-library.json") }
            )
            BackupRow(
                label = stringResource(R.string.settings_import_label),
                desc = stringResource(R.string.settings_import_desc),
                actionLabel = stringResource(R.string.settings_import_action),
                onClick = { importLauncher.launch(arrayOf("application/json")) }
            )
            if (backupMessage != null) {
                Text(
                    backupMessage.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        SectionCard(stringResource(R.string.settings_about_section)) {
            Text(
                stringResource(R.string.settings_about_body, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.padding(top = 16.dp).fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Column(modifier = Modifier.padding(top = 8.dp)) { content() }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    label: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                desc,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun BackupRow(label: String, desc: String, actionLabel: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                desc,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        OutlinedButton(onClick = onClick) { Text(actionLabel) }
    }
}
