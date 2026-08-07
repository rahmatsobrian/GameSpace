pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // libsu (root shell library) is published via JitPack.
        // If `dev.rikka.shizuku` fails to resolve from mavenCentral() on your
        // machine, add JitPack for it too — some Rikka artifacts still mirror there.
        maven("https://jitpack.io")
    }
}

rootProject.name = "GameSpace"
include(":app")
