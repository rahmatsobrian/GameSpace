package com.siroha.gamespace.data.backup

import android.net.Uri

/**
 * Scoped to the game library specifically, not a full app backup. It's
 * the one piece of persisted state that's genuinely irreplaceable user
 * data (favorites, manual additions, dismissals) — Booster/Blocker/
 * Settings preferences are device-local conveniences a fresh install
 * loses nothing important by resetting.
 */
interface BackupRepository {
    suspend fun exportLibrary(destination: Uri): Boolean
    suspend fun importLibrary(source: Uri): Boolean
}
