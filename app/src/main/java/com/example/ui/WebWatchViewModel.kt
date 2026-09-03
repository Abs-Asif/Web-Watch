package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.model.SnapshotHistory
import com.example.data.model.Watcher
import com.example.network.FetchResult
import com.example.network.WebpageFetcher
import com.example.notification.NotificationHelper
import com.example.util.BatteryUtils
import com.example.util.DiffResult
import com.example.util.DiffUtils
import com.example.worker.WorkScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class MainTab {
    WATCHERS,
    HISTORY,
    SETTINGS
}

enum class DiffDisplayMode {
    UNIFIED,
    SIDE_BY_SIDE,
    RAW_HTML
}

data class UiState(
    val watchers: List<Watcher> = emptyList(),
    val history: List<SnapshotHistory> = emptyList(),
    val selectedWatcherId: Long? = null,
    val selectedSnapshot: SnapshotHistory? = null,
    val currentTab: MainTab = MainTab.WATCHERS,
    val isAddDialogOpen: Boolean = false,
    val editingWatcher: Watcher? = null,
    val isCheckingNow: Boolean = false,
    val checkingWatcherId: Long? = null,
    val isTestingUrl: Boolean = false,
    val testFetchResult: FetchResult? = null,
    val isBatteryOptimized: Boolean = false,
    val diffDisplayMode: DiffDisplayMode = DiffDisplayMode.UNIFIED,
    val onlyChangedLines: Boolean = true,
    val userMessage: String? = null
)

class WebWatchViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val watcherDao = database.watcherDao()
    private val historyDao = database.snapshotHistoryDao()

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            watcherDao.getAllWatchers().collect { list ->
                _uiState.update { it.copy(watchers = list) }
                if (list.isEmpty()) {
                    seedDefaultWatchers()
                }
            }
        }

        viewModelScope.launch {
            historyDao.getAllHistory().collect { hist ->
                _uiState.update { it.copy(history = hist) }
            }
        }

        refreshBatteryStatus()
    }

    private suspend fun seedDefaultWatchers() {
        val sample1 = Watcher(
            name = "VS Code Releases",
            url = "https://code.visualstudio.com",
            intervalMinutes = 15,
            isActive = true,
            lastStatus = "PENDING"
        )
        val sample2 = Watcher(
            name = "Hacker News Top",
            url = "https://news.ycombinator.com",
            intervalMinutes = 30,
            isActive = true,
            lastStatus = "PENDING"
        )
        val id1 = watcherDao.insertWatcher(sample1)
        val id2 = watcherDao.insertWatcher(sample2)
        
        // Auto check first sample
        checkWatcherNow(id1)
        checkWatcherNow(id2)
    }

    fun refreshBatteryStatus() {
        val isIgnored = BatteryUtils.isBatteryOptimizationIgnored(getApplication())
        _uiState.update { it.copy(isBatteryOptimized = isIgnored) }
    }

    fun setTab(tab: MainTab) {
        _uiState.update { it.copy(currentTab = tab, selectedWatcherId = null, selectedSnapshot = null) }
    }

    fun selectWatcher(id: Long?) {
        _uiState.update { it.copy(selectedWatcherId = id, selectedSnapshot = null) }
    }

    fun selectSnapshot(snapshot: SnapshotHistory?) {
        _uiState.update { it.copy(selectedSnapshot = snapshot) }
    }

    fun openAddDialog(watcherToEdit: Watcher? = null) {
        _uiState.update {
            it.copy(
                isAddDialogOpen = true,
                editingWatcher = watcherToEdit,
                testFetchResult = null
            )
        }
    }

    fun closeAddDialog() {
        _uiState.update {
            it.copy(
                isAddDialogOpen = false,
                editingWatcher = null,
                testFetchResult = null
            )
        }
    }

    fun setDiffMode(mode: DiffDisplayMode) {
        _uiState.update { it.copy(diffDisplayMode = mode) }
    }

    fun toggleOnlyChangedLines() {
        _uiState.update { it.copy(onlyChangedLines = !it.onlyChangedLines) }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    fun testFetch(url: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingUrl = true, testFetchResult = null) }
            val result = WebpageFetcher.fetchUrl(url)
            _uiState.update { it.copy(isTestingUrl = false, testFetchResult = result) }
        }
    }

    fun saveWatcher(
        id: Long = 0,
        name: String,
        url: String,
        intervalMinutes: Int,
        stripScripts: Boolean = true,
        formatHtml: Boolean = true
    ) {
        viewModelScope.launch {
            val normalizedUrl = WebpageFetcher.normalizeUrl(url)
            val finalName = if (name.isNotBlank()) name else normalizedUrl.removePrefix("https://").removePrefix("http://").take(30)

            if (id > 0) {
                val existing = watcherDao.getWatcherById(id)
                if (existing != null) {
                    val isIntervalChanged = existing.intervalMinutes != intervalMinutes
                    val updated = existing.copy(
                        name = finalName,
                        url = normalizedUrl,
                        intervalMinutes = intervalMinutes,
                        stripScripts = stripScripts,
                        formatHtml = formatHtml,
                        scheduleStartTime = if (isIntervalChanged) System.currentTimeMillis() else existing.scheduleStartTime
                    )
                    watcherDao.updateWatcher(updated)
                    _uiState.update { it.copy(userMessage = "Watcher updated: $finalName") }
                }
            } else {
                val newWatcher = Watcher(
                    name = finalName,
                    url = normalizedUrl,
                    intervalMinutes = intervalMinutes,
                    stripScripts = stripScripts,
                    formatHtml = formatHtml,
                    isActive = true,
                    scheduleStartTime = System.currentTimeMillis()
                )
                val newId = watcherDao.insertWatcher(newWatcher)
                _uiState.update { it.copy(userMessage = "Added watcher: $finalName") }
                // Immediately perform initial check
                checkWatcherNow(newId)
            }
            closeAddDialog()
        }
    }

    fun toggleWatcherActive(watcher: Watcher) {
        viewModelScope.launch {
            val updated = watcher.copy(isActive = !watcher.isActive)
            watcherDao.updateWatcher(updated)
            _uiState.update {
                it.copy(userMessage = if (updated.isActive) "${watcher.name} resumed" else "${watcher.name} paused")
            }
        }
    }

    fun deleteWatcher(watcher: Watcher) {
        viewModelScope.launch {
            watcherDao.deleteWatcher(watcher)
            historyDao.deleteHistoryForWatcher(watcher.id)
            if (_uiState.value.selectedWatcherId == watcher.id) {
                _uiState.update { it.copy(selectedWatcherId = null) }
            }
            _uiState.update { it.copy(userMessage = "Deleted ${watcher.name}") }
        }
    }

    fun markReviewed(watcherId: Long) {
        viewModelScope.launch {
            watcherDao.markChangesReviewed(watcherId)
            _uiState.update { it.copy(userMessage = "Marked changes as reviewed") }
        }
    }

    fun checkWatcherNow(watcherId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingNow = true, checkingWatcherId = watcherId) }
            withContext(Dispatchers.IO) {
                val watcher = watcherDao.getWatcherById(watcherId)
                if (watcher != null) {
                    performCheck(watcher)
                }
            }
            _uiState.update { it.copy(isCheckingNow = false, checkingWatcherId = null) }
        }
    }

    fun checkAllNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingNow = true) }
            withContext(Dispatchers.IO) {
                val watchers = watcherDao.getAllWatchers().first()
                for (w in watchers) {
                    if (w.isActive) {
                        performCheck(w)
                    }
                }
            }
            _uiState.update { it.copy(isCheckingNow = false, userMessage = "All active watchers checked") }
        }
    }

    private suspend fun performCheck(watcher: Watcher) {
        val fetchResult = WebpageFetcher.fetchUrl(watcher.url)
        val now = System.currentTimeMillis()

        if (!fetchResult.isSuccess || fetchResult.html == null) {
            val updated = watcher.copy(
                lastCheckedTime = now,
                lastStatus = "ERROR",
                lastErrorMessage = fetchResult.errorMessage ?: "Failed to fetch"
            )
            watcherDao.updateWatcher(updated)
            historyDao.insertSnapshot(
                SnapshotHistory(
                    watcherId = watcher.id,
                    watcherName = watcher.name,
                    timestamp = now,
                    status = "ERROR",
                    errorMessage = fetchResult.errorMessage ?: "Failed to fetch"
                )
            )
            return
        }

        val normalized = DiffUtils.normalizeHtml(
            fetchResult.html,
            stripScripts = watcher.stripScripts,
            formatTags = watcher.formatHtml
        )

        val oldHtml = watcher.lastHtml

        if (oldHtml == null) {
            val updated = watcher.copy(
                lastCheckedTime = now,
                lastHtml = normalized,
                previousHtml = normalized,
                lastStatus = "SUCCESS",
                lastErrorMessage = null
            )
            watcherDao.updateWatcher(updated)
            historyDao.insertSnapshot(
                SnapshotHistory(
                    watcherId = watcher.id,
                    watcherName = watcher.name,
                    timestamp = now,
                    status = "INITIAL",
                    newHtml = normalized,
                    diffSummary = "Initial snapshot (${normalized.lines().size} lines)"
                )
            )
        } else if (oldHtml != normalized) {
            val diff = DiffUtils.computeDiff(oldHtml, normalized)
            if (diff.hasChanges) {
                val summary = "+${diff.additions} / -${diff.deletions} lines"
                val updated = watcher.copy(
                    lastCheckedTime = now,
                    previousHtml = oldHtml,
                    lastHtml = normalized,
                    lastStatus = "CHANGED",
                    lastErrorMessage = null,
                    changeCount = watcher.changeCount + 1,
                    lastAdditions = diff.additions,
                    lastDeletions = diff.deletions
                )
                watcherDao.updateWatcher(updated)
                historyDao.insertSnapshot(
                    SnapshotHistory(
                        watcherId = watcher.id,
                        watcherName = watcher.name,
                        timestamp = now,
                        status = "CHANGED",
                        additions = diff.additions,
                        deletions = diff.deletions,
                        diffSummary = summary,
                        oldHtml = oldHtml,
                        newHtml = normalized
                    )
                )
                NotificationHelper.notifyChangeDetected(
                    getApplication(),
                    watcher.id,
                    watcher.name,
                    watcher.url,
                    diff.additions,
                    diff.deletions
                )
            } else {
                val updated = watcher.copy(lastCheckedTime = now, lastStatus = "SUCCESS")
                watcherDao.updateWatcher(updated)
            }
        } else {
            val updated = watcher.copy(lastCheckedTime = now, lastStatus = "SUCCESS")
            watcherDao.updateWatcher(updated)
            historyDao.insertSnapshot(
                SnapshotHistory(
                    watcherId = watcher.id,
                    watcherName = watcher.name,
                    timestamp = now,
                    status = "UNCHANGED",
                    diffSummary = "No changes"
                )
            )
        }
    }

    /**
     * Injects a simulated test change on a watcher to let users immediately see and test the GitHub diff viewer and notification!
     */
    fun simulateChangeForTesting(watcherId: Long) {
        viewModelScope.launch {
            val watcher = watcherDao.getWatcherById(watcherId) ?: return@launch
            val now = System.currentTimeMillis()
            val oldHtml = watcher.lastHtml ?: "<html>\n<body>\n<h1>${watcher.name}</h1>\n<p>Original content</p>\n</body>\n</html>"
            
            val updatedHtml = oldHtml
                .replace("</h1>", " v2.0 - Live Update</h1>\n<div class=\"badge\">NEW RELEASE</div>")
                .replace("Original content", "Updated content at ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(now))}")
                .plus("\n<!-- Detected dynamic change -->\n<footer>Last updated: ${System.currentTimeMillis()}</footer>")

            val diff = DiffUtils.computeDiff(oldHtml, updatedHtml)
            val updated = watcher.copy(
                lastCheckedTime = now,
                previousHtml = oldHtml,
                lastHtml = updatedHtml,
                lastStatus = "CHANGED",
                changeCount = watcher.changeCount + 1,
                lastAdditions = diff.additions,
                lastDeletions = diff.deletions
            )
            watcherDao.updateWatcher(updated)
            historyDao.insertSnapshot(
                SnapshotHistory(
                    watcherId = watcher.id,
                    watcherName = watcher.name,
                    timestamp = now,
                    status = "CHANGED",
                    additions = diff.additions,
                    deletions = diff.deletions,
                    diffSummary = "+${diff.additions} / -${diff.deletions} simulated change",
                    oldHtml = oldHtml,
                    newHtml = updatedHtml
                )
            )
            NotificationHelper.notifyChangeDetected(
                getApplication(),
                watcher.id,
                watcher.name,
                watcher.url,
                diff.additions,
                diff.deletions
            )
            _uiState.update { it.copy(userMessage = "Simulated change created for ${watcher.name}!") }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            historyDao.clearAllHistory()
            _uiState.update { it.copy(userMessage = "History cleared") }
        }
    }
}
