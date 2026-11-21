package com.haskatech.myday.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, isTopPriority DESC, CASE priority WHEN 'High' THEN 0 WHEN 'Normal' THEN 1 ELSE 2 END, CASE WHEN scheduledAtMillis IS NULL THEN 1 ELSE 0 END, scheduledAtMillis ASC, createdAtMillis DESC")
    fun observeTasks(): Flow<List<TaskEntity>>

    @Query("""
        UPDATE tasks
        SET isTopPriority = :isTop
        WHERE id = :taskId
          AND (
            :isTop = 0
            OR (
              isCompleted = 0
              AND (
                SELECT COUNT(*) FROM tasks
                WHERE isTopPriority = 1 AND isCompleted = 0 AND id != :taskId
              ) < 3
            )
          )
    """)
    suspend fun setTopPriorityIfAllowed(taskId: Long, isTop: Boolean): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Query("UPDATE tasks SET isCompleted = :completed, isTopPriority = CASE WHEN :completed = 1 THEN 0 ELSE isTopPriority END WHERE id = :taskId")
    suspend fun setCompleted(taskId: Long, completed: Boolean)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun delete(taskId: Long)
}
