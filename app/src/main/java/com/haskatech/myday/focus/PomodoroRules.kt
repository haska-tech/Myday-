package com.haskatech.myday.focus

data class PomodoroTransition(
    val nextPhase: String,
    val completedFocusSessions: Int
)

object PomodoroRules {
    fun durationMillis(
        phase: String,
        focusMinutes: Int,
        shortBreakMinutes: Int,
        longBreakMinutes: Int
    ): Long {
        val minutes = when (phase) {
            "SHORT_BREAK" -> shortBreakMinutes
            "LONG_BREAK" -> longBreakMinutes
            else -> focusMinutes
        }
        return minutes.coerceIn(1, 120) * 60_000L
    }

    fun remainingMillis(endAtMillis: Long, nowMillis: Long): Long =
        (endAtMillis - nowMillis).coerceAtLeast(0L)

    fun nextPhase(currentPhase: String, completedFocusSessions: Int): PomodoroTransition {
        val sessions = if (currentPhase == "FOCUS") completedFocusSessions + 1
            else completedFocusSessions
        val next = when (currentPhase) {
            "FOCUS" -> if (sessions % 4 == 0) "LONG_BREAK" else "SHORT_BREAK"
            else -> "FOCUS"
        }
        return PomodoroTransition(next, sessions)
    }
}
