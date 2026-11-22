package com.haskatech.myday.data

import java.util.Calendar
import java.util.TimeZone

/**
 * Calendar-based recurrence arithmetic in the device's current timezone.
 * Returns dates strictly after the supplied date and no later than the horizon.
 */
object RecurrenceSchedule {
    fun nextDate(afterMillis: Long, rule: String): Long? {
        val field = when (rule) {
            "Daily" -> Calendar.DAY_OF_YEAR
            "Weekly" -> Calendar.WEEK_OF_YEAR
            "Monthly" -> Calendar.MONTH
            else -> return null
        }
        return Calendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = afterMillis
            add(field, 1)
        }.timeInMillis
    }

    fun datesThrough(startMillis: Long, rule: String, throughMillis: Long): List<Long> {
        if (rule == "None" || throughMillis <= startMillis) return emptyList()
        val result = mutableListOf<Long>()
        var next = nextDate(startMillis, rule) ?: return emptyList()
        while (next <= throughMillis && result.size < 400) {
            result += next
            next = nextDate(next, rule) ?: break
        }
        return result
    }
}
