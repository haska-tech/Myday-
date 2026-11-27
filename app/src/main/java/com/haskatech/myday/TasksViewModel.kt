package com.haskatech.myday

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.haskatech.myday.data.MyDayDatabase
import com.haskatech.myday.data.TaskEntity
import com.haskatech.myday.data.CategoryEntity
import com.haskatech.myday.data.CategoryRepository
import com.haskatech.myday.data.TaskRepository
import com.haskatech.myday.data.TaskTimeTrackingRepository
import com.haskatech.myday.reminders.ReminderScheduler
import com.haskatech.myday.reminders.ReminderRecoveryRules
import com.haskatech.myday.widget.MyDayWidgetProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TasksViewModel(application: Application) : AndroidViewModel(application) {
    private val database = MyDayDatabase.getDatabase(application)
    private val repository = TaskRepository(database.taskDao())
    private val categoryRepository = CategoryRepository(database.categoryDao())
    val categories = categoryRepository.categories
    private val timeTrackingRepository = TaskTimeTrackingRepository(database)

    init {
        viewModelScope.launch { categoryRepository.ensureDefaults() }
        viewModelScope.launch {
            repository.refreshRecurringOccurrences()
            // AlarmManager schedules are not durable across force-stop; rebuild them from Room
            // whenever the user next opens the app.
            database.taskDao().observeTasks().first().asSequence()
                .filter { ReminderRecoveryRules.needsRecovery(it, System.currentTimeMillis()) }
                .forEach { task ->
                    ReminderScheduler.schedule(getApplication(), task.id, task.title, task.reminderAtMillis!!)
                }
            MyDayWidgetProvider.requestRefresh(getApplication())
        }
    }

    val activeTracking = timeTrackingRepository.activeTracking.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), null
    )
    val timeTrackingEntries = timeTrackingRepository.entries.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )
    val tasks = repository.tasks.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )

    fun addTask(title: String, priority: String = "Normal", scheduledAtMillis: Long? = null, reminderAtMillis: Long? = null, recurrenceRule: String = "None", plannedDurationMinutes: Int? = null, deadlineAtMillis: Long? = null, urgency: String = "Normal") {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (plannedDurationMinutes != null && plannedDurationMinutes <= 0) return@launch
            if (scheduledAtMillis != null && scheduledAtMillis <= now) return@launch
            if (reminderAtMillis != null && (scheduledAtMillis == null || reminderAtMillis <= now || reminderAtMillis > scheduledAtMillis)) return@launch
            if (deadlineAtMillis != null && (deadlineAtMillis <= now || (scheduledAtMillis != null && deadlineAtMillis < scheduledAtMillis))) return@launch

            val createdTasks = repository.addTask(title, priority, scheduledAtMillis, reminderAtMillis, recurrenceRule, plannedDurationMinutes, deadlineAtMillis, urgency)
            createdTasks.orEmpty().forEach { task ->
                val reminder = task.reminderAtMillis
                if (reminder != null && reminder > System.currentTimeMillis()) {
                    ReminderScheduler.schedule(getApplication(), task.id, task.title, reminder)
                }
            }
            MyDayWidgetProvider.requestRefresh(getApplication())
        }
    }

    fun updateTask(task: TaskEntity, title: String, priority: String, scheduledAtMillis: Long?, reminderAtMillis: Long?, plannedDurationMinutes: Int?, deadlineAtMillis: Long?, applyToSeries: Boolean = false, urgency: String = task.urgency) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (!com.haskatech.myday.data.TaskEditRules.isValid(
                    title, scheduledAtMillis, reminderAtMillis, now,
                    task.scheduledAtMillis, task.reminderAtMillis, deadlineAtMillis, task.deadlineAtMillis
                ) || (deadlineAtMillis != null && scheduledAtMillis != null && deadlineAtMillis < scheduledAtMillis)) return@launch
            val updatedTasks = repository.updateTask(task, title, priority, scheduledAtMillis, reminderAtMillis, plannedDurationMinutes, deadlineAtMillis, applyToSeries, urgency)
            updatedTasks.forEach { updated ->
                ReminderScheduler.cancel(getApplication(), updated.id)
                updated.reminderAtMillis?.takeIf { it > System.currentTimeMillis() }?.let {
                    ReminderScheduler.schedule(getApplication(), updated.id, updated.title, it)
                }
            }
            if (updatedTasks.isNotEmpty()) MyDayWidgetProvider.requestRefresh(getApplication())
        }
    }

    fun approveSuggestedTime(taskId: Long, startMillis: Long, durationMinutes: Int) {
        viewModelScope.launch {
            val updated = repository.approveSuggestedTime(taskId, startMillis, durationMinutes) ?: return@launch
            ReminderScheduler.cancel(getApplication(), taskId)
            updated.reminderAtMillis?.takeIf { it > System.currentTimeMillis() }?.let {
                ReminderScheduler.schedule(getApplication(), taskId, updated.title, it)
            }
            MyDayWidgetProvider.requestRefresh(getApplication())
        }
    }

    fun correctPlannedDuration(taskId: Long, minutes: Int) {
        viewModelScope.launch {
            if (repository.updatePlannedDuration(taskId, minutes)) MyDayWidgetProvider.requestRefresh(getApplication())
        }
    }

    fun setTaskCategory(taskId: Long, categoryId: Long?) {
        viewModelScope.launch {
            database.taskDao().setCategory(taskId, categoryId)
            MyDayWidgetProvider.requestRefresh(getApplication())
        }
    }

    fun createCategory(name: String, colorHex: String, iconKey: String = "folder") {
        viewModelScope.launch { categoryRepository.create(name, colorHex, iconKey) }
    }

    fun renameCategory(id: Long, name: String, colorHex: String, iconKey: String = "folder") {
        viewModelScope.launch { categoryRepository.rename(id, name, colorHex, iconKey) }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch { categoryRepository.delete(id) }
    }

    fun startTracking(task: TaskEntity) { viewModelScope.launch { timeTrackingRepository.start(task.id) } }
    fun pauseTracking(task: TaskEntity) { viewModelScope.launch { timeTrackingRepository.pause(task.id) } }
    fun stopTracking(task: TaskEntity) { viewModelScope.launch { timeTrackingRepository.stop(task.id) } }

    fun setCompleted(task: TaskEntity, completed: Boolean) {
        viewModelScope.launch {
            if (completed) timeTrackingRepository.stop(task.id)
            val generatedTasks = repository.setCompleted(task, completed)
            if (completed) ReminderScheduler.cancel(getApplication(), task.id)
            generatedTasks.forEach { generated ->
                val reminder = generated.reminderAtMillis
                if (reminder != null && reminder > System.currentTimeMillis()) {
                    ReminderScheduler.schedule(getApplication(), generated.id, generated.title, reminder)
                }
            }
            MyDayWidgetProvider.requestRefresh(getApplication())
        }
    }

    fun setTopPriority(task: TaskEntity, isTop: Boolean, onLimitReached: () -> Unit) {
        viewModelScope.launch {
            if (!repository.setTopPriority(task.id, isTop)) onLimitReached()
            MyDayWidgetProvider.requestRefresh(getApplication())
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            timeTrackingRepository.stop(task.id)
            ReminderScheduler.cancel(getApplication(), task.id)
            repository.deleteTask(task.id)
            MyDayWidgetProvider.requestRefresh(getApplication())
        }
    }
}
