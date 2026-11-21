package com.haskatech.myday.focus

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlin.math.max

data class PomodoroSnapshot(
    val phase: String,
    val endAtMillis: Long,
    val remainingMillis: Long,
    val isRunning: Boolean,
    val completedFocusSessions: Int,
    val focusMinutes: Int,
    val shortBreakMinutes: Int,
    val longBreakMinutes: Int
)

object PomodoroTimerManager {
    const val ACTION_FINISHED = "com.haskatech.myday.focus.POMODORO_FINISHED"
    private const val PREFS = "pomodoro_timer"
    private const val CHANNEL = "myday_pomodoro"
    private const val NOTIFICATION_ID = 4201
    private const val REQUEST_CODE = 4210

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun duration(snapshot: PomodoroSnapshot, phase: String = snapshot.phase): Long {
        val minutes = when (phase) {
            "SHORT_BREAK" -> snapshot.shortBreakMinutes
            "LONG_BREAK" -> snapshot.longBreakMinutes
            else -> snapshot.focusMinutes
        }
        return minutes.coerceIn(1, 120) * 60_000L
    }

    private fun read(context: Context): PomodoroSnapshot {
        val p = prefs(context)
        val phase = p.getString("phase", "FOCUS") ?: "FOCUS"
        val focus = p.getInt("focus_minutes", 25).coerceIn(1, 120)
        val short = p.getInt("short_break_minutes", 5).coerceIn(1, 120)
        val long = p.getInt("long_break_minutes", 15).coerceIn(1, 120)
        val running = p.getBoolean("running", false)
        val end = p.getLong("end_at", 0L)
        val remaining = p.getLong("remaining", focus * 60_000L)
        return PomodoroSnapshot(phase, end, remaining, running, p.getInt("sessions", 0), focus, short, long)
    }

    fun snapshot(context: Context): PomodoroSnapshot {
        val current = read(context)
        if (current.isRunning && current.endAtMillis <= System.currentTimeMillis()) {
            finish(context)
            return read(context)
        }
        return if (current.isRunning) current.copy(
            remainingMillis = max(0L, current.endAtMillis - System.currentTimeMillis())
        ) else current
    }

    fun start(context: Context) {
        val current = snapshot(context)
        if (current.isRunning) return
        val remaining = max(1_000L, current.remainingMillis.takeIf { it > 0L } ?: duration(current))
        val end = System.currentTimeMillis() + remaining
        prefs(context).edit().putBoolean("running", true).putLong("end_at", end)
            .putLong("remaining", remaining).apply()
        schedule(context, end)
    }

    fun pause(context: Context) {
        val current = read(context)
        if (!current.isRunning) return
        val remaining = max(0L, current.endAtMillis - System.currentTimeMillis())
        prefs(context).edit().putBoolean("running", false).putLong("remaining", remaining).apply()
        cancel(context)
    }

    fun reset(context: Context) {
        val current = read(context)
        cancel(context)
        prefs(context).edit().putBoolean("running", false).putLong("end_at", 0L)
            .putLong("remaining", duration(current)).apply()
    }

    fun setDuration(context: Context, phase: String, minutes: Int) {
        val current = read(context)
        if (current.isRunning) return
        val value = minutes.coerceIn(1, 120)
        val key = when (phase) {
            "SHORT_BREAK" -> "short_break_minutes"
            "LONG_BREAK" -> "long_break_minutes"
            else -> "focus_minutes"
        }
        val editor = prefs(context).edit().putInt(key, value)
        if (current.phase == phase) editor.putLong("remaining", value * 60_000L)
        editor.apply()
    }

    fun restore(context: Context) {
        val current = read(context)
        if (current.isRunning) {
            if (current.endAtMillis <= System.currentTimeMillis()) finish(context)
            else schedule(context, current.endAtMillis)
        }
    }

    private fun schedule(context: Context, atMillis: Long) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, PomodoroFinishedReceiver::class.java).setAction(ACTION_FINISHED)
        val pending = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending)
    }

    private fun cancel(context: Context) {
        val intent = Intent(context, PomodoroFinishedReceiver::class.java).setAction(ACTION_FINISHED)
        val pending = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pending)
    }

    fun finish(context: Context) {
        val current = read(context)
        if (!current.isRunning) return
        cancel(context)
        val sessions = if (current.phase == "FOCUS") current.completedFocusSessions + 1
            else current.completedFocusSessions
        val nextPhase = if (current.phase == "FOCUS") {
            if (sessions % 4 == 0) "LONG_BREAK" else "SHORT_BREAK"
        } else "FOCUS"
        val nextRemaining = duration(current, nextPhase)
        prefs(context).edit().putString("phase", nextPhase).putBoolean("running", false)
            .putLong("end_at", 0L).putLong("remaining", nextRemaining)
            .putInt("sessions", sessions).apply()
        showNotification(context, nextPhase)
    }

    private fun showNotification(context: Context, nextPhase: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, "Pomodoro timer", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val open = Intent(context, PomodoroActivity::class.java)
        val contentIntent = PendingIntent.getActivity(
            context, 4211, open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val message = if (nextPhase == "FOCUS") "Break complete. Ready for another focus session?"
            else "Focus session complete. Time for a break."
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("MyDay · Focus timer")
            .setContentText(message)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }
}
