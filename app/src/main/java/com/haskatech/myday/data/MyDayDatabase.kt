package com.haskatech.myday.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TaskEntity::class], version = 1, exportSchema = false)
abstract class MyDayDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile private var INSTANCE: MyDayDatabase? = null

        fun getDatabase(context: Context): MyDayDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MyDayDatabase::class.java,
                    "myday_database"
                ).build().also { INSTANCE = it }
            }
    }
}
