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


    @Test fun migrations1To7PreserveLegacySchemaByApplyingExpectedAdditions() {
        val cases = listOf(
            Triple(MyDayDatabase.MIGRATION_1_2, 1 to 2, listOf("ALTER TABLE tasks ADD COLUMN isTopPriority INTEGER NOT NULL DEFAULT 0")),
            Triple(MyDayDatabase.MIGRATION_2_3, 2 to 3, listOf("ALTER TABLE tasks ADD COLUMN reminderAtMillis INTEGER")),
            Triple(MyDayDatabase.MIGRATION_3_4, 3 to 4, listOf("ALTER TABLE tasks ADD COLUMN recurrenceRule TEXT NOT NULL DEFAULT 'None'")),
            Triple(MyDayDatabase.MIGRATION_4_5, 4 to 5, listOf(
                "ALTER TABLE tasks ADD COLUMN timeSpentMillis INTEGER NOT NULL DEFAULT 0",
                "CREATE TABLE IF NOT EXISTS time_tracking_session (\n                        id INTEGER NOT NULL,\n                        taskId INTEGER NOT NULL,\n                        startedAtMillis INTEGER,\n                        PRIMARY KEY(id)\n                    )"
            )),
            Triple(MyDayDatabase.MIGRATION_5_6, 5 to 6, listOf(
                "CREATE TABLE IF NOT EXISTS time_tracking_entry (\n                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,\n                        taskId INTEGER NOT NULL,\n                        startedAtMillis INTEGER NOT NULL,\n                        endedAtMillis INTEGER NOT NULL,\n                        durationMillis INTEGER NOT NULL\n                    )",
                "CREATE INDEX IF NOT EXISTS index_time_tracking_entry_startedAtMillis ON time_tracking_entry(startedAtMillis)"
            )),
            Triple(MyDayDatabase.MIGRATION_6_7, 6 to 7, listOf("ALTER TABLE tasks ADD COLUMN recurrenceSeriesId TEXT"))
        )
        cases.forEach { (migration, versions, expectedSql) ->
            assertEquals(versions.first, migration.startVersion)
            assertEquals(versions.second, migration.endVersion)
            val statements = mutableListOf<String>()
            migration.migrate(recordingDatabase(statements))
            assertEquals(expectedSql.map(::normalizeSql), statements.map(::normalizeSql))
        }
    }

    private fun normalizeSql(sql: String) = sql.replace(Regex("\\s+"), " ").trim()

    private fun recordingDatabase(statements: MutableList<String>) =
        Proxy.newProxyInstance(SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)) { _, method, args ->
            if (method.name == "execSQL") statements += args?.get(0) as String
            null
        } as SupportSQLiteDatabase
}
