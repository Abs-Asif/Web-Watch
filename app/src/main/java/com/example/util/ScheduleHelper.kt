package com.example.util

import java.util.Locale

object ScheduleHelper {

    const val DEFAULT_SCHEDULE_DAYS = 30

    /**
     * Generates a 1-month long schedule list of future check timestamps
     * starting from [startTimeMs] up to [days] days (default 30 days).
     *
     * Example: For a 5-minute interval over 30 days, generates 8,640 timestamps.
     */
    fun createOneMonthSchedule(
        startTimeMs: Long = System.currentTimeMillis(),
        intervalMinutes: Int,
        days: Int = DEFAULT_SCHEDULE_DAYS
    ): List<Long> {
        val safeInterval = intervalMinutes.coerceAtLeast(1)
        val totalMinutes = days * 24 * 60
        val count = totalMinutes / safeInterval
        val intervalMs = safeInterval * 60 * 1000L

        val schedule = ArrayList<Long>(count)
        var nextTime = startTimeMs + intervalMs
        for (i in 0 until count) {
            schedule.add(nextTime)
            nextTime += intervalMs
        }
        return schedule
    }

    /**
     * Searches the [scheduleList] for the closest next scheduled timestamp
     * that is strictly greater than [currentTimeMs].
     *
     * If all scheduled times have elapsed or list is empty, generates a new schedule.
     */
    fun getClosestNextScheduledTime(
        scheduleList: List<Long>,
        currentTimeMs: Long = System.currentTimeMillis()
    ): Long? {
        if (scheduleList.isEmpty()) return null
        return scheduleList.firstOrNull { it > currentTimeMs }
    }

    /**
     * Computes the closest next scheduled time for a given watcher configuration
     * by creating or querying its 1-month schedule list.
     */
    fun getNextScheduledTimeForWatcher(
        scheduleStartTimeMs: Long,
        intervalMinutes: Int,
        currentTimeMs: Long = System.currentTimeMillis()
    ): Long {
        val scheduleList = createOneMonthSchedule(scheduleStartTimeMs, intervalMinutes)
        val nextTime = getClosestNextScheduledTime(scheduleList, currentTimeMs)
        if (nextTime != null) {
            return nextTime
        }
        // If expired beyond 30 days, generate a fresh schedule from currentTimeMs
        val freshSchedule = createOneMonthSchedule(currentTimeMs, intervalMinutes)
        return freshSchedule.first()
    }

    /**
     * Formats remaining milliseconds into a human-readable countdown string.
     * E.g. "04m 12s" or "02h 15m 08s" or "00m 00s".
     */
    fun formatCountdown(remainingMs: Long): String {
        if (remainingMs <= 0) return "00m 00s"
        val totalSeconds = remainingMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02dh %02dm %02ds", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02dm %02ds", minutes, seconds)
        }
    }

    /**
     * Returns total number of schedule entries generated over 30 days for an interval.
     */
    fun getScheduleCount(intervalMinutes: Int, days: Int = DEFAULT_SCHEDULE_DAYS): Int {
        val safeInterval = intervalMinutes.coerceAtLeast(1)
        return (days * 24 * 60) / safeInterval
    }
}
