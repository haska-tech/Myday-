package com.haskatech.myday.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class RecurrenceScheduleTest {
    private val timezone = TimeZone.getTimeZone("Africa/Kampala")

    private fun date(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance(timezone).apply {
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

    @Test fun monthlyCreatesCalendarMonthOccurrences() {
        val start = date(2026, 1, 15)
        val dates = RecurrenceSchedule.datesThrough(start, "Monthly", date(2026, 4, 15))
        assertEquals(listOf(date(2026, 2, 15), date(2026, 3, 15), date(2026, 4, 15)), dates)
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
}
