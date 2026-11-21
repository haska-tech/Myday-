package com.haskatech.myday.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeTrackingDao {
    @Query("SELECT * FROM time_tracking_session WHERE id = 1")
    fun observeSession(): Flow<TimeTrackingSessionEntity?>

    @Query("SELECT * FROM time_tracking_session WHERE id = 1")
    suspend fun getSession(): TimeTrackingSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSession(session: TimeTrackingSessionEntity)

    @Query("DELETE FROM time_tracking_session WHERE id = 1")
    suspend fun clearSession()

    @Insert
    suspend fun insertEntry(entry: TimeTrackingEntryEntity)

    @Query("SELECT * FROM time_tracking_entry ORDER BY startedAtMillis ASC")
    fun observeEntries(): Flow<List<TimeTrackingEntryEntity>>
}
