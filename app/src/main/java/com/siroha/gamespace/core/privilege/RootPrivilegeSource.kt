package com.siroha.gamespace.core.privilege

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Root access via libsu (com.github.topjohnwu.libsu). This talks to
 * whatever su binary is on the device — Magisk, KernelSU, and APatch all
 * expose a compatible one, so there is deliberately no per-solution branch
 * here. If you test on an APatch device and something behaves differently,
 * that's new information worth feeding back in, not something this file
 * currently accounts for.
 *
 * Two libsu method names below (`getCachedShell`, `Shell.Result.err`) are
 * the parts of this file most worth double-checking against whatever libsu
 * version Android Studio actually resolves — this was written without a
 * compiler in the loop.
 */
@Singleton
class RootPrivilegeSource @Inject constructor() {

    /**
     * Non-invasive: reports what we already know without prompting anything.
     * Returns UNKNOWN until [requestAccess] has been called at least once
     * this process lifetime — libsu has no side-channel to ask "is a root
     * solution installed" without going through the actual grant flow.
     */
    fun peekStatus(): SourceStatus {
        val cached = Shell.getCachedShell() ?: return SourceStatus(PrivilegeTier.ROOT, AccessState.UNKNOWN)
        return if (cached.isRoot) {
            SourceStatus(PrivilegeTier.ROOT, AccessState.GRANTED, detail = "su shell aktif")
        } else {
            SourceStatus(PrivilegeTier.ROOT, AccessState.PERMISSION_DENIED, detail = "shell tersedia, bukan root")
        }
    }

    /**
     * This IS the request — obtaining the shell is what surfaces the
     * Magisk/KernelSU/APatch grant dialog. Only call this from an explicit
     * user action (a button tap), never automatically on screen load.
     */
    suspend fun requestAccess(): SourceStatus = withContext(Dispatchers.IO) {
        try {
            val shell = Shell.getShell()
            if (shell.isRoot) {
                SourceStatus(PrivilegeTier.ROOT, AccessState.GRANTED, detail = "su shell aktif")
            } else {
                SourceStatus(
                    PrivilegeTier.ROOT,
                    AccessState.PERMISSION_DENIED,
                    detail = "tidak ada su terdeteksi, atau permintaan ditolak"
                )
            }
        } catch (e: Exception) {
            SourceStatus(PrivilegeTier.ROOT, AccessState.NOT_AVAILABLE, detail = e.message)
        }
    }

    suspend fun exec(command: String): PrivilegedExecResult = withContext(Dispatchers.IO) {
        val cached = Shell.getCachedShell()
        if (cached == null || !cached.isRoot) {
            return@withContext PrivilegedExecResult.Unavailable
        }
        val result = Shell.cmd(command).exec()
        if (result.isSuccess) {
            PrivilegedExecResult.Success(result.out)
        } else {
            PrivilegedExecResult.Failure("exit ${result.code}: ${result.err.joinToString("\n")}")
        }
    }
}
