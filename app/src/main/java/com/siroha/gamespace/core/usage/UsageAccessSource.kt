package com.siroha.gamespace.core.usage

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class PlaySnapshot(val lastTimeUsed: Long?, val totalForegroundMillis: Long)

/**
 * "Usage access" (PACKAGE_USAGE_STATS) is a special permission: declaring
 * it in the manifest grants nothing by itself, the user has to flip it on
 * per-app from a system Settings screen, checked via AppOps rather than
 * checkSelfPermission. It needs neither root nor Shizuku — this is a third,
 * independent kind of "more than default" access.
 */
@Singleton
class UsageAccessSource @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun hasAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        @Suppress("DEPRECATION")
        // unsafeCheckOpNoThrow is the non-deprecated replacement for this;
        // kept as checkOpNoThrow here since its exact minimum API level is
        // the one thing in this file not double-checked against current
        // docs. Functionally identical either way — safe to switch once
        // confirmed against whatever compileSdk Android Studio resolves.
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Opens the system "Usage access" list — there's no reliable deep link
     *  straight to this app's row the way overlay/notification-listener
     *  settings support, so this lands on the list and the user finds
     *  GameSpace in it. */
    fun openAccessSettings() {
        context.startActivity(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /**
     * One batched query for every installed package rather than one query
     * per game — queryUsageStats returns system-wide results regardless of
     * how narrowly you'd like to filter, so calling it per-item would just
     * mean repeating the same expensive scan N times.
     *
     * This reflects whatever history Android itself still retains, not a
     * true lifetime total — usage history is bucketed at day/week/month/
     * year granularity and the OS ages out the finer buckets over time, so
     * the retention window (and therefore how far back these numbers
     * reach) depends on the device, not on this app.
     *
     * Takes the single most-recent UsageStats bucket per package rather
     * than summing every entry INTERVAL_BEST returns for it — summing
     * risks double-counting where buckets overlap, so this trades a
     * possible undercount for not risking an inflated one. Precise
     * session-level playtime would need queryEvents() and pairing
     * MOVE_TO_FOREGROUND/BACKGROUND transitions by hand — worth doing if
     * the approximate number here turns out not to be good enough.
     */
    suspend fun snapshotForAll(): Map<String, PlaySnapshot> = withContext(Dispatchers.IO) {
        if (!hasAccess()) return@withContext emptyMap()

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val start = end - TimeUnit.DAYS.toMillis(365)

        usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, start, end)
            .groupBy { it.packageName }
            .mapValues { (_, entries) ->
                val mostRecent = entries.maxBy { it.lastTimeUsed }
                PlaySnapshot(
                    lastTimeUsed = mostRecent.lastTimeUsed.takeIf { it > 0 },
                    totalForegroundMillis = mostRecent.totalTimeInForeground
                )
            }
    }
}
