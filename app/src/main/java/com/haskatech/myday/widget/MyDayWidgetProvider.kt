package com.haskatech.myday.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.SizeF
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
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH || intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            refreshAll(context)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        requestRefresh(context)
    }

    private fun refreshAll(context: Context) {
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
            val options = manager.getAppWidgetOptions(widgetId)
            val viewSpecs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                listOf(
                    SizeF(110f, 110f) to R.layout.myday_widget_small,
                    SizeF(180f, 110f) to R.layout.myday_widget_medium,
                    SizeF(300f, 180f) to R.layout.myday_widget_large
                )
            } else {
                val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
                val layoutId = when {
                    minWidth < 180 -> R.layout.myday_widget_small
                    minWidth < 300 -> R.layout.myday_widget_medium
                    else -> R.layout.myday_widget_large
                }
                listOf(SizeF(minWidth.toFloat(), 110f) to layoutId)
            }

            val zone = ZoneId.of("Africa/Kampala")
            val today = LocalDate.now(zone)
            val widgetDateFormatter = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).apply {
                timeZone = java.util.TimeZone.getTimeZone(zone)
            }
            val widgetTimeFormatter = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).apply {
                timeZone = java.util.TimeZone.getTimeZone(zone)
            }
            val todayTasks = tasks.filter { task ->
                task.scheduledAtMillis?.let {
                    Instant.ofEpochMilli(it).atZone(zone).toLocalDate() == today
                } == true && !task.isCompleted
            }.sortedBy { it.scheduledAtMillis }
            val priorities = tasks.filter { it.isTopPriority && !it.isCompleted }
            val topPriorities = priorities.take(3)
            val remainingSlots = (6 - topPriorities.size).coerceAtLeast(0)
            val lines = buildList {
                topPriorities.forEach { add("★ ${it.title}") }
                todayTasks.filterNot { it.isTopPriority }.take(remainingSlots).forEach { task ->
                    val time = task.scheduledAtMillis?.let {
                        widgetTimeFormatter.format(Date(it))
                    }
                    add("• ${time?.let { "$it  " } ?: ""}${task.title}")
                }
            }

            val openPending = PendingIntent.getActivity(
                context, widgetId, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val refreshPending = PendingIntent.getBroadcast(
                context, widgetId,
                Intent(context, MyDayWidgetProvider::class.java).setAction(ACTION_REFRESH),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val viewsBySize = linkedMapOf<SizeF, RemoteViews>()
            viewSpecs.forEach { (size, layoutId) ->
                val views = RemoteViews(context.packageName, layoutId)
                views.setTextViewText(
                    R.id.widget_date,
                    widgetDateFormatter.format(Date())
                )
                views.setTextViewText(
                    R.id.widget_priorities,
                    if (topPriorities.isEmpty()) "TODAY'S TASKS" else "TOP PRIORITIES"
                )
                views.setTextViewText(
                    R.id.widget_tasks,
                    lines.take(6).joinToString("\n").ifBlank { "Nothing planned yet.\nTap to add a task." }
                )
                views.setOnClickPendingIntent(R.id.widget_tasks, openPending)
                views.setOnClickPendingIntent(R.id.widget_title, openPending)
                views.setOnClickPendingIntent(R.id.widget_footer, openPending)
                views.setOnClickPendingIntent(R.id.widget_refresh, refreshPending)
                viewsBySize[size] = views
            }

            val responsiveViews = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                RemoteViews(viewsBySize)
            } else {
                viewsBySize.values.first()
            }
            manager.updateAppWidget(widgetId, responsiveViews)
        }
    }
}
