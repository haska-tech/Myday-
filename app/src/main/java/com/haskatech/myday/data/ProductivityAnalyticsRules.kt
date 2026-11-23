package com.haskatech.myday.data

import com.haskatech.myday.focus.TimeInterval
import com.haskatech.myday.focus.TimeTrackingRules
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class AnalyticsRange(val label: String, val days: Long?) {
    WEEK("7 days", 7), MONTH("30 days", 30), QUARTER("90 days", 90), ALL("All time", null)
}
data class DailyAnalyticsPoint(val date: LocalDate, val tasksCreated: Int, val trackedMillis: Long)
data class ProductivityAnalytics(
    val rangeStart: LocalDate, val rangeEnd: LocalDate, val tasksCreated: Int,
    val completedFromCohort: Int, val completionRatePercent: Int, val overdueActive: Int,
    val trackedMillis: Long, val daily: List<DailyAnalyticsPoint>
)
/** Completion is a current-status cohort: TaskEntity has no completion timestamp. */
object ProductivityAnalyticsRules {
    fun calculate(tasks: List<TaskEntity>, entries: List<TimeTrackingEntryEntity>, activeStartedAtMillis: Long?,
        nowMillis: Long, zone: ZoneId, range: AnalyticsRange): ProductivityAnalytics {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val earliest = sequenceOf(
            tasks.minOfOrNull { Instant.ofEpochMilli(it.createdAtMillis).atZone(zone).toLocalDate() },
            entries.minOfOrNull { Instant.ofEpochMilli(it.startedAtMillis).atZone(zone).toLocalDate() }
        ).filterNotNull().minOrNull() ?: today
        val start = range.days?.let { today.minusDays(it - 1) } ?: earliest
        val selected = tasks.filter {
            val date = Instant.ofEpochMilli(it.createdAtMillis).atZone(zone).toLocalDate()
            !date.isBefore(start) && !date.isAfter(today)
        }
        val intervals = entries.map { TimeInterval(it.startedAtMillis, it.endedAtMillis) } +
            listOfNotNull(activeStartedAtMillis?.let { TimeInterval(it, nowMillis) })
        val totals = TimeTrackingRules.durationByDate(intervals, zone)
        val createdByDate = selected.groupingBy {
            Instant.ofEpochMilli(it.createdAtMillis).atZone(zone).toLocalDate()
        }.eachCount()
        val dailyStart = maxOf(start, today.minusDays(29))
        val daily = generateSequence(dailyStart) { d -> if (d.isBefore(today)) d.plusDays(1) else null }.map { date ->
            DailyAnalyticsPoint(date, createdByDate[date] ?: 0, totals[date] ?: 0L)
        }.toList()
        val completed = selected.count { it.isCompleted }
        val trackedInRange = totals.filterKeys { !it.isBefore(start) && !it.isAfter(today) }.values.sum()
        return ProductivityAnalytics(start, today, selected.size, completed,
            if (selected.isEmpty()) 0 else completed * 100 / selected.size,
            tasks.count { !it.isCompleted && it.deadlineAtMillis != null && it.deadlineAtMillis < nowMillis },
            trackedInRange, daily)
    }
}
