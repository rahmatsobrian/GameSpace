package com.siroha.gamespace.core.privilege

/**
 * The three ways this app can reach beyond public Android APIs, in the
 * order features should prefer them: a feature that needs to, say, read
 * another process's memory info should try ROOT, then SHIZUKU, then fall
 * back to whatever PUBLIC_ONLY can approximate (or hide itself if there's
 * no honest approximation).
 */
enum class PrivilegeTier {
    ROOT,
    SHIZUKU,
    PUBLIC_ONLY
}

/** Status of a single privilege source (root, or Shizuku). */
enum class AccessState {
    /** Not checked yet this session. */
    UNKNOWN,
    /** Detection/request in flight. */
    CHECKING,
    /** No provider found at all (no su binary; Shizuku not installed). */
    NOT_AVAILABLE,
    /** Provider is installed but its service isn't running (Shizuku only — it
     *  can be installed but not started, e.g. after a reboot on a non-rooted
     *  device where it needs a fresh adb pairing). */
    NOT_RUNNING,
    /** Provider is reachable but the user has not granted this app access. */
    NOT_REQUESTED,
    /** User explicitly denied the request. */
    PERMISSION_DENIED,
    /** Usable right now. */
    GRANTED
}

data class SourceStatus(
    val tier: PrivilegeTier,
    val state: AccessState = AccessState.UNKNOWN,
    /** Human-readable detail for the status line, e.g. which su provider
     *  answered, or why a request failed. Not user-facing copy by itself —
     *  the screen decides how/whether to show it. */
    val detail: String? = null
)

data class PrivilegeState(
    val root: SourceStatus = SourceStatus(PrivilegeTier.ROOT),
    val shizuku: SourceStatus = SourceStatus(PrivilegeTier.SHIZUKU)
) {
    /** First granted source wins, root preferred — see [PrivilegeTier] ordering. */
    val activeTier: PrivilegeTier
        get() = when {
            root.state == AccessState.GRANTED -> PrivilegeTier.ROOT
            shizuku.state == AccessState.GRANTED -> PrivilegeTier.SHIZUKU
            else -> PrivilegeTier.PUBLIC_ONLY
        }

    val hasElevatedAccess: Boolean
        get() = activeTier != PrivilegeTier.PUBLIC_ONLY
}

/** Outcome of running one privileged command through whichever tier is active. */
sealed interface PrivilegedExecResult {
    data class Success(val output: List<String>) : PrivilegedExecResult
    data class Failure(val reason: String) : PrivilegedExecResult
    /** No elevated tier is active — caller should fall back to a public API. */
    data object Unavailable : PrivilegedExecResult
}
