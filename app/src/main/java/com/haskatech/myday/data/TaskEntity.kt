package com.haskatech.myday.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String = "",
    val scheduledAtMillis: Long? = null,
    val reminderAtMillis: Long? = null,
    val recurrenceRule: String = "None",
    val priority: String = "Normal",
    val isTopPriority: Boolean = false,
    val isCompleted: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0") val timeSpentMillis: Long = 0L,
    val recurrenceSeriesId: String? = null,
    /** Optional planned block length; null preserves the legacy point-in-time task model. */
    val plannedDurationMinutes: Int? = null,
    val deadlineAtMillis: Long? = null,
    @ColumnInfo(defaultValue = "'Normal'") val urgency: String = "Normal"
)
