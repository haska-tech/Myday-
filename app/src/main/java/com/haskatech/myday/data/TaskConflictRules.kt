package com.haskatech.myday.data

/** Detects overlaps only when both tasks have an explicitly planned duration. */
data class TaskScheduleConflict(val taskId: Long, val title: String, val startMillis: Long, val endMillis: Long)

object TaskConflictRules {
    fun findConflicts(
        candidateStartMillis: Long?,
        candidateDurationMinutes: Int?,
        tasks: List<TaskEntity>,
        excludeTaskId: Long? = null
    ): List<TaskScheduleConflict> {
        if (candidateStartMillis == null || candidateDurationMinutes == null || candidateDurationMinutes <= 0) return emptyList()
        val candidateEnd = safeEnd(candidateStartMillis, candidateDurationMinutes) ?: return emptyList()
        return tasks.asSequence()
            .filter { it.id != excludeTaskId && !it.isCompleted }
            .mapNotNull { task ->
                val start = task.scheduledAtMillis ?: return@mapNotNull null
                val duration = task.plannedDurationMinutes ?: return@mapNotNull null
                if (duration <= 0) return@mapNotNull null
                val end = safeEnd(start, duration) ?: return@mapNotNull null
                if (candidateStartMillis < end && start < candidateEnd) TaskScheduleConflict(task.id, task.title, start, end) else null
            }
            .sortedBy { it.startMillis }
            .toList()
    }

    private fun safeEnd(startMillis: Long, durationMinutes: Int): Long? {
        val durationMillis = durationMinutes.toLong() * 60_000L
        if (durationMillis <= 0L || startMillis > Long.MAX_VALUE - durationMillis) return null
        return startMillis + durationMillis
    }
}
