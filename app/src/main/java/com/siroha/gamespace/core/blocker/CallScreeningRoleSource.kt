package com.siroha.gamespace.core.blocker

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallScreeningRoleSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val roleManager: RoleManager
        get() = context.getSystemService(Context.ROLE_SERVICE) as RoleManager

    fun isAvailable(): Boolean = roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)

    fun hasRole(): Boolean = roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)

    /**
     * Unlike the Settings-screen permissions elsewhere in this app, this
     * shows the system's own role-request dialog. Firing it with a plain
     * startActivity (rather than startActivityForResult) still works
     * fine here since the caller re-checks [hasRole] on resume — same
     * pattern as every other permission source in this app.
     */
    fun requestRole() {
        context.startActivity(requestRoleIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun requestRoleIntent(): Intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
}
