package com.haskatech.myday.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [TaskEntity::class, TimeTrackingSessionEntity::class], version = 5, exportSchema = false)
abstract class MyDayDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun timeTrackingDao(): TimeTrackingDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tasks ADD COLUMN isTopPriority INTEGER NOT NULL DEFAULT 0")
            }
        }
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tasks ADD COLUMN reminderAtMillis INTEGER")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tasks ADD COLUMN recurrenceRule TEXT NOT NULL DEFAULT 'None'")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
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

        @Volatile private var INSTANCE: MyDayDatabase? = null

        fun getDatabase(context: Context): MyDayDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MyDayDatabase::class.java,
                    "myday_database"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build().also { INSTANCE = it }
            }
    }
}
