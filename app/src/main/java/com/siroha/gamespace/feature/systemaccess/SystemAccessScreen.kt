package com.siroha.gamespace.feature.systemaccess

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.siroha.gamespace.R
import com.siroha.gamespace.core.privilege.AccessState
import com.siroha.gamespace.core.privilege.PrivilegeTier
import com.siroha.gamespace.core.privilege.SourceStatus
import com.siroha.gamespace.core.theme.StatusDenied
import com.siroha.gamespace.core.theme.StatusGranted
import com.siroha.gamespace.core.theme.StatusNeutral
import com.siroha.gamespace.core.theme.StatusWarning

@Composable
fun SystemAccessScreen(
    onContinue: () -> Unit,
    viewModel: SystemAccessViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(text = stringResource(R.string.system_access_title), style = MaterialTheme.typography.titleLarge)
        Text(
            text = stringResource(R.string.system_access_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
        )

        PrivilegeSourceCard(
            title = stringResource(R.string.privilege_root_title),
            description = stringResource(R.string.privilege_root_desc),
            status = state.root,
            primaryLabel = when (state.root.state) {
                AccessState.PERMISSION_DENIED, AccessState.NOT_AVAILABLE -> stringResource(R.string.action_recheck)
                else -> stringResource(R.string.action_request_root)
            },
            onPrimaryAction = viewModel::onRequestRoot,
            secondaryLabel = null,
            onSecondaryAction = null
        )

        PrivilegeSourceCard(
            title = stringResource(R.string.privilege_shizuku_title),
            description = stringResource(R.string.privilege_shizuku_desc),
            status = state.shizuku,
            primaryLabel = when (state.shizuku.state) {
                AccessState.NOT_AVAILABLE -> stringResource(R.string.action_install_shizuku)
                AccessState.NOT_RUNNING -> stringResource(R.string.action_open_shizuku)
                AccessState.PERMISSION_DENIED, AccessState.NOT_REQUESTED ->
                    stringResource(R.string.action_request_shizuku_permission)
                else -> stringResource(R.string.action_recheck)
            },
            onPrimaryAction = {
                when (state.shizuku.state) {
                    AccessState.NOT_AVAILABLE -> openUrl(context, SHIZUKU_RELEASES_URL)
                    AccessState.NOT_RUNNING -> launchApp(context, SHIZUKU_PACKAGE_NAME) {
                        openUrl(context, SHIZUKU_RELEASES_URL)
                    }
                    AccessState.GRANTED -> viewModel.onRecheck()
                    else -> viewModel.onRequestShizuku()
                }
            },
            secondaryLabel = null,
            onSecondaryAction = null
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = activeTierLabel(state.activeTier),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Button(
            onClick = {
                viewModel.markOnboardingComplete()
                onContinue()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.action_continue))
        }
    }
}

@Composable
private fun PrivilegeSourceCard(
    title: String,
    description: String,
    status: SourceStatus,
    primaryLabel: String,
    onPrimaryAction: () -> Unit,
    secondaryLabel: String?,
    onSecondaryAction: (() -> Unit)?
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusIcon(status.state)
                Text(text = title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    text = stateLabel(status.state),
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor(status.state)
                )
            }
            Text(text = description, style = MaterialTheme.typography.bodyMedium)

            if (status.state == AccessState.CHECKING) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (status.state != AccessState.GRANTED) {
                        Button(onClick = onPrimaryAction) { Text(primaryLabel) }
                    } else {
                        OutlinedButton(onClick = onPrimaryAction) { Text(primaryLabel) }
                    }
                    if (secondaryLabel != null && onSecondaryAction != null) {
                        TextButton(onClick = onSecondaryAction) { Text(secondaryLabel) }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusIcon(state: AccessState) {
    val (icon, tint) = when (state) {
        AccessState.GRANTED -> Icons.Filled.CheckCircle to StatusGranted
        AccessState.PERMISSION_DENIED, AccessState.NOT_AVAILABLE -> Icons.Filled.Close to StatusDenied
        else -> Icons.Filled.Warning to StatusNeutral
    }
    Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
}

@Composable
private fun stateLabel(state: AccessState): String = when (state) {
    AccessState.UNKNOWN -> stringResource(R.string.state_unknown)
    AccessState.CHECKING -> stringResource(R.string.state_checking)
    AccessState.NOT_AVAILABLE -> stringResource(R.string.state_not_available)
    AccessState.NOT_RUNNING -> stringResource(R.string.state_not_running)
    AccessState.NOT_REQUESTED -> stringResource(R.string.state_unknown)
    AccessState.PERMISSION_DENIED -> stringResource(R.string.state_permission_denied)
    AccessState.GRANTED -> stringResource(R.string.state_granted)
}

@Composable
private fun statusColor(state: AccessState): Color = when (state) {
    AccessState.GRANTED -> StatusGranted
    AccessState.PERMISSION_DENIED, AccessState.NOT_AVAILABLE -> StatusDenied
    AccessState.CHECKING -> StatusWarning
    else -> StatusNeutral
}

@Composable
private fun activeTierLabel(tier: PrivilegeTier): String = when (tier) {
    PrivilegeTier.ROOT -> stringResource(R.string.active_tier_root)
    PrivilegeTier.SHIZUKU -> stringResource(R.string.active_tier_shizuku)
    PrivilegeTier.PUBLIC_ONLY -> stringResource(R.string.active_tier_public)
}

private const val SHIZUKU_PACKAGE_NAME = "moe.shizuku.privileged.api"
private const val SHIZUKU_RELEASES_URL = "https://github.com/RikkaApps/Shizuku/releases"

private fun openUrl(context: android.content.Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

private fun launchApp(context: android.content.Context, packageName: String, onNotInstalled: () -> Unit) {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
    if (intent != null) {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } else {
        onNotInstalled()
    }
}
