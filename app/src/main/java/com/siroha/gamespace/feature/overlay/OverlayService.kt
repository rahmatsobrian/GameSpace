package com.siroha.gamespace.feature.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.siroha.gamespace.R
import com.siroha.gamespace.data.monitoring.DeviceSnapshot
import com.siroha.gamespace.data.monitoring.MonitoringRepository
import com.siroha.gamespace.data.quicktoggle.QuickToggleRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Started/stopped explicitly by the user (see DeviceStatusScreen's toggle)
 * — never launched automatically. `foregroundServiceType="specialUse"` is
 * declared in the manifest alongside this class; see the manifest comment
 * for why that's the type used.
 *
 * The Lifecycle/ViewModelStore/SavedStateRegistry wiring here (via
 * OverlayLifecycleOwner) is the least-certain part of this whole project —
 * see that file's doc comment.
 */
@AndroidEntryPoint
class OverlayService : Service() {

    @Inject
    lateinit var monitoringRepository: MonitoringRepository

    @Inject
    lateinit var quickToggleRepository: QuickToggleRepository

    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private val overlayLifecycleOwner = OverlayLifecycleOwner()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var latestSnapshot by mutableStateOf<DeviceSnapshot?>(null)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())

        overlayLifecycleOwner.performRestore(null)
        overlayLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        overlayLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        overlayLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        addOverlayView()

        // Collecting on Dispatchers.Main (this scope) is what makes the
        // `latestSnapshot = it` write below main-thread-safe — the
        // repository does its own polling work on IO and hands off to
        // whatever context called collect().
        serviceScope.launch {
            monitoringRepository.observe().collect { latestSnapshot = it }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        removeOverlayView()
        overlayLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }

    private fun addOverlayView() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            // minSdk is 29 (well past API 26), so TYPE_APPLICATION_OVERLAY
            // always applies here — no need for the older TYPE_PHONE
            // fallback other guides show for supporting pre-Oreo devices.
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 300
        }

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(overlayLifecycleOwner)
            setViewTreeViewModelStoreOwner(overlayLifecycleOwner)
            setViewTreeSavedStateRegistryOwner(overlayLifecycleOwner)
        }
        composeView.setContent {
            OverlayBubbleContent(
                snapshot = latestSnapshot,
                quickToggleRepository = quickToggleRepository,
                onDrag = { dxPx, dyPx ->
                    params.x += dxPx.toInt()
                    params.y += dyPx.toInt()
                    runCatching { windowManager.updateViewLayout(composeView, params) }
                },
                onClose = { stopSelf() }
            )
        }

        overlayView = composeView
        windowManager.addView(composeView, params)
    }

    private fun removeOverlayView() {
        overlayView?.let { view -> runCatching { windowManager.removeView(view) } }
        overlayView = null
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Overlay GameSpace",
                NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_body))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "overlay_service"

        fun start(context: Context) {
            context.startForegroundService(Intent(context, OverlayService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, OverlayService::class.java))
        }
    }
}
