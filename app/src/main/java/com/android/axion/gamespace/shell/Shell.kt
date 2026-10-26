package com.android.axion.gamespace.shell

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.android.axion.gamespace.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import rikka.shizuku.Shizuku
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

data class ShellResult(val code: Int, val out: String) {
    val ok get() = code == 0
    val text get() = out.trim()
}

enum class Backend { NONE, SHIZUKU, ROOT }

data class ShellStatus(
    val backend: Backend = Backend.NONE,
    val shizukuInstalled: Boolean = false,
    val shizukuRunning: Boolean = false,
    val shizukuGranted: Boolean = false,
    val rootAvailable: Boolean = false,
    val uid: Int = -1,
)

/**
 * Akses perintah shell berhak tinggi. Prioritas: Shizuku (UserService) -> root (su) -> tidak ada.
 * Jangan panggil exec() dari main thread; pakai run() untuk fire-and-forget.
 */
object Shell {
    private const val SHIZUKU_PKG = "moe.shizuku.privileged.api"
    private const val REQUEST_CODE = 4107

    private lateinit var appContext: Context
    private val io = Executors.newCachedThreadPool()

    @Volatile private var service: IShellService? = null
    @Volatile private var rootChecked = false
    @Volatile private var rootOk = false
    private var initialized = false

    private val _status = MutableStateFlow(ShellStatus())
    val status: StateFlow<ShellStatus> get() = _status

    @Volatile private var bindingSince = 0L

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if (binder != null && binder.pingBinder()) {
                service = IShellService.Stub.asInterface(binder)
                bindingSince = 0L
                refresh()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            bindingSince = 0L
            refresh()
        }

        override fun onBindingDied(name: ComponentName?) {
            service = null
            bindingSince = 0L
            refresh()
        }
    }

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        appContext = context.applicationContext
        try {
            Shizuku.addBinderReceivedListenerSticky { bind() }
            Shizuku.addBinderDeadListener { service = null; refresh() }
            Shizuku.addRequestPermissionResultListener { _, result ->
                if (result == PackageManager.PERMISSION_GRANTED) bind()
                refresh()
            }
        } catch (_: Throwable) {}
        io.execute { probeRoot(); refresh() }
        refresh()
    }

    private fun shizukuInstalled(): Boolean = try {
        appContext.packageManager.getPackageInfo(SHIZUKU_PKG, 0); true
    } catch (_: Exception) { false }

    private fun shizukuRunning(): Boolean = try { Shizuku.pingBinder() } catch (_: Throwable) { false }

    private fun shizukuGranted(): Boolean = try {
        shizukuRunning() && !Shizuku.isPreV11() &&
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Throwable) { false }

    fun refresh() {
        val svc = service
        val uid = try { svc?.uid() ?: -1 } catch (_: Exception) { -1 }
        val backend = when {
            svc != null -> Backend.SHIZUKU
            rootOk -> Backend.ROOT
            else -> Backend.NONE
        }
        _status.value = ShellStatus(
            backend = backend,
            shizukuInstalled = shizukuInstalled(),
            shizukuRunning = shizukuRunning(),
            shizukuGranted = shizukuGranted(),
            rootAvailable = rootOk,
            uid = if (backend == Backend.ROOT) 0 else uid,
        )
    }

    /** Meminta izin Shizuku (menampilkan dialog dari aplikasi Shizuku). */
    fun requestShizukuPermission() {
        try {
            if (!shizukuRunning()) return
            if (shizukuGranted()) { bind(); return }
            Shizuku.requestPermission(REQUEST_CODE)
        } catch (_: Throwable) {}
    }

    private fun bind() {
        if (service != null) return
        val now = android.os.SystemClock.elapsedRealtime()
        if (bindingSince != 0L && now - bindingSince < 5000L) return // sedang menunggu koneksi
        try {
            if (!shizukuGranted()) return
            bindingSince = now
            val args = Shizuku.UserServiceArgs(
                ComponentName(appContext.packageName, ShellService::class.java.name)
            ).daemon(false)
                .processNameSuffix("shell")
                .debuggable(BuildConfig.DEBUG)
                .version(BuildConfig.VERSION_CODE)
            Shizuku.bindUserService(args, connection)
        } catch (_: Throwable) {}
    }

    private fun probeRoot() {
        rootChecked = true
        rootOk = try {
            val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "id -u"))
            val done = p.waitFor(3, TimeUnit.SECONDS)
            done && p.inputStream.bufferedReader().readText().trim() == "0"
        } catch (_: Exception) { false }
    }

    /** Jika service Shizuku belum/tidak lagi terhubung tetapi izin sudah ada, coba bind ulang. */
    fun available(): Boolean {
        if (service == null && initialized) bind()
        return service != null || rootOk
    }

    /** Blocking. Jangan dipanggil dari main thread. */
    fun exec(command: String, timeoutMs: Int = 8000): ShellResult {
        service?.let { svc ->
            return try {
                parse(svc.exec(command, timeoutMs))
            } catch (e: android.os.DeadObjectException) {
                service = null // binder mati: biarkan available() melakukan bind ulang
                bindingSince = 0L
                ShellResult(-1, "binder mati")
            } catch (e: Exception) {
                ShellResult(-1, e.message ?: "binder error")
            }
        }
        if (!rootChecked) probeRoot()
        if (rootOk) {
            return try {
                val p = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
                val finished = p.waitFor(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
                if (!finished) p.destroyForcibly()
                val out = p.inputStream.bufferedReader().readText() + p.errorStream.bufferedReader().readText()
                ShellResult(if (finished) p.exitValue() else -2, out)
            } catch (e: Exception) {
                ShellResult(-1, e.message ?: "su error")
            }
        }
        return ShellResult(-1, "Tidak ada akses Shizuku/root")
    }

    /** Fire-and-forget di thread latar. */
    fun run(command: String, onDone: ((ShellResult) -> Unit)? = null) {
        io.execute {
            val r = exec(command)
            onDone?.invoke(r)
        }
    }

    fun background(block: () -> Unit) = io.execute(block)

    private fun parse(raw: String?): ShellResult {
        if (raw == null) return ShellResult(-1, "")
        val i = raw.indexOf('\n')
        if (i < 0) return ShellResult(raw.trim().toIntOrNull() ?: -1, "")
        return ShellResult(raw.substring(0, i).trim().toIntOrNull() ?: -1, raw.substring(i + 1))
    }

    fun openShizukuApp(context: Context): Boolean {
        val i = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PKG) ?: return false
        i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(i)
        return true
    }
}
