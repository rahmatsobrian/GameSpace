package com.siroha.gamespace.core.settings

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationPolicyPermissionSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager
        get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun hasPermission(): Boolean = notificationManager.isNotificationPolicyAccessGranted

    fun openPermissionSettings() {
        context.startActivity(
            Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
