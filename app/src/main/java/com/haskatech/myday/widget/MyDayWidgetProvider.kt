package com.haskatech.myday.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.haskatech.myday.MainActivity
import com.haskatech.myday.R
import com.haskatech.myday.data.MyDayDatabase
import com.haskatech.myday.data.TaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

class MyDayWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(ComponentName(context, MyDayWidgetProvider::class.java))
                if (ids.isNotEmpty()) {
                    val tasks = MyDayDatabase.getDatabase(context).taskDao().observeTasks().first()
                    ids.forEach { render(context, manager, it, tasks) }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.haskatech.myday.widget.REFRESH"

        fun requestRefresh(context: Context) {
            context.sendBroadcast(Intent(context, MyDayWidgetProvider::class.java).setAction(ACTION_REFRESH))
        }

        private fun render(context: Context, manager: AppWidgetManager, widgetId: Int, tasks: List<TaskEntity>) {
            val views = RemoteViews(context.packageName, R.layout.myday_widget)
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val todayTasks = tasks.filter { task ->
                task.scheduledAtMillis?.let {
                    Instant.ofEpochMilli(it).atZone(zone).toLocalDate() == today
                } == true && !task.isCompleted
            }.sortedBy { it.scheduledAtMillis }
            val priorities = tasks.filter { it.isTopPriority && !it.isCompleted }
            val lines = buildList {
                priorities.take(3).forEach { add("★ ${it.title}") }
                todayTasks.filterNot { it.isTopPriority }.take(6 - priorities.take(3).size).forEach { task ->
                    val time = task.scheduledAtMillis?.let {
                        DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(it))
                    }
                    add("• ${time?.let { "$it  " } ?: ""}${task.title}")
                }
            }
            views.setTextViewText(R.id.widget_date, SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()))
            views.setTextViewText(R.id.widget_priorities, if (priorities.isEmpty()) "TODAY'S TASKS" else "TOP PRIORITIES")
            views.setTextViewText(
                R.id.widget_tasks,
                lines.take(6).joinToString("\n").ifBlank { "Nothing planned yet.\nTap to add a task." }
            )

            val openPending = PendingIntent.getActivity(
                context, widgetId, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_tasks, openPending)
            views.setOnClickPendingIntent(R.id.widget_title, openPending)
            views.setOnClickPendingIntent(R.id.widget_footer, openPending)
            val refreshPending = PendingIntent.getBroadcast(
                context, widgetId,
                Intent(context, MyDayWidgetProvider::class.java).setAction(ACTION_REFRESH),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_refresh, refreshPending)
            manager.updateAppWidget(widgetId, views)
        }
    }
}
