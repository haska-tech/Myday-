package com.haskatech.myday.focus

import org.junit.Assert.assertEquals
import org.junit.Test

class PomodoroRulesTest {
    @Test
    fun defaultDurationsAreTwentyFiveFiveAndFifteenMinutes() {
        assertEquals(25 * 60_000L, PomodoroRules.durationMillis("FOCUS", 25, 5, 15))
        assertEquals(5 * 60_000L, PomodoroRules.durationMillis("SHORT_BREAK", 25, 5, 15))
        assertEquals(15 * 60_000L, PomodoroRules.durationMillis("LONG_BREAK", 25, 5, 15))
    }

    @Test
    fun durationsAreClampedToOneThroughOneHundredTwentyMinutes() {
        assertEquals(60_000L, PomodoroRules.durationMillis("FOCUS", 0, 5, 15))
        assertEquals(120 * 60_000L, PomodoroRules.durationMillis("FOCUS", 500, 5, 15))
    }

    @Test
    fun remainingTimeNeverBecomesNegative() {
        assertEquals(30_000L, PomodoroRules.remainingMillis(90_000L, 60_000L))
        assertEquals(0L, PomodoroRules.remainingMillis(60_000L, 90_000L))
    }

    @Test
    fun firstThreeFocusSessionsLeadToShortBreaks() {
        for (completed in 0..2) {
            val result = PomodoroRules.nextPhase("FOCUS", completed)
            assertEquals("SHORT_BREAK", result.nextPhase)
            assertEquals(completed + 1, result.completedFocusSessions)
        }
    }

    @Test
    fun fourthFocusSessionLeadsToLongBreak() {
        val result = PomodoroRules.nextPhase("FOCUS", 3)
        assertEquals("LONG_BREAK", result.nextPhase)
        assertEquals(4, result.completedFocusSessions)
    }

    @Test
    fun completingEitherBreakReturnsToFocusWithoutChangingSessionCount() {
        val short = PomodoroRules.nextPhase("SHORT_BREAK", 2)
        val long = PomodoroRules.nextPhase("LONG_BREAK", 4)
        assertEquals("FOCUS", short.nextPhase)
        assertEquals(2, short.completedFocusSessions)
        assertEquals("FOCUS", long.nextPhase)
        assertEquals(4, long.completedFocusSessions)
    }
}
