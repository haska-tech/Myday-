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
        plannedDurationMinutes: Int? = null,
        deadlineAtMillis: Long? = null,
        urgency: String = "Normal"
    ): List<TaskEntity>? {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return null
        if (plannedDurationMinutes != null && plannedDurationMinutes <= 0) return null
        if (deadlineAtMillis != null && scheduledAtMillis != null && deadlineAtMillis < scheduledAtMillis) return null

        val seriesId = if (recurrenceRule != "None" && scheduledAtMillis != null) UUID.randomUUID().toString() else null
        val first = TaskEntity(
            title = cleanTitle,
            priority = priority,
            scheduledAtMillis = scheduledAtMillis,
            reminderAtMillis = reminderAtMillis,
            recurrenceRule = recurrenceRule,
            recurrenceSeriesId = seriesId,
            plannedDurationMinutes = plannedDurationMinutes,
            deadlineAtMillis = deadlineAtMillis,
            urgency = urgency
        )
        val deadlineOffset = if (scheduledAtMillis != null && deadlineAtMillis != null) deadlineAtMillis - scheduledAtMillis else null
        val occurrences = if (seriesId != null && scheduledAtMillis != null) {
            val horizon = System.currentTimeMillis() + ROLLING_WINDOW_MILLIS
            RecurrenceSchedule.datesThrough(scheduledAtMillis, recurrenceRule, horizon).map { date ->
                first.copy(
                    id = 0,
                    scheduledAtMillis = date,
                    reminderAtMillis = reminderAtMillis?.let { date + (it - scheduledAtMillis) },
                    deadlineAtMillis = deadlineOffset?.let { date + it },
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
        if (!completed) return emptyList()
        return generateNextOccurrences(task)
    }

    suspend fun completeFromWidget(taskId: Long): List<TaskEntity>? {
        val task = taskDao.completeIfActive(taskId) ?: return null
        return generateNextOccurrences(task)
    }

    private suspend fun generateNextOccurrences(task: TaskEntity): List<TaskEntity> {
        if (task.recurrenceRule == "None" || task.scheduledAtMillis == null) return emptyList()
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
        val deadlineOffset = task.deadlineAtMillis?.minus(task.scheduledAtMillis)
        val templates = newDates.map { date ->
            task.copy(id = 0, scheduledAtMillis = date,
                reminderAtMillis = reminderOffset?.let { date + it },
                deadlineAtMillis = deadlineOffset?.let { date + it },
                recurrenceSeriesId = seriesId, isCompleted = false, isTopPriority = false,
                timeSpentMillis = 0L, createdAtMillis = System.currentTimeMillis())
        }
        val ids = taskDao.insertAll(templates)
        return templates.mapIndexed { index, item -> item.copy(id = ids[index]) }
    }

    /** Replenishes the rolling horizon when the app opens, even if no occurrence was completed. */
    suspend fun refreshRecurringOccurrences(): List<TaskEntity> {
        val grouped = linkedMapOf<String, MutableList<TaskEntity>>()
        taskDao.getRecurringTasks().forEach { task ->
            val seriesId = task.recurrenceSeriesId ?: UUID.randomUUID().toString().also {
                taskDao.setRecurrenceSeriesId(task.id, it)
            }
            grouped.getOrPut(seriesId) { mutableListOf() }.add(task.copy(recurrenceSeriesId = seriesId))
        }
        return grouped.values.flatMap { series ->
            val first = series.minByOrNull { it.scheduledAtMillis ?: Long.MAX_VALUE } ?: return@flatMap emptyList()
            ensureOccurrences(first)
        }
    }

    private suspend fun ensureOccurrences(task: TaskEntity): List<TaskEntity> {
        val seriesId = task.recurrenceSeriesId ?: return emptyList()
        val series = taskDao.getRecurrenceSeries(seriesId)
        val anchor = series.mapNotNull { it.scheduledAtMillis }.minOrNull() ?: return emptyList()
        val latest = series.mapNotNull { it.scheduledAtMillis }.maxOrNull() ?: return emptyList()
        val horizon = System.currentTimeMillis() + ROLLING_WINDOW_MILLIS
        val existing = series.mapNotNull { it.scheduledAtMillis }.toHashSet()
        val dates = RecurrenceSchedule.datesThrough(anchor, task.recurrenceRule, horizon)
            .filter { it > latest && it !in existing }
        if (dates.isEmpty()) return emptyList()
        val template = series.firstOrNull() ?: task
        val reminderOffset = template.scheduledAtMillis?.let { start -> template.reminderAtMillis?.minus(start) }
        val deadlineOffset = template.scheduledAtMillis?.let { start -> template.deadlineAtMillis?.minus(start) }
        val rows = dates.map { date ->
            template.copy(
                id = 0,
                scheduledAtMillis = date,
                reminderAtMillis = reminderOffset?.let { date + it },
                deadlineAtMillis = deadlineOffset?.let { date + it },
                recurrenceSeriesId = seriesId,
                isCompleted = false,
                isTopPriority = false,
                timeSpentMillis = 0L,
                createdAtMillis = System.currentTimeMillis()
            )
        }
        val ids = taskDao.insertAll(rows)
        return rows.mapIndexed { index, row -> row.copy(id = ids[index]) }
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
        deadlineAtMillis: Long?,
        applyToSeries: Boolean = false,
        urgency: String = task.urgency
    ): List<TaskEntity> {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty() || (plannedDurationMinutes != null && plannedDurationMinutes <= 0) ||
            (deadlineAtMillis != null && scheduledAtMillis != null && deadlineAtMillis < scheduledAtMillis)) return emptyList()

        val oldStart = task.scheduledAtMillis
        val newStart = scheduledAtMillis
        val reanchorSeries = applyToSeries && oldStart != null && newStart != null && oldStart != newStart
        val targets = if (applyToSeries && task.recurrenceSeriesId != null) {
            taskDao.getRecurrenceSeries(task.recurrenceSeriesId)
                .filter { (it.scheduledAtMillis ?: Long.MIN_VALUE) >= (oldStart ?: Long.MIN_VALUE) && (!it.isCompleted || it.id == task.id) }
        } else listOf(task)
        val reminderOffset = if (newStart != null && reminderAtMillis != null) reminderAtMillis - newStart else null
        val deadlineOffset = if (newStart != null && deadlineAtMillis != null) deadlineAtMillis - newStart else null
        val updated = mutableListOf<TaskEntity>()

        for (target in targets) {
            val targetStart = when {
                target.id == task.id -> newStart
                !applyToSeries -> newStart
                !reanchorSeries -> target.scheduledAtMillis
                oldStart != null && newStart != null && target.scheduledAtMillis != null -> {
                    val steps = RecurrenceSchedule.stepsBetween(oldStart, target.scheduledAtMillis, task.recurrenceRule)
                    steps?.let { RecurrenceSchedule.occurrenceAt(newStart, task.recurrenceRule, it) }
                }
                else -> target.scheduledAtMillis
            }
            val targetReminder = targetStart?.let { start -> reminderOffset?.let { start + it } }
            val targetDeadline = if (target.id == task.id) deadlineAtMillis else targetStart?.let { start -> if (reanchorSeries) deadlineOffset?.let { start + it } else target.deadlineAtMillis }
            val changed = taskDao.updateDetails(target.id, cleanTitle, priority, targetStart, targetReminder, plannedDurationMinutes, targetDeadline, urgency)
            if (changed > 0) updated += target.copy(
                title = cleanTitle,
                priority = priority,
                scheduledAtMillis = targetStart,
                reminderAtMillis = targetReminder,
                plannedDurationMinutes = plannedDurationMinutes,
                deadlineAtMillis = targetDeadline,
                urgency = urgency
            )
        }
        return updated
    }
    suspend fun approveSuggestedTime(taskId: Long, startMillis: Long, durationMinutes: Int): TaskEntity? =
        taskDao.applyApprovedSchedule(taskId, startMillis, durationMinutes)

    suspend fun updatePlannedDuration(taskId: Long, minutes: Int): Boolean =
        minutes in 5..1440 && taskDao.updatePlannedDuration(taskId, minutes) == 1

    suspend fun deleteTask(id: Long) = taskDao.delete(id)

    companion object {
        const val ROLLING_WINDOW_MILLIS = 90L * 24L * 60L * 60L * 1000L
    }
}
