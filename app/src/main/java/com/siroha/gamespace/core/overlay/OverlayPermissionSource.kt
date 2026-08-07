package com.siroha.gamespace.core.overlay

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SYSTEM_ALERT_WINDOW is a "special" permission like usage access — listed
 * in the manifest, but only actually granted through a dedicated Settings
 * screen the user has to visit themselves, not a runtime permission
 * dialog. [Settings.canDrawOverlays] is the correct live-status check;
 * the permission being in the manifest tells the system nothing about
 * whether it's actually granted.
 */
@Singleton
class OverlayPermissionSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun hasPermission(): Boolean = Settings.canDrawOverlays(context)

    /** Deep-links straight to this app's entry in the overlay-permission
     *  list — unlike usage access, this one does support a package URI. */
    fun openPermissionSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
