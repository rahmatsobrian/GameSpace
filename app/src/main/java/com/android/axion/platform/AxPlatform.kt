package com.android.axion.platform

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import com.android.axion.gamespace.shell.Shell
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor

/** Pengganti `ax_platform_hooks`: kunci fitur tile. */
object AxPlatformFeature {
    const val WIFI = "wifi"
    const val BLUETOOTH = "bluetooth"
    const val ZEN = "zen"
    const val ROTATION = "rotation"
    const val MOBILE_DATA = "mobile_data"
    const val AIRPLANE_MODE = "airplane_mode"
    const val FLASHLIGHT = "flashlight"
    const val DARK_MODE = "dark_mode"
    const val LOCATION = "location"
    const val BATTERY_SAVER = "battery_saver"
    const val HOTSPOT = "hotspot"
    const val NFC = "nfc"
    const val NIGHT_LIGHT = "night_light"
    const val AOD = "aod"
    const val DATA_SAVER = "data_saver"
    const val COLOR_INVERSION = "color_inversion"
    const val COLOR_CORRECTION = "color_correction"
    const val REDUCE_BRIGHTNESS = "reduce_brightness"
    const val ONE_HANDED_MODE = "one_handed_mode"
    const val HEADS_UP = "heads_up"
    const val AUTO_SYNC = "auto_sync"
    const val CAMERA_PRIVACY = "camera_privacy"
    const val MIC_PRIVACY = "mic_privacy"
    const val WORK_PROFILE = "work_profile"
    const val USB_TETHER = "usb_tether"
    const val DREAM = "dream"
    const val READING_MODE = "reading_mode"
    const val POWER_SHARE = "power_share"
    const val CAFFEINE = "caffeine"
    const val VPN = "vpn"
    const val CAST = "cast"
    const val PROFILES = "profiles"
    const val SMART_PIXELS = "smart_pixels"
    const val SCREEN_RECORD = "screen_record"
    const val SCREENSHOT = "screenshot"
}

class AxFeatureState(
    val isActive: Boolean = false,
    val label: String? = null,
    val isEmpty: Boolean = false,
) {
    companion object {
        val EMPTY = AxFeatureState(isEmpty = true)
    }
}

/**
 * Pengganti `AxPlatformClient`. Status dibaca dan fitur dinyalakan lewat perintah shell
 * (Shizuku/root); flashlight dan auto-sync memakai API publik.
 */
class AxPlatformClient private constructor() {

    fun interface StateCallback {
        fun onStateChanged(key: String, state: AxFeatureState)
    }

    private class Feat(
        val key: String,
        val read: String? = null,           // perintah pembacaan (stdout)
        val isOn: (String) -> Boolean = { false },
        val on: String? = null,
        val off: String? = null,
        val feature: String? = null,        // PackageManager feature yang disyaratkan
    )

    private lateinit var context: Context
    private val main = Handler(Looper.getMainLooper())
    private val callbacks = CopyOnWriteArrayList<Pair<Executor, StateCallback>>()
    private val states = HashMap<String, AxFeatureState>()
    private var torchOn = false
    private var polling = false
    private var torchRegistered = false
    private var screenRecording = false

    private fun nz(s: String) = s.trim().let { it.isNotEmpty() && it != "null" && it != "0" }
    private fun settingsGet(ns: String, key: String) = "settings get $ns $key"

