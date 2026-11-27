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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.haskatech.myday.MyDayColors
import com.haskatech.myday.MyDayTheme
import com.haskatech.myday.settings.SettingsRepository
import com.haskatech.myday.settings.MyDaySettings
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class PomodoroActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settingsRepository = remember { SettingsRepository(applicationContext) }
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = MyDaySettings())
            MyDayTheme(themeMode = settings.themeMode) {
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
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { (context as? android.app.Activity)?.finish() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                }
                Text("MYDAY", color = MyDayColors.Navy, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp,
                    letterSpacing = 1.sp, modifier = Modifier.weight(1f))
                Text("POMODORO", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Text("Stay focused.", color = MyDayColors.Navy, style = MaterialTheme.typography.headlineLarge)
            Text("One thing at a time. Give it your best.", color = MyDayColors.Muted, fontSize = 13.sp)
            Surface(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.24f), shape = RoundedCornerShape(50)) {
                Text(phaseLabel, color = MyDayColors.Navy, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp))
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(26.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 22.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BoxTimer(progress = progress, time = "%02d:%02d".format(minutes, seconds), diameter = 190.dp)
                    Spacer(Modifier.height(18.dp))
                    Text(timer.completedFocusSessions.toString() + " focus sessions completed",
                        color = MyDayColors.Muted, fontSize = 13.sp)
                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                if (timer.isRunning) PomodoroTimerManager.pause(context)
                                else PomodoroTimerManager.start(context)
                                timer = PomodoroTimerManager.snapshot(context)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MyDayColors.Navy)
                        , modifier = Modifier.weight(1f)) { Text(if (timer.isRunning) "Pause session" else "Start focus") }
                        OutlinedButton(onClick = {
                            PomodoroTimerManager.reset(context)
                            timer = PomodoroTimerManager.snapshot(context)
                        }, modifier = Modifier.weight(1f)) { Text("Reset") }
                    }
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = MyDayColors.White), shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("CUSTOMISE YOUR TIMER", color = MyDayColors.Muted, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp)
                    Text("Timer lengths", color = MyDayColors.Navy, fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
                        color = MyDayColors.Muted, fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxTimer(progress: Float, time: String, diameter: androidx.compose.ui.unit.Dp = 190.dp) {
    Box(contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(diameter),
            color = MyDayColors.Lime,
            trackColor = MyDayColors.Paper,
            strokeWidth = 10.dp
        )
        Text(time, color = MyDayColors.Navy, fontSize = 42.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DurationControl(label: String, minutes: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = MyDayColors.Navy, modifier = Modifier.weight(1f))
        OutlinedButton(enabled = enabled && minutes > 1, onClick = { onChange(minutes - 1) }) {
            Text("−")
        }
        Text("$minutes min", color = MyDayColors.Navy, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp))
        OutlinedButton(enabled = enabled && minutes < 120, onClick = { onChange(minutes + 1) }) {
            Text("+")
        }
    }
}
