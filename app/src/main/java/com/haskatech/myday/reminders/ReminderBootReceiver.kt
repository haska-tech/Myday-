package com.haskatech.myday.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.haskatech.myday.data.MyDayDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tasks = MyDayDatabase.getDatabase(context).taskDao().observeTasks().first()
                val now = System.currentTimeMillis()
                tasks.asSequence()
                    .filter { !it.isCompleted && it.reminderAtMillis != null && it.reminderAtMillis > now }
                    .forEach { task ->
                        ReminderScheduler.schedule(
                            context,
                            task.id,
                            task.title,
                            task.reminderAtMillis!!
                        )
                    }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
