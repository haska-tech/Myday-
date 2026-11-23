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
        if (intent.action == ACTION_REFRESH) {
            refreshAll(context)
        } else {
            super.onReceive(context, intent)
        }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        refreshAll(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, manager, appWidgetId, newOptions)
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
            } finally {
                pending.finish()
            }
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
            return PendingIntent.getBroadcast(
                context, taskId.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun quickAddPendingIntent(context: Context, widgetId: Int): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_OPEN_QUICK_ADD, true)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            return PendingIntent.getActivity(
                context, 10_000 + widgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun render(context: Context, manager: AppWidgetManager, widgetId: Int, snapshot: WidgetTaskSnapshot) {
            val options = manager.getAppWidgetOptions(widgetId)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
            val specs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                listOf(
                    Triple(SizeF(110f, 110f), R.layout.myday_widget_small, WidgetSizeClass.SMALL),
                    Triple(SizeF(180f, 110f), R.layout.myday_widget_medium, WidgetSizeClass.MEDIUM),
                    Triple(SizeF(260f, 160f), R.layout.myday_widget_large, WidgetSizeClass.LARGE)
                )
            } else {
                val sizeClass = WidgetSizeRules.choose(minWidth, minHeight)
                val layout = when (sizeClass) {
                    WidgetSizeClass.SMALL -> R.layout.myday_widget_small
                    WidgetSizeClass.MEDIUM -> R.layout.myday_widget_medium
                    WidgetSizeClass.LARGE -> R.layout.myday_widget_large
                }
                listOf(Triple(SizeF(minWidth.toFloat(), minHeight.toFloat()), layout, sizeClass))
            }

            val dateFormat = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).apply {
                timeZone = java.util.TimeZone.getTimeZone(snapshot.zoneId)
            }
            val timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).apply {
                timeZone = java.util.TimeZone.getTimeZone(snapshot.zoneId)
            }
            val visibleTasks = snapshot.visibleTasks()
            val empty = when (snapshot.emptyState) {
                WidgetEmptyState.NONE_PLANNED -> "Nothing planned yet.\nTap + Add to begin."
                WidgetEmptyState.ALL_COMPLETED -> "All caught up!\nYou've completed your tasks."
                WidgetEmptyState.HAS_TASKS -> ""
            }
            val open = PendingIntent.getActivity(
                context, widgetId, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val refresh = PendingIntent.getBroadcast(
                context, widgetId,
                Intent(context, MyDayWidgetProvider::class.java).setAction(ACTION_REFRESH),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val quickAdd = quickAddPendingIntent(context, widgetId)
            val viewsBySize = linkedMapOf<SizeF, RemoteViews>()

            specs.forEach { (size, layout, sizeClass) ->
                val views = RemoteViews(context.packageName, layout)
                views.setTextViewText(R.id.widget_date, dateFormat.format(Date(snapshot.generatedAtMillis)))
                views.setTextViewText(
                    R.id.widget_priorities,
                    if (snapshot.topPriorities.isEmpty()) "TODAY'S TASKS" else "TOP PRIORITIES"
                )
                views.setTextViewText(R.id.widget_progress_label, snapshot.progressLabel)
                views.setProgressBar(R.id.widget_progress_bar, 100, snapshot.progressPercent, false)
                views.setOnClickPendingIntent(R.id.widget_title, open)
                views.setOnClickPendingIntent(R.id.widget_footer, open)
                views.setOnClickPendingIntent(R.id.widget_refresh, refresh)
                views.setOnClickPendingIntent(R.id.widget_add, quickAdd)
                views.removeAllViews(R.id.widget_task_list)
                val rows = visibleTasks.take(WidgetSizeRules.taskLimit(sizeClass))
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

            val responsive = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                RemoteViews(viewsBySize)
            } else {
                viewsBySize.values.first()
            }
            manager.updateAppWidget(widgetId, responsive)
        }
    }
}
