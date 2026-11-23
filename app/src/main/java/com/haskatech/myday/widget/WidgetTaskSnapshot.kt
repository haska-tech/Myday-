package com.haskatech.myday.widget

import com.haskatech.myday.data.PlannerTime
import com.haskatech.myday.data.TaskEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class WidgetEmptyState { HAS_TASKS, NONE_PLANNED, ALL_COMPLETED }
enum class WidgetSizeClass { SMALL, MEDIUM, LARGE }

data class WidgetTaskSnapshot(
    val generatedAtMillis: Long,
    val zoneId: ZoneId,
    val date: LocalDate,
    val topPriorities: List<TaskEntity>,
    val todayTasks: List<TaskEntity>,
    val emptyState: WidgetEmptyState,
    val completedTodayCount: Int,
    val totalTodayCount: Int
) {
    val progressPercent: Int
        get() = if (totalTodayCount == 0) 0 else (completedTodayCount * 100 / totalTodayCount)

    val progressLabel: String
        get() = if (totalTodayCount == 0) "No scheduled tasks" else "$completedTodayCount/$totalTodayCount done"

    fun visibleTasks(): List<TaskEntity> =
        topPriorities + todayTasks.filterNot { it.isTopPriority }.take((6 - topPriorities.size).coerceAtLeast(0))

    companion object {
        fun create(
            tasks: List<TaskEntity>,
            nowMillis: Long = System.currentTimeMillis(),
            zone: ZoneId = PlannerTime.zoneId()
        ): WidgetTaskSnapshot {
            val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
            val scheduledToday = tasks.filter { task ->
                task.scheduledAtMillis?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() == today } == true
            }
            val priorities = tasks.filter { it.isTopPriority && !it.isCompleted }.take(3)
            val todayTasks = scheduledToday.filter { !it.isCompleted }.sortedBy { it.scheduledAtMillis }
            val visibleCount = priorities.size + todayTasks.count { !it.isTopPriority }
                .coerceAtMost((6 - priorities.size).coerceAtLeast(0))
            val state = when {
                visibleCount > 0 -> WidgetEmptyState.HAS_TASKS
                tasks.isNotEmpty() && tasks.all { it.isCompleted } -> WidgetEmptyState.ALL_COMPLETED
                else -> WidgetEmptyState.NONE_PLANNED
            }
            return WidgetTaskSnapshot(
                generatedAtMillis = nowMillis,
                zoneId = zone,
                date = today,
                topPriorities = priorities,
                todayTasks = todayTasks,
                emptyState = state,
                completedTodayCount = scheduledToday.count { it.isCompleted },
                totalTodayCount = scheduledToday.size
            )
        }
    }
}

object WidgetTaskCompletionRules {
    fun pendingIntentUri(taskId: Long): String {
        require(taskId > 0L) { "Task ID must be positive" }
        return "myday://widget/complete/$taskId"
    }
}

object WidgetSizeRules {
    fun choose(widthDp: Int, heightDp: Int): WidgetSizeClass = when {
        widthDp >= 260 && heightDp >= 150 -> WidgetSizeClass.LARGE
        widthDp >= 170 && heightDp >= 100 -> WidgetSizeClass.MEDIUM
        else -> WidgetSizeClass.SMALL
    }

    fun taskLimit(size: WidgetSizeClass): Int = when (size) {
        WidgetSizeClass.SMALL -> 2
        WidgetSizeClass.MEDIUM -> 5
        WidgetSizeClass.LARGE -> 8
    }
}
