package com.siroha.gamespace.core.privilege

import kotlinx.coroutines.flow.StateFlow

/**
 * Single point every feature goes through for elevated access. A feature
 * that wants to, say, read another app's memory info should call
 * [execPrivileged] and handle [PrivilegedExecResult.Unavailable] by falling
 * back to a public-API approximation — never assume elevated access exists.
 */
interface PrivilegeRepository {

    val state: StateFlow<PrivilegeState>

    /** Refreshes [state] from what's already known, without prompting anything. */
    fun refresh()

    /** Prompts for root. Updates [state] with the outcome. */
    suspend fun requestRoot()

    /** Prompts for Shizuku permission. Updates [state] with the outcome. */
    suspend fun requestShizuku()

    /**
     * Runs [command] through whichever tier is currently active (root
     * preferred over Shizuku), or returns [PrivilegedExecResult.Unavailable]
     * if neither is granted.
     */
    suspend fun execPrivileged(command: String): PrivilegedExecResult
}
