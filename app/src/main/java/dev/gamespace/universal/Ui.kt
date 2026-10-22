package dev.gamespace.universal

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val Dark = darkColorScheme(
    primary = Color(0xFF8AB4F8),
    background = Color(0xFF070C16),
    surface = Color(0xFF12161C),
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun GameSpaceTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = Dark, content = content)

private enum class Screen { HUB, ADD, SETTINGS }

// ---------- Helper ----------
private fun safeStart(ctx: Context, i: Intent) {
    try { ctx.startActivity(i) } catch (_: Exception) {
        try {
            ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")))
        } catch (_: Exception) {}
    }
}

@Suppress("DEPRECATION")
private fun labelOf(ctx: Context, pkg: String): String = try {
    val pm = ctx.packageManager
    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
} catch (_: Exception) { pkg }

private fun iconOf(ctx: Context, pkg: String): ImageBitmap? = try {
    ctx.packageManager.getApplicationIcon(pkg).toBitmap(96, 96).asImageBitmap()
} catch (_: Exception) { null }

private class AppEntry(val pkg: String, val label: String, val icon: ImageBitmap?)

@Suppress("DEPRECATION")
private fun loadApps(ctx: Context): List<AppEntry> {
    val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return ctx.packageManager.queryIntentActivities(i, 0)
        .map { it.activityInfo.packageName }.distinct().filter { it != ctx.packageName }
        .map { AppEntry(it, labelOf(ctx, it), iconOf(ctx, it)) }
        .sortedBy { it.label.lowercase() }
}

// ---------- Root ----------
@Composable
fun App() {
    val ctx = LocalContext.current
    val store = remember { Store(ctx) }
    var screen by rememberSaveable { mutableStateOf(Screen.HUB) }
    var games by remember { mutableStateOf(store.games) }
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    BackHandler(screen != Screen.HUB) { screen = Screen.HUB }

    fun setGames(v: List<String>) { store.games = v; games = v }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.systemBarsPadding()) {
            when (screen) {
                Screen.HUB -> Hub(store, games, tick, ::setGames, { screen = Screen.SETTINGS }, { screen = Screen.ADD })
                Screen.ADD -> AddGame(games) { setGames(games + it); screen = Screen.HUB }
                Screen.SETTINGS -> SettingsScreen(store) { screen = Screen.HUB }
            }
        }
    }
}

