package com.example.worker

import android.content.Context
import androidx.work.*
import com.example.util.ScheduleHelper
import java.util.concurrent.TimeUnit

object WorkScheduler {

    private const val UNIQUE_PERIODIC_WORK_NAME = "webwatch_periodic_sync"
    private const val TAG_WEBWATCH = "webwatch_sync"

    fun schedulePeriodicMonitoring(context: Context, intervalMinutes: Long = 15) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // WorkManager minimum periodic interval is 15 minutes.
        val safeInterval = intervalMinutes.coerceAtLeast(15)

        val periodicRequest = PeriodicWorkRequestBuilder<WebWatchWorker>(
            safeInterval, TimeUnit.MINUTES,
            5, TimeUnit.MINUTES // Flex interval
        )
            .setConstraints(constraints)
            .addTag(TAG_WEBWATCH)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                1, TimeUnit.MINUTES
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest
        )
    }

    fun triggerImmediateCheck(context: Context, watcherId: Long? = null) {
        val data = if (watcherId != null) {
            Data.Builder().putLong(WebWatchWorker.KEY_WATCHER_ID, watcherId).build()
        } else {
            Data.EMPTY
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val oneTimeRequest = OneTimeWorkRequestBuilder<WebWatchWorker>()
            .setConstraints(constraints)
            .setInputData(data)
            .addTag(TAG_WEBWATCH)
            .build()

        WorkManager.getInstance(context).enqueue(oneTimeRequest)
    }

    fun scheduleNextCheckFromSchedule(
        context: Context,
        watcherId: Long,
        scheduleStartTimeMs: Long,
        intervalMinutes: Int
    ) {
        val nextTimeMs = ScheduleHelper.getNextScheduledTimeForWatcher(scheduleStartTimeMs, intervalMinutes)
        val delayMs = (nextTimeMs - System.currentTimeMillis()).coerceAtLeast(0L)

        val data = Data.Builder().putLong(WebWatchWorker.KEY_WATCHER_ID, watcherId).build()
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val scheduledRequest = OneTimeWorkRequestBuilder<WebWatchWorker>()
            .setConstraints(constraints)
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(TAG_WEBWATCH)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "webwatch_schedule_watcher_$watcherId",
            ExistingWorkPolicy.REPLACE,
            scheduledRequest
        )
    }

    fun cancelAllMonitoring(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_PERIODIC_WORK_NAME)
    }
}
