package com.siroha.gamespace.core.blocker

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deliberately SharedPreferences, not DataStore, even though every other
 * piece of persisted state in this app either already is or eventually
 * will be DataStore. `NotificationListenerService.onNotificationPosted`
 * and `CallScreeningService.onScreenCall` are plain synchronous callbacks
 * — there's no coroutine scope to collect a Flow from, and blocking that
 * callback on `runBlocking` to read a DataStore value would be worse than
 * just using the synchronous API actually built for this.
 */
@Singleton
class BlockerSettingsStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("blocker_settings", Context.MODE_PRIVATE)

    var isNotificationBlockingActive: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATION_BLOCKING, false)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATION_BLOCKING, value).apply()

    var isCallBlockingActive: Boolean
        get() = prefs.getBoolean(KEY_CALL_BLOCKING, false)
        set(value) = prefs.edit().putBoolean(KEY_CALL_BLOCKING, value).apply()

    private companion object {
        const val KEY_NOTIFICATION_BLOCKING = "notification_blocking_active"
        const val KEY_CALL_BLOCKING = "call_blocking_active"
    }
}
