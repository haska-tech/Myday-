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
import com.haskatech.myday.data.TaskRepository
import com.haskatech.myday.data.TaskTimeTrackingRepository
import com.haskatech.myday.reminders.ReminderScheduler
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
  if (intent.action == ACTION_COMPLETE_TASK) { completeTask(context, intent); return }
  super.onReceive(context, intent)
  if (intent.action == ACTION_REFRESH || intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) refreshAll(context)
 }
 override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: android.os.Bundle) {
  super.onAppWidgetOptionsChanged(context, manager, id, options); requestRefresh(context)
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
 private fun completeTask(context: Context, intent: Intent) {
  val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
  val pending = goAsync()
  CoroutineScope(Dispatchers.IO).launch {
   try {
    if (taskId <= 0L) return@launch
    val database = MyDayDatabase.getDatabase(context)
    val generated = TaskRepository(database.taskDao()).completeFromWidget(taskId) ?: return@launch
    TaskTimeTrackingRepository(database).stop(taskId)
    ReminderScheduler.cancel(context, taskId)
    generated.forEach { task ->
     task.reminderAtMillis?.takeIf { it > System.currentTimeMillis() }?.let {
      ReminderScheduler.schedule(context, task.id, task.title, it)
     }
    }
    requestRefresh(context)
   } finally { pending.finish() }
  }
 }
 companion object {
  const val ACTION_REFRESH = "com.haskatech.myday.widget.REFRESH"
  const val ACTION_COMPLETE_TASK = "com.haskatech.myday.widget.COMPLETE_TASK"
  const val EXTRA_TASK_ID = "com.haskatech.myday.widget.EXTRA_TASK_ID"
  fun requestRefresh(context: Context) {
   context.sendBroadcast(Intent(context, MyDayWidgetProvider::class.java).setAction(ACTION_REFRESH))
  }
  private fun completionPendingIntent(context: Context, taskId: Long): PendingIntent {
   val intent = Intent(context, MyDayWidgetProvider::class.java).apply {
    action = ACTION_COMPLETE_TASK
    data = Uri.parse(WidgetTaskCompletionRules.pendingIntentUri(taskId))
    putExtra(EXTRA_TASK_ID, taskId)
   }
   return PendingIntent.getBroadcast(context, taskId.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  }
  private fun render(context: Context, manager: AppWidgetManager, widgetId: Int, snapshot: WidgetTaskSnapshot) {
   val options = manager.getAppWidgetOptions(widgetId)
   val specs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) listOf(
    Triple(SizeF(110f,110f), R.layout.myday_widget_small, 3),
    Triple(SizeF(180f,110f), R.layout.myday_widget_medium, 5),
    Triple(SizeF(300f,180f), R.layout.myday_widget_large, 8)
   ) else {
    val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
    val (layout, limit) = when { width < 180 -> R.layout.myday_widget_small to 3; width < 300 -> R.layout.myday_widget_medium to 5; else -> R.layout.myday_widget_large to 8 }
    listOf(Triple(SizeF(width.toFloat(),110f), layout, limit))
   }
   val dateFormat = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).apply { timeZone = java.util.TimeZone.getTimeZone(snapshot.zoneId) }
   val timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).apply { timeZone = java.util.TimeZone.getTimeZone(snapshot.zoneId) }
   val visibleTasks = snapshot.visibleTasks()
   val empty = when(snapshot.emptyState) {
    WidgetEmptyState.NONE_PLANNED -> "Nothing planned yet.\nTap to add a task."
    WidgetEmptyState.ALL_COMPLETED -> "All caught up!\nYou've completed your tasks."
    WidgetEmptyState.HAS_TASKS -> ""
   }
   val open = PendingIntent.getActivity(context, widgetId, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
   val refresh = PendingIntent.getBroadcast(context, widgetId, Intent(context, MyDayWidgetProvider::class.java).setAction(ACTION_REFRESH), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
   val viewsBySize = linkedMapOf<SizeF, RemoteViews>()
   specs.forEach { (size, layout, limit) ->
    val views = RemoteViews(context.packageName, layout)
    views.setTextViewText(R.id.widget_date, dateFormat.format(Date(snapshot.generatedAtMillis)))
    views.setTextViewText(R.id.widget_priorities, if(snapshot.topPriorities.isEmpty()) "TODAY'S TASKS" else "TOP PRIORITIES")
    views.setOnClickPendingIntent(R.id.widget_title, open)
    views.setOnClickPendingIntent(R.id.widget_footer, open)
    views.setOnClickPendingIntent(R.id.widget_refresh, refresh)
    views.removeAllViews(R.id.widget_task_list)
    val rows = visibleTasks.take(limit)
    views.setTextViewText(R.id.widget_tasks, empty)
    views.setViewVisibility(R.id.widget_tasks, if(rows.isEmpty()) View.VISIBLE else View.GONE)
    rows.forEach { task ->
     val row = RemoteViews(context.packageName, R.layout.myday_widget_task_row)
     val time = task.scheduledAtMillis?.let { timeFormat.format(Date(it)) }
     val label = buildString {
      append(if(task.isTopPriority) "★ " else "○ ")
      if(time != null && !task.isTopPriority) append("$time  ")
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
   val responsive = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) RemoteViews(viewsBySize) else viewsBySize.values.first()
   manager.updateAppWidget(widgetId, responsive)
  }
 }
}