    private val feats: List<Feat> by lazy {
        listOf(
            Feat(AxPlatformFeature.WIFI, settingsGet("global", "wifi_on"), ::nz, "svc wifi enable", "svc wifi disable", PackageManager.FEATURE_WIFI),
            Feat(AxPlatformFeature.BLUETOOTH, settingsGet("global", "bluetooth_on"), ::nz, "svc bluetooth enable", "svc bluetooth disable", PackageManager.FEATURE_BLUETOOTH),
            Feat(AxPlatformFeature.ZEN, settingsGet("global", "zen_mode"), ::nz, "cmd notification set_dnd on", "cmd notification set_dnd off"),
            Feat(AxPlatformFeature.ROTATION, settingsGet("system", "accelerometer_rotation"), ::nz, "settings put system accelerometer_rotation 1", "settings put system accelerometer_rotation 0"),
            Feat(AxPlatformFeature.MOBILE_DATA, settingsGet("global", "mobile_data"), ::nz, "svc data enable", "svc data disable", PackageManager.FEATURE_TELEPHONY),
            Feat(AxPlatformFeature.AIRPLANE_MODE, settingsGet("global", "airplane_mode_on"), ::nz, "cmd connectivity airplane-mode enable", "cmd connectivity airplane-mode disable"),
            Feat(AxPlatformFeature.DARK_MODE, "cmd uimode night", { it.contains("yes", true) }, "cmd uimode night yes", "cmd uimode night no"),
            Feat(AxPlatformFeature.LOCATION, settingsGet("secure", "location_mode"), ::nz, "cmd location set-location-enabled true", "cmd location set-location-enabled false"),
            Feat(AxPlatformFeature.BATTERY_SAVER, settingsGet("global", "low_power"), ::nz, "settings put global low_power 1", "settings put global low_power 0"),
            Feat(AxPlatformFeature.NFC, "dumpsys nfc | grep -m1 mState", { it.contains("=on", true) }, "svc nfc enable", "svc nfc disable", PackageManager.FEATURE_NFC),
            Feat(AxPlatformFeature.NIGHT_LIGHT, settingsGet("secure", "night_display_activated"), ::nz, "settings put secure night_display_activated 1", "settings put secure night_display_activated 0"),
            Feat(AxPlatformFeature.AOD, settingsGet("secure", "doze_always_on"), ::nz, "settings put secure doze_always_on 1", "settings put secure doze_always_on 0"),
            Feat(AxPlatformFeature.DATA_SAVER, "cmd netpolicy get restrict-background", { it.contains("enabled", true) && !it.contains("disabled", true) }, "cmd netpolicy set restrict-background true", "cmd netpolicy set restrict-background false"),
            Feat(AxPlatformFeature.COLOR_INVERSION, settingsGet("secure", "accessibility_display_inversion_enabled"), ::nz, "settings put secure accessibility_display_inversion_enabled 1", "settings put secure accessibility_display_inversion_enabled 0"),
            Feat(AxPlatformFeature.COLOR_CORRECTION, settingsGet("secure", "accessibility_display_daltonizer_enabled"), ::nz, "settings put secure accessibility_display_daltonizer_enabled 1", "settings put secure accessibility_display_daltonizer_enabled 0"),
            Feat(AxPlatformFeature.REDUCE_BRIGHTNESS, settingsGet("secure", "reduce_bright_colors_activated"), ::nz, "settings put secure reduce_bright_colors_activated 1", "settings put secure reduce_bright_colors_activated 0"),
            Feat(AxPlatformFeature.ONE_HANDED_MODE, settingsGet("secure", "one_handed_mode_enabled"), ::nz, "settings put secure one_handed_mode_enabled 1", "settings put secure one_handed_mode_enabled 0"),
            Feat(AxPlatformFeature.HEADS_UP, settingsGet("global", "heads_up_notifications_enabled"), ::nz, "settings put global heads_up_notifications_enabled 1", "settings put global heads_up_notifications_enabled 0"),
            Feat(AxPlatformFeature.DREAM, settingsGet("secure", "screensaver_enabled"), ::nz, "settings put secure screensaver_enabled 1", "settings put secure screensaver_enabled 0"),
            Feat(AxPlatformFeature.SCREEN_RECORD, "pidof screenrecord", { it.trim().isNotEmpty() }),
        )
    }

    private val inApp = setOf(
        AxPlatformFeature.FLASHLIGHT, AxPlatformFeature.AUTO_SYNC, AxPlatformFeature.SCREENSHOT,
    )

    fun init(context: Context) {
        this.context = context.applicationContext
    }

    /** Fitur yang bisa dijalankan di perangkat ini (tile lain disembunyikan). */
    fun supports(key: String): Boolean {
        val pm = context.packageManager
        return when (key) {
            AxPlatformFeature.FLASHLIGHT -> pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
            AxPlatformFeature.AUTO_SYNC -> true
            AxPlatformFeature.SCREENSHOT -> true
            else -> feats.firstOrNull { it.key == key }?.let { f ->
                f.feature == null || pm.hasSystemFeature(f.feature)
            } ?: false
        }
    }

    fun getState(key: String): AxFeatureState = synchronized(states) { states[key] } ?: AxFeatureState.EMPTY

