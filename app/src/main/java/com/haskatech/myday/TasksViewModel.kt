package com.haskatech.myday

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.haskatech.myday.data.MyDayDatabase
import com.haskatech.myday.data.TaskEntity
import com.haskatech.myday.data.TaskRepository
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

    fun addTask(title: String, priority: String = "Normal") {
        viewModelScope.launch { repository.addTask(title, priority) }
    }

    fun setCompleted(task: TaskEntity, completed: Boolean) {
        viewModelScope.launch { repository.setCompleted(task.id, completed) }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch { repository.deleteTask(task.id) }
    }
}
