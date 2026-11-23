package com.haskatech.myday.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.haskatech.myday.data.MyDayDatabase
import com.haskatech.myday.data.TaskRepository
import com.haskatech.myday.data.TaskTimeTrackingRepository
import com.haskatech.myday.reminders.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Private explicit receiver for immutable task-completion PendingIntents. */
class WidgetTaskActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (intent.action != ACTION_COMPLETE_TASK || taskId <= 0L) return@launch
                val database = MyDayDatabase.getDatabase(context)
                val generated = TaskRepository(database.taskDao()).completeFromWidget(taskId) ?: return@launch
                TaskTimeTrackingRepository(database).stop(taskId)
                ReminderScheduler.cancel(context, taskId)
                generated.forEach { task ->
                    task.reminderAtMillis?.takeIf { it > System.currentTimeMillis() }?.let {
                        ReminderScheduler.schedule(context, task.id, task.title, it)
                    }
                }
                MyDayWidgetProvider.requestRefresh(context)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_COMPLETE_TASK = "com.haskatech.myday.widget.COMPLETE_TASK"
        const val EXTRA_TASK_ID = "com.haskatech.myday.widget.EXTRA_TASK_ID"
    }
}
