package com.siroha.gamespace.data.game

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class InstalledAppInfo(
    val packageName: String,
    val label: String,
    /** CATEGORY_GAME (API 26+) or the legacy pre-Oreo isGame flag. Neither
     *  is a guarantee — plenty of real games set neither — so this is a
     *  pre-check for auto-scan, not a filter that hides anything from the
     *  manual-add picker. */
    val looksLikeGame: Boolean
)

@Singleton
class InstalledAppsScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val packageManager get() = context.packageManager

    suspend fun scanLaunchableApps(includeSystemApps: Boolean = false): List<InstalledAppInfo> =
        withContext(Dispatchers.IO) {
            val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

            val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(launcherIntent, 0)
            }

            resolveInfos
                .asSequence()
                .mapNotNull { it.activityInfo?.applicationInfo }
                .distinctBy { it.packageName }
                .filter { includeSystemApps || !it.isSystemApp() }
                .map { appInfo ->
                    InstalledAppInfo(
                        packageName = appInfo.packageName,
                        label = appInfo.loadLabel(packageManager).toString(),
                        looksLikeGame = appInfo.looksLikeGame()
                    )
                }
                .sortedBy { it.label.lowercase() }
                .toList()
        }

    fun resolveLabel(packageName: String): String? = try {
        packageManager.getApplicationInfo(packageName, 0).loadLabel(packageManager).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    fun isInstalled(packageName: String): Boolean = try {
        packageManager.getApplicationInfo(packageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    fun launchIntentFor(packageName: String): Intent? = packageManager.getLaunchIntentForPackage(packageName)

    private fun ApplicationInfo.isSystemApp(): Boolean = (flags and ApplicationInfo.FLAG_SYSTEM) != 0

    private fun ApplicationInfo.looksLikeGame(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && category == ApplicationInfo.CATEGORY_GAME) {
            return true
        }
        @Suppress("DEPRECATION")
        return (flags and ApplicationInfo.FLAG_IS_GAME) != 0
    }
}
