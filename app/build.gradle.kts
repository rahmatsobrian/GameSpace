plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.siroha.gamespace"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.siroha.gamespace"
        // Android 10 (API 29) is the floor the spec asks for.
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.6.0-phase5-wip"

        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Keep debug unminified so stack traces from the privilege layer
            // (root/Shizuku failures) stay readable while wiring up new features.
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.core.splashscreen)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.activity.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.compose.ui.tooling.preview)

    implementation(libs.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.datastore.preferences)
    implementation(libs.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
    // Explicit rather than relying on transitive resolution through
    // navigation-compose — OverlayLifecycleOwner (feature/overlay) needs
    // these directly and I'd rather not guess whether they're already on
    // the classpath some other way.
    implementation(libs.savedstate.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)

    // Privilege layer: root (any su provider — Magisk / KernelSU / APatch all
    // expose a compatible su binary, so one client library covers all three)
    implementation(libs.libsu.core)
    implementation(libs.libsu.service)

    // Privilege layer: Shizuku (ADB/root-level access without full root)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
}

ksp {
    // Without this, @Database(exportSchema = true) just warns at build time
    // instead of writing schema JSON — schema history is what makes a
    // future Room migration (rather than a destructive fallback) possible.
    arg("room.schemaLocation", "$projectDir/schemas")
}
