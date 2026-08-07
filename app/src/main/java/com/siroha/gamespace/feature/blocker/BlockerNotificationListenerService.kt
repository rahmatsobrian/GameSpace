package com.siroha.gamespace.feature.blocker

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.siroha.gamespace.core.blocker.BlockerSettingsStore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The system binds/unbinds this automatically based on whether
 * "Notification access" is granted — this app never starts or stops it
 * directly. [BlockerSettingsStore.isNotificationBlockingActive] is read
 * fresh on every call, so flipping that flag from BlockerScreen takes
 * effect on the very next notification without needing to touch this
 * service's lifecycle at all.
 *
 * Blocks everything from every other app while active, on purpose — no
 * separate per-app blocklist to manage yet, which also means an SMS/
 * messaging app's notifications are covered automatically without this
 * needing its own SMS-specific permission or logic.
 */
@AndroidEntryPoint
class BlockerNotificationListenerService : NotificationListenerService() {

    @Inject
    lateinit var blockerSettingsStore: BlockerSettingsStore

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!blockerSettingsStore.isNotificationBlockingActive) return
        if (sbn.packageName == packageName) return // never cancel our own (e.g. the overlay's foreground-service notification)
        cancelNotification(sbn.key)
    }
}
