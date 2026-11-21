package com.haskatech.myday.focus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class TimeTrackingRulesTest {
    private val kampala = ZoneId.of("Africa/Kampala")

    @Test
    fun elapsedTimeIsCalculatedFromStartAndCurrentTime() {
        assertEquals(90_000L, TimeTrackingRules.elapsedMillis(10_000L, 100_000L))
    }

    @Test
    fun elapsedTimeNeverBecomesNegativeWhenClockMovesBackwards() {
        assertEquals(0L, TimeTrackingRules.elapsedMillis(100_000L, 10_000L))
    }

    @Test
    fun formatsMinutesAndHoursClearly() {
        assertEquals("0m", TimeTrackingRules.formatDuration(59_999L))
        assertEquals("12m", TimeTrackingRules.formatDuration(12 * 60_000L))
        assertEquals("1h 05m", TimeTrackingRules.formatDuration(65 * 60_000L))
        assertEquals("2h 00m", TimeTrackingRules.formatDuration(120 * 60_000L))
    }

    @Test
    fun negativeDurationsFormatAsZero() {
        assertEquals("0m", TimeTrackingRules.formatDuration(-5L))
    }

    @Test
    fun sessionCrossingMidnightIsSplitAcrossBothLocalDates() {
        val start = LocalDateTime.of(2026, 11, 21, 23, 30).atZone(kampala).toInstant().toEpochMilli()
        val end = LocalDateTime.of(2026, 11, 22, 0, 30).atZone(kampala).toInstant().toEpochMilli()
        val result = TimeTrackingRules.durationByDate(listOf(TimeInterval(start, end)), kampala)
        assertEquals(30 * 60_000L, result[LocalDate.of(2026, 11, 21)])
        assertEquals(30 * 60_000L, result[LocalDate.of(2026, 11, 22)])
    }

    @Test
    fun sessionEndingExactlyAtMidnightDoesNotAddTimeToNextDay() {
        val start = LocalDateTime.of(2026, 11, 21, 23, 45).atZone(kampala).toInstant().toEpochMilli()
        val end = LocalDateTime.of(2026, 11, 22, 0, 0).atZone(kampala).toInstant().toEpochMilli()
        val result = TimeTrackingRules.durationByDate(listOf(TimeInterval(start, end)), kampala)
        assertEquals(15 * 60_000L, result[LocalDate.of(2026, 11, 21)])
        assertFalse(result.containsKey(LocalDate.of(2026, 11, 22)))
    }

    @Test
    fun separateSessionsAreAddedToTheSameDate() {
        val day = LocalDate.of(2026, 11, 21)
        val firstStart = day.atTime(9, 0).atZone(kampala).toInstant().toEpochMilli()
        val firstEnd = day.atTime(9, 30).atZone(kampala).toInstant().toEpochMilli()
        val secondStart = day.atTime(14, 0).atZone(kampala).toInstant().toEpochMilli()
        val secondEnd = day.atTime(15, 0).atZone(kampala).toInstant().toEpochMilli()
        val result = TimeTrackingRules.durationByDate(
            listOf(TimeInterval(firstStart, firstEnd), TimeInterval(secondStart, secondEnd)),
            kampala
        )
        assertEquals(90 * 60_000L, result[day])
    }
}
