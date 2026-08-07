// Top-level build file. Real logic lives in module build.gradle.kts files;
// this only registers plugin versions via the version catalog so every
// module resolves the same versions.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
