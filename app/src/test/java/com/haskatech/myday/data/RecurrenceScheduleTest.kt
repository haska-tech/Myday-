package com.haskatech.myday.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar
import java.util.TimeZone

class RecurrenceScheduleTest {
    private fun date(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance().apply {
            set(year, month - 1, day, 9, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    @Test fun dailyCreatesEveryDayWithinHorizon() {
        val start = date(2026, 1, 1)
        val dates = RecurrenceSchedule.datesThrough(start, "Daily", date(2026, 1, 4))
        assertEquals(3, dates.size)
        assertEquals(date(2026, 1, 2), dates[0])
        assertEquals(date(2026, 1, 4), dates[2])
    }

    @Test fun weeklyCreatesSevenDayOccurrences() {
        val start = date(2026, 1, 1)
        val dates = RecurrenceSchedule.datesThrough(start, "Weekly", date(2026, 1, 16))
        assertEquals(listOf(date(2026, 1, 8), date(2026, 1, 15)), dates)
    }

    @Test fun monthlyKeepsOriginalDayAfterShortMonth() {
        val start = date(2026, 1, 31)
        val dates = RecurrenceSchedule.datesThrough(start, "Monthly", date(2026, 4, 30))
        assertEquals(listOf(date(2026, 2, 28), date(2026, 3, 31), date(2026, 4, 30)), dates)
    }

    @Test fun monthlyLeapYearClampsAndThenReturnsToAnchorDay() {
        val start = date(2024, 1, 31)
        val dates = RecurrenceSchedule.datesThrough(start, "Monthly", date(2024, 3, 31))
        assertEquals(listOf(date(2024, 2, 29), date(2024, 3, 31)), dates)
    }

    @Test fun monthlySeriesEditUsesStableMonthAnchor() {
        val february = date(2026, 2, 28)
        assertEquals(date(2026, 3, 28), RecurrenceSchedule.occurrenceAt(february, "Monthly", 1))
        assertEquals(date(2026, 4, 28), RecurrenceSchedule.occurrenceAt(february, "Monthly", 2))
        assertEquals(1L, RecurrenceSchedule.stepsBetween(february, date(2026, 3, 31), "Monthly"))
    }

    @Test fun weeklySeriesEditUsesWholeWeekSteps() {
        assertEquals(2L, RecurrenceSchedule.stepsBetween(date(2026, 1, 1), date(2026, 1, 15), "Weekly"))
    }

    @Test fun unsupportedAndNoneRulesDoNotGenerateDates() {
        assertTrue(RecurrenceSchedule.datesThrough(100L, "None", 10_000L).isEmpty())
        assertTrue(RecurrenceSchedule.datesThrough(100L, "Yearly", 10_000L).isEmpty())
    }

    @Test fun occurrencesAreStrictlyAfterStartAndDoNotExceedHorizon() {
        val start = date(2026, 2, 1)
        val dates = RecurrenceSchedule.datesThrough(start, "Daily", date(2026, 2, 3))
        assertTrue(dates.all { it > start && it <= date(2026, 2, 3) })
    }

    @Test fun recurrencePreservesWallClockTimeInDeviceTimezoneAcrossDst() {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
            val start = Calendar.getInstance().apply {
                set(2026, Calendar.MARCH, 7, 9, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val next = RecurrenceSchedule.datesThrough(start, "Daily", start + 3L * 24 * 60 * 60 * 1000)
            val localTimes = next.map { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).hour }
            assertEquals(listOf(9, 9, 9), localTimes)
        } finally {
            TimeZone.setDefault(original)
        }
    }
}
