package com.haskatech.myday.data

import androidx.room.withTransaction
import com.haskatech.myday.focus.TimeTrackingRules
import kotlinx.coroutines.flow.Flow

class TaskTimeTrackingRepository(private val database: MyDayDatabase) {
    private val tasks = database.taskDao()
    private val sessions = database.timeTrackingDao()
    val activeTracking: Flow<TimeTrackingSessionEntity?> = sessions.observeSession()
    val entries: Flow<List<TimeTrackingEntryEntity>> = sessions.observeEntries()

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

    private suspend fun recordSegment(taskId: Long, startedAtMillis: Long, endedAtMillis: Long) {
        val duration = TimeTrackingRules.elapsedMillis(startedAtMillis, endedAtMillis)
        if (duration <= 0L) return
        sessions.insertEntry(TimeTrackingEntryEntity(
            taskId = taskId,
            startedAtMillis = startedAtMillis,
            endedAtMillis = maxOf(startedAtMillis, endedAtMillis),
            durationMillis = duration
        ))
        tasks.addTimeSpent(taskId, duration)
    }

    suspend fun pause(taskId: Long, nowMillis: Long = System.currentTimeMillis()) {
        database.withTransaction {
            val current = sessions.getSession() ?: return@withTransaction
            val startedAt = current.startedAtMillis ?: return@withTransaction
            if (current.taskId != taskId) return@withTransaction
            recordSegment(taskId, startedAt, nowMillis)
            sessions.saveSession(current.copy(startedAtMillis = null))
        }
    }

    suspend fun stop(taskId: Long, nowMillis: Long = System.currentTimeMillis()) {
        database.withTransaction {
            val current = sessions.getSession() ?: return@withTransaction
            if (current.taskId != taskId) return@withTransaction
            current.startedAtMillis?.let { recordSegment(taskId, it, nowMillis) }
            sessions.clearSession()
        }
    }
}
