package com.haskatech.myday.data

import androidx.room.Room
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class MyDayBackupRestoreTest {
    private lateinit var database: MyDayDatabase

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), MyDayDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After fun tearDown() { database.close() }

    @Test fun restoreReplacesTasksEntriesAndSessionAsOneRoomOperation() = runBlocking {
        val oldId = database.taskDao().insert(TaskEntity(title = "Old local task"))
        val replacement = TaskEntity(id = 41, title = "Restored task", timeSpentMillis = 2_500L)
        val entry = TimeTrackingEntryEntity(8, 41, 1_000L, 3_500L, 2_500L)
        val session = TimeTrackingSessionEntity(1, 41, 4_000L)
        val snapshot = MyDayBackupManager.decode(
            MyDayBackupManager.encode(listOf(replacement), listOf(entry), session)
        )

        val previous = MyDayBackupManager.restoreRecords(database, snapshot)
        assertEquals(listOf(oldId), previous.map { it.id })
        assertNull(database.taskDao().getTaskById(oldId))
        assertEquals(replacement, database.taskDao().getTaskById(41L))
        assertEquals(listOf(entry), database.timeTrackingDao().getAllEntries())
        assertEquals(session, database.timeTrackingDao().getSession())
        assertTrue(database.taskDao().taskExists(41L))
    }
}
