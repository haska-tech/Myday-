package com.haskatech.myday.focus

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeTrackingRulesTest {
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
}
