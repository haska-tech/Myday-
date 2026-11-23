package com.haskatech.myday.reminders

import com.haskatech.myday.data.TaskEntity

object ReminderRecoveryRules {
    fun needsRecovery(task: TaskEntity, nowMillis: Long): Boolean =
        !task.isCompleted && task.reminderAtMillis?.let { it > nowMillis } == true

    fun actions(): Set<String> = setOf(
        android.content.Intent.ACTION_BOOT_COMPLETED,
        android.content.Intent.ACTION_MY_PACKAGE_REPLACED,
        android.content.Intent.ACTION_TIME_CHANGED,
        android.content.Intent.ACTION_TIMEZONE_CHANGED
    )
}
