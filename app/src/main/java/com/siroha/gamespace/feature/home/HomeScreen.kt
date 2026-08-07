package com.siroha.gamespace.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.siroha.gamespace.R
import com.siroha.gamespace.core.theme.StatusGranted
import com.siroha.gamespace.core.util.toImageBitmap
import com.siroha.gamespace.data.game.Game
import com.siroha.gamespace.data.game.InstalledAppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenDeviceStatus: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val addGameSheet by viewModel.addGameSheet.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isGridView = uiState.isGridView

    // AppOps grants don't push a change notification — re-check whenever
    // the user comes back to this screen (e.g. from the usage-access
    // settings page).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshUsageAccessStatus()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::openAddGameSheet) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.home_add_game_cd))
            }
        }
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(scaffoldPadding)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.home_search_hint)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true
                )
                Spacer(modifier = Modifier.size(8.dp))
                IconButton(onClick = onOpenDeviceStatus) {
                    Icon(Icons.Filled.Info, contentDescription = stringResource(R.string.home_device_status_cd))
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                }
                if (uiState.isScanning) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = viewModel::rescan) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.home_scan_cd))
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                FilterChip(selected = isGridView, onClick = { viewModel.onGridViewChange(true) }, label = { Text("Grid") })
                FilterChip(selected = !isGridView, onClick = { viewModel.onGridViewChange(false) }, label = { Text("List") })
            }

            if (!uiState.hasUsageAccess) {
                UsageAccessBanner(onGrant = viewModel::requestUsageAccess)
                Spacer(modifier = Modifier.size(12.dp))
            }

            if (uiState.games.isEmpty()) {
                EmptyLibrary(modifier = Modifier.weight(1f))
            } else if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(uiState.games, key = { it.packageName }) { game ->
                        GameGridItem(
                            game = game,
                            onLaunch = { launchGame(context, viewModel, game.packageName) },
                            onToggleFavorite = { viewModel.toggleFavorite(game.packageName, !game.isFavorite) },
                            onRemove = { viewModel.removeFromLibrary(game.packageName) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(uiState.games, key = { it.packageName }) { game ->
                        GameListItem(
                            game = game,
                            onLaunch = { launchGame(context, viewModel, game.packageName) },
                            onToggleFavorite = { viewModel.toggleFavorite(game.packageName, !game.isFavorite) },
                            onRemove = { viewModel.removeFromLibrary(game.packageName) }
                        )
                    }
                }
            }
        }
    }

    if (addGameSheet.isOpen) {
        AddGameSheet(
            state = addGameSheet,
            onQueryChange = viewModel::onAddGameQueryChange,
            onPick = viewModel::addGame,
            onDismiss = viewModel::closeAddGameSheet
        )
    }
}

private fun launchGame(context: android.content.Context, viewModel: HomeViewModel, packageName: String) {
    viewModel.launchIntentFor(packageName)?.let { context.startActivity(it) }
}

@Composable
private fun UsageAccessBanner(onGrant: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.home_usage_banner_text),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onGrant) { Text(stringResource(R.string.home_usage_banner_action)) }
        }
    }
}

@Composable
private fun EmptyLibrary(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = stringResource(R.string.home_empty_title), style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = stringResource(R.string.home_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun GameGridItem(game: Game, onLaunch: () -> Unit, onToggleFavorite: () -> Unit, onRemove: () -> Unit) {
    Card(modifier = Modifier.clickable(onClick = onLaunch)) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                AppIcon(packageName = game.packageName, modifier = Modifier.fillMaxSize())
                IconButton(onClick = onRemove, modifier = Modifier.align(Alignment.TopEnd).size(28.dp)) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.home_remove_cd),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = game.displayName,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatLastPlayed(game.lastPlayedAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(28.dp)) {
                    Icon(
                        if (game.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = stringResource(R.string.home_favorite_cd),
                        modifier = Modifier.size(16.dp),
                        tint = if (game.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun GameListItem(game: Game, onLaunch: () -> Unit, onToggleFavorite: () -> Unit, onRemove: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onLaunch)) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AppIcon(packageName = game.packageName, modifier = Modifier.size(48.dp))
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        game.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (game.isManuallyAdded) {
                        Spacer(modifier = Modifier.size(6.dp))
                        Surface(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), shape = CircleShape) {
                            Text(
                                stringResource(R.string.home_manual_badge),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = formatLastPlayed(game.lastPlayedAt) + playtimeSuffix(game.totalPlaytimeMillis),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (game.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = stringResource(R.string.home_favorite_cd),
                    tint = if (game.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.home_remove_cd))
            }
        }
    }
}

@Composable
private fun AppIcon(packageName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bitmap by remember(packageName) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(packageName) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching { context.packageManager.getApplicationIcon(packageName).toImageBitmap() }.getOrNull()
        }
    }

    val resolved = bitmap
    if (resolved != null) {
        Image(
            painter = BitmapPainter(resolved),
            contentDescription = null,
            modifier = modifier.clip(RoundedCornerShape(12.dp))
        )
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGameSheet(
    state: AddGameSheetState,
    onQueryChange: (String) -> Unit,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp).height(480.dp)) {
            Text(stringResource(R.string.home_add_game_title), style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.size(12.dp))
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.home_add_game_search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true
            )
            Spacer(modifier = Modifier.size(12.dp))

            val filtered = if (state.query.isBlank()) {
                state.results
            } else {
                state.results.filter { it.label.contains(state.query, ignoreCase = true) }
            }

            when {
                state.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                filtered.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.home_add_game_empty), style = MaterialTheme.typography.bodyMedium)
                }
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(filtered, key = { it.packageName }) { app ->
                        PickableAppRow(app = app, onClick = { onPick(app.packageName) })
                    }
                }
            }
        }
    }
}

@Composable
private fun PickableAppRow(app: InstalledAppInfo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(packageName = app.packageName, modifier = Modifier.size(40.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp).weight(1f)
        )
        if (app.looksLikeGame) {
            Surface(color = StatusGranted.copy(alpha = 0.15f), shape = CircleShape) {
                Text(
                    "Game",
                    style = MaterialTheme.typography.labelMedium,
                    color = StatusGranted,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

// Deliberately plain string concatenation, not string resources with format
// args — this hardcodes Indonesian relative-time phrasing. Fine for now;
// move to proper resource-based formatting once the spec's Language
// setting actually exists (there's no i18n framework wired up yet to
// switch this by).
private fun formatLastPlayed(timestamp: Long?): String {
    if (timestamp == null) return "Belum pernah main"
    val diffMillis = System.currentTimeMillis() - timestamp
    val days = diffMillis / 86_400_000L
    return when {
        days <= 0 -> "Hari ini"
        days == 1L -> "Kemarin"
        days < 7 -> "$days hari lalu"
        days < 30 -> "${days / 7} minggu lalu"
        else -> "${days / 30} bulan lalu"
    }
}

private fun playtimeSuffix(totalMillis: Long): String {
    if (totalMillis <= 0) return ""
    val totalMinutes = totalMillis / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    val formatted = if (hours > 0) "${hours}j ${minutes}m" else "${minutes}m"
    return " · $formatted"
}
