package com.haskatech.myday

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haskatech.myday.data.SmartPlan
import com.haskatech.myday.data.SmartRecommendation
import com.haskatech.myday.data.SmartScheduleRules
import com.haskatech.myday.data.SmartWarningType
import com.haskatech.myday.data.TaskEntity
import com.haskatech.myday.focus.VoicePlanCommand
import com.haskatech.myday.focus.VoicePlanCommandRules
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

@Composable
fun SmartPlanScreen(
    tasks: List<TaskEntity>,
    nowMillis: Long,
    onApprove: (Long, Long, Int) -> Unit,
    onDurationCorrection: (Long, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var clockNow by remember { mutableLongStateOf(nowMillis) }
    val deferredIds = remember { mutableStateListOf<Long>() }
    LaunchedEffect(Unit) {
        while (true) {
            clockNow = System.currentTimeMillis()
            delay(30_000L)
        }
    }
    val plan = remember(tasks, clockNow) { SmartScheduleRules.analyze(tasks, clockNow) }
    val visible = plan.recommendations.filterNot { it.taskId in deferredIds }
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val spoken = if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        } else null
        val command = spoken?.let { VoicePlanCommandRules.parse(it, plan.recommendations) }
        when (command) {
            is VoicePlanCommand.Approve -> {
                val recommendation = plan.recommendations.firstOrNull { it.taskId == command.taskId }
                if (recommendation?.proposedStartMillis != null) {
                    onApprove(recommendation.taskId, recommendation.proposedStartMillis, recommendation.durationMinutes)
                    Toast.makeText(context, "Approved: ${recommendation.title}", Toast.LENGTH_LONG).show()
                } else Toast.makeText(context, "No safe time was found. Use manual planning.", Toast.LENGTH_LONG).show()
            }
            is VoicePlanCommand.ChangeDuration -> {
                onDurationCorrection(command.taskId, command.minutes)
                Toast.makeText(context, "Duration updated. Review the refreshed plan.", Toast.LENGTH_LONG).show()
            }
            is VoicePlanCommand.Defer -> {
                deferredIds.add(command.taskId)
                Toast.makeText(context, "Recommendation deferred for this session.", Toast.LENGTH_SHORT).show()
            }
            null -> Toast.makeText(context, "Command not recognised. Use the manual controls below.", Toast.LENGTH_LONG).show()
        }
    }

    Column(modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 18.dp)) {
        Text("SMART SCHEDULING", color = Color(0xFF748094), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
        Text("Your plan", color = Color(0xFF14243A), fontSize = 29.sp, fontWeight = FontWeight.ExtraBold)
        Text("Suggestions are calculated on this device. Nothing moves until you approve it.",
            color = Color(0xFF748094), fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF14243A)), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("AVAILABLE TODAY", color = Color(0xFFB7E445), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("${plan.availableMinutesToday / 60}h ${plan.availableMinutesToday % 60}m",
                    color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                Text("Estimated free time between 08:00 and 18:00, after scheduled commitments.",
                    color = Color.LightGray, fontSize = 11.sp)
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("TOP RECOMMENDATIONS", color = Color(0xFF14243A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = {
                val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-UG")
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Say approve task name, change task name to 90 minutes, or skip task name")
                }
                try {
                    voiceLauncher.launch(speechIntent)
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(context, "Voice input is unavailable. Use the manual controls.", Toast.LENGTH_LONG).show()
                } catch (_: SecurityException) {
                    Toast.makeText(context, "Voice input permission was not granted. Use the manual controls.", Toast.LENGTH_LONG).show()
                }
            }) {
                Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(17.dp))
                Text("Voice command", fontSize = 11.sp)
            }
        }
        if (visible.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                Text(if (plan.recommendations.isEmpty()) "No active tasks to prioritise. Add tasks to build a plan."
                    else "All current recommendations are deferred. You can still plan tasks manually.",
                    modifier = Modifier.padding(18.dp), color = Color(0xFF748094), fontSize = 13.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(visible, key = { it.taskId }) { recommendation ->
                    RecommendationCard(recommendation, onApprove)
                }
                if (plan.warnings.isNotEmpty()) {
                    item {
                        Text("SCHEDULE CHECKS", color = Color(0xFF14243A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    items(plan.warnings) { warning ->
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4E5)), shape = RoundedCornerShape(13.dp)) {
                            Row(Modifier.fillMaxWidth().padding(11.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Color(0xFF8A4B08), modifier = Modifier.size(18.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(warning.type.label(), color = Color(0xFF8A4B08), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text(warning.taskTitle + " · " + warning.message, color = Color(0xFF8A4B08), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(18.dp)) }
            }
        }
        if (plan.warnings.isNotEmpty() && visible.isEmpty()) {
            Text("${plan.warnings.size} schedule checks need attention.", color = Color(0xFF748094), fontSize = 11.sp)
        }
        Text("Offline fallback: edit a task or approve a proposed time manually. Voice recognition depends on an installed Android speech service.",
            color = Color(0xFF748094), fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun RecommendationCard(item: SmartRecommendation, onApprove: (Long, Long, Int) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Color(0xFFEAF2D4), shape = RoundedCornerShape(8.dp)) {
                    Text("#${item.score}", color = Color(0xFF14243A), fontWeight = FontWeight.Bold, fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(item.title, color = Color(0xFF14243A), fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
            }
            Text(item.reason, color = Color(0xFF748094), fontSize = 11.sp)
            if (item.proposedStartMillis != null) {
                Text(
                    (if (item.changesSchedule) "Suggested time · " else "Current time · ") +
                        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(item.proposedStartMillis)),
                    color = Color(0xFF14243A), fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                )
                if (item.changesSchedule) {
                    Button(onClick = { onApprove(item.taskId, item.proposedStartMillis, item.durationMinutes) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB7E445), contentColor = Color(0xFF14243A))) {
                        Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Approve suggested time")
                    }
                } else {
                    Text("Your existing time fits the current plan. No change is needed.", color = Color(0xFF748094), fontSize = 10.sp)
                }
            } else {
                Text("No suitable slot found. Adjust the deadline, duration or commitments manually.",
                    color = Color(0xFF8A4B08), fontSize = 11.sp)
            }
        }
    }
}

private fun SmartWarningType.label(): String = when (this) {
    SmartWarningType.OVERLAP -> "OVERLAPPING COMMITMENTS"
    SmartWarningType.OUTSIDE_WORK_HOURS -> "OUTSIDE PLANNING HOURS"
    SmartWarningType.DEADLINE_INSUFFICIENT -> "DEADLINE CAPACITY RISK"
    SmartWarningType.DAILY_OVERLOAD -> "DAILY OVERLOAD"
    SmartWarningType.NO_AVAILABLE_SLOT -> "NO AVAILABLE SLOT"
}
