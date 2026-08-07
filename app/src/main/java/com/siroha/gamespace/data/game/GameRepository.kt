package com.siroha.gamespace.data.game

import android.content.Intent
import kotlinx.coroutines.flow.Flow

interface GameRepository {

    /** Favorites first, then alphabetical. Already filtered to currently-
     *  installed packages — an uninstalled game just disappears rather
     *  than showing as a broken entry. */
    val library: Flow<List<Game>>

    /** Runs the CATEGORY_GAME auto-scan and upserts anything new found.
     *  Does not touch entries already in the library (so it can't silently
     *  revive something the user removed). */
    suspend fun rescan()

    suspend fun addManually(packageName: String)
    suspend fun remove(packageName: String)
    suspend fun setFavorite(packageName: String, isFavorite: Boolean)

    /** Launchable apps not already in the library — what the "add
     *  manually" picker shows. */
    suspend fun pickableApps(): List<InstalledAppInfo>

    fun launchIntentFor(packageName: String): Intent?
}
