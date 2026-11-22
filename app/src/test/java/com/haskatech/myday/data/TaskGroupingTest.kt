package com.haskatech.myday.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TaskGroupingTest {
    @Test fun partitionsScheduledAndUnscheduledTasksWithoutLosingCompletedItems() {
        val tasks = listOf(
            TaskEntity(id = 1, title = "Unscheduled"),
            TaskEntity(id = 2, title = "Scheduled", scheduledAtMillis = 1_800_000_000_000L),
            TaskEntity(id = 3, title = "Completed but unscheduled", isCompleted = true)
        )

        val (unscheduled, scheduled) = TaskGrouping.partition(tasks)

        assertEquals(listOf(1L, 3L), unscheduled.map { it.id })
        assertEquals(listOf(2L), scheduled.map { it.id })
        assertEquals(tasks.size, unscheduled.size + scheduled.size)
    }

    @Test fun emptyTaskListProducesTwoEmptySections() {
        val (unscheduled, scheduled) = TaskGrouping.partition(emptyList())
        assertEquals(emptyList<TaskEntity>(), unscheduled)
        assertEquals(emptyList<TaskEntity>(), scheduled)
    }
}
