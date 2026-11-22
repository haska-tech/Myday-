package com.haskatech.myday.data

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.TimeZone

class PlannerTimeTest {
    private val originalTimezone = TimeZone.getDefault()

    @After fun restoreTimezone() {
        TimeZone.setDefault(originalTimezone)
    }

    @Test fun planningZoneFollowsDeviceTimezone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"))
        assertEquals(ZoneId.of("Asia/Kolkata"), PlannerTime.zoneId())
    }

    @Test fun localDateTimeRoundTripsInDeviceTimezone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"))
        val local = LocalDateTime.of(2026, 11, 22, 14, 35)
        val stored = PlannerTime.toEpochMillis(local)
        assertEquals(local, java.time.Instant.ofEpochMilli(stored).atZone(PlannerTime.zoneId()).toLocalDateTime())
        assertEquals(LocalDate.of(2026, 11, 22), PlannerTime.localDateAt(stored))
    }

    @Test fun startOfDayUsesLocalCalendarBoundaryAcrossDst() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
        val day = LocalDate.of(2026, 3, 8)
        val start = PlannerTime.startOfDayMillis(day)
        val nextStart = PlannerTime.startOfDayMillis(day.plusDays(1))
        assertEquals(23L * 60L * 60L * 1000L, nextStart - start)
    }
}
