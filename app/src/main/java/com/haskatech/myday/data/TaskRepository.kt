package com.haskatech.myday.data

import java.util.Calendar

class TaskRepository(private val taskDao: TaskDao) {
    val tasks = taskDao.observeTasks()

    suspend fun addTask(title: String, priority: String = "Normal", scheduledAtMillis: Long? = null, reminderAtMillis: Long? = null, recurrenceRule: String = "None"): Long? {
        val cleanTitle = title.trim()
        if (cleanTitle.isNotEmpty()) return taskDao.insert(
            TaskEntity(title = cleanTitle, priority = priority, scheduledAtMillis = scheduledAtMillis, reminderAtMillis = reminderAtMillis, recurrenceRule = recurrenceRule)
        )
        return null
    }

    suspend fun setCompleted(task: TaskEntity, completed: Boolean): TaskEntity? {
        taskDao.setCompleted(task.id, completed)
        if (!completed || task.recurrenceRule == "None" || task.scheduledAtMillis == null) return null

        val nextDate = Calendar.getInstance().apply {
            timeInMillis = task.scheduledAtMillis
            when (task.recurrenceRule) {
                "Daily" -> add(Calendar.DAY_OF_YEAR, 1)
                "Weekly" -> add(Calendar.WEEK_OF_YEAR, 1)
                "Monthly" -> add(Calendar.MONTH, 1)
                else -> return null
            }
        }.timeInMillis
        val reminderOffset = if (task.reminderAtMillis != null && task.scheduledAtMillis != null) {
            task.reminderAtMillis - task.scheduledAtMillis
        } else null
        val nextTask = task.copy(
            id = 0,
            scheduledAtMillis = nextDate,
            reminderAtMillis = reminderOffset?.let { nextDate + it },
            isCompleted = false,
            isTopPriority = false,
            createdAtMillis = System.currentTimeMillis()
        )
        val newId = taskDao.insert(nextTask)
        return nextTask.copy(id = newId)
    }

    suspend fun setTopPriority(id: Long, isTop: Boolean): Boolean {
        if (isTop && taskDao.countActiveTopPriorities() >= 3) return false
        taskDao.setTopPriority(id, isTop)
        return true
    }

    suspend fun deleteTask(id: Long) = taskDao.delete(id)
}
