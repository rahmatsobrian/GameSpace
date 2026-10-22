package com.android.axion.gamespace.compat

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import com.android.axion.gamespace.shell.Shell

/**
 * Menulis pengaturan sistem: langsung jika WRITE_SECURE_SETTINGS/WRITE_SETTINGS sudah diberikan,
 * jika tidak lewat shell (Shizuku/root). Aman dipanggil dari main thread.
 */
object Sys {
    private fun hasSecure(ctx: Context) =
        ctx.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    fun putGlobal(ctx: Context, key: String, value: String) {
        if (hasSecure(ctx)) {
            try { Settings.Global.putString(ctx.contentResolver, key, value); return } catch (_: Exception) {}
        }
        Shell.run("settings put global $key $value")
    }

    fun putSecure(ctx: Context, key: String, value: String) {
        if (hasSecure(ctx)) {
            try { Settings.Secure.putString(ctx.contentResolver, key, value); return } catch (_: Exception) {}
        }
        Shell.run("settings put secure $key '${value.replace("'", "'\\''")}'")
    }

    fun putSystem(ctx: Context, key: String, value: String) {
        if (Settings.System.canWrite(ctx)) {
            try { Settings.System.putString(ctx.contentResolver, key, value); return } catch (_: Exception) {}
        }
        Shell.run("settings put system $key $value")
    }
}
