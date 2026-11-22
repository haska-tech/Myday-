package com.haskatech.myday.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.Instant

/**
 * Planning timestamps are stored as epoch milliseconds (an absolute instant).
 * Calendar dates and wall-clock times are interpreted in the device's current timezone.
 */
object PlannerTime {
    fun zoneId(): ZoneId = ZoneId.systemDefault()

    /** Every hour shown by the weekly timetable, including midnight through 11 PM. */
    val dayHours: IntRange = 0..23

    fun hourAt(epochMillis: Long, zone: ZoneId = zoneId()): Int =
        Instant.ofEpochMilli(epochMillis).atZone(zone).hour

    fun localDateAt(epochMillis: Long): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zoneId()).toLocalDate()

    fun toEpochMillis(localDateTime: LocalDateTime): Long =
        localDateTime.atZone(zoneId()).toInstant().toEpochMilli()

    fun startOfDayMillis(date: LocalDate): Long =
        date.atStartOfDay(zoneId()).toInstant().toEpochMilli()

    fun utcOffsetLabel(atMillis: Long = System.currentTimeMillis()): String {
        val offset = zoneId().rules.getOffset(Instant.ofEpochMilli(atMillis)).id
        return if (offset == "Z") "UTC" else "UTC$offset"
    }
}
