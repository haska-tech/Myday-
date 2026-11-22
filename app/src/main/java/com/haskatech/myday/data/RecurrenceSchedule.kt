package com.haskatech.myday.data

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * Calendar-based recurrence arithmetic in the device's current timezone.
 * Occurrences preserve local wall-clock time across daylight-saving transitions.
 * Monthly occurrences remain anchored to the original day (clamped to month end).
 */
object RecurrenceSchedule {
    fun nextDate(afterMillis: Long, rule: String): Long? {
        val zone = ZoneId.systemDefault()
        val start = Instant.ofEpochMilli(afterMillis).atZone(zone)
        val next = when (rule) {
            "Daily" -> start.plusDays(1)
            "Weekly" -> start.plusWeeks(1)
            "Monthly" -> {
                val month = YearMonth.from(start).plusMonths(1)
                month.atDay(start.dayOfMonth.coerceAtMost(month.lengthOfMonth()))
                    .atTime(start.toLocalTime()).atZone(zone)
            }
            else -> return null
        }
        return next.toInstant().toEpochMilli()
    }

    fun datesThrough(startMillis: Long, rule: String, throughMillis: Long): List<Long> {
        if (rule == "None" || throughMillis <= startMillis) return emptyList()
        val zone = ZoneId.systemDefault()
        val anchor = Instant.ofEpochMilli(startMillis).atZone(zone)
        val result = mutableListOf<Long>()
        var index = 1L
        while (result.size < 400) {
            val occurrence = when (rule) {
                "Daily" -> anchor.plusDays(index)
                "Weekly" -> anchor.plusWeeks(index)
                "Monthly" -> {
                    val month = YearMonth.from(anchor).plusMonths(index)
                    month.atDay(anchor.dayOfMonth.coerceAtMost(month.lengthOfMonth()))
                        .atTime(anchor.toLocalTime()).atZone(zone)
                }
                else -> return emptyList()
            }
            val millis = occurrence.toInstant().toEpochMilli()
            if (millis > throughMillis) break
            result += millis
            index++
        }
        return result
    }

    /** Returns the local date of an occurrence, useful to group persisted rows in calendars. */
    fun localDateAt(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
}
