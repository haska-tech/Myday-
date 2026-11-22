package com.haskatech.myday.data

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Calendar recurrence arithmetic using the device timezone and preserving local wall-clock time. */
object RecurrenceSchedule {
    fun occurrenceAt(startMillis: Long, rule: String, index: Long): Long? {
        if (index < 0) return null
        val zone = ZoneId.systemDefault()
        val start = Instant.ofEpochMilli(startMillis).atZone(zone)
        val occurrence = when (rule) {
            "Daily" -> start.plusDays(index)
            "Weekly" -> start.plusWeeks(index)
            "Monthly" -> {
                val month = YearMonth.from(start).plusMonths(index)
                month.atDay(start.dayOfMonth.coerceAtMost(month.lengthOfMonth())).atTime(start.toLocalTime()).atZone(zone)
            }
            else -> return null
        }
        return occurrence.toInstant().toEpochMilli()
    }

    fun stepsBetween(fromMillis: Long, toMillis: Long, rule: String): Long? {
        val zone = ZoneId.systemDefault()
        val from = Instant.ofEpochMilli(fromMillis).atZone(zone)
        val to = Instant.ofEpochMilli(toMillis).atZone(zone)
        if (to.isBefore(from)) return null
        return when (rule) {
            "Daily" -> ChronoUnit.DAYS.between(from.toLocalDate(), to.toLocalDate())
            "Weekly" -> ChronoUnit.DAYS.between(from.toLocalDate(), to.toLocalDate()).takeIf { it % 7L == 0L }?.div(7L)
            "Monthly" -> ChronoUnit.MONTHS.between(YearMonth.from(from), YearMonth.from(to))
            else -> null
        }
    }

    fun nextDate(afterMillis: Long, rule: String): Long? = occurrenceAt(afterMillis, rule, 1L)


    fun datesThrough(startMillis: Long, rule: String, throughMillis: Long): List<Long> {
        if (rule == "None" || throughMillis <= startMillis) return emptyList()
        val result = mutableListOf<Long>()
        var index = 1L
        while (result.size < 400) {
            val millis = occurrenceAt(startMillis, rule, index) ?: return emptyList()
            if (millis > throughMillis) break
            result += millis
            index++
        }
        return result
    }


    fun localDateAt(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
}