package com.siroha.gamespace.data.monitoring

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatterySource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /** Passing a null receiver to a sticky-broadcast filter is the standard
     *  way to read the current battery state on demand instead of holding
     *  a live BroadcastReceiver registered the whole time this app runs. */
    fun snapshot(): BatterySnapshot? {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return null

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return null

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val tenthsOfDegree = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)

        return BatterySnapshot(
            percent = (level * 100) / scale,
            temperatureCelsius = tenthsOfDegree / 10f,
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL,
            chargePlug = when (plugged) {
                BatteryManager.BATTERY_PLUGGED_AC -> ChargePlug.AC
                BatteryManager.BATTERY_PLUGGED_USB -> ChargePlug.USB
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> ChargePlug.WIRELESS
                else -> ChargePlug.NONE
            }
        )
    }
}
