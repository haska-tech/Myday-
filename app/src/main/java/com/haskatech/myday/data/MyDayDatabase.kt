package com.haskatech.myday.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [TaskEntity::class, CategoryEntity::class, TimeTrackingSessionEntity::class, TimeTrackingEntryEntity::class], version = 12, exportSchema = false)
abstract class MyDayDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun categoryDao(): CategoryDao
    abstract fun timeTrackingDao(): TimeTrackingDao

    companion object {
        internal val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tasks ADD COLUMN isTopPriority INTEGER NOT NULL DEFAULT 0")
            }
        }
        internal val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tasks ADD COLUMN reminderAtMillis INTEGER")
            }
        }
        internal val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tasks ADD COLUMN recurrenceRule TEXT NOT NULL DEFAULT 'None'")
            }
        }
        internal val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tasks ADD COLUMN timeSpentMillis INTEGER NOT NULL DEFAULT 0")
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS time_tracking_session (
                        id INTEGER NOT NULL,
                        taskId INTEGER NOT NULL,
                        startedAtMillis INTEGER,
                        PRIMARY KEY(id)
                    )
                """.trimIndent())
            }
        }
        internal val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS time_tracking_entry (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        taskId INTEGER NOT NULL,
                        startedAtMillis INTEGER NOT NULL,
                        endedAtMillis INTEGER NOT NULL,
                        durationMillis INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS index_time_tracking_entry_startedAtMillis ON time_tracking_entry(startedAtMillis)")
            }
        }
        internal val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tasks ADD COLUMN recurrenceSeriesId TEXT")
            }
        }
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tasks ADD COLUMN plannedDurationMinutes INTEGER")
            }
        }
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE INDEX IF NOT EXISTS index_time_tracking_entry_taskId ON time_tracking_entry(taskId)")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tasks ADD COLUMN deadlineAtMillis INTEGER")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tasks ADD COLUMN urgency TEXT NOT NULL DEFAULT 'Normal'")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tasks ADD COLUMN categoryId INTEGER")
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS categories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        colorHex TEXT NOT NULL,
                        iconKey TEXT NOT NULL,
                        isDefault INTEGER NOT NULL DEFAULT 0,
                        createdAtMillis INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_categories_name ON categories(name)")
                val now = System.currentTimeMillis()
                val defaults = listOf(
                    "Personal" to "#5B8DEF",
                    "Work" to "#7C6EE6",
                    "Family" to "#E58A4E",
                    "Health" to "#38A88A",
                    "Finance" to "#C49A36",
                    "Education" to "#B36BC7"
                )
                defaults.forEachIndexed { index, item ->
                    database.execSQL(
                        "INSERT INTO categories(id, name, colorHex, iconKey, isDefault, createdAtMillis) VALUES(?, ?, ?, ?, 1, ?)",
                        arrayOf(index + 1, item.first, item.second, "folder", now)
                    )
                }
            }
        }

        @Volatile private var INSTANCE: MyDayDatabase? = null

        fun getDatabase(context: Context): MyDayDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MyDayDatabase::class.java,
                    "myday_database"
                ).addMigrations(
                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
                    MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
                    MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12
                ).build().also { INSTANCE = it }
            }
    }
}
