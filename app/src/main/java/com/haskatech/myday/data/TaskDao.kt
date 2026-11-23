package com.haskatech.myday.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, isTopPriority DESC, CASE priority WHEN 'High' THEN 0 WHEN 'Normal' THEN 1 ELSE 2 END, CASE WHEN scheduledAtMillis IS NULL THEN 1 ELSE 0 END, scheduledAtMillis ASC, createdAtMillis DESC")
    fun observeTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY id ASC")
    suspend fun getAllTasks(): List<TaskEntity>

    @Query("DELETE FROM tasks")
    suspend fun clearAllTasks()

    @Query("""
        UPDATE tasks SET isTopPriority = :isTop WHERE id = :taskId AND (
        :isTop = 0 OR (isCompleted = 0 AND (SELECT COUNT(*) FROM tasks WHERE isTopPriority = 1 AND isCompleted = 0 AND id != :taskId) < 3))
    """)
    suspend fun setTopPriorityIfAllowed(taskId: Long, isTop: Boolean): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>): List<Long>

    @Query("SELECT * FROM tasks WHERE recurrenceSeriesId = :seriesId ORDER BY scheduledAtMillis ASC")
    suspend fun getRecurrenceSeries(seriesId: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE recurrenceRule != 'None' AND scheduledAtMillis IS NOT NULL ORDER BY scheduledAtMillis ASC")
    suspend fun getRecurringTasks(): List<TaskEntity>

    @Query("UPDATE tasks SET recurrenceSeriesId = :seriesId WHERE id = :taskId")
    suspend fun setRecurrenceSeriesId(taskId: Long, seriesId: String)

    @Query("UPDATE tasks SET isCompleted = :completed, isTopPriority = CASE WHEN :completed = 1 THEN 0 ELSE isTopPriority END WHERE id = :taskId")
    suspend fun setCompleted(taskId: Long, completed: Boolean)

    @Query("UPDATE tasks SET timeSpentMillis = timeSpentMillis + :durationMillis WHERE id = :taskId")
    suspend fun addTimeSpent(taskId: Long, durationMillis: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM tasks WHERE id = :taskId)")
    suspend fun taskExists(taskId: Long): Boolean

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Long): TaskEntity?

    @Query("UPDATE tasks SET isCompleted = 1, isTopPriority = 0 WHERE id = :taskId AND isCompleted = 0")
    suspend fun markCompletedIfActive(taskId: Long): Int

    @Transaction
    suspend fun completeIfActive(taskId: Long): TaskEntity? {
        val current = getTaskById(taskId) ?: return null
        if (current.isCompleted) return null
        return current.takeIf { markCompletedIfActive(taskId) == 1 }
    }

    @Query("""
        UPDATE tasks SET title = :title, priority = :priority, scheduledAtMillis = :scheduledAtMillis,
        reminderAtMillis = :reminderAtMillis, plannedDurationMinutes = :plannedDurationMinutes, deadlineAtMillis = :deadlineAtMillis
        WHERE id = :taskId
    """)
    suspend fun updateDetails(taskId: Long, title: String, priority: String, scheduledAtMillis: Long?, reminderAtMillis: Long?, plannedDurationMinutes: Int?, deadlineAtMillis: Long?): Int

    @Query("UPDATE tasks SET plannedDurationMinutes = :minutes WHERE id = :taskId AND isCompleted = 0")
    suspend fun updatePlannedDuration(taskId: Long, minutes: Int): Int

    @Transaction
    suspend fun applyApprovedSchedule(taskId: Long, startMillis: Long, durationMinutes: Int): TaskEntity? {
        val current = getTaskById(taskId) ?: return null
        if (current.isCompleted || !SmartScheduleRules.isWithinDefaultWorkHours(startMillis, durationMinutes) ||
            (current.deadlineAtMillis != null && (startMillis > current.deadlineAtMillis || durationMinutes <= 0 || startMillis > Long.MAX_VALUE - durationMinutes.toLong() * 60000L || startMillis + durationMinutes.toLong() * 60000L > current.deadlineAtMillis)) || TaskConflictRules.findConflicts(startMillis, durationMinutes, getAllTasks().map { it.copy(plannedDurationMinutes = it.plannedDurationMinutes ?: 60) }, taskId).isNotEmpty()) return null
        val reminderOffset = current.scheduledAtMillis?.let { old -> current.reminderAtMillis?.minus(old) }
        val reminder = reminderOffset?.let { startMillis + it }
        val changed = updateDetails(taskId, current.title, current.priority, startMillis, reminder, durationMinutes, current.deadlineAtMillis)
        return if (changed == 1) current.copy(scheduledAtMillis = startMillis, reminderAtMillis = reminder, plannedDurationMinutes = durationMinutes) else null
    }

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun delete(taskId: Long)
}
