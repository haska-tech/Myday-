package com.haskatech.myday.data

class TaskRepository(private val taskDao: TaskDao) {
    val tasks = taskDao.observeTasks()

    suspend fun addTask(title: String, priority: String = "Normal") {
        val cleanTitle = title.trim()
        if (cleanTitle.isNotEmpty()) taskDao.insert(TaskEntity(title = cleanTitle, priority = priority))
    }

    suspend fun setCompleted(id: Long, completed: Boolean) =
        taskDao.setCompleted(id, completed)

    suspend fun deleteTask(id: Long) = taskDao.delete(id)
}
