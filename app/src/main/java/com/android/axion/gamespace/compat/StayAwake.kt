package com.android.axion.gamespace.compat

import android.content.Context
import android.os.PowerManager

/** Menjaga layar tetap menyala selama sesi game (pengganti kunci "gamespace_stay_awake" milik ROM Axion). */
@Suppress("DEPRECATION")
object StayAwake {
    @Volatile var sessionActive = false
    @Volatile var enabled = false
    private var lock: PowerManager.WakeLock? = null

    @Synchronized
    fun update(ctx: Context) {
        val want = sessionActive && enabled
        if (want && lock == null) {
            val pm = ctx.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
            lock = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE,
                "GameSpace:StayAwake"
            ).apply { setReferenceCounted(false); acquire() }
        } else if (!want) {
            lock?.let { if (it.isHeld) it.release() }
            lock = null
        }
    }
}
