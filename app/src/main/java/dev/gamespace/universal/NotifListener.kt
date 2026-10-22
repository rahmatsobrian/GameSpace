package dev.gamespace.universal

import android.app.Notification
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/** Meneruskan notifikasi ke overlay "danmaku" selama sesi game. */
class NotifListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.isOngoing || sbn.packageName == packageName) return
        val ex = sbn.notification.extras
        val title = ex.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = ex.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return
        val line = "$title: $text".trim(' ', ':')
        Handler(Looper.getMainLooper()).post {
            GameMonitorService.instance?.onNotification(sbn.packageName, line)
        }
    }
}
