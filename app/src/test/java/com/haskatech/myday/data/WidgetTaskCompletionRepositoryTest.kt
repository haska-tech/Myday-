package com.haskatech.myday.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class WidgetTaskCompletionRepositoryTest {
    private val database = Room.inMemoryDatabaseBuilder(
        RuntimeEnvironment.getApplication(),
        MyDayDatabase::class.java
    ).allowMainThreadQueries().build()

    @After fun closeDatabase() = database.close()

    @Test fun firstWidgetCompletionSucceedsAndDuplicateIsIgnored() = runBlocking {
        val taskId = database.taskDao().insert(TaskEntity(title = "Widget task"))
        val repository = TaskRepository(database.taskDao())

        assertNotNull(repository.completeFromWidget(taskId))
        assertNull(repository.completeFromWidget(taskId))
        assertTrue(database.taskDao().getTaskById(taskId)!!.isCompleted)
    }

    @Test fun deletedTaskIsTreatedAsStaleWidgetAction() = runBlocking {
        val taskId = database.taskDao().insert(TaskEntity(title = "Deleted task"))
        database.taskDao().delete(taskId)

        assertNull(TaskRepository(database.taskDao()).completeFromWidget(taskId))
    }
}
