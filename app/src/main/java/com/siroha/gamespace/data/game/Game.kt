package com.siroha.gamespace.data.game

data class Game(
    val packageName: String,
    val displayName: String,
    val isFavorite: Boolean,
    val isManuallyAdded: Boolean,
    val addedAt: Long,
    /** Null = no usage-access grant, or genuinely never opened within the
     *  OS's retained history window. See UsageAccessSource for why this
     *  isn't a guaranteed lifetime figure. */
    val lastPlayedAt: Long?,
    val totalPlaytimeMillis: Long
)
