package com.haskatech.myday.focus

object TimeTrackingRules {
    fun elapsedMillis(startedAtMillis: Long, nowMillis: Long): Long =
        (nowMillis - startedAtMillis).coerceAtLeast(0L)

    fun formatDuration(durationMillis: Long): String {
        val totalMinutes = durationMillis.coerceAtLeast(0L) / 60_000L
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        return if (hours > 0L) hours.toString() + "h " + minutes.toString().padStart(2, '0') + "m"
        else minutes.toString() + "m"
    }
}
