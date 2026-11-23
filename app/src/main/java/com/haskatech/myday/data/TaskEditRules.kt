package com.haskatech.myday.data

/** Validation shared by task editing and its unit tests. */
object TaskEditRules {
    fun isValid(
        title: String,
        scheduledAtMillis: Long?,
        reminderAtMillis: Long?,
        nowMillis: Long,
        originalScheduledAtMillis: Long?,
        originalReminderAtMillis: Long?,
        deadlineAtMillis: Long? = null,
        originalDeadlineAtMillis: Long? = null
    ): Boolean {
        if (title.isBlank()) return false
        if (scheduledAtMillis != null &&
            scheduledAtMillis <= nowMillis &&
            scheduledAtMillis != originalScheduledAtMillis
        ) return false
        if (reminderAtMillis != null) {
            if (scheduledAtMillis == null || reminderAtMillis > scheduledAtMillis) return false
            if (reminderAtMillis <= nowMillis &&
                reminderAtMillis != originalReminderAtMillis
            ) return false
        }
        if (deadlineAtMillis != null) {
            if (deadlineAtMillis < (scheduledAtMillis ?: nowMillis)) return false
            if (deadlineAtMillis <= nowMillis && deadlineAtMillis != originalDeadlineAtMillis) return false
        }
        return true
    }
}
