package com.haskatech.myday.data

import java.time.Instant
import java.time.ZoneId
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
        val anchor = series.mapNotNull { it.scheduledAtMillis }.minOrNull() ?: task.scheduledAtMillis
        val latestScheduled = series.mapNotNull { it.scheduledAtMillis }.maxOrNull() ?: task.scheduledAtMillis
        val horizon = System.currentTimeMillis() + ROLLING_WINDOW_MILLIS
        val existingDates = series.mapNotNull { it.scheduledAtMillis }.toHashSet()
        val newDates = RecurrenceSchedule.datesThrough(anchor, task.recurrenceRule, horizon)
            .filter { it > latestScheduled && it !in existingDates }
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

    suspend fun setTopPriority(id: Long, isTop: Boolean): Boolean =
        taskDao.setTopPriorityIfAllowed(id, isTop) > 0

    suspend fun updateTask(
        task: TaskEntity,
        title: String,
        priority: String,
        scheduledAtMillis: Long?,
        reminderAtMillis: Long?,
        plannedDurationMinutes: Int?,
        applyToSeries: Boolean = false
    ): List<TaskEntity> {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty() || (plannedDurationMinutes != null && plannedDurationMinutes <= 0)) return emptyList()

        val targets = if (applyToSeries && task.recurrenceSeriesId != null) {
            taskDao.getRecurrenceSeries(task.recurrenceSeriesId)
                .filter { (it.scheduledAtMillis ?: Long.MIN_VALUE) >= (task.scheduledAtMillis ?: Long.MIN_VALUE) && (!it.isCompleted || it.id == task.id) }
        } else listOf(task)
        val oldStart = task.scheduledAtMillis
        val newStart = scheduledAtMillis
        val zone = ZoneId.systemDefault()
        val oldLocal = oldStart?.let { Instant.ofEpochMilli(it).atZone(zone) }
        val newLocal = newStart?.let { Instant.ofEpochMilli(it).atZone(zone) }
        val dayShift = if (applyToSeries && oldLocal != null && newLocal != null)
            java.time.temporal.ChronoUnit.DAYS.between(oldLocal.toLocalDate(), newLocal.toLocalDate()) else 0L
        val reminderOffset = if (newStart != null && reminderAtMillis != null) reminderAtMillis - newStart else null

        val updated = mutableListOf<TaskEntity>()
        for (target in targets) {
            val targetStart = if (applyToSeries && target.id != task.id && target.scheduledAtMillis != null && newLocal != null) {
                val targetLocal = Instant.ofEpochMilli(target.scheduledAtMillis).atZone(zone)
                targetLocal.toLocalDate().plusDays(dayShift).atTime(newLocal.toLocalTime()).atZone(zone)
                    .toInstant().toEpochMilli()
            } else newStart
            val targetReminder = targetStart?.let { start -> reminderOffset?.let { start + it } }
            val changed = taskDao.updateDetails(target.id, cleanTitle, priority, targetStart, targetReminder, plannedDurationMinutes)
            if (changed > 0) updated += target.copy(
                title = cleanTitle,
                priority = priority,
                scheduledAtMillis = targetStart,
                reminderAtMillis = targetReminder,
                plannedDurationMinutes = plannedDurationMinutes
            )
        }
        return updated
    }

    suspend fun deleteTask(id: Long) = taskDao.delete(id)

    companion object {
        const val ROLLING_WINDOW_MILLIS = 90L * 24L * 60L * 60L * 1000L
    }
}
