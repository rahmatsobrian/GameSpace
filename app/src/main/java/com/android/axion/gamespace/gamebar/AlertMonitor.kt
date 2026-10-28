package com.android.axion.gamespace.gamebar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.android.axion.gamespace.R
import com.android.axion.gamespace.compat.NetInfo
import com.android.axion.gamespace.compat.PerfModes
import com.android.axion.gamespace.data.AppSettings

/**
 * Peringatan selama sesi game: suhu tinggi, baterai rendah, dan lonjakan lag.
 * Auto Cool: saat suhu mencapai ambang, mode Cool dinyalakan otomatis dan dimatikan lagi
 * setelah suhu turun 4 derajat.
 */
class AlertMonitor(private val context: Context, private val appSettings: AppSettings) {
    private val main = Handler(Looper.getMainLooper())
    private var registered = false
    private var lastTempAlert = 0L
    private var lastBatteryAlert = 0L
    private var coolByUs = false

    /** Ambang Auto Cool dalam derajat Celsius; 0 = mati. Diisi sebelum start(). */
    var autoCoolTemp = 0

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) = evaluate(intent)
    }

    fun start() {
        if (registered) return
        if (appSettings.alertTemp <= 0 && appSettings.alertBattery <= 0 && autoCoolTemp <= 0) return
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
        coolByUs = false
    }

    /** Dipanggil saat PingMonitor mendeteksi lonjakan ping. */
    fun notifyLag(ms: Int) {
        notifyUser(context.getString(R.string.lag_spike_message, ms, NetInfo.describe(context)))
    }

    private fun evaluate(i: Intent) {
        val now = SystemClock.elapsedRealtime()
        val temp = i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10
        val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val plugged = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
        val pct = if (level >= 0 && scale > 0) level * 100 / scale else -1

        val tl = appSettings.alertTemp
        if (tl > 0 && temp >= tl && now - lastTempAlert > 180_000L) {
            lastTempAlert = now
            notifyUser(context.getString(R.string.alert_temp_message, temp))
        }
        val bl = appSettings.alertBattery
        if (bl > 0 && pct in 0..bl && !plugged && now - lastBatteryAlert > 300_000L) {
            lastBatteryAlert = now
            notifyUser(context.getString(R.string.alert_battery_message, pct))
        }

        val ac = autoCoolTemp
        if (ac > 0) {
            if (temp >= ac && !PerfModes.coolActive.value) {
                coolByUs = true
                PerfModes.setCool(context, true)
                notifyUser(context.getString(R.string.auto_cool_on, temp))
            } else if (coolByUs && temp <= ac - 4 && PerfModes.coolActive.value) {
                coolByUs = false
                PerfModes.setCool(context, false)
                notifyUser(context.getString(R.string.auto_cool_off, temp))
            }
        }
    }

    private fun notifyUser(message: String) {
        main.post {
            try {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                context.getSystemService(Vibrator::class.java)
                    ?.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
            } catch (_: Exception) {
            }
        }
    }
}
