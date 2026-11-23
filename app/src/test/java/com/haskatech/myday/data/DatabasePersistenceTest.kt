package com.haskatech.myday.data

import androidx.room.Room
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DatabasePersistenceTest {
    private val context = RuntimeEnvironment.getApplication()
    private val databaseName = "myday_persistence_test.db"

    @Before fun cleanBefore() { context.deleteDatabase(databaseName) }
    @After fun cleanAfter() { context.deleteDatabase(databaseName) }

    @Test fun taskAndRunningTimerSurviveDatabaseCloseAndReopen() = runBlocking {
        val firstOpen = Room.databaseBuilder(context, MyDayDatabase::class.java, databaseName).build()
        val taskId = firstOpen.taskDao().insert(TaskEntity(title = "Persist after restart", timeSpentMillis = 12_000L))
        firstOpen.timeTrackingDao().saveSession(TimeTrackingSessionEntity(taskId = taskId, startedAtMillis = 50_000L))
        firstOpen.close()

        val secondOpen = Room.databaseBuilder(context, MyDayDatabase::class.java, databaseName).build()
        val restoredTask = secondOpen.taskDao().getTaskById(taskId)
        val restoredSession = secondOpen.timeTrackingDao().getSession()
        assertNotNull(restoredTask)
        assertEquals("Persist after restart", restoredTask!!.title)
        assertEquals(12_000L, restoredTask.timeSpentMillis)
        assertEquals(taskId, restoredSession!!.taskId)
        assertEquals(50_000L, restoredSession.startedAtMillis)
        secondOpen.close()
    }
}
