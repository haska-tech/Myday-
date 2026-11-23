package com.haskatech.myday.widget

import com.haskatech.myday.data.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class WidgetDailyProgressTest {
    private val zone = ZoneId.of("Africa/Kampala")
    private val now = Instant.parse("2026-11-23T08:00:00Z").toEpochMilli()

    @Test fun countsCompletedAndActiveTasksScheduledForDeviceLocalToday() {
        val tasks = listOf(
            TaskEntity(id = 1, title = "Done", scheduledAtMillis = now - 3_600_000, isCompleted = true),
            TaskEntity(id = 2, title = "Active", scheduledAtMillis = now + 3_600_000),
            TaskEntity(id = 3, title = "Later", scheduledAtMillis = now + 7_200_000),
            TaskEntity(id = 4, title = "Tomorrow", scheduledAtMillis = now + 86_400_000),
            TaskEntity(id = 5, title = "Inbox")
        )
        val snapshot = WidgetTaskSnapshot.create(tasks, now, zone)
        assertEquals(1, snapshot.completedTodayCount)
        assertEquals(3, snapshot.totalTodayCount)
        assertEquals(33, snapshot.progressPercent)
        assertEquals("1/3 done", snapshot.progressLabel)
        assertEquals(listOf(2L, 3L), snapshot.todayTasks.map { it.id })
    }

    @Test fun noScheduledTasksHaveZeroProgress() {
        val snapshot = WidgetTaskSnapshot.create(listOf(TaskEntity(id = 1, title = "Inbox")), now, zone)
        assertEquals(0, snapshot.totalTodayCount)
        assertEquals(0, snapshot.progressPercent)
        assertEquals("No scheduled tasks", snapshot.progressLabel)
    }

    @Test fun fullyCompletedDayShowsOneHundredPercent() {
        val snapshot = WidgetTaskSnapshot.create(
            listOf(TaskEntity(id = 1, title = "Done", scheduledAtMillis = now, isCompleted = true)),
            now, zone
        )
        assertEquals(100, snapshot.progressPercent)
    }
}
