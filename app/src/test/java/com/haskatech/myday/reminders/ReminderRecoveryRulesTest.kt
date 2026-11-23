package com.haskatech.myday.reminders

import com.haskatech.myday.data.TaskEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderRecoveryRulesTest {
    @Test fun onlyActiveFutureRemindersAreRebuilt() {
        val now = 1_000L
        assertTrue(ReminderRecoveryRules.needsRecovery(TaskEntity(id = 1, title = "Future", reminderAtMillis = 2_000L), now))
        assertFalse(ReminderRecoveryRules.needsRecovery(TaskEntity(id = 2, title = "Past", reminderAtMillis = 999L), now))
        assertFalse(ReminderRecoveryRules.needsRecovery(TaskEntity(id = 3, title = "Done", reminderAtMillis = 2_000L, isCompleted = true), now))
        assertFalse(ReminderRecoveryRules.needsRecovery(TaskEntity(id = 4, title = "None"), now))
    }

    @Test fun recoveryCoversBootReplacementAndClockChanges() {
        val actions = ReminderRecoveryRules.actions()
        assertTrue(android.content.Intent.ACTION_BOOT_COMPLETED in actions)
        assertTrue(android.content.Intent.ACTION_MY_PACKAGE_REPLACED in actions)
        assertTrue(android.content.Intent.ACTION_TIME_CHANGED in actions)
        assertTrue(android.content.Intent.ACTION_TIMEZONE_CHANGED in actions)
    }
}
