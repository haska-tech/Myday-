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

    @Test fun legacyTaskDurationDefaultsToUnspecified() {
        val task = TaskEntity(title = "Existing task")
        assertEquals(null, task.plannedDurationMinutes)
    }

    @Test fun offsetLabelReflectsDeviceTimezone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"))
        assertEquals("UTC+05:30", PlannerTime.utcOffsetLabel())
    }

    @Test fun timetableCoversEveryHourFromMidnightThroughElevenPm() {
        assertEquals((0..23).toList(), PlannerTime.dayHours.toList())
    }

    @Test fun timetableHourUsesTheSelectedDeviceTimezone() {
        val instant = java.time.Instant.parse("2026-11-22T00:30:00Z").toEpochMilli()
        assertEquals(6, PlannerTime.hourAt(instant, ZoneId.of("Asia/Kolkata")))
        assertEquals(0, PlannerTime.hourAt(instant, ZoneId.of("UTC")))
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
