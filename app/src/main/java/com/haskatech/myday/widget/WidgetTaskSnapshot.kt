package com.haskatech.myday.widget
import com.haskatech.myday.data.PlannerTime
import com.haskatech.myday.data.TaskEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
enum class WidgetEmptyState { HAS_TASKS, NONE_PLANNED, ALL_COMPLETED }
/** Immutable, date-aware view of Room tasks for one widget refresh. */
data class WidgetTaskSnapshot(
 val generatedAtMillis: Long,
 val zoneId: ZoneId,
 val date: LocalDate,
 val topPriorities: List<TaskEntity>,
 val todayTasks: List<TaskEntity>,
 val emptyState: WidgetEmptyState
) {
 companion object {
  fun create(tasks: List<TaskEntity>, nowMillis: Long = System.currentTimeMillis(), zone: ZoneId = PlannerTime.zoneId()): WidgetTaskSnapshot {
   val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
   val priorities = tasks.filter { it.isTopPriority && !it.isCompleted }.take(3)
   val todayTasks = tasks.asSequence().filter { !it.isCompleted }.filter { task ->
    task.scheduledAtMillis?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() == today } == true
   }.sortedBy { it.scheduledAtMillis }.toList()
   val visibleCount = priorities.size + todayTasks.count { !it.isTopPriority }.coerceAtMost((6-priorities.size).coerceAtLeast(0))
   val state = when { visibleCount > 0 -> WidgetEmptyState.HAS_TASKS; tasks.none { !it.isCompleted } -> WidgetEmptyState.ALL_COMPLETED; else -> WidgetEmptyState.NONE_PLANNED }
   return WidgetTaskSnapshot(nowMillis,zone,today,priorities,todayTasks,state)
  }
 }
}
