package com.haskatech.myday.data

import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class DatabaseMigrationTest {
    @Test fun migration7To8AddsNullableDurationWithoutChangingExistingColumns() {
        val statements = mutableListOf<String>()
        val database = recordingDatabase(statements)
        assertEquals(7, MyDayDatabase.MIGRATION_7_8.startVersion)
        assertEquals(8, MyDayDatabase.MIGRATION_7_8.endVersion)
        MyDayDatabase.MIGRATION_7_8.migrate(database)
        assertEquals(listOf("ALTER TABLE tasks ADD COLUMN plannedDurationMinutes INTEGER"), statements)
    }

    @Test fun migration8To9AddsTaskLookupIndexWithoutRebuildingData() {
        val statements = mutableListOf<String>()
        val database = recordingDatabase(statements)
        assertEquals(8, MyDayDatabase.MIGRATION_8_9.startVersion)
        assertEquals(9, MyDayDatabase.MIGRATION_8_9.endVersion)
        MyDayDatabase.MIGRATION_8_9.migrate(database)
        assertEquals(listOf("CREATE INDEX IF NOT EXISTS index_time_tracking_entry_taskId ON time_tracking_entry(taskId)"), statements)
        assertTrue(statements.single().startsWith("CREATE INDEX"))
    }

    @Test fun migration9To10AddsNullableDeadlineColumn() {
        val statements = mutableListOf<String>()
        val database = recordingDatabase(statements)
        assertEquals(9, MyDayDatabase.MIGRATION_9_10.startVersion)
        assertEquals(10, MyDayDatabase.MIGRATION_9_10.endVersion)
        MyDayDatabase.MIGRATION_9_10.migrate(database)
        assertEquals(listOf("ALTER TABLE tasks ADD COLUMN deadlineAtMillis INTEGER"), statements)
    }


    @Test fun migration10To11AddsUrgencyWithNormalDefault() {
        val statements = mutableListOf<String>()
        val database = recordingDatabase(statements)
        assertEquals(10, MyDayDatabase.MIGRATION_10_11.startVersion)
        assertEquals(11, MyDayDatabase.MIGRATION_10_11.endVersion)
        MyDayDatabase.MIGRATION_10_11.migrate(database)
        assertEquals(listOf("ALTER TABLE tasks ADD COLUMN urgency TEXT NOT NULL DEFAULT 'Normal'"), statements)
    }

    private fun recordingDatabase(statements: MutableList<String>) =
        Proxy.newProxyInstance(SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)) { _, method, args ->
            if (method.name == "execSQL") statements += args?.get(0) as String
            null
        } as SupportSQLiteDatabase
}
