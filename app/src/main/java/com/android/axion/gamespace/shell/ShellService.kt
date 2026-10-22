package com.android.axion.gamespace.shell

import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.system.exitProcess

/**
 * Berjalan di proses terpisah milik Shizuku (UID shell atau root), bukan di proses aplikasi.
 * Jangan memakai Context/Hilt di sini.
 */
class ShellService : IShellService.Stub() {

    override fun destroy() {
        exitProcess(0)
    }

    override fun uid(): Int = android.os.Process.myUid()

    override fun exec(command: String, timeoutMs: Int): String {
        return try {
            val p = ProcessBuilder("sh", "-c", command).redirectErrorStream(true).start()
            val sb = StringBuffer()
            val reader = thread(isDaemon = true) {
                try {
                    p.inputStream.bufferedReader().use { r ->
                        val buf = CharArray(4096)
                        while (true) {
                            val n = r.read(buf)
                            if (n < 0) break
                            sb.append(buf, 0, n)
                        }
                    }
                } catch (_: Exception) {}
            }
            val finished = p.waitFor(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
            if (!finished) p.destroyForcibly()
            reader.join(500)
            val code = if (finished) p.exitValue() else -2
            "$code\n$sb"
        } catch (e: Exception) {
            "-1\n${e.message}"
        }
    }
}
