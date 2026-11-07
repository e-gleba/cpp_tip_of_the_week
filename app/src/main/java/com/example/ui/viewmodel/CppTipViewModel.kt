package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.CppDatabase
import com.example.data.model.CppTip
import com.example.data.repository.CppTipRepository
import com.example.ui.theme.AppThemeMode
import com.example.util.NetworkMonitor
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CppTipUiState(
    val allTips: List<CppTip> = emptyList(),
    val filteredTips: List<CppTip> = emptyList(),
    val bookmarkedTips: List<CppTip> = emptyList(),
    val selectedTip: CppTip? = null,
    val randomTip: CppTip? = null,
    val searchQuery: String = "",
    val activeFilter: String = "All", // "All", "C++26", "C++23", "C++20", "C++17", "C++14/11", "Bookmarked", "Offline"
    val isOnline: Boolean = false,
    val isSyncing: Boolean = false,
    val syncProgressText: String? = null,
    val statusMessage: String? = null,
    val isErrorMessage: Boolean = false,
    val downloadedCount: Int = 0,
    val totalCount: Int = 0,
    val isLoadingTipDetails: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM
)

class CppTipViewModel(application: Application) : AndroidViewModel(application) {
    private val database = CppDatabase.getDatabase(application)
    private val repository = CppTipRepository(database.cppTipDao())
    private val networkMonitor = NetworkMonitor(application)
    private val prefs = application.getSharedPreferences("cpp_totw_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(CppTipUiState())
    val uiState: StateFlow<CppTipUiState> = _uiState.asStateFlow()

    private var syncJob: Job? = null

    init {
        // Load saved theme preference
        val savedThemeStr = prefs.getString("theme_mode", AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name
        val initialTheme = try {
            AppThemeMode.valueOf(savedThemeStr)
        } catch (_: Exception) {
            AppThemeMode.SYSTEM
        }
        _uiState.update { it.copy(themeMode = initialTheme) }

        viewModelScope.launch {
            repository.initializeIfNeeded(application)
        }

        // Observe network state & trigger fast archive sync when connected if needed
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _uiState.update { it.copy(isOnline = online) }
            }
        }

        // Observe all tips from database
        viewModelScope.launch {
            repository.getAllTips().collect { tips ->
                _uiState.update { state ->
                    val downloaded = tips.count { it.isDownloaded }
                    val random = state.randomTip ?: tips.filter { it.isDownloaded }.randomOrNull() ?: tips.firstOrNull()
                    val updatedSelected = state.selectedTip?.let { sel ->
                        tips.find { it.id == sel.id } ?: sel
                    }
                    val filtered = applyFilter(tips, state.searchQuery, state.activeFilter)
                    state.copy(
                        allTips = tips,
                        filteredTips = filtered,
                        randomTip = random,
                        selectedTip = updatedSelected,
                        downloadedCount = downloaded,
                        totalCount = tips.size
                    )
                }
            }
        }

        // Observe bookmarks
        viewModelScope.launch {
            repository.getBookmarkedTips().collect { bookmarks ->
                _uiState.update { state ->
                    val filtered = applyFilter(state.allTips, state.searchQuery, state.activeFilter)
                    state.copy(bookmarkedTips = bookmarks, filteredTips = filtered)
                }
            }
        }
    }

