package com.android.axion.gamespace.compat

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import com.android.axion.gamespace.shell.Shell

/** Pengganti AxMemoryManager.releaseMemory(): mematikan proses latar. Blocking, panggil dari background. */
object MemoryBooster {
    @Suppress("DEPRECATION")
    fun release(ctx: Context): Long {
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        fun avail(): Long = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }.availMem
        val before = avail()
        if (Shell.available()) {
            Shell.exec("am kill-all", 6000)
        } else {
            val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            ctx.packageManager.queryIntentActivities(i, 0)
                .map { it.activityInfo.packageName }.distinct()
                .filter { it != ctx.packageName }
                .forEach { try { am.killBackgroundProcesses(it) } catch (_: Exception) {} }
        }
        Thread.sleep(400)
        return ((avail() - before) / (1024 * 1024)).coerceAtLeast(0)
    }
}
