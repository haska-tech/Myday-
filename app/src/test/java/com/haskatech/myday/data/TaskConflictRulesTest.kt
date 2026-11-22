package com.haskatech.myday.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskConflictRulesTest {
    private fun task(id: Long, title: String, start: Long?, duration: Int?, completed: Boolean = false) =
        TaskEntity(id = id, title = title, scheduledAtMillis = start, plannedDurationMinutes = duration, isCompleted = completed)

    @Test fun detectsPartialAndContainedOverlaps() {
        val existing = listOf(task(1, "Existing", 1_000_000L, 60))
        assertEquals(1, TaskConflictRules.findConflicts(1_030_000L, 30, existing).size)
        assertEquals(1, TaskConflictRules.findConflicts(1_010_000L, 10, existing).size)
    }

    @Test fun touchingBoundariesAreNotConflicts() {
        val existing = listOf(task(1, "Existing", 1_000_000L, 30))
        assertTrue(TaskConflictRules.findConflicts(2_800_000L, 30, existing).isEmpty())
    }

    @Test fun ignoresCompletedUnscheduledAndUnknownDurationTasks() {
        val existing = listOf(task(1, "Completed", 1_000_000L, 60, true), task(2, "No duration", 1_000_000L, null), task(3, "Unscheduled", null, 60))
        assertTrue(TaskConflictRules.findConflicts(1_030_000L, 30, existing).isEmpty())
    }

    @Test fun excludesTheTaskBeingEdited() {
        val existing = listOf(task(7, "Same task", 1_000_000L, 60))
        assertTrue(TaskConflictRules.findConflicts(1_030_000L, 30, existing, 7).isEmpty())
    }

    @Test fun ignoresMissingOrInvalidCandidateDuration() {
        val existing = listOf(task(1, "Existing", 1_000_000L, 60))
        assertTrue(TaskConflictRules.findConflicts(null, 30, existing).isEmpty())
        assertTrue(TaskConflictRules.findConflicts(1_000_000L, null, existing).isEmpty())
        assertTrue(TaskConflictRules.findConflicts(1_000_000L, 0, existing).isEmpty())
    }

    @Test fun returnsConflictsInStartTimeOrder() {
        val existing = listOf(task(2, "Later", 1_100_000L, 60), task(1, "Earlier", 1_000_000L, 60))
        assertEquals(listOf("Earlier", "Later"), TaskConflictRules.findConflicts(1_050_000L, 90, existing).map { it.title })
    }
}
