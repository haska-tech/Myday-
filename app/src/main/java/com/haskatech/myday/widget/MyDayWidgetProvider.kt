package com.haskatech.myday.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import com.haskatech.myday.MainActivity
import com.haskatech.myday.R
import com.haskatech.myday.data.MyDayDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyDayWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (WidgetRefreshRules.shouldRefresh(intent.action)) refreshAll(context)
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: android.os.Bundle) {
        super.onAppWidgetOptionsChanged(context, manager, id, options)
        requestRefresh(context)
    }

    private fun refreshAll(context: Context) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(ComponentName(context, MyDayWidgetProvider::class.java))
                if (ids.isNotEmpty()) {
                    val tasks = MyDayDatabase.getDatabase(context).taskDao().observeTasks().first()
                    val snapshot = WidgetTaskSnapshot.create(tasks)
                    ids.forEach { render(context, manager, it, snapshot) }
                }
            } finally { pending.finish() }
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.haskatech.myday.widget.REFRESH"

        fun requestRefresh(context: Context) {
            context.sendBroadcast(Intent(context, MyDayWidgetProvider::class.java).setAction(ACTION_REFRESH))
        }

        private fun completionPendingIntent(context: Context, taskId: Long): PendingIntent {
            val intent = Intent(context, WidgetTaskActionReceiver::class.java).apply {
                action = WidgetTaskActionReceiver.ACTION_COMPLETE_TASK
                data = Uri.parse(WidgetTaskCompletionRules.pendingIntentUri(taskId))
                putExtra(WidgetTaskActionReceiver.EXTRA_TASK_ID, taskId)
            }
            return PendingIntent.getBroadcast(context, taskId.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        private fun quickAddPendingIntent(context: Context, widgetId: Int): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                action = MainActivity.ACTION_QUICK_ADD
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            return PendingIntent.getActivity(context, 10_000 + widgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        private fun layoutResource(size: WidgetLayoutSize): Int = when (size) {
            WidgetLayoutSize.SMALL -> R.layout.myday_widget_small
            WidgetLayoutSize.MEDIUM -> R.layout.myday_widget_medium
            WidgetLayoutSize.LARGE -> R.layout.myday_widget_large
        }

        private fun rowLimit(size: WidgetLayoutSize): Int = when (size) {
            WidgetLayoutSize.SMALL -> 3
            WidgetLayoutSize.MEDIUM -> 5
            WidgetLayoutSize.LARGE -> 8
        }

        private fun render(context: Context, manager: AppWidgetManager, widgetId: Int, snapshot: WidgetTaskSnapshot) {
            val options = manager.getAppWidgetOptions(widgetId)
            val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH).takeIf { it > 0 }?.toFloat() ?: 110f
            val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT).takeIf { it > 0 }?.toFloat() ?: 110f
            val maxW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH).takeIf { it >= minW }?.toFloat() ?: maxOf(minW, 300f)
            val maxH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT).takeIf { it >= minH }?.toFloat() ?: maxOf(minH, 180f)
            val specs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                WidgetLayoutRules.responsiveBreakpoints(minW, maxW, minH, maxH)
                    .map { Triple(SizeF(it.widthDp, it.heightDp), it.layout, rowLimit(it.layout)) }
            } else {
                val selected = WidgetLayoutRules.legacyLayout(minW.toInt(), minH.toInt())
                listOf(Triple(SizeF(minW, minH), selected, rowLimit(selected)))
            }

            val dateFormat = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).apply {
                timeZone = java.util.TimeZone.getTimeZone(snapshot.zoneId)
            }
            val timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).apply {
                timeZone = java.util.TimeZone.getTimeZone(snapshot.zoneId)
            }
            val visibleTasks = snapshot.visibleTasks()
            val empty = when (snapshot.emptyState) {
                WidgetEmptyState.NONE_PLANNED -> "Nothing planned yet.\\nTap + Add task to get started."
                WidgetEmptyState.ALL_COMPLETED -> "All caught up!\\nYou've completed your tasks."
                WidgetEmptyState.HAS_TASKS -> ""
            }
            val open = PendingIntent.getActivity(context, widgetId, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val refresh = PendingIntent.getBroadcast(context, widgetId,
                Intent(context, MyDayWidgetProvider::class.java).setAction(ACTION_REFRESH),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val quickAdd = quickAddPendingIntent(context, widgetId)
            val viewsBySize = linkedMapOf<SizeF, RemoteViews>()

            specs.forEach { (size, layout, limit) ->
                val views = RemoteViews(context.packageName, layoutResource(layout))
                views.setTextViewText(R.id.widget_date, dateFormat.format(Date(snapshot.generatedAtMillis)))
                views.setTextViewText(R.id.widget_priorities,
                    if (snapshot.topPriorities.isEmpty()) "TODAY'S TASKS" else "TOP PRIORITIES")
                views.setTextViewText(R.id.widget_progress_label,
                    if (snapshot.todayTotalCount == 0) "TODAY'S PROGRESS · NO SCHEDULED TASKS"
                    else "TODAY'S PROGRESS · ${snapshot.todayCompletedCount}/${snapshot.todayTotalCount} DONE")
                views.setProgressBar(R.id.widget_progress, 100, snapshot.progressPercent, false)
                views.setViewVisibility(R.id.widget_progress, if (snapshot.todayTotalCount == 0) View.GONE else View.VISIBLE)
                views.setOnClickPendingIntent(R.id.widget_title, open)
                views.setOnClickPendingIntent(R.id.widget_footer, open)
                views.setOnClickPendingIntent(R.id.widget_refresh, refresh)
                views.setOnClickPendingIntent(R.id.widget_quick_add, quickAdd)
                views.removeAllViews(R.id.widget_task_list)
                val rows = visibleTasks.take(limit)
                views.setTextViewText(R.id.widget_tasks, empty)
                views.setViewVisibility(R.id.widget_tasks, if (rows.isEmpty()) View.VISIBLE else View.GONE)
                rows.forEach { task ->
                    val row = RemoteViews(context.packageName, R.layout.myday_widget_task_row)
                    val time = task.scheduledAtMillis?.let { timeFormat.format(Date(it)) }
                    val label = buildString {
                        append(if (task.isTopPriority) "★ " else "○ ")
                        if (time != null && !task.isTopPriority) append("$time  ")
                        append(task.title)
                    }
                    row.setTextViewText(R.id.widget_task_row, label)
                    row.setContentDescription(R.id.widget_task_row, "Mark ${task.title} completed")
                    row.setOnClickPendingIntent(R.id.widget_task_row, completionPendingIntent(context, task.id))
                    views.addView(R.id.widget_task_list, row)
                }
                views.setOnClickPendingIntent(R.id.widget_tasks, open)
                viewsBySize[size] = views
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) manager.updateAppWidget(widgetId, RemoteViews(viewsBySize))
            else manager.updateAppWidget(widgetId, viewsBySize.values.first())
        }
    }
}
