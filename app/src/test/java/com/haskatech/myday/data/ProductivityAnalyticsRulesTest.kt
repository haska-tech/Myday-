package com.haskatech.myday.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ProductivityAnalyticsRulesTest {
    private val zone = ZoneId.of("Africa/Kampala")
    private val today = LocalDate.of(2026, 11, 23)
    private val now = today.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()

    @Test fun monthRangeUsesLocalCalendarDaysAndCurrentCompletionCohort() {
        val tasks = listOf(
            TaskEntity(id=1, title="Done", isCompleted=true, createdAtMillis=today.minusDays(2).atStartOfDay(zone).toInstant().toEpochMilli()),
            TaskEntity(id=2, title="Open", createdAtMillis=today.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()),
            TaskEntity(id=3, title="Old", isCompleted=true, createdAtMillis=today.minusDays(40).atStartOfDay(zone).toInstant().toEpochMilli())
        )
        val r=ProductivityAnalyticsRules.calculate(tasks, emptyList(), null, now, zone, AnalyticsRange.MONTH)
        assertEquals(today.minusDays(29), r.rangeStart); assertEquals(2, r.tasksCreated)
        assertEquals(1, r.completedFromCohort); assertEquals(50, r.completionRatePercent); assertEquals(30, r.daily.size)
    }

    @Test fun trackedTimeSplitsAtMidnightAndIncludesActiveSession() {
        val start=today.minusDays(1).atTime(23,30).atZone(zone).toInstant().toEpochMilli()
        val end=today.atTime(1,0).atZone(zone).toInstant().toEpochMilli()
        val entry=TimeTrackingEntryEntity(taskId=1, startedAtMillis=start, endedAtMillis=end, durationMillis=end-start)
        val active=today.atTime(11,0).atZone(zone).toInstant().toEpochMilli()
        val r=ProductivityAnalyticsRules.calculate(emptyList(), listOf(entry), active, now, zone, AnalyticsRange.WEEK)
        assertEquals(30*60_000L, r.daily.first { it.date==today.minusDays(1) }.trackedMillis)
        assertEquals(2*60*60_000L, r.daily.last().trackedMillis); assertEquals(150*60_000L, r.trackedMillis)
    }

    @Test fun overdueExcludesCompletedAndFutureDeadlineTasks() {
        val tasks=listOf(TaskEntity(id=1,title="Late",deadlineAtMillis=now-1),
            TaskEntity(id=2,title="Done",isCompleted=true,deadlineAtMillis=now-1),
            TaskEntity(id=3,title="Future",deadlineAtMillis=now+1))
        val r=ProductivityAnalyticsRules.calculate(tasks, emptyList(), null, now, zone, AnalyticsRange.ALL)
        assertEquals(1, r.overdueActive)
    }
}
