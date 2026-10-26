package com.android.axion.gamespace.compat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import com.android.axion.gamespace.data.AppSettings

/**
 * Batas pengisian selama sesi game: jeda pengisian (node charge) saat level mencapai batas,
 * lanjutkan saat turun 3% di bawahnya. Butuh node charge yang bisa ditulis (root).
 */
class ChargeLimiter(private val context: Context, private val appSettings: AppSettings) {
    private var registered = false
    private var pausedByUs = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) = evaluate(intent)
    }

    fun start() {
        if (registered) return
        if (!appSettings.chargeLimitEnabled || !HwNodes.isSupported(context, HwNodes.Kind.CHARGE)) return
        ContextCompat.registerReceiver(
            context, receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED
        )
        registered = true
    }

    fun stop() {
        if (registered) {
            try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
            registered = false
        }
        if (pausedByUs) {
            HwNodes.set(context, HwNodes.Kind.CHARGE, false)
            pausedByUs = false
        }
    }

    private fun evaluate(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        if (level < 0 || scale <= 0) return
        val pct = level * 100 / scale
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
        val limit = appSettings.chargeLimit
        if (plugged && pct >= limit && !pausedByUs) {
            pausedByUs = true
            HwNodes.set(context, HwNodes.Kind.CHARGE, true)
        } else if (pausedByUs && pct <= limit - 3) {
            pausedByUs = false
            HwNodes.set(context, HwNodes.Kind.CHARGE, false)
        }
    }
}