    fun toggle(key: String) {
        when (key) {
            AxPlatformFeature.FLASHLIGHT -> setTorch(!torchOn)
            AxPlatformFeature.AUTO_SYNC -> {
                val on = !ContentResolver.getMasterSyncAutomatically()
                ContentResolver.setMasterSyncAutomatically(on)
                publish(key, AxFeatureState(on))
            }
            AxPlatformFeature.SCREENSHOT -> Shell.run("sleep 0.6; input keyevent 120")
            AxPlatformFeature.SCREEN_RECORD -> toggleRecord()
            else -> {
                val f = feats.firstOrNull { it.key == key } ?: return
                val next = !getState(key).isActive
                publish(key, AxFeatureState(next)) // optimistis
                val cmd = (if (next) f.on else f.off) ?: return
                Shell.run(cmd) { refreshAsync() }
            }
        }
    }

    private fun toggleRecord() {
        if (!screenRecording) {
            screenRecording = true
            publish(AxPlatformFeature.SCREEN_RECORD, AxFeatureState(true))
            val name = "GameSpace_${System.currentTimeMillis()}.mp4"
            Shell.run("mkdir -p /sdcard/Movies; setsid nohup screenrecord --time-limit 180 /sdcard/Movies/$name >/dev/null 2>&1 &") {
                refreshAsync()
            }
        } else {
            screenRecording = false
            publish(AxPlatformFeature.SCREEN_RECORD, AxFeatureState(false))
            Shell.run("pkill -2 screenrecord") { refreshAsync() }
        }
    }

    // ---- Flashlight (API publik) ----
    private fun torchManager() = context.getSystemService(CameraManager::class.java)

    private fun torchId(): String? = try {
        val cm = torchManager()
        cm.cameraIdList.firstOrNull {
            val c = cm.getCameraCharacteristics(it)
            c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        }
    } catch (_: Exception) { null }

    private fun setTorch(on: Boolean) {
        try {
            val id = torchId() ?: return
            torchManager().setTorchMode(id, on)
        } catch (_: Exception) {}
    }

    private fun registerTorch() {
        if (torchRegistered) return
        torchRegistered = true
        try {
            torchManager().registerTorchCallback(object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                    torchOn = enabled
                    publish(AxPlatformFeature.FLASHLIGHT, AxFeatureState(enabled))
                }
            }, main)
        } catch (_: Exception) {}
    }

    // ---- Polling status ----
    fun registerCallback(executor: Executor, callback: StateCallback) {
        callbacks.add(executor to callback)
        registerTorch()
        startPolling()
    }

    fun unregisterCallback(callback: StateCallback) {
        callbacks.removeAll { it.second === callback }
        if (callbacks.isEmpty()) polling = false
    }

    private fun startPolling() {
        if (polling) return
        polling = true
        refreshAsync()
        main.postDelayed(object : Runnable {
            override fun run() {
                if (!polling) return
                refreshAsync()
                main.postDelayed(this, 2500L)
            }
        }, 2500L)
    }

    @Volatile private var refreshing = false

    private fun refreshAsync() {
        if (refreshing || !Shell.available()) return
        refreshing = true
        Shell.background {
            try {
                val script = feats.filter { it.read != null }.joinToString("; ") {
                    "echo \"@@${it.key}=\$(${it.read} 2>/dev/null | head -n 1)\""
                }
                val r = Shell.exec(script, 6000)
                r.out.lineSequence().forEach { line ->
                    if (!line.startsWith("@@")) return@forEach
                    val i = line.indexOf('=')
                    if (i < 0) return@forEach
                    val key = line.substring(2, i)
                    val value = line.substring(i + 1)
                    val f = feats.firstOrNull { it.key == key } ?: return@forEach
                    if (key == AxPlatformFeature.SCREEN_RECORD) screenRecording = f.isOn(value)
                    publish(key, AxFeatureState(f.isOn(value)))
                }
                try {
                    publish(AxPlatformFeature.AUTO_SYNC, AxFeatureState(ContentResolver.getMasterSyncAutomatically()))
                } catch (_: Exception) {}
            } finally {
                refreshing = false
            }
        }
    }

    private fun publish(key: String, state: AxFeatureState) {
        val changed = synchronized(states) {
            val old = states[key]
            states[key] = state
            old == null || old.isActive != state.isActive
        }
        if (!changed) return
        callbacks.forEach { (ex, cb) -> ex.execute { cb.onStateChanged(key, state) } }
    }

    companion object {
        @Volatile private var instance: AxPlatformClient? = null
        fun getInstance(): AxPlatformClient =
            instance ?: synchronized(this) { instance ?: AxPlatformClient().also { instance = it } }
    }
}
