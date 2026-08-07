package com.siroha.gamespace.data.settings

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setDarkMode(mode: DarkModePreference)
    suspend fun setAmoledEnabled(enabled: Boolean)
    suspend fun setDynamicColorEnabled(enabled: Boolean)
    suspend fun setHomeGridView(isGrid: Boolean)
    suspend fun setOnboardingCompleted(completed: Boolean)
}
