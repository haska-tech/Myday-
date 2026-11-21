package com.haskatech.myday.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "time_tracking_entry", indices = [Index(value = ["startedAtMillis"])])
data class TimeTrackingEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val startedAtMillis: Long,
    val endedAtMillis: Long,
    val durationMillis: Long
)
