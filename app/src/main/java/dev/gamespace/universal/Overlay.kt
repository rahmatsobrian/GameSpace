package dev.gamespace.universal

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs
import kotlin.random.Random

/** Bubble, panel, crosshair, dan lapisan notifikasi, semuanya TYPE_APPLICATION_OVERLAY. */
class Overlay(private val ctx: Context, private val store: Store) {

    private val wm = ctx.getSystemService(WindowManager::class.java)
    private val type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    private val baseFlags =
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
    private val keepOn = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
    private val noTouch = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE

    private var bubble: TextView? = null
    private var bubbleLp: WindowManager.LayoutParams? = null
    private var panel: View? = null
    private var cross: View? = null
    private var layer: FrameLayout? = null
    private var torchOn = false
    private var gamePkg: String? = null

    private fun dp(v: Int) = (v * ctx.resources.displayMetrics.density).toInt()

    fun show(pkg: String) {
        if (!Settings.canDrawOverlays(ctx)) return
        gamePkg = pkg
        addBubble()
        if (store.crosshair) addCross()
        if (store.danmaku) addLayer()
    }

    fun hide() {
        closePanel()
        bubble?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        bubble = null; bubbleLp = null
        removeCross()
        removeLayer()
        if (torchOn) setTorch(false)
        gamePkg = null
    }

