package com.haskatech.myday.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.myDaySettingsDataStore by preferencesDataStore(name = "myday_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class WeekStartDay { MONDAY, SUNDAY }

data class MyDaySettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val weekStartDay: WeekStartDay = WeekStartDay.MONDAY,
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val defaultReminderMinutes: Int = 15
)

class SettingsRepository(private val context: Context) {
    val settings: Flow<MyDaySettings> = context.myDaySettingsDataStore.data.map { p ->
        MyDaySettings(
            themeMode = runCatching { ThemeMode.valueOf(p[THEME] ?: ThemeMode.SYSTEM.name) }.getOrDefault(ThemeMode.SYSTEM),
            weekStartDay = runCatching { WeekStartDay.valueOf(p[WEEK_START] ?: WeekStartDay.MONDAY.name) }.getOrDefault(WeekStartDay.MONDAY),
            focusMinutes = (p[FOCUS] ?: 25).coerceIn(1, 120),
            shortBreakMinutes = (p[SHORT_BREAK] ?: 5).coerceIn(1, 120),
            longBreakMinutes = (p[LONG_BREAK] ?: 15).coerceIn(1, 120),
            defaultReminderMinutes = (p[REMINDER] ?: 15).let { if (it in setOf(-1, 0, 5, 10, 15)) it else 15 }
        )
    }

    suspend fun setTheme(value: ThemeMode) = context.myDaySettingsDataStore.edit { it[THEME] = value.name }
    suspend fun setWeekStart(value: WeekStartDay) = context.myDaySettingsDataStore.edit { it[WEEK_START] = value.name }
    suspend fun setFocusMinutes(value: Int) = context.myDaySettingsDataStore.edit { it[FOCUS] = value.coerceIn(1, 120) }
    suspend fun setShortBreakMinutes(value: Int) = context.myDaySettingsDataStore.edit { it[SHORT_BREAK] = value.coerceIn(1, 120) }
    suspend fun setLongBreakMinutes(value: Int) = context.myDaySettingsDataStore.edit { it[LONG_BREAK] = value.coerceIn(1, 120) }
    suspend fun setDefaultReminderMinutes(value: Int) = context.myDaySettingsDataStore.edit { it[REMINDER] = value.takeIf { it in setOf(-1, 0, 5, 10, 15) } ?: 15 }

    private companion object {
        val THEME = stringPreferencesKey("theme_mode")
        val WEEK_START = stringPreferencesKey("week_start_day")
        val FOCUS = intPreferencesKey("focus_duration_minutes")
        val SHORT_BREAK = intPreferencesKey("short_break_duration_minutes")
        val LONG_BREAK = intPreferencesKey("long_break_duration_minutes")
        val REMINDER = intPreferencesKey("default_reminder_minutes")
    }
}
