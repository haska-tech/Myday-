package com.haskatech.myday.focus

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class TimeInterval(val startedAtMillis: Long, val endedAtMillis: Long)

object TimeTrackingRules {
    fun elapsedMillis(startedAtMillis: Long, nowMillis: Long): Long =
        (nowMillis - startedAtMillis).coerceAtLeast(0L)

    fun formatDuration(durationMillis: Long): String {
        val totalMinutes = durationMillis.coerceAtLeast(0L) / 60_000L
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        return if (hours > 0L) hours.toString() + "h " + minutes.toString().padStart(2, '0') + "m"
        else minutes.toString() + "m"
    }

    // End time is exclusive; sessions are split at local midnight boundaries.
    fun durationByDate(intervals: List<TimeInterval>, zone: ZoneId): Map<LocalDate, Long> {
        val totals = mutableMapOf<LocalDate, Long>()
        intervals.forEach { interval ->
            val start = interval.startedAtMillis
            val end = interval.endedAtMillis
            if (end <= start) return@forEach
            var date = Instant.ofEpochMilli(start).atZone(zone).toLocalDate()
            val lastDate = Instant.ofEpochMilli(end - 1L).atZone(zone).toLocalDate()
            while (!date.isAfter(lastDate)) {
                val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
                val nextDayStart = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val overlapStart = maxOf(start, dayStart)
                val overlapEnd = minOf(end, nextDayStart)
                if (overlapEnd > overlapStart) totals[date] = (totals[date] ?: 0L) + overlapEnd - overlapStart
                date = date.plusDays(1)
            }
        }
        return totals
    }
}
