package com.android.axion.gamespace.gamebar

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import com.android.axion.gamespace.R
import com.android.axion.gamespace.compat.FpsSource
import com.android.axion.gamespace.compat.GameTimers
import com.android.axion.gamespace.compat.NetStats
import com.android.axion.gamespace.compat.PingMonitor
import com.android.axion.gamespace.compat.SystemStats
import com.android.axion.gamespace.compat.TaskFpsCallback
import com.android.axion.gamespace.data.AppSettings

/** Jembatan antara tile "Mini HUD" dan sesi yang sedang berjalan. */
object HudBus {
    @Volatile var handler: ((Boolean) -> Unit)? = null
}

/** Bit isi HUD. */
object HudFields {
    const val FPS = 1
    const val PING = 2
    const val CPU = 4
    const val TEMP = 8
    const val RAM = 16
    const val GPU = 32
    const val NET = 64
    const val POWER = 128
    const val MATCH = 256
    const val TIMERS = 512
    const val DEFAULT = FPS or PING or TEMP
}

/**
 * HUD mengambang kecil yang tidak bisa disentuh, jadi tidak mengganggu game. Isinya bisa mengikuti
 * genre game (fieldsOverride) atau pilihan manual pengguna.
 */
class MiniHud(private val context: Context, private val appSettings: AppSettings) {
    private val wm = context.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var view: TextView? = null
    @Volatile private var fps = 0f
    private var fpsRegistered = false

    /** Jika tidak null, dipakai menggantikan isi HUD pilihan pengguna (mode "ikuti genre"). */
    @Volatile var fieldsOverride: Int? = null

    private val fpsCallback = object : TaskFpsCallback() {
        override fun onFpsReported(fps: Float) {
            this@MiniHud.fps = fps
        }
    }

    private val ticker = object : Runnable {
        override fun run() {
            view?.text = build()
            handler.postDelayed(this, 1000L)
        }
    }

    private fun dp(v: Int) = (v * context.resources.displayMetrics.density).toInt()

    private fun currentFields() = fieldsOverride ?: appSettings.hudFields

    fun show(packageName: String) {
        if (view != null) return
        if (!Settings.canDrawOverlays(context)) return
        val fields = currentFields()
        if (fields and HudFields.FPS != 0) {
            FpsSource.register(fpsCallback, packageName)
            fpsRegistered = true
        }
        if (fields and (HudFields.PING or HudFields.NET) != 0) PingMonitor.start()

        val tv = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setShadowLayer(3f, 0f, 0f, Color.BLACK)
            setPadding(dp(8), dp(4), dp(8), dp(4))
            background = GradientDrawable().apply {
                cornerRadius = dp(8).toFloat()
                setColor(0xB0000000.toInt())
            }
            alpha = appSettings.hudOpacity / 100f
            text = build()
        }
        val gravity = when (appSettings.hudPosition) {
            1 -> Gravity.TOP or Gravity.END
            2 -> Gravity.BOTTOM or Gravity.START
            3 -> Gravity.BOTTOM or Gravity.END
            else -> Gravity.TOP or Gravity.START
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            this.gravity = gravity
            x = dp(10)
            y = dp(10)
        }
        try {
            wm.addView(tv, lp)
            view = tv
            handler.postDelayed(ticker, 1000L)
        } catch (_: Exception) {
        }
    }

    fun hide() {
        handler.removeCallbacks(ticker)
        view?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        view = null
        if (fpsRegistered) {
            FpsSource.unregister(fpsCallback)
            fpsRegistered = false
        }
    }

    private fun netLabel(n: NetStats): String = when {
        n.avg < 0 -> "-"
        n.quality == 2 -> context.getString(R.string.net_poor)
        n.quality == 1 -> context.getString(R.string.net_fair)
        else -> context.getString(R.string.net_good)
    }

    private fun build(): String {
        val f = currentFields()
        val s = SystemStats.stats.value
        val net = PingMonitor.net.value
        val lines = ArrayList<String>()

        val l1 = ArrayList<String>()
        if (f and HudFields.FPS != 0) l1.add("FPS ${fps.toInt()}")
        if (f and HudFields.PING != 0) {
            val p = PingMonitor.ping.value
            l1.add(if (p >= 0) "PING ${p}ms" else "PING -")
        }
        if (f and HudFields.NET != 0) {
            l1.add(
                "NET " + netLabel(net) +
                    (if (net.jitter >= 0) " \u00B1${net.jitter} L${net.loss}%" else "")
            )
        }
        if (l1.isNotEmpty()) lines.add(l1.joinToString("  "))

        val l2 = ArrayList<String>()
        if (f and HudFields.CPU != 0) {
            l2.add(
                if (s.cpuPercent >= 0) "CPU ${s.cpuPercent}%" +
                    (if (s.cpuFreqMhz > 0) " ${"%.1f".format(s.cpuFreqMhz / 1000f)}GHz" else "")
                else "CPU -"
            )
        }
        if (f and HudFields.GPU != 0) l2.add(if (s.gpuPercent >= 0) "GPU ${s.gpuPercent}%" else "GPU -")
        if (f and HudFields.TEMP != 0) l2.add(if (s.cpuTempC >= 0) "${s.cpuTempC}\u00B0C" else "-\u00B0C")
        if (f and HudFields.RAM != 0) l2.add(if (s.ramPercent >= 0) "RAM ${s.ramPercent}%" else "RAM -")
        if (f and HudFields.POWER != 0) l2.add(if (s.powerMw >= 0) "${"%.1f".format(s.powerMw / 1000f)}W" else "-W")
        if (l2.isNotEmpty()) lines.add(l2.joinToString("  "))

        val l3 = ArrayList<String>()
        if (f and HudFields.MATCH != 0) {
            val m = GameTimers.matchText()
            if (m.isNotEmpty()) l3.add("MATCH $m")
        }
        if (f and HudFields.TIMERS != 0) {
            val t = GameTimers.activeText()
            if (t.isNotEmpty()) l3.add(t)
        }
        if (l3.isNotEmpty()) lines.add(l3.joinToString("  "))

        return if (lines.isEmpty()) "-" else lines.joinToString("\n")
    }
}
