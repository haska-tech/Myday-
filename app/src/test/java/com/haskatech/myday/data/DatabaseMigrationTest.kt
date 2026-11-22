package com.haskatech.myday.data

import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class DatabaseMigrationTest {
    @Test fun migration7To8AddsNullableDurationWithoutChangingExistingColumns() {
        val statements = mutableListOf<String>()
        val database = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL") statements += args?.get(0) as String
            null
        } as SupportSQLiteDatabase

        assertEquals(7, MyDayDatabase.MIGRATION_7_8.startVersion)
        assertEquals(8, MyDayDatabase.MIGRATION_7_8.endVersion)
        MyDayDatabase.MIGRATION_7_8.migrate(database)

        assertEquals(1, statements.size)
        assertEquals("ALTER TABLE tasks ADD COLUMN plannedDurationMinutes INTEGER", statements.single())
        assertTrue(statements.single().contains("ADD COLUMN"))
    }
}
