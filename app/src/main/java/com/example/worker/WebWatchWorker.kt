package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import com.example.data.model.SnapshotHistory
import com.example.data.model.Watcher
import com.example.network.WebpageFetcher
import com.example.notification.NotificationHelper
import com.example.util.DiffUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WebWatchWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val database = AppDatabase.getDatabase(appContext)
        val watcherDao = database.watcherDao()
        val historyDao = database.snapshotHistoryDao()

        val specificWatcherId = inputData.getLong(KEY_WATCHER_ID, -1L)

        val watchersToCheck = if (specificWatcherId > 0) {
            val singleWatcher = watcherDao.getWatcherById(specificWatcherId)
            if (singleWatcher != null) listOf(singleWatcher) else emptyList()
        } else {
            watcherDao.getActiveWatchers()
        }

        if (watchersToCheck.isEmpty()) {
            return@withContext Result.success()
        }

        var anyFailed = false

        for (watcher in watchersToCheck) {
            try {
                checkSingleWatcher(watcher, watcherDao, historyDao)
            } catch (e: Exception) {
                e.printStackTrace()
                anyFailed = true
            }
        }

        if (anyFailed && specificWatcherId > 0) {
            Result.retry()
        } else {
            Result.success()
        }
    }

    private suspend fun checkSingleWatcher(
        watcher: Watcher,
        watcherDao: com.example.data.dao.WatcherDao,
        historyDao: com.example.data.dao.SnapshotHistoryDao
    ) {
        val fetchResult = WebpageFetcher.fetchUrl(watcher.url)
        val currentTime = System.currentTimeMillis()

        if (!fetchResult.isSuccess || fetchResult.html == null) {
            val updated = watcher.copy(
                lastCheckedTime = currentTime,
                lastStatus = "ERROR",
                lastErrorMessage = fetchResult.errorMessage ?: "Failed to fetch webpage"
            )
            watcherDao.updateWatcher(updated)

            historyDao.insertSnapshot(
                SnapshotHistory(
                    watcherId = watcher.id,
                    watcherName = watcher.name,
                    timestamp = currentTime,
                    status = "ERROR",
                    errorMessage = fetchResult.errorMessage ?: "Failed to fetch webpage"
                )
            )
            return
        }

        val normalizedHtml = DiffUtils.normalizeHtml(
            fetchResult.html,
            stripScripts = watcher.stripScripts,
            formatTags = watcher.formatHtml
        )

        val oldHtml = watcher.lastHtml

        if (oldHtml == null) {
            // Initial check snapshot
            val updated = watcher.copy(
                lastCheckedTime = currentTime,
                lastHtml = normalizedHtml,
                previousHtml = normalizedHtml,
                lastStatus = "SUCCESS",
                lastErrorMessage = null
            )
            watcherDao.updateWatcher(updated)

            historyDao.insertSnapshot(
                SnapshotHistory(
                    watcherId = watcher.id,
                    watcherName = watcher.name,
                    timestamp = currentTime,
                    status = "INITIAL",
                    newHtml = normalizedHtml,
                    diffSummary = "Initial snapshot captured (${normalizedHtml.lines().size} lines)"
                )
            )
        } else if (oldHtml != normalizedHtml) {
            // Content changed! Compute diff
            val diffResult = DiffUtils.computeDiff(oldHtml, normalizedHtml)

            if (diffResult.hasChanges) {
                val summary = "+${diffResult.additions} / -${diffResult.deletions} lines changed"

                val updated = watcher.copy(
                    lastCheckedTime = currentTime,
                    previousHtml = oldHtml,
                    lastHtml = normalizedHtml,
                    lastStatus = "CHANGED",
                    lastErrorMessage = null,
                    changeCount = watcher.changeCount + 1,
                    lastAdditions = diffResult.additions,
                    lastDeletions = diffResult.deletions
                )
                watcherDao.updateWatcher(updated)

                historyDao.insertSnapshot(
                    SnapshotHistory(
                        watcherId = watcher.id,
                        watcherName = watcher.name,
                        timestamp = currentTime,
                        status = "CHANGED",
                        additions = diffResult.additions,
                        deletions = diffResult.deletions,
                        diffSummary = summary,
                        oldHtml = oldHtml,
                        newHtml = normalizedHtml
                    )
                )

                // Send user notification!
                NotificationHelper.notifyChangeDetected(
                    context = appContext,
                    watcherId = watcher.id,
                    watcherName = watcher.name,
                    url = watcher.url,
                    additions = diffResult.additions,
                    deletions = diffResult.deletions
                )
            } else {
                // Formatting identical
                val updated = watcher.copy(
                    lastCheckedTime = currentTime,
                    lastStatus = "SUCCESS",
                    lastErrorMessage = null
                )
                watcherDao.updateWatcher(updated)
            }
        } else {
            // Unchanged
            val updated = watcher.copy(
                lastCheckedTime = currentTime,
                lastStatus = "SUCCESS",
                lastErrorMessage = null
            )
            watcherDao.updateWatcher(updated)

            historyDao.insertSnapshot(
                SnapshotHistory(
                    watcherId = watcher.id,
                    watcherName = watcher.name,
                    timestamp = currentTime,
                    status = "UNCHANGED",
                    diffSummary = "No changes detected"
                )
            )
        }
    }

    companion object {
        const val KEY_WATCHER_ID = "key_watcher_id"
    }
}
