package com.siroha.gamespace

import android.app.Application
import com.topjohnwu.superuser.Shell
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SirohaGameSpaceApp : Application() {

    override fun onCreate() {
        super.onCreate()
        configureRootShell()
    }

    /**
     * libsu keeps one shared shell process per app. Its config must be set
     * before the first [Shell.getShell] call anywhere in the app, so this
     * runs once here rather than lazily inside [com.siroha.gamespace.core.privilege.RootPrivilegeSource].
     * This does NOT request root yet — it only configures how the shell
     * will behave the first time something does.
     */
    private fun configureRootShell() {
        Shell.enableVerboseLogging = BuildConfig.DEBUG
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(10)
        )
    }
}