// ---------- Hub ----------
@Composable
private fun Hub(
    store: Store, games: List<String>, tick: Int,
    setGames: (List<String>) -> Unit, onSettings: () -> Unit, onAdd: () -> Unit
) {
    val ctx = LocalContext.current
    var selected by remember { mutableStateOf<String?>(null) }
    val sel = selected?.takeIf { it in games } ?: games.firstOrNull()
    var enabled by remember { mutableStateOf(store.serviceEnabled) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Game Space", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, "Settings") }
        }

        Card {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Game detection", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (enabled) "Service running" else "Service stopped",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(checked = enabled, onCheckedChange = { on ->
                    enabled = on; store.serviceEnabled = on
                    if (on) GameMonitorService.start(ctx) else GameMonitorService.stop(ctx)
                })
            }
        }

        PermissionsCard(tick)

        Text("Library", style = MaterialTheme.typography.titleMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Column(
                    Modifier.size(96.dp).clip(RoundedCornerShape(14.dp))
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)), RoundedCornerShape(14.dp))
                        .clickable { onAdd() },
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.Add, "Add game")
                    Text("Add", style = MaterialTheme.typography.bodySmall)
                }
            }
            items(games, key = { it }) { pkg ->
                val icon = remember(pkg) { iconOf(ctx, pkg) }
                val on = pkg == sel
                Column(
                    Modifier.size(96.dp).clip(RoundedCornerShape(14.dp))
                        .border(
                            BorderStroke(if (on) 2.dp else 1.dp, Color.White.copy(alpha = if (on) 0.8f else 0.15f)),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { selected = pkg }.padding(8.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    icon?.let { Image(it, null, Modifier.size(44.dp)) }
                    Text(
                        labelOf(ctx, pkg), style = MaterialTheme.typography.bodySmall,
                        maxLines = 1, textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (sel == null) {
            Text("Belum ada game. Tekan Add untuk memilih aplikasi.", style = MaterialTheme.typography.bodyMedium)
        } else {
            Text(labelOf(ctx, sel), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    ctx.packageManager.getLaunchIntentForPackage(sel)?.let { safeStart(ctx, it) }
                }) { Text("Launch Game") }
                OutlinedButton(onClick = { setGames(games - sel) }) { Text("Remove") }
            }
        }
    }
}

private class PermItem(val label: String, val ok: Boolean, val grant: () -> Unit)

@Composable
private fun PermissionsCard(@Suppress("UNUSED_PARAMETER") tick: Int) {
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}
    val pkgUri = Uri.parse("package:${ctx.packageName}")

    val phoneOk = Perms.granted(ctx, Manifest.permission.READ_PHONE_STATE) &&
        Perms.granted(ctx, Manifest.permission.ANSWER_PHONE_CALLS)

    val items = buildList {
        add(PermItem("Usage access (deteksi game)", Perms.hasUsage(ctx)) {
            safeStart(ctx, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        })
        add(PermItem("Display over other apps (overlay)", Perms.canOverlay(ctx)) {
            safeStart(ctx, Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, pkgUri))
        })
        add(PermItem("Modify system settings (brightness)", Perms.canWrite(ctx)) {
            safeStart(ctx, Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, pkgUri))
        })
        add(PermItem("Do Not Disturb access", Perms.hasDnd(ctx)) {
            safeStart(ctx, Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        })
        add(PermItem("Notification access (notification overlay)", Perms.hasListener(ctx)) {
            safeStart(ctx, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        })
        add(PermItem("Phone state & answer calls", phoneOk) {
            launcher.launch(arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.ANSWER_PHONE_CALLS))
        })
        if (Build.VERSION.SDK_INT >= 33) {
            add(PermItem("Post notifications", Perms.granted(ctx, Manifest.permission.POST_NOTIFICATIONS)) {
                launcher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            })
        }
        add(PermItem("Battery: unrestricted", Perms.ignoringBattery(ctx)) {
            safeStart(ctx, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        })
    }

    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Permissions", style = MaterialTheme.typography.titleMedium)
            items.forEach { p ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(p.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    if (p.ok) Text("OK", style = MaterialTheme.typography.bodySmall)
                    else OutlinedButton(onClick = p.grant) { Text("Grant") }
                }
            }
            Text(
                "Di beberapa ROM (HyperOS, ColorOS, Funtouch, XOS, dll.), aktifkan juga autostart dan " +
                    "matikan pembatasan baterai untuk Game Space lewat aplikasi keamanan/baterai bawaan, " +
                    "agar service tidak dimatikan sistem.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// ---------- Add game ----------
@Composable
private fun AddGame(existing: List<String>, onPick: (String) -> Unit) {
    val ctx = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    val apps by produceState(emptyList<AppEntry>()) { value = withContext(Dispatchers.Default) { loadApps(ctx) } }
    val list = apps.filter { it.pkg !in existing && it.label.contains(query, ignoreCase = true) }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Add game", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = query, onValueChange = { query = it }, singleLine = true,
            label = { Text("Search") }, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(list, key = { it.pkg }) { a ->
                Row(
                    Modifier.fillMaxWidth().clickable { onPick(a.pkg) }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    a.icon?.let { Image(it, null, Modifier.size(40.dp)) } ?: Spacer(Modifier.size(40.dp))
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(a.label, style = MaterialTheme.typography.bodyLarge)
                        Text(a.pkg, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

// ---------- Settings ----------
@Composable
private fun ToggleRow(label: String, initial: Boolean, onChange: (Boolean) -> Unit) {
    var v by remember { mutableStateOf(initial) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = v, onCheckedChange = { v = it; onChange(it) })
    }
}

@Composable
private fun Chips(options: List<String>, initial: Int, onSelect: (Int) -> Unit) {
    var sel by remember { mutableIntStateOf(initial) }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { idx, o ->
            FilterChip(selected = idx == sel, onClick = { sel = idx; onSelect(idx) }, label = { Text(o) })
        }
    }
}

@Composable
private fun SettingsScreen(store: Store, onBack: () -> Unit) {
    var alpha by remember { mutableStateOf(store.idleAlpha.toFloat()) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text("Settings", style = MaterialTheme.typography.titleLarge)
        }
        ToggleRow("Disable auto-brightness", store.noAutoBrightness) { store.noAutoBrightness = it }
        ToggleRow("Stay awake", store.stayAwake) { store.stayAwake = it }
        ToggleRow("Auto Do Not Disturb", store.autoDnd) { store.autoDnd = it }
        ToggleRow("Notification overlay", store.danmaku) { store.danmaku = it }
        ToggleRow("Crosshair", store.crosshair) { store.crosshair = it }

        Text("In-game call")
        Chips(listOf("No action", "Auto answer", "Auto reject"), store.callsMode) { store.callsMode = it }

        Text("In-game ringer mode")
        Chips(listOf("Silent", "Vibrate", "Normal", "No change"), store.ringerMode) { store.ringerMode = it }

        Text("Bubble opacity: ${alpha.toInt()}%")
        Slider(
            value = alpha, onValueChange = { alpha = it; store.idleAlpha = it.toInt() },
            valueRange = 5f..100f
        )
        Text(
            "Perubahan berlaku pada sesi game berikutnya.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
