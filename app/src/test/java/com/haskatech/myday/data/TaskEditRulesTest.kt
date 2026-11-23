package com.haskatech.myday.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskEditRulesTest {
    @Test fun rejectsBlankTaskTitles() {
        assertFalse(TaskEditRules.isValid("  ", null, null, 100L, null, null))
    }

    @Test fun permitsKeepingAnExistingPastScheduleAndReminderWhileEditingDetails() {
        assertTrue(TaskEditRules.isValid("Updated title", 50L, 40L, 100L, 50L, 40L))
    }

    @Test fun rejectsMovingATaskIntoThePast() {
        assertFalse(TaskEditRules.isValid("Task", 50L, null, 100L, 60L, null))
    }

    @Test fun rejectsReminderAfterScheduledTime() {
        assertFalse(TaskEditRules.isValid("Task", 200L, 201L, 100L, null, null))
    }

    @Test fun rejectsNewReminderInThePastButAllowsAnUnchangedPastReminder() {
        assertFalse(TaskEditRules.isValid("Task", 200L, 90L, 100L, null, null))
        assertTrue(TaskEditRules.isValid("Task", 200L, 90L, 100L, 200L, 90L))
    }

    @Test fun validatesDeadlinesAgainstScheduleAndCurrentTime() {
        assertFalse(TaskEditRules.isValid("Task", 200L, null, 100L, null, null, 150L, null))
        assertFalse(TaskEditRules.isValid("Task", null, null, 100L, null, null, 90L, null))
        assertTrue(TaskEditRules.isValid("Task", 200L, null, 100L, 200L, null, 150L, 150L))
    }

    @Test fun allowsUnscheduledTasksWithoutReminders() {
        assertTrue(TaskEditRules.isValid("Task", null, null, 100L, null, null))
    }
}
