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
 companion object {
  const val ACTION_REFRESH = "com.haskatech.myday.widget.REFRESH"
  fun requestRefresh(context: Context) {
   context.sendBroadcast(Intent(context, MyDayWidgetProvider::class.java).setAction(ACTION_REFRESH))
  }
  private fun render(context: Context, manager: AppWidgetManager, widgetId: Int, snapshot: WidgetTaskSnapshot) {
   val options = manager.getAppWidgetOptions(widgetId)
   val specs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) listOf(
    SizeF(110f,110f) to R.layout.myday_widget_small,
    SizeF(180f,110f) to R.layout.myday_widget_medium,
    SizeF(300f,180f) to R.layout.myday_widget_large
   ) else {
    val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
    val layout = when { width < 180 -> R.layout.myday_widget_small; width < 300 -> R.layout.myday_widget_medium; else -> R.layout.myday_widget_large }
    listOf(SizeF(width.toFloat(),110f) to layout)
   }
   val zone = snapshot.zoneId
   val dateFormat = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).apply { timeZone = java.util.TimeZone.getTimeZone(zone) }
   val timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).apply { timeZone = java.util.TimeZone.getTimeZone(zone) }
   val lines = buildList {
    snapshot.topPriorities.forEach { add("★ ${it.title}") }
    snapshot.todayTasks.filterNot { it.isTopPriority }.take((6-snapshot.topPriorities.size).coerceAtLeast(0)).forEach { task ->
     val time = task.scheduledAtMillis?.let { timeFormat.format(Date(it)) }
     add("• ${time?.let { "$it  " } ?: ""}${task.title}")
    }
   }
   val empty = when(snapshot.emptyState) {
    WidgetEmptyState.NONE_PLANNED -> "Nothing planned yet.\nTap to add a task."
    WidgetEmptyState.ALL_COMPLETED -> "All caught up!\nYou've completed your tasks."
    WidgetEmptyState.HAS_TASKS -> ""
   }
   val open = PendingIntent.getActivity(context,widgetId,Intent(context,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
   val refresh = PendingIntent.getBroadcast(context,widgetId,Intent(context,MyDayWidgetProvider::class.java).setAction(ACTION_REFRESH),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
   val viewsBySize = linkedMapOf<SizeF,RemoteViews>()
   specs.forEach { (size,layout) ->
    val views = RemoteViews(context.packageName,layout)
    views.setTextViewText(R.id.widget_date,dateFormat.format(Date(snapshot.generatedAtMillis)))
    views.setTextViewText(R.id.widget_priorities,if(snapshot.topPriorities.isEmpty()) "TODAY'S TASKS" else "TOP PRIORITIES")
    views.setTextViewText(R.id.widget_tasks,lines.joinToString("\n").ifBlank { empty })
    views.setOnClickPendingIntent(R.id.widget_tasks,open)
    views.setOnClickPendingIntent(R.id.widget_title,open)
    views.setOnClickPendingIntent(R.id.widget_footer,open)
    views.setOnClickPendingIntent(R.id.widget_refresh,refresh)
    viewsBySize[size]=views
   }
   val responsive = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) RemoteViews(viewsBySize) else viewsBySize.values.first()
   manager.updateAppWidget(widgetId,responsive)
  }
 }
}
