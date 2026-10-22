package com.android.axion.gamespace.gamebar

import android.content.Context
import android.content.Intent
import com.android.axion.gamespace.shell.Shell

/**
 * Pengganti FreeformLauncher milik ROM Axion: membuka aplikasi dalam mode freeform lewat
 * `am start --windowingMode 5` (butuh Shizuku/root). Tanpa itu, aplikasi dibuka biasa.
 */
object FreeformLauncher {
    @Volatile private var ctx: Context? = null

    fun init(context: Context) {
        ctx = context.applicationContext
    }

    fun launch(packageName: String) {
        val c = ctx ?: return
        val intent = c.packageManager.getLaunchIntentForPackage(packageName) ?: return
        val component = intent.component ?: return
        if (Shell.available()) {
            Shell.run(
                "settings put global enable_freeform_support 1; " +
                    "settings put global force_resizable_activities 1; " +
                    "am start -n ${component.flattenToShortString()} --windowingMode 5"
            )
        } else {
            c.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