    private fun applyFilter(tips: List<CppTip>, query: String, filter: String): List<CppTip> {
        val q = query.trim()
        return tips.filter { tip ->
            val matchesQuery = q.isBlank() ||
                tip.title.contains(q, ignoreCase = true) ||
                tip.summary.contains(q, ignoreCase = true) ||
                tip.content.contains(q, ignoreCase = true) ||
                tip.id.toString() == q.removePrefix("#")

            val matchesFilter = when (filter) {
                "All" -> true
                "C++26" -> tip.standard.equals("C++26", ignoreCase = true)
                "C++23" -> tip.standard.equals("C++23", ignoreCase = true)
                "C++20" -> tip.standard.equals("C++20", ignoreCase = true)
                "C++17" -> tip.standard.equals("C++17", ignoreCase = true)
                "C++14/11" -> tip.standard.equals("C++14", ignoreCase = true) || tip.standard.equals("C++11", ignoreCase = true)
                "Bookmarked", "Saved" -> tip.isBookmarked
                "Offline" -> tip.isDownloaded
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }

    fun onSetThemeMode(mode: AppThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            val filtered = applyFilter(state.allTips, query, state.activeFilter)
            state.copy(searchQuery = query, filteredTips = filtered)
        }
    }

    fun onFilterSelected(filter: String) {
        _uiState.update { state ->
            val newFilter = if (state.activeFilter == filter && filter != "All") "All" else filter
            val filtered = applyFilter(state.allTips, state.searchQuery, newFilter)
            state.copy(activeFilter = newFilter, filteredTips = filtered)
        }
    }

    fun onSelectTip(tip: CppTip) {
        _uiState.update { it.copy(selectedTip = tip) }
        if (!tip.isDownloaded && _uiState.value.isOnline) {
            downloadSingleTip(tip.id)
        }
    }

    fun downloadSingleTip(tipId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingTipDetails = true) }
            val result = repository.fetchAndCacheTip(tipId)
            result.onSuccess { fullTip ->
                _uiState.update { state ->
                    val updatedSel = if (state.selectedTip?.id == tipId) fullTip else state.selectedTip
                    state.copy(
                        selectedTip = updatedSel,
                        isLoadingTipDetails = false
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoadingTipDetails = false,
                        statusMessage = err.message ?: "Failed to download tip",
                        isErrorMessage = true
                    )
                }
            }
        }
    }

    fun onClearSelectedTip() {
        _uiState.update { it.copy(selectedTip = null) }
    }

    fun onToggleBookmark(tip: CppTip) {
        viewModelScope.launch {
            repository.toggleBookmark(tip.id, tip.isBookmarked)
            _uiState.update { state ->
                val updatedSelected = if (state.selectedTip?.id == tip.id) {
                    state.selectedTip.copy(isBookmarked = !tip.isBookmarked)
                } else state.selectedTip
                state.copy(selectedTip = updatedSelected)
            }
        }
    }

    fun onRollRandomTip() {
        viewModelScope.launch {
            val random = repository.getRandomTip() ?: _uiState.value.allTips.randomOrNull()
            if (random != null) {
                _uiState.update { it.copy(randomTip = random) }
                onSelectTip(random)
            }
        }
    }

    /**
     * Automatically syncs on app startup in the background from tip-of-the-week.github.io/cpp/
     * without any manual batch clicks needed by the user.
     */
    fun autoBackgroundSync() {
        if (syncJob?.isActive == true) return

        syncJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSyncing = true,
                    syncProgressText = "Syncing tips catalog from tip-of-the-week.github.io/cpp/..."
                )
            }

            // Step 1: Discover latest tips from GitHub Pages
            repository.syncCatalogFromWeb()

            // Step 2: Auto-download ALL un-downloaded tips sequentially for complete offline use
            repository.syncAllUndownloadedTips(delayMs = 80, maxItems = Int.MAX_VALUE) { current, total, tipId ->
                _uiState.update {
                    it.copy(syncProgressText = "Downloading tip #$tipId for offline ($current/$total)...")
                }
            }

            _uiState.update {
                it.copy(
                    isSyncing = false,
                    syncProgressText = null
                )
            }
        }
    }

    /**
     * User-triggered "Sync" action from Settings.
     * Uses fast single-archive download from GitHub repository (~200KB)
     * with fallback to web catalog.
     */
    fun checkUpdatesAndResync() {
        if (syncJob?.isActive == true) return

        syncJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSyncing = true,
                    syncProgressText = "Syncing with GitHub repository...",
                    isErrorMessage = false
                )
            }

            val archiveResult = repository.syncFullRepoArchive()
            if (archiveResult.isSuccess) {
                val count = archiveResult.getOrDefault(0)
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        syncProgressText = null,
                        statusMessage = "Sync complete! $count tips are available offline."
                    )
                }
            } else {
                val catalogResult = repository.syncCatalogFromWeb()
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        syncProgressText = null,
                        statusMessage = if (catalogResult.isSuccess) "Sync complete! Tips are up to date." else "Offline mode active. All tips available from local cache."
                    )
                }
            }
        }
    }

    fun onSaveUserNotes(tipId: Int, notes: String) {
        viewModelScope.launch {
            repository.updateNotes(tipId, notes.ifBlank { null })
            _uiState.update { state ->
                val updated = if (state.selectedTip?.id == tipId) {
                    state.selectedTip.copy(userNotes = notes.ifBlank { null })
                } else state.selectedTip
                state.copy(selectedTip = updated)
            }
        }
    }

    fun onDismissStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}
