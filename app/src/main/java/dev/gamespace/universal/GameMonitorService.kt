package dev.gamespace.universal

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

/**
 * Foreground service: mendeteksi game di layar lewat UsageStatsManager,
 * lalu menerapkan efek sesi dan menampilkan overlay.
 */
class GameMonitorService : Service() {

    companion object {
        @Volatile var instance: GameMonitorService? = null
        private const val CH = "gamespace"

        fun start(ctx: Context) =
            ContextCompat.startForegroundService(ctx, Intent(ctx, GameMonitorService::class.java))

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, GameMonitorService::class.java))
        }
    }

    private lateinit var store: Store
    private lateinit var effects: SessionEffects
    private lateinit var overlay: Overlay
    private lateinit var usm: UsageStatsManager
    private val handler = Handler(Looper.getMainLooper())

    private var current: String? = null
    private var active: String? = null
    private var lastQuery = 0L
    private var callRxRegistered = false

    private val tick = object : Runnable {
        override fun run() {
            try { poll() } catch (_: Exception) {}
            handler.postDelayed(this, 1500L)
        }
    }

    private val callRx = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            if (i.getStringExtra(TelephonyManager.EXTRA_STATE) != TelephonyManager.EXTRA_STATE_RINGING) return
            val tm = getSystemService(TelecomManager::class.java)
            try {
                @Suppress("DEPRECATION")
                when (store.callsMode) {
                    1 -> tm.acceptRingingCall()
                    2 -> tm.endCall()
                    else -> {}
                }
            } catch (_: Exception) {}
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        store = Store(this)
        effects = SessionEffects(this, store)
        overlay = Overlay(this, store)
        usm = getSystemService(UsageStatsManager::class.java)

        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH, "Game Space", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val n = Notification.Builder(this, CH)
            .setContentTitle("Game Space aktif")
            .setContentText("Memantau game di daftar kamu")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(open)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, n)
        }
        handler.post(tick)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    private fun poll() {
        if (Perms.hasUsage(this)) {
            val now = System.currentTimeMillis()
            val from = if (lastQuery == 0L) now - 15_000L else lastQuery - 500L
            val events = usm.queryEvents(from, now)
            val e = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(e)
                when (e.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> current = e.packageName
                    UsageEvents.Event.ACTIVITY_PAUSED -> if (current == e.packageName) current = null
                }
            }
            lastQuery = now
        }
        val target = current?.takeIf { it in store.games }
        if (target != active) {
            if (active != null) endSession()
            if (target != null) startSession(target)
        }
    }

    private fun startSession(pkg: String) {
        active = pkg
        effects.apply()
        overlay.show(pkg)
        if (store.callsMode != 0 && !callRxRegistered) {
            ContextCompat.registerReceiver(
                this, callRx, IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            callRxRegistered = true
        }
    }

    private fun endSession() {
        overlay.hide()
        effects.restore()
        if (callRxRegistered) {
            try { unregisterReceiver(callRx) } catch (_: Exception) {}
            callRxRegistered = false
        }
        active = null
    }

    fun onNotification(pkg: String, line: String) {
        if (active != null && store.danmaku && pkg != active) overlay.danmaku(line)
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        if (active != null) endSession()
        instance = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
