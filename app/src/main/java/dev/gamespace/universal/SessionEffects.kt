package dev.gamespace.universal

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.provider.Settings

/** Menerapkan dan mengembalikan pengaturan sistem selama sesi game (hanya API publik). */
class SessionEffects(private val ctx: Context, private val store: Store) {
    private var origBrightnessMode: Int? = null
    private var origFilter: Int? = null
    private var origRinger: Int? = null

    fun apply() {
        val cr = ctx.contentResolver
        if (store.noAutoBrightness && Settings.System.canWrite(ctx)) {
            try {
                origBrightnessMode = Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE)
                Settings.System.putInt(
                    cr, Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                )
            } catch (_: Exception) {}
        }

        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (store.autoDnd && nm.isNotificationPolicyAccessGranted) {
            try {
                origFilter = nm.currentInterruptionFilter
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            } catch (_: Exception) {}
        }

        val target = when (store.ringerMode) {
            0 -> AudioManager.RINGER_MODE_SILENT
            1 -> AudioManager.RINGER_MODE_VIBRATE
            2 -> AudioManager.RINGER_MODE_NORMAL
            else -> null
        }
        if (target != null) {
            try {
                val am = ctx.getSystemService(AudioManager::class.java)
                origRinger = am.ringerMode
                am.ringerMode = target
            } catch (_: Exception) {} // mode senyap butuh akses DND
        }
    }

    fun restore() {
        origBrightnessMode?.let {
            try { Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, it) } catch (_: Exception) {}
        }
        origFilter?.let {
            try { ctx.getSystemService(NotificationManager::class.java).setInterruptionFilter(it) } catch (_: Exception) {}
        }
        origRinger?.let {
            try { ctx.getSystemService(AudioManager::class.java).ringerMode = it } catch (_: Exception) {}
        }
        origBrightnessMode = null; origFilter = null; origRinger = null
    }
}
