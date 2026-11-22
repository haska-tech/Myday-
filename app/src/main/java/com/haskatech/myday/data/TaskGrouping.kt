package com.haskatech.myday.data

/** Separates captured tasks from tasks assigned to a calendar date/time. */
object TaskGrouping {
    fun partition(tasks: List<TaskEntity>): Pair<List<TaskEntity>, List<TaskEntity>> =
        tasks.partition { it.scheduledAtMillis == null }
}
