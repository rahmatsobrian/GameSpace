package com.siroha.gamespace.core.privilege

import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import rikka.shizuku.Shizuku
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Shizuku access via dev.rikka.shizuku:api. Shizuku itself is a separate
 * app the user installs; on a non-rooted device its service is started via
 * one-time adb/wireless-debugging pairing, on a rooted device it can start
 * itself. Either way, once its binder is alive, permission works like any
 * other Android runtime permission.
 *
 * [newRemoteExec] is the one method in this file I'd double check first —
 * `Shizuku.newProcess(...)` returning an `IRemoteProcess` is the shape I
 * remember for shell-style execution over Shizuku, but this was written
 * without a compiler to confirm the exact current signature. Permission
 * check/request below (`checkSelfPermission` / `requestPermission` /
 * `addRequestPermissionResultListener`) is the part I'm confident in —
 * that shape is stable across recent Shizuku-API releases.
 */
@Singleton
class ShizukuPrivilegeSource @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val listeners = mutableMapOf<Int, Shizuku.OnRequestPermissionResultListener>()
    private var nextRequestCode = 9000

    fun peekStatus(): SourceStatus {
        if (!isShizukuAppInstalled()) {
            return SourceStatus(PrivilegeTier.SHIZUKU, AccessState.NOT_AVAILABLE, detail = "Shizuku belum terpasang")
        }
        if (!Shizuku.pingBinder()) {
            return SourceStatus(PrivilegeTier.SHIZUKU, AccessState.NOT_RUNNING, detail = "servis belum aktif")
        }
        val granted = runCatching { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED }
            .getOrDefault(false)
        return if (granted) {
            SourceStatus(PrivilegeTier.SHIZUKU, AccessState.GRANTED)
        } else {
            SourceStatus(PrivilegeTier.SHIZUKU, AccessState.NOT_REQUESTED)
        }
    }

    /**
     * Suspends until the user answers the Shizuku permission dialog. Safe
     * to call even when a shell/service isn't running yet — it just
     * reports NOT_RUNNING immediately instead of hanging, since there's no
     * dialog Shizuku can show in that state.
     */
    suspend fun requestAccess(): SourceStatus {
        val current = peekStatus()
        if (current.state != AccessState.NOT_REQUESTED) return current

        return suspendCancellableCoroutine { continuation ->
            val requestCode = nextRequestCode++
            val listener = Shizuku.OnRequestPermissionResultListener { code, grantResult ->
                if (code != requestCode) return@OnRequestPermissionResultListener
                listeners.remove(requestCode)?.let { Shizuku.removeRequestPermissionResultListener(it) }
                val granted = grantResult == PackageManager.PERMISSION_GRANTED
                val result = if (granted) {
                    SourceStatus(PrivilegeTier.SHIZUKU, AccessState.GRANTED)
                } else {
                    SourceStatus(PrivilegeTier.SHIZUKU, AccessState.PERMISSION_DENIED, detail = "ditolak pengguna")
                }
                if (continuation.isActive) continuation.resume(result)
            }
            listeners[requestCode] = listener
            Shizuku.addRequestPermissionResultListener(listener)

            continuation.invokeOnCancellation {
                listeners.remove(requestCode)?.let { Shizuku.removeRequestPermissionResultListener(it) }
            }

            Shizuku.requestPermission(requestCode)
        }
    }

    suspend fun exec(command: String): PrivilegedExecResult {
        if (peekStatus().state != AccessState.GRANTED) return PrivilegedExecResult.Unavailable
        return newRemoteExec(command)
    }

    private fun newRemoteExec(command: String): PrivilegedExecResult = runCatching {
        val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
        val output = process.inputStream.bufferedReader().readLines()
        val exit = process.waitFor()
        if (exit == 0) {
            PrivilegedExecResult.Success(output)
        } else {
            PrivilegedExecResult.Failure("exit $exit")
        }
    }.getOrElse { e ->
        PrivilegedExecResult.Failure(e.message ?: "Shizuku exec gagal")
    }

    private fun isShizukuAppInstalled(): Boolean = try {
        context.packageManager.getPackageInfo(SHIZUKU_PACKAGE_NAME, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    private companion object {
        const val SHIZUKU_PACKAGE_NAME = "moe.shizuku.privileged.api"
    }
}
