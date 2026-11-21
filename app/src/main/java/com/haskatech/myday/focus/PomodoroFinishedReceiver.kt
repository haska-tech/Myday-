package com.haskatech.myday.focus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PomodoroFinishedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == PomodoroTimerManager.ACTION_FINISHED) {
            PomodoroTimerManager.finish(context)
        }
    }
}
