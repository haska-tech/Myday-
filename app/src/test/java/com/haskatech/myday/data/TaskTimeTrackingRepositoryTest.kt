package com.haskatech.myday.data

import androidx.room.Room
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class TaskTimeTrackingRepositoryTest {
    private lateinit var database: MyDayDatabase
    private lateinit var repository: TaskTimeTrackingRepository

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), MyDayDatabase::class.java)
            .allowMainThreadQueries().build()
        repository = TaskTimeTrackingRepository(database)
    }

    @After fun tearDown() { database.close() }

    @Test fun timeSegmentAndTaskTotalCommitTogether() = runBlocking {
        val taskId = database.taskDao().insert(TaskEntity(title = "Study"))
        assertTrue(repository.start(taskId, 1_000L))
        repository.pause(taskId, 4_000L)
        val task = database.taskDao().getTaskById(taskId)!!
        val entries = database.timeTrackingDao().getAllEntries()
        assertEquals(3_000L, task.timeSpentMillis)
        assertEquals(1, entries.size)
        assertEquals(3_000L, entries.single().durationMillis)
        assertEquals(null, database.timeTrackingDao().getSession()!!.startedAtMillis)
    }

    @Test fun refusesMissingTaskAndDoesNotCreateSession() = runBlocking {
        assertFalse(repository.start(999L, 1_000L))
        assertEquals(null, database.timeTrackingDao().getSession())
    }
}
