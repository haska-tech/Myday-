package com.haskatech.myday.data

import java.util.UUID

class TaskRepository(private val taskDao: TaskDao) {
    val tasks = taskDao.observeTasks()

    suspend fun addTask(
        title: String,
        priority: String = "Normal",
        scheduledAtMillis: Long? = null,
        reminderAtMillis: Long? = null,
        recurrenceRule: String = "None",
        plannedDurationMinutes: Int? = null
    ): List<TaskEntity>? {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return null
        if (plannedDurationMinutes != null && plannedDurationMinutes <= 0) return null

        val seriesId = if (recurrenceRule != "None" && scheduledAtMillis != null) UUID.randomUUID().toString() else null
        val first = TaskEntity(
            title = cleanTitle,
            priority = priority,
            scheduledAtMillis = scheduledAtMillis,
            reminderAtMillis = reminderAtMillis,
            recurrenceRule = recurrenceRule,
            recurrenceSeriesId = seriesId,
            plannedDurationMinutes = plannedDurationMinutes
        )
        val occurrences = if (seriesId != null && scheduledAtMillis != null) {
            val horizon = System.currentTimeMillis() + ROLLING_WINDOW_MILLIS
            RecurrenceSchedule.datesThrough(scheduledAtMillis, recurrenceRule, horizon).map { date ->
                first.copy(
                    id = 0,
                    scheduledAtMillis = date,
                    reminderAtMillis = reminderAtMillis?.let { date + (it - scheduledAtMillis) },
                    createdAtMillis = System.currentTimeMillis()
                )
            }
        } else emptyList()

        val inserted = listOf(first) + occurrences
        val ids = taskDao.insertAll(inserted)
        return inserted.mapIndexed { index, task -> task.copy(id = ids[index]) }
    }

    suspend fun setCompleted(task: TaskEntity, completed: Boolean): List<TaskEntity> {
        taskDao.setCompleted(task.id, completed)
        if (!completed || task.recurrenceRule == "None" || task.scheduledAtMillis == null) return emptyList()

        val seriesId = task.recurrenceSeriesId ?: UUID.randomUUID().toString().also {
            taskDao.setRecurrenceSeriesId(task.id, it)
        }
        val series = taskDao.getRecurrenceSeries(seriesId)
        val latestScheduled = series.mapNotNull { it.scheduledAtMillis }.maxOrNull() ?: task.scheduledAtMillis
        val horizon = System.currentTimeMillis() + ROLLING_WINDOW_MILLIS
        val newDates = RecurrenceSchedule.datesThrough(latestScheduled, task.recurrenceRule, horizon)
        if (newDates.isEmpty()) return emptyList()

        val reminderOffset = task.reminderAtMillis?.minus(task.scheduledAtMillis)
        val templates = newDates.map { date ->
            task.copy(
                id = 0,
                scheduledAtMillis = date,
                reminderAtMillis = reminderOffset?.let { date + it },
                recurrenceSeriesId = seriesId,
                isCompleted = false,
                isTopPriority = false,
                timeSpentMillis = 0L,
                createdAtMillis = System.currentTimeMillis()
            )
        }
        val ids = taskDao.insertAll(templates)
        return templates.mapIndexed { index, item -> item.copy(id = ids[index]) }
    }

    suspend fun setTopPriority(id: Long, isTop: Boolean): Boolean {
        return taskDao.setTopPriorityIfAllowed(id, isTop) > 0
    }

    suspend fun updateTask(taskId: Long, title: String, priority: String, scheduledAtMillis: Long?, reminderAtMillis: Long?, plannedDurationMinutes: Int?): Boolean {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return false
        if (plannedDurationMinutes != null && plannedDurationMinutes <= 0) return false
        return taskDao.updateDetails(taskId, cleanTitle, priority, scheduledAtMillis, reminderAtMillis, plannedDurationMinutes) > 0
    }

    suspend fun deleteTask(id: Long) = taskDao.delete(id)

    companion object {
        const val ROLLING_WINDOW_MILLIS = 90L * 24L * 60L * 60L * 1000L
    }
}
