package dev.gamespace.universal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && Store(context).serviceEnabled) {
            try { GameMonitorService.start(context) } catch (_: Exception) {}
        }
    }
}
