package com.siroha.gamespace.data.monitoring

import android.app.ActivityManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RamSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    fun snapshot(): RamSnapshot {
        val info = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(info)
        val used = info.totalMem - info.availMem
        val usedPercent = if (info.totalMem > 0) ((used * 100) / info.totalMem).toInt() else 0
        return RamSnapshot(
            totalBytes = info.totalMem,
            availableBytes = info.availMem,
            usedPercent = usedPercent.coerceIn(0, 100),
            isLowMemory = info.lowMemory
        )
    }
}
