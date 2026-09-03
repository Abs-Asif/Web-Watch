package com.example.util

import org.junit.Assert.*
import org.junit.Test

class ScheduleHelperTest {

    @Test
    fun testCreateOneMonthSchedule_FiveMinutesInterval() {
        val baseTime = 1_000_000_000_000L
        val intervalMinutes = 5
        val schedule = ScheduleHelper.createOneMonthSchedule(baseTime, intervalMinutes, days = 30)

        // 30 days * 24 hrs/day * 60 mins/hr / 5 mins = 8,640 schedule entries
        val expectedCount = (30 * 24 * 60) / 5
        assertEquals(expectedCount, schedule.size)
        assertEquals(8640, schedule.size)

        // First item should be baseTime + 5 minutes
        val expectedFirst = baseTime + (5 * 60 * 1000L)
        assertEquals(expectedFirst, schedule.first())

        // Last item should be baseTime + (8,640 * 5 minutes)
        val expectedLast = baseTime + (8640 * 5 * 60 * 1000L)
        assertEquals(expectedLast, schedule.last())

        // Verify strictly increasing step
        val stepMs = 5 * 60 * 1000L
        assertEquals(stepMs, schedule[1] - schedule[0])
        assertEquals(stepMs, schedule[10] - schedule[9])
    }

    @Test
    fun testCreateOneMonthSchedule_FifteenMinutesInterval() {
        val baseTime = 1_000_000_000_000L
        val intervalMinutes = 15
        val schedule = ScheduleHelper.createOneMonthSchedule(baseTime, intervalMinutes, days = 30)

        // 30 * 24 * 60 / 15 = 2,880
        assertEquals(2880, schedule.size)
    }

    @Test
    fun testGetClosestNextScheduledTime_MiddleOfSchedule() {
        val baseTime = 100_000L
        val intervalMinutes = 5
        val schedule = ScheduleHelper.createOneMonthSchedule(baseTime, intervalMinutes, days = 1)
        val intervalMs = 5 * 60 * 1000L

        // schedule[0] = baseTime + intervalMs = 400_000L
        // schedule[1] = baseTime + 2 * intervalMs = 700_000L
        val currentTime = 450_000L
        val closestNext = ScheduleHelper.getClosestNextScheduledTime(schedule, currentTime)

        assertNotNull(closestNext)
        assertEquals(baseTime + 2 * intervalMs, closestNext)
    }

    @Test
    fun testGetClosestNextScheduledTime_BeforeFirstItem() {
        val baseTime = 100_000L
        val schedule = listOf(200_000L, 300_000L, 400_000L)
        val currentTime = 150_000L

        val closestNext = ScheduleHelper.getClosestNextScheduledTime(schedule, currentTime)
        assertEquals(200_000L, closestNext)
    }

    @Test
    fun testGetClosestNextScheduledTime_AfterAllItems() {
        val schedule = listOf(200_000L, 300_000L, 400_000L)
        val currentTime = 500_000L

        val closestNext = ScheduleHelper.getClosestNextScheduledTime(schedule, currentTime)
        assertNull(closestNext)
    }

    @Test
    fun testGetNextScheduledTimeForWatcher_ExpiredRegenerates() {
        val startTime = 100_000L
        val intervalMinutes = 5
        val currentTime = 100_000_000_000L // far in the future

        val nextTime = ScheduleHelper.getNextScheduledTimeForWatcher(startTime, intervalMinutes, currentTime)
        assertTrue(nextTime > currentTime)
        assertEquals(currentTime + 5 * 60 * 1000L, nextTime)
    }

    @Test
    fun testFormatCountdown() {
        assertEquals("00m 00s", ScheduleHelper.formatCountdown(0L))
        assertEquals("00m 00s", ScheduleHelper.formatCountdown(-5000L))

        // 4 mins 12 secs = 252,000 ms
        assertEquals("04m 12s", ScheduleHelper.formatCountdown(252_000L))

        // 2 hrs 15 mins 20 secs = (2*3600 + 15*60 + 20) * 1000 = 8,120,000 ms
        assertEquals("02h 15m 20s", ScheduleHelper.formatCountdown(8_120_000L))
    }

    @Test
    fun testGetScheduleCount() {
        assertEquals(8640, ScheduleHelper.getScheduleCount(5))
        assertEquals(2880, ScheduleHelper.getScheduleCount(15))
        assertEquals(1440, ScheduleHelper.getScheduleCount(30))
        assertEquals(720, ScheduleHelper.getScheduleCount(60))
    }
}
