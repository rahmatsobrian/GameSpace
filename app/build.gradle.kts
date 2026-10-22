import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("kotlin-kapt")
}

android {
    namespace = "com.android.axion.gamespace"
    compileSdk = 36

    defaultConfig {
        // applicationId dibedakan dari versi sistem agar bisa dipasang berdampingan di ROM mana pun.
        applicationId = "com.android.axion.gamespace.universal"
        minSdk = 33
        targetSdk = 36
        versionCode = 101
        versionName = "0.2-universal"
    }

    signingConfigs {
        create("release") {
            val f = rootProject.file("keystore.properties")
            if (f.exists()) {
                val p = Properties().apply { f.inputStream().use { load(it) } }
                storeFile = rootProject.file(p.getProperty("storeFile"))
                storePassword = p.getProperty("storePassword")
                keyAlias = p.getProperty("keyAlias")
                keyPassword = p.getProperty("keyPassword")
            } else {
                System.getenv("KEYSTORE_FILE")?.let { storeFile = file(it) }
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("debug") { isDebuggable = true }
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
        aidl = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        optIn.addAll("kotlin.ExperimentalStdlibApi", "kotlin.RequiresOptIn")
    }
}

kapt {
    correctErrorTypes = true
}

dependencies {
    implementation("androidx.activity:activity-compose:1.12.2")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation(platform("androidx.compose:compose-bom:2025.12.01"))
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.compose.material3:material3:1.5.0-alpha11")
    implementation("androidx.compose.ui:ui:1.10.0")
    implementation("androidx.compose.ui:ui-graphics:1.10.0")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.palette:palette-ktx:1.0.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("com.google.android.material:material:1.13.0")
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("com.google.dagger:hilt-android:2.57.2")
    kapt("com.google.dagger:hilt-compiler:2.57.2")

    // Shizuku (prioritas utama untuk akses shell tanpa root)
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
}

// ---------------------------------------------------------------------------
// Ikon Material Symbols (Rounded, Filled). Aplikasi asli membawa ikon ini dari
// library sistem; di sini diunduh saat build lalu dibuat sebagai vector drawable.
// Jika unduhan gagal, dipakai ikon pengganti sederhana agar build tetap jalan.
// ---------------------------------------------------------------------------
val symbolNames = listOf(
    "adjust", "android_cell_4_bar", "aod", "bar_chart", "battery_android_0", "battery_android_1",
    "battery_android_2", "battery_android_3", "battery_android_4", "battery_android_full",
    "battery_charging_full", "battery_saver", "bedtime", "bluetooth", "brightness_6", "brightness_7",
    "brightness_auto", "brightness_low", "call", "cast", "dark_mode", "dashboard", "data_saver_on",
    "delete", "device_thermostat", "do_not_disturb_on", "edit", "expand_more", "flashlight_on", "flight",
    "gesture", "grid_on", "invert_colors", "local_cafe", "location_on", "lock_open", "lock",
    "manage_accounts", "menu_book", "mic", "nfc", "nights_stay", "notifications_active", "notifications",
    "opacity", "palette", "phone_android", "phone_in_talk", "photo_camera", "screen_rotation_up",
    "screenshot", "search", "settings", "speed", "sports_esports", "sync", "touch_app", "usb",
    "videocam", "view_in_ar", "visibility", "volume_up", "vpn_key", "wifi", "wifi_tethering", "work"
)

val generateSymbols by tasks.registering {
    val outDir = layout.buildDirectory.dir("generated/symbols/res/drawable")
    outputs.dir(outDir)
    doLast {
        val dir = outDir.get().asFile.apply { mkdirs() }
        val placeholder = """<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFFFF" android:pathData="M12,4a8,8 0 1,0 0,16a8,8 0 1,0 0,-16z"/>
</vector>"""
        fun fetch(url: String): String? = try {
            val c = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            c.connectTimeout = 8000; c.readTimeout = 8000
            if (c.responseCode == 200) c.inputStream.bufferedReader().readText() else null
        } catch (_: Exception) { null }

        var fromNet = 0
        symbolNames.forEach { n ->
            val f = File(dir, "materialsymbols_ic_${n}_rounded_filled.xml")
            if (f.exists() && f.length() > 0) return@forEach
            val base = "https://raw.githubusercontent.com/google/material-design-icons/master/symbols"
            val xml = fetch("$base/android/$n/materialsymbolsrounded/${n}_fill1_24px.xml")
                ?: fetch("$base/android/$n/materialsymbolsrounded/${n}_wght400_fill1_24px.xml")
            val svg = if (xml == null) fetch("$base/web/$n/materialsymbolsrounded/${n}_fill1_24px.svg") else null
            val out = when {
                xml != null && xml.contains("<vector") -> xml.replace("android:tint=\"?attr/colorControlNormal\"", "")
                svg != null && svg.contains("<path") -> {
                    val d = Regex("""<path[^>]*\sd="([^"]+)"""").find(svg)?.groupValues?.get(1)
                    if (d == null) placeholder else """<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="960" android:viewportHeight="960">
    <group android:translateY="960"><path android:fillColor="#FFFFFFFF" android:pathData="$d"/></group>
</vector>"""
                }
                else -> { logger.warn("Material Symbol '$n' gagal diunduh, memakai ikon pengganti"); placeholder }
            }
            if (out !== placeholder) fromNet++
            f.writeText(out)
        }
        logger.lifecycle("Material Symbols: $fromNet/${symbolNames.size} diunduh")
    }
}

android.sourceSets.getByName("main").res.srcDir(layout.buildDirectory.dir("generated/symbols/res"))
tasks.matching { it.name == "preBuild" }.configureEach { dependsOn(generateSymbols) }
