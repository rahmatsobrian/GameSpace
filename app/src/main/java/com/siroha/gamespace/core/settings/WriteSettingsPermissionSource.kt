package com.siroha.gamespace.core.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WRITE_SETTINGS — another special AppOps-style permission, this one
 * gating writes to the Settings.System table (brightness here). Reading
 * Settings.System needs nothing special; only writing does.
 */
@Singleton
class WriteSettingsPermissionSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun hasPermission(): Boolean = Settings.System.canWrite(context)

    fun openPermissionSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_WRITE_SETTINGS,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
