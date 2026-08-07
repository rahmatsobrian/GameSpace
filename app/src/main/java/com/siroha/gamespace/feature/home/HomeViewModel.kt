package com.siroha.gamespace.feature.home

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siroha.gamespace.core.usage.UsageAccessSource
import com.siroha.gamespace.data.game.Game
import com.siroha.gamespace.data.game.GameRepository
import com.siroha.gamespace.data.game.InstalledAppInfo
import com.siroha.gamespace.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val games: List<Game> = emptyList(),
    val query: String = "",
    val isScanning: Boolean = false,
    val hasUsageAccess: Boolean = false,
    val isGridView: Boolean = true
)

data class AddGameSheetState(
    val isOpen: Boolean = false,
    val query: String = "",
    val results: List<InstalledAppInfo> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val usageAccessSource: UsageAccessSource,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val isScanning = MutableStateFlow(false)
    private val hasUsageAccess = MutableStateFlow(usageAccessSource.hasAccess())

    val uiState: StateFlow<HomeUiState> = combine(
        gameRepository.library,
        query,
        isScanning,
        hasUsageAccess,
        settingsRepository.settings
    ) { games, currentQuery, scanning, usageAccess, appSettings ->
        val filtered = if (currentQuery.isBlank()) {
            games
        } else {
            games.filter { it.displayName.contains(currentQuery, ignoreCase = true) }
        }
        HomeUiState(
            games = filtered,
            query = currentQuery,
            isScanning = scanning,
            hasUsageAccess = usageAccess,
            isGridView = appSettings.homeGridView
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private val _addGameSheet = MutableStateFlow(AddGameSheetState())
    val addGameSheet: StateFlow<AddGameSheetState> = _addGameSheet.asStateFlow()

    init {
        rescan()
    }

    /** Call from onResume-ish points (e.g. after the user comes back from
     *  the Usage Access settings screen) — AppOps grants don't push a
     *  change notification, so this has to be re-checked explicitly. */
    fun refreshUsageAccessStatus() {
        hasUsageAccess.value = usageAccessSource.hasAccess()
    }

    fun onQueryChange(newQuery: String) {
        query.value = newQuery
    }

    fun onGridViewChange(isGrid: Boolean) {
        viewModelScope.launch { settingsRepository.setHomeGridView(isGrid) }
    }

    fun rescan() {
        viewModelScope.launch {
            isScanning.value = true
            gameRepository.rescan()
            isScanning.value = false
        }
    }

    fun toggleFavorite(packageName: String, isFavorite: Boolean) {
        viewModelScope.launch { gameRepository.setFavorite(packageName, isFavorite) }
    }

    fun removeFromLibrary(packageName: String) {
        viewModelScope.launch { gameRepository.remove(packageName) }
    }

    fun launchIntentFor(packageName: String): Intent? = gameRepository.launchIntentFor(packageName)

    fun requestUsageAccess() = usageAccessSource.openAccessSettings()

    // ---- Add-game sheet ----------------------------------------------

    fun openAddGameSheet() {
        _addGameSheet.update { it.copy(isOpen = true) }
        loadPickableApps()
    }

    fun closeAddGameSheet() {
        _addGameSheet.value = AddGameSheetState()
    }

    fun onAddGameQueryChange(newQuery: String) {
        _addGameSheet.update { it.copy(query = newQuery) }
    }

    private fun loadPickableApps() {
        viewModelScope.launch {
            _addGameSheet.update { it.copy(isLoading = true) }
            val apps = gameRepository.pickableApps()
            _addGameSheet.update { it.copy(results = apps, isLoading = false) }
        }
    }

    fun addGame(packageName: String) {
        viewModelScope.launch {
            gameRepository.addManually(packageName)
            closeAddGameSheet()
        }
    }
}