    // ---------- Bubble ----------
    private fun addBubble() {
        val v = TextView(ctx).apply {
            text = "GS"; setTextColor(Color.WHITE); textSize = 12f; gravity = Gravity.CENTER
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(0xCC202830.toInt()) }
            alpha = store.idleAlpha / 100f
        }
        val lp = WindowManager.LayoutParams(
            dp(40), dp(40), type,
            baseFlags or (if (store.stayAwake) keepOn else 0),
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 0; y = dp(120) }
        v.setOnTouchListener(dragListener(lp))
        wm.addView(v, lp)
        bubble = v; bubbleLp = lp
    }

    private fun dragListener(lp: WindowManager.LayoutParams): View.OnTouchListener {
        var sx = 0f; var sy = 0f; var ox = 0; var oy = 0; var moved = false
        return View.OnTouchListener { v, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = e.rawX; sy = e.rawY; ox = lp.x; oy = lp.y; moved = false; true }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - sx).toInt(); val dy = (e.rawY - sy).toInt()
                    if (abs(dx) > dp(6) || abs(dy) > dp(6)) moved = true
                    if (moved) { lp.x = ox + dx; lp.y = oy + dy; wm.updateViewLayout(v, lp) }
                    true
                }
                MotionEvent.ACTION_UP -> { if (!moved) togglePanel(); true }
                else -> false
            }
        }
    }

    private fun applyAwake() {
        val lp = bubbleLp ?: return
        lp.flags = if (store.stayAwake) lp.flags or keepOn else lp.flags and keepOn.inv()
        bubble?.let { wm.updateViewLayout(it, lp) }
    }

    // ---------- Panel ----------
    private fun togglePanel() { if (panel == null) openPanel() else closePanel() }

    private fun closePanel() {
        panel?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        panel = null
    }

    private fun row(label: String, state: () -> Boolean, action: () -> Unit): TextView {
        val t = TextView(ctx)
        fun refresh() {
            t.text = label + "  " + (if (state()) "ON" else "OFF")
            t.setTextColor(if (state()) Color.WHITE else 0x99FFFFFF.toInt())
        }
        t.textSize = 14f
        t.setPadding(0, dp(10), 0, dp(10))
        refresh()
        t.setOnClickListener { action(); refresh() }
        return t
    }

    private fun openPanel() {
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(10), dp(16), dp(14))
            background = GradientDrawable().apply { cornerRadius = dp(16).toFloat(); setColor(0xEE12161C.toInt()) }
        }
        box.addView(row("Stay awake", { store.stayAwake }) { store.stayAwake = !store.stayAwake; applyAwake() })
        box.addView(row("Do not disturb", { dndOn() }) { toggleDnd() })
        box.addView(row("Flashlight", { torchOn }) { setTorch(!torchOn) })
        box.addView(row("Crosshair", { cross != null }) {
            if (cross == null) { store.crosshair = true; addCross() } else { removeCross(); store.crosshair = false }
        })
        box.addView(row("Notification overlay", { layer != null }) {
            if (layer == null) { store.danmaku = true; addLayer() } else { removeLayer(); store.danmaku = false }
        })
        box.addView(TextView(ctx).apply {
            text = "Boost memory"; setTextColor(Color.WHITE); textSize = 14f
            setPadding(0, dp(10), 0, dp(10))
            setOnClickListener { boost() }
        })
        box.addView(TextView(ctx).apply {
            text = "Brightness"; setTextColor(0x99FFFFFF.toInt()); textSize = 12f
            setPadding(0, dp(6), 0, 0)
        })
        val current = try {
            Settings.System.getInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (_: Exception) { 128 }
        box.addView(SeekBar(ctx).apply {
            max = 254
            progress = (current - 1).coerceIn(0, 254)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    if (fromUser) setBrightness(p + 1)
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        })
        val lp = WindowManager.LayoutParams(
            dp(240), WindowManager.LayoutParams.WRAP_CONTENT, type, baseFlags, PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER }
        wm.addView(box, lp)
        panel = box
    }

    private fun setBrightness(v: Int) {
        if (!Settings.System.canWrite(ctx)) {
            Toast.makeText(ctx, "Izinkan 'Modify system settings' untuk Game Space", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            Settings.System.putInt(
                ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS, v)
        } catch (_: Exception) {}
    }

    private fun dndOn(): Boolean {
        val f = ctx.getSystemService(NotificationManager::class.java).currentInterruptionFilter
        return f != NotificationManager.INTERRUPTION_FILTER_ALL && f != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
    }

    private fun toggleDnd() {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (!nm.isNotificationPolicyAccessGranted) {
            try {
                ctx.startActivity(
                    Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (_: Exception) {}
            return
        }
        nm.setInterruptionFilter(
            if (dndOn()) NotificationManager.INTERRUPTION_FILTER_ALL else NotificationManager.INTERRUPTION_FILTER_PRIORITY
        )
    }

    private fun setTorch(on: Boolean) {
        try {
            val cm = ctx.getSystemService(CameraManager::class.java)
            val id = cm.cameraIdList.firstOrNull {
                cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return
            cm.setTorchMode(id, on)
            torchOn = on
        } catch (_: Exception) {}
    }

    @Suppress("DEPRECATION")
    private fun boost() {
        val am = ctx.getSystemService(ActivityManager::class.java)
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val pkgs = ctx.packageManager.queryIntentActivities(i, 0)
            .map { it.activityInfo.packageName }.distinct()
            .filter { it != ctx.packageName && it != gamePkg }
        pkgs.forEach { try { am.killBackgroundProcesses(it) } catch (_: Exception) {} }
        Toast.makeText(ctx, "Background apps dibersihkan: ${pkgs.size}", Toast.LENGTH_SHORT).show()
    }

    // ---------- Crosshair ----------
    private inner class CrossView : View(ctx) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.GREEN; strokeWidth = dp(2).toFloat(); style = Paint.Style.STROKE
        }
        override fun onDraw(c: Canvas) {
            val cx = width / 2f; val cy = height / 2f; val r = dp(4).toFloat()
            c.drawCircle(cx, cy, r, p)
            c.drawLine(cx - dp(16), cy, cx - r - dp(2), cy, p)
            c.drawLine(cx + r + dp(2), cy, cx + dp(16), cy, p)
            c.drawLine(cx, cy - dp(16), cx, cy - r - dp(2), p)
            c.drawLine(cx, cy + r + dp(2), cx, cy + dp(16), p)
        }
    }

    private fun addCross() {
        if (cross != null) return
        val v = CrossView()
        val lp = WindowManager.LayoutParams(dp(40), dp(40), type, baseFlags or noTouch, PixelFormat.TRANSLUCENT)
            .apply { gravity = Gravity.CENTER }
        wm.addView(v, lp)
        cross = v
    }

    private fun removeCross() {
        cross?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        cross = null
    }

    // ---------- Notifikasi bergulir ----------
    private fun addLayer() {
        if (layer != null) return
        val l = FrameLayout(ctx)
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, dp(140), type, baseFlags or noTouch, PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP }
        wm.addView(l, lp)
        layer = l
    }

    private fun removeLayer() {
        layer?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        layer = null
    }

    fun danmaku(text: String) {
        val l = layer ?: return
        val tv = TextView(ctx).apply {
            this.text = text; setTextColor(Color.WHITE); textSize = 16f
            setShadowLayer(4f, 0f, 0f, Color.BLACK); maxLines = 1
        }
        val flp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = Random.nextInt(0, 4) * dp(32) }
        l.addView(tv, flp)
        tv.translationX = ctx.resources.displayMetrics.widthPixels.toFloat()
        tv.post {
            tv.animate().translationX(-tv.width.toFloat()).setDuration(7000L)
                .setInterpolator(LinearInterpolator())
                .withEndAction { l.removeView(tv) }.start()
        }
    }
}
