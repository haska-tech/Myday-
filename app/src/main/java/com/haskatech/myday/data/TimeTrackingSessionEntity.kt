package com.haskatech.myday.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "time_tracking_session")
data class TimeTrackingSessionEntity(
    @PrimaryKey val id: Int = 1,
    val taskId: Long,
    val startedAtMillis: Long?
)
