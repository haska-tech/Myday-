package com.haskatech.myday.data

enum class TaskStatusFilter { ALL, ACTIVE, COMPLETED }

/** Separates captured tasks from tasks assigned to a calendar date/time. */
object TaskGrouping {
    fun partition(tasks: List<TaskEntity>): Pair<List<TaskEntity>, List<TaskEntity>> =
        tasks.partition { it.scheduledAtMillis == null }

    /** Applies the Tasks-screen status filter without changing source task records. */
    fun filterByStatus(tasks: List<TaskEntity>, status: TaskStatusFilter): List<TaskEntity> =
        when (status) {
            TaskStatusFilter.ALL -> tasks
            TaskStatusFilter.ACTIVE -> tasks.filterNot { it.isCompleted }
            TaskStatusFilter.COMPLETED -> tasks.filter { it.isCompleted }
        }
}
