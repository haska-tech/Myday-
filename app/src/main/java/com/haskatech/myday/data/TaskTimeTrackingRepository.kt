package com.haskatech.myday.data

import androidx.room.withTransaction
import com.haskatech.myday.focus.TimeTrackingRules
import kotlinx.coroutines.flow.Flow

class TaskTimeTrackingRepository(private val database: MyDayDatabase) {
    private val tasks = database.taskDao()
    private val sessions = database.timeTrackingDao()
    val activeTracking: Flow<TimeTrackingSessionEntity?> = sessions.observeSession()

    suspend fun start(taskId: Long, nowMillis: Long = System.currentTimeMillis()): Boolean =
        database.withTransaction {
            if (!tasks.taskExists(taskId)) return@withTransaction false
            val current = sessions.getSession()
            if (current != null && current.taskId != taskId) return@withTransaction false
            if (current == null) {
                sessions.saveSession(TimeTrackingSessionEntity(taskId = taskId, startedAtMillis = nowMillis))
            } else if (current.startedAtMillis == null) {
                sessions.saveSession(current.copy(startedAtMillis = nowMillis))
            }
            true
        }

    suspend fun pause(taskId: Long, nowMillis: Long = System.currentTimeMillis()) {
        database.withTransaction {
            val current = sessions.getSession() ?: return@withTransaction
            if (current.taskId != taskId || current.startedAtMillis == null) return@withTransaction
            val elapsed = TimeTrackingRules.elapsedMillis(current.startedAtMillis, nowMillis)
            if (elapsed > 0L) tasks.addTimeSpent(taskId, elapsed)
            sessions.saveSession(current.copy(startedAtMillis = null))
        }
    }

    suspend fun stop(taskId: Long, nowMillis: Long = System.currentTimeMillis()) {
        database.withTransaction {
            val current = sessions.getSession() ?: return@withTransaction
            if (current.taskId != taskId) return@withTransaction
            if (current.startedAtMillis != null) {
                val elapsed = TimeTrackingRules.elapsedMillis(current.startedAtMillis, nowMillis)
                if (elapsed > 0L) tasks.addTimeSpent(taskId, elapsed)
            }
            sessions.clearSession()
        }
    }
}
