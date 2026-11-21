package com.haskatech.myday.focus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val FocusNavy = Color(0xFF14243A)
private val FocusLime = Color(0xFFB7E445)
private val FocusPaper = Color(0xFFF6F8F4)
private val FocusMuted = Color(0xFF667085)

class PomodoroActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = androidx.compose.material3.lightColorScheme(
                    primary = FocusNavy, secondary = FocusLime, background = FocusPaper
                )
            ) {
                PomodoroScreen()
            }
        }
    }
}

@Composable
private fun PomodoroScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var timer by remember { mutableStateOf(PomodoroTimerManager.snapshot(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            timer = PomodoroTimerManager.snapshot(context)
        }
    }
    val total = when (timer.phase) {
        "SHORT_BREAK" -> timer.shortBreakMinutes * 60_000L
        "LONG_BREAK" -> timer.longBreakMinutes * 60_000L
        else -> timer.focusMinutes * 60_000L
    }.coerceAtLeast(1L)
    val remaining = if (timer.isRunning)
        (timer.endAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
    else timer.remainingMillis
    val progress = (1f - remaining.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    val minutes = (remaining / 60_000L).toInt()
    val seconds = ((remaining % 60_000L) / 1_000L).toInt()
    val phaseLabel = when (timer.phase) {
        "SHORT_BREAK" -> "SHORT BREAK"
        "LONG_BREAK" -> "LONG BREAK"
        else -> "FOCUS SESSION"
    }
    Surface(Modifier.fillMaxSize(), color = FocusPaper) {
        Column(
            Modifier.fillMaxSize().padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("‹  MyDay", color = FocusNavy, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                    modifier = Modifier.weight(1f))
                Text("POMODORO", color = FocusMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Text("Stay focused.", color = FocusNavy, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(phaseLabel, color = FocusMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(28.dp)) {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 30.dp, horizontal = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BoxTimer(progress = progress, time = "%02d:%02d".format(minutes, seconds))
                    Spacer(Modifier.height(18.dp))
                    Text(timer.completedFocusSessions.toString() + " focus sessions completed",
                        color = FocusMuted, fontSize = 13.sp)
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                if (timer.isRunning) PomodoroTimerManager.pause(context)
                                else PomodoroTimerManager.start(context)
                                timer = PomodoroTimerManager.snapshot(context)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FocusNavy)
                        ) { Text(if (timer.isRunning) "Pause" else "Start") }
                        OutlinedButton(onClick = {
                            PomodoroTimerManager.reset(context)
                            timer = PomodoroTimerManager.snapshot(context)
                        }) { Text("Reset") }
                    }
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Timer lengths", color = FocusNavy, fontWeight = FontWeight.Bold)
                    DurationControl("Focus", timer.focusMinutes, !timer.isRunning) {
                        PomodoroTimerManager.setDuration(context, "FOCUS", it)
                        timer = PomodoroTimerManager.snapshot(context)
                    }
                    DurationControl("Short break", timer.shortBreakMinutes, !timer.isRunning) {
                        PomodoroTimerManager.setDuration(context, "SHORT_BREAK", it)
                        timer = PomodoroTimerManager.snapshot(context)
                    }
                    DurationControl("Long break", timer.longBreakMinutes, !timer.isRunning) {
                        PomodoroTimerManager.setDuration(context, "LONG_BREAK", it)
                        timer = PomodoroTimerManager.snapshot(context)
                    }
                    Text(
                        "After four focus sessions, MyDay schedules a long break. The timer continues when you leave the app.",
                        color = FocusMuted, fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxTimer(progress: Float, time: String) {
    Box(contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(210.dp),
            color = FocusLime,
            trackColor = FocusPaper,
            strokeWidth = 10.dp
        )
        Text(time, color = FocusNavy, fontSize = 42.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DurationControl(label: String, minutes: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = FocusNavy, modifier = Modifier.weight(1f))
        OutlinedButton(enabled = enabled && minutes > 1, onClick = { onChange(minutes - 1) }) {
            Text("−")
        }
        Text("$minutes min", color = FocusNavy, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp))
        OutlinedButton(enabled = enabled && minutes < 120, onClick = { onChange(minutes + 1) }) {
            Text("+")
        }
    }
}
