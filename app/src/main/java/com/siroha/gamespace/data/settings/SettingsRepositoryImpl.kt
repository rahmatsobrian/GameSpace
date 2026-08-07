package com.siroha.gamespace.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    override val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            darkMode = prefs[KEY_DARK_MODE]?.let { runCatching { DarkModePreference.valueOf(it) }.getOrNull() }
                ?: DarkModePreference.SYSTEM,
            amoledEnabled = prefs[KEY_AMOLED] ?: false,
            dynamicColorEnabled = prefs[KEY_DYNAMIC_COLOR] ?: true,
            homeGridView = prefs[KEY_HOME_GRID_VIEW] ?: true,
            onboardingCompleted = prefs[KEY_ONBOARDING_COMPLETED] ?: false
        )
    }

    override suspend fun setDarkMode(mode: DarkModePreference) {
        dataStore.edit { it[KEY_DARK_MODE] = mode.name }
    }

    override suspend fun setAmoledEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_AMOLED] = enabled }
    }

    override suspend fun setDynamicColorEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_DYNAMIC_COLOR] = enabled }
    }

    override suspend fun setHomeGridView(isGrid: Boolean) {
        dataStore.edit { it[KEY_HOME_GRID_VIEW] = isGrid }
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[KEY_ONBOARDING_COMPLETED] = completed }
    }

    private companion object {
        val KEY_DARK_MODE = stringPreferencesKey("dark_mode")
        val KEY_AMOLED = booleanPreferencesKey("amoled_enabled")
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color_enabled")
        val KEY_HOME_GRID_VIEW = booleanPreferencesKey("home_grid_view")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }
}
