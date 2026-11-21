package com.haskatech.myday

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.haskatech.myday.data.MyDayDatabase
import com.haskatech.myday.data.TaskEntity
import com.haskatech.myday.data.TaskRepository
import com.haskatech.myday.reminders.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TasksViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TaskRepository(MyDayDatabase.getDatabase(application).taskDao())

    val tasks = repository.tasks.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    fun addTask(title: String, priority: String = "Normal", scheduledAtMillis: Long? = null, reminderAtMillis: Long? = null) {
        viewModelScope.launch {
            val taskId = repository.addTask(title, priority, scheduledAtMillis, reminderAtMillis)
            if (taskId != null && reminderAtMillis != null && reminderAtMillis > System.currentTimeMillis()) {
                ReminderScheduler.schedule(getApplication(), taskId, title.trim(), reminderAtMillis)
            }
        }
    }

    fun setCompleted(task: TaskEntity, completed: Boolean) {
        viewModelScope.launch { repository.setCompleted(task.id, completed) }
    }

    fun setTopPriority(task: TaskEntity, isTop: Boolean, onLimitReached: () -> Unit) {
        viewModelScope.launch {
            if (!repository.setTopPriority(task.id, isTop)) onLimitReached()
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            ReminderScheduler.cancel(getApplication(), task.id)
            repository.deleteTask(task.id)
        }
    }
}
