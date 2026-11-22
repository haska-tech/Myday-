package com.haskatech.myday

import android.os.Bundle
import android.app.Activity
import android.content.ActivityNotFoundException
import android.speech.RecognizerIntent
import android.widget.Toast
import android.content.Intent
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.haskatech.myday.data.TaskEntity
import com.haskatech.myday.data.PlannerTime
import com.haskatech.myday.data.TimeTrackingSessionEntity
import com.haskatech.myday.data.TimeTrackingEntryEntity
import com.haskatech.myday.focus.TimeTrackingRules
import com.haskatech.myday.focus.VoiceCaptureRules
import kotlinx.coroutines.delay

private val Navy = Color(0xFF14243A)
private val Lime = Color(0xFFB7E445)
private val Paper = Color(0xFFF4F6F9)
private val Muted = Color(0xFF748094)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Navy,
                    onPrimary = Color.White,
                    secondary = Lime,
                    onSecondary = Navy,
                    background = Paper,
                    onBackground = Navy,
                    surface = Color.White,
                    onSurface = Navy,
                    surfaceVariant = Color(0xFFEDF1F5),
                    onSurfaceVariant = Muted,
                    outline = Color(0xFFE3E8EF),
                    primaryContainer = Color(0xFFEAF2D4)
                )
            ) {
                MyDayApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyDayApp(tasksViewModel: TasksViewModel = viewModel()) {
    var tab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showTopThreeLimit by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val tasks by tasksViewModel.tasks.collectAsStateWithLifecycle()
    val activeTracking by tasksViewModel.activeTracking.collectAsStateWithLifecycle()
    val timeTrackingEntries by tasksViewModel.timeTrackingEntries.collectAsStateWithLifecycle()
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(activeTracking?.startedAtMillis) {
        if (activeTracking?.startedAtMillis != null) {
            while (true) { nowMillis = System.currentTimeMillis(); delay(1_000) }
        }
    }
    val labels = listOf("My Day", "Tasks", "Calendar", "Insights", "Settings")
    val icons = listOf(Icons.Default.Today, Icons.Default.CheckCircle, Icons.Default.CalendarMonth, Icons.Default.Insights, Icons.Default.Settings)
    val toggleTop: (TaskEntity, Boolean) -> Unit = { task, selected ->
        tasksViewModel.setTopPriority(task, selected) { showTopThreeLimit = true }
    }

    Scaffold(
        containerColor = Paper,
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text("MyDay", color = Navy, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp, letterSpacing = (-0.4).sp)
                        Text("PLAN WITH PURPOSE", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    }
                },
                navigationIcon = {
                    Surface(
                        modifier = Modifier.padding(start = 16.dp, end = 10.dp).size(36.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Lime
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "MyDay", tint = Navy, modifier = Modifier.size(19.dp))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Paper)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 6.dp) {
                labels.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Icon(icons[index], contentDescription = label, modifier = Modifier.size(21.dp)) },
                        label = { Text(label, fontSize = 10.sp, fontWeight = if (tab == index) FontWeight.Bold else FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Navy,
                            selectedTextColor = Navy,
                            indicatorColor = Lime.copy(alpha = 0.38f),
                            unselectedIconColor = Muted,
                            unselectedTextColor = Muted
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Lime,
                contentColor = Navy,
                shape = RoundedCornerShape(18.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 5.dp, pressedElevation = 2.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add task", modifier = Modifier.size(25.dp))
            }
        }
    ) { padding ->
        when (tab) {
            0 -> TodayScreen(tasks, activeTracking, nowMillis,
                { task, done -> tasksViewModel.setCompleted(task, done) }, toggleTop,
                { tasksViewModel.startTracking(it) }, { tasksViewModel.pauseTracking(it) }, { tasksViewModel.stopTracking(it) }, Modifier.padding(padding))
            1 -> TasksScreen(tasks, activeTracking, nowMillis,
                { task, done -> tasksViewModel.setCompleted(task, done) }, toggleTop,
                { tasksViewModel.deleteTask(it) }, { tasksViewModel.startTracking(it) },
                { tasksViewModel.pauseTracking(it) }, { tasksViewModel.stopTracking(it) }, Modifier.padding(padding))
            2 -> CalendarScreen(tasks, Modifier.padding(padding))
            3 -> InsightsScreen(tasks, activeTracking, timeTrackingEntries, nowMillis, Modifier.padding(padding))
            else -> SettingsScreen(Modifier.padding(padding))
        }
    }

    if (showAddDialog) {
        AddTaskDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { title, priority, scheduledAt, reminderAt, recurrenceRule ->
                tasksViewModel.addTask(title, priority, scheduledAt, reminderAt, recurrenceRule)
                showAddDialog = false
            }
        )
    }
    if (showTopThreeLimit) {
        AlertDialog(
            onDismissRequest = { showTopThreeLimit = false },
            title = { Text("Daily top three is full") },
            text = { Text("You can select up to three active top-priority tasks. Remove one before adding another.") },
            confirmButton = { TextButton(onClick = { showTopThreeLimit = false }) { Text("Got it") } }
        )
    }
}

@Composable
private fun TodayScreen(
    tasks: List<TaskEntity>,
    tracking: TimeTrackingSessionEntity?,
    nowMillis: Long,
    onToggle: (TaskEntity, Boolean) -> Unit,
    onTopPriority: (TaskEntity, Boolean) -> Unit,
    onStartTracking: (TaskEntity) -> Unit,
    onPauseTracking: (TaskEntity) -> Unit,
    onStopTracking: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val completed = tasks.count { it.isCompleted }
    val topTasks = tasks.filter { it.isTopPriority && !it.isCompleted }
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Spacer(Modifier.height(8.dp))
            Text(
                java.time.LocalDate.now(PlannerTime.zoneId())
                    .format(java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))
                    .uppercase(Locale.getDefault()),
                color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp
            )
            Text("Make today count.", color = Navy, fontSize = 29.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.7).sp)
            Text("A little progress, every day.", color = Muted, fontSize = 14.sp)

        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("FOCUS TIMER", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Make time for what matters.", color = Navy, fontWeight = FontWeight.SemiBold)
                        Text("Start a focused work session.", color = Muted, fontSize = 12.sp)
                    }
                    Button(onClick = { context.startActivity(Intent(context, com.haskatech.myday.focus.PomodoroActivity::class.java)) },
                        colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Navy)) {
                        Text("Focus")
                    }
                }
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Navy),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("TODAY'S TOP 3 · ${topTasks.size}/3", color = Lime, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    if (topTasks.isEmpty()) {
                        Text("Choose up to three tasks that matter most.", color = Color.White, fontSize = 16.sp)
                    } else {
                        topTasks.forEach { Text("• ${it.title}", color = Color.White, fontSize = 15.sp) }
                    }
                    Text("Tap TOP 3 beside a task to add it here.", color = Color.LightGray, fontSize = 12.sp)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Your tasks", color = Navy, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("$completed / ${tasks.size} done", color = Muted, fontSize = 12.sp)
            }
        }
        if (tasks.isEmpty()) {
            item { EmptyState(Icons.Default.EventNote, "A fresh start", "Your day is clear. Add a task to give it direction.") }
        } else {
            items(tasks, key = { it.id }) { task -> TaskRow(task, onToggle, onTopPriority, tracking = tracking, nowMillis = nowMillis, onStartTracking = onStartTracking, onPauseTracking = onPauseTracking, onStopTracking = onStopTracking) }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun TasksScreen(
    tasks: List<TaskEntity>,
    tracking: TimeTrackingSessionEntity?,
    nowMillis: Long,
    onToggle: (TaskEntity, Boolean) -> Unit,
    onTopPriority: (TaskEntity, Boolean) -> Unit,
    onDelete: (TaskEntity) -> Unit,
    onStartTracking: (TaskEntity) -> Unit,
    onPauseTracking: (TaskEntity) -> Unit,
    onStopTracking: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val (unscheduledTasks, scheduledTasks) = com.haskatech.myday.data.TaskGrouping.partition(tasks)
    Column(modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 20.dp)) {
        Text("YOUR WORKSPACE", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
        Text("Tasks", color = Navy, fontSize = 29.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.6).sp)
        Text("Everything you need to move forward.", color = Muted, fontSize = 14.sp)
        Spacer(Modifier.height(16.dp))
        if (tasks.isEmpty()) {
            EmptyState(Icons.Default.CheckCircleOutline, "Your task list is clear", "Capture your next action and keep moving forward.")
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (unscheduledTasks.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("UNSCHEDULED · ${unscheduledTasks.size}", color = Navy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Tasks you have captured but not assigned a date or time.", color = Muted, fontSize = 12.sp)
                        }
                    }
                    items(unscheduledTasks, key = { it.id }) { task ->
                        TaskRow(task, onToggle, onTopPriority, onDelete, tracking, nowMillis, onStartTracking, onPauseTracking, onStopTracking)
                    }
                }
                if (scheduledTasks.isNotEmpty()) {
                    item {
                        Text("SCHEDULED · ${scheduledTasks.size}", color = Navy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    items(scheduledTasks, key = { it.id }) { task ->
                        TaskRow(task, onToggle, onTopPriority, onDelete, tracking, nowMillis, onStartTracking, onPauseTracking, onStopTracking)
                    }
                }
            }
        }
    }
}
@Composable
private fun TaskRow(
    task: TaskEntity,
    onToggle: (TaskEntity, Boolean) -> Unit,
    onTopPriority: (TaskEntity, Boolean) -> Unit,
    onDelete: ((TaskEntity) -> Unit)? = null,
    tracking: TimeTrackingSessionEntity? = null,
    nowMillis: Long = System.currentTimeMillis(),
    onStartTracking: (TaskEntity) -> Unit = {},
    onPauseTracking: (TaskEntity) -> Unit = {},
    onStopTracking: (TaskEntity) -> Unit = {}
) {
    val isTrackingThisTask = tracking?.taskId == task.id
    val elapsed = if (isTrackingThisTask && tracking?.startedAtMillis != null) {
        TimeTrackingRules.elapsedMillis(tracking.startedAtMillis, nowMillis)
    } else 0L
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = task.isCompleted, onCheckedChange = { onToggle(task, it) },
                    colors = CheckboxDefaults.colors(checkedColor = Navy))
                Column(Modifier.weight(1f).padding(start = 4.dp)) {
                    Text(task.title, color = Navy, fontWeight = FontWeight.SemiBold,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null)
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Surface(color = Color(0xFFEDF1F5), shape = RoundedCornerShape(6.dp)) {
                            Text(task.priority, color = Muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                        }
                        Text("Tracked " + TimeTrackingRules.formatDuration(task.timeSpentMillis + elapsed),
                            color = Muted, fontSize = 11.sp)
                    }
                }
                if (onDelete != null) {
                    IconButton(onClick = { onDelete(task) }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete task", tint = Muted)
                    }
                }
            }
            if (!task.isCompleted) {
                HorizontalDivider(color = Color(0xFFF0F2F5))
                Row(Modifier.fillMaxWidth().padding(start = 44.dp, top = 3.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isTrackingThisTask) {
                            if (tracking?.startedAtMillis == null) {
                                TextButton(onClick = { onStartTracking(task) }, contentPadding = PaddingValues(horizontal = 6.dp)) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(17.dp))
                                    Text("Resume", fontSize = 11.sp)
                                }
                            } else {
                                TextButton(onClick = { onPauseTracking(task) }, contentPadding = PaddingValues(horizontal = 6.dp)) {
                                    Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(17.dp))
                                    Text("Pause", fontSize = 11.sp)
                                }
                            }
                            TextButton(onClick = { onStopTracking(task) }, contentPadding = PaddingValues(horizontal = 6.dp)) {
                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(17.dp))
                                Text("Stop", fontSize = 11.sp)
                            }
                        } else {
                            TextButton(onClick = { onStartTracking(task) }, enabled = tracking == null,
                                contentPadding = PaddingValues(horizontal = 6.dp)) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(17.dp))
                                Text("Start timer", fontSize = 11.sp)
                            }
                        }
                    }
                    TextButton(onClick = { onTopPriority(task, !task.isTopPriority) },
                        contentPadding = PaddingValues(horizontal = 6.dp)) {
                        Icon(if (task.isTopPriority) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(if (task.isTopPriority) "TOP 3" else "Top 3", fontSize = 10.sp)
                    }
                }
            } else {
                Text("Completed", color = Muted, fontSize = 11.sp,
                    modifier = Modifier.padding(start = 52.dp, top = 2.dp))
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTaskDialog(onDismiss: () -> Unit, onAdd: (String, String, Long?, Long?, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Normal") }
    var recurrenceRule by remember { mutableStateOf("None") }
    var scheduledAt by remember { mutableStateOf<Long?>(null) }
    var reminderEnabled by remember { mutableStateOf(true) }
    var reminderOffsetMinutes by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val nowMillis = System.currentTimeMillis()
    val scheduledInFuture = scheduledAt == null || scheduledAt!! > nowMillis
    val reminderInFuture = !reminderEnabled || scheduledAt == null ||
        scheduledAt!! - reminderOffsetMinutes * 60_000L > nowMillis
    val canSave = title.isNotBlank() && scheduledInFuture && reminderInFuture
    val dateLabel = scheduledAt?.let { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it)) }
        ?: "Not scheduled"

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        containerColor = Color.White,
        tonalElevation = 4.dp,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("New task", color = Navy, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text("Capture it now. Organise it when you're ready.", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Normal)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("What do you need to do?") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Navy, focusedLabelColor = Navy, cursorColor = Navy)
                    )
                    Surface(shape = RoundedCornerShape(13.dp), color = Color(0xFFF0F3F6)) {
                    val voiceCaptureLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        if (result.resultCode == Activity.RESULT_OK) {
                            val spokenText = result.data
                                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                                ?.firstOrNull()
                            if (!spokenText.isNullOrBlank()) {
                                title = VoiceCaptureRules.appendRecognizedText(title, spokenText)
                            }
                        }
                    }
                    IconButton(
                        onClick = {
                            val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-UG")
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Say the task you want to add")
                            }
                            try {
                                voiceCaptureLauncher.launch(speechIntent)
                            } catch (_: ActivityNotFoundException) {
                                Toast.makeText(context, "Speech recognition is not available on this device.", Toast.LENGTH_LONG).show()
                            } catch (_: SecurityException) {
                                Toast.makeText(context, "Speech recognition permission was not granted.", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Add task by voice", tint = Navy)
                    }
                    }
                }
                Text("PRIORITY", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("High", "Normal", "Low").forEach { option ->
                        FilterChip(selected = priority == option, onClick = { priority = option }, label = { Text(option) })
                    }
                }
                OutlinedButton(onClick = {
                    val now = Calendar.getInstance()
                    DatePickerDialog(context, { _, year, month, day ->
                        val chosen = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year); set(Calendar.MONTH, month); set(Calendar.DAY_OF_MONTH, day)
                        }
                        TimePickerDialog(context, { _, hour, minute ->
                            chosen.set(Calendar.HOUR_OF_DAY, hour)
                            chosen.set(Calendar.MINUTE, minute)
                            chosen.set(Calendar.SECOND, 0)
                            chosen.set(Calendar.MILLISECOND, 0)
                            scheduledAt = chosen.timeInMillis
                        }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), false).show()
                    }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).apply {
                        datePicker.minDate = System.currentTimeMillis()
                    }.show()
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Schedule, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Schedule: $dateLabel")
                }
                if (!scheduledInFuture) {
                    Text("Choose a future date and time.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
                if (scheduledAt != null) {
                    Text("REPEAT", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("None", "Daily", "Weekly", "Monthly").forEach { option ->
                            FilterChip(selected = recurrenceRule == option, onClick = { recurrenceRule = option }, label = { Text(option, fontSize = 11.sp) })
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
                        Text("Enable reminder", color = Navy, fontSize = 13.sp)
                    }
                    if (reminderEnabled) {
                        Text("REMIND ME", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(0, 5, 10, 15).forEach { minutes ->
                                FilterChip(
                                    selected = reminderOffsetMinutes == minutes,
                                    enabled = minutes == 0 || (scheduledAt?.minus(minutes * 60_000L) ?: 0L) > nowMillis,
                                    onClick = { reminderOffsetMinutes = minutes },
                                    label = { Text(if (minutes == 0) "At time" else "$minutes min before", fontSize = 10.sp) }
                                )
                            }
                        }
                        if (!reminderInFuture) {
                            Text("This reminder would be in the past. Choose a shorter lead time.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }
                    }
                    TextButton(onClick = { scheduledAt = null; recurrenceRule = "None"; reminderOffsetMinutes = 0 }) { Text("Clear schedule") }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val reminder = if (reminderEnabled) scheduledAt?.minus(reminderOffsetMinutes * 60_000L) else null
                onAdd(title, priority, scheduledAt, reminder, recurrenceRule)
            }, enabled = canSave, shape = RoundedCornerShape(13.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Navy)) {
                Text("Save task", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(13.dp)) { Text("Cancel", color = Muted) } }
    )
}

@Composable
private fun CalendarScreen(tasks: List<TaskEntity>, modifier: Modifier = Modifier) {
    val zone = PlannerTime.zoneId()
    var selectedDate by remember { mutableStateOf(java.time.LocalDate.now(zone)) }
    var view by remember { mutableStateOf("Week") }
    val scheduled = tasks.filter { it.scheduledAtMillis != null }
    val dateFormatter = remember { java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault()) }
    val monthFormatter = remember { java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()) }
    val weekStart = selectedDate.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
    val monthStart = selectedDate.withDayOfMonth(1)
    val gridStart = monthStart.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
    val visibleDates = when (view) {
        "Day" -> listOf(selectedDate)
        "Week" -> (0L..6L).map { weekStart.plusDays(it) }
        else -> (0L..41L).map { gridStart.plusDays(it) }
    }
    fun tasksFor(date: java.time.LocalDate): List<TaskEntity> = scheduled.filter { task ->
        PlannerTime.localDateAt(task.scheduledAtMillis!!) == date
    }.sortedBy { it.scheduledAtMillis }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 18.dp)) {
        Text("YOUR SCHEDULE", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
        Text("Calendar", color = Navy, fontSize = 29.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.6).sp)
        Text("Make space for what matters.", color = Muted, fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Day", "Week", "Month").forEach { option ->
                FilterChip(selected = view == option, onClick = { view = option }, label = { Text(option) })
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { selectedDate = java.time.LocalDate.now(zone) }) { Text("Today") }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = {
                selectedDate = when (view) {
                    "Day" -> selectedDate.minusDays(1)
                    "Week" -> selectedDate.minusWeeks(1)
                    else -> selectedDate.minusMonths(1)
                }
            }) { Icon(Icons.Default.ChevronLeft, contentDescription = "Previous") }
            Text(
                when (view) {
                    "Day" -> selectedDate.format(dateFormatter)
                    "Week" -> "${weekStart.format(java.time.format.DateTimeFormatter.ofPattern("d MMM"))} – ${weekStart.plusDays(6).format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy"))}"
                    else -> selectedDate.format(monthFormatter)
                },
                color = Navy, fontWeight = FontWeight.Bold
            )
            IconButton(onClick = {
                selectedDate = when (view) {
                    "Day" -> selectedDate.plusDays(1)
                    "Week" -> selectedDate.plusWeeks(1)
                    else -> selectedDate.plusMonths(1)
                }
            }) { Icon(Icons.Default.ChevronRight, contentDescription = "Next") }
        }
        if (view == "Month") {
            Row(Modifier.fillMaxWidth()) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(day, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            visibleDates.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        val count = tasksFor(date).size
                        val inMonth = date.month == selectedDate.month
                        Surface(
                            onClick = { selectedDate = date },
                            modifier = Modifier.weight(1f).padding(2.dp).height(54.dp),
                            color = when {
                                date == selectedDate -> Lime
                                date == java.time.LocalDate.now(zone) -> Color(0xFFEAF2D4)
                                else -> Color.White
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = if (date == selectedDate) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE8ECF1))
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center) {
                                Text(date.dayOfMonth.toString(), color = if (inMonth) Navy else Muted,
                                    fontWeight = if (date == selectedDate) FontWeight.Bold else FontWeight.Normal)
                                if (count > 0) Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    repeat(count.coerceAtMost(3)) {
                                        Box(Modifier.size(3.dp).background(if (date == selectedDate) Navy else Color(0xFF82A92D), RoundedCornerShape(50)))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Selected day · ${selectedDate.format(dateFormatter)}", color = Navy, fontWeight = FontWeight.Bold)
        }
        if (view == "Week") {
            val tasksByDate = visibleDates.associateWith { tasksFor(it) }
            WeeklyTimetable(
                dates = visibleDates,
                tasksByDate = tasksByDate,
                selectedDate = selectedDate,
                onSelectDate = { selectedDate = it },
                zone = zone
            )
        } else {
            val datesToShow = if (view == "Month") listOf(selectedDate) else visibleDates
            val entries = datesToShow.flatMap { date -> tasksFor(date).map { date to it } }
            if (entries.isEmpty()) {
                EmptyState(Icons.Default.CalendarToday, "Nothing scheduled", "Your calendar is open. Add a task with a date and time to see it here.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    entries.forEach { (date, task) ->
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(17.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                            Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.width(76.dp)) {
                                    Text(date.format(java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM")),
                                        color = Muted, fontSize = 11.sp)
                                    Text(DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).apply {
                                        timeZone = java.util.TimeZone.getTimeZone(zone)
                                    }.format(Date(task.scheduledAtMillis!!)),
                                        color = Navy, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(task.title, color = Navy, fontWeight = FontWeight.SemiBold,
                                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null)
                                    val details = listOfNotNull(
                                        if (task.reminderAtMillis != null) "Reminder" else null,
                                        if (task.recurrenceRule != "None") "Repeats ${task.recurrenceRule.lowercase()}" else null,
                                        if (task.isCompleted) "Completed" else null
                                    )
                                    if (details.isNotEmpty()) Text(details.joinToString(" · "), color = Muted, fontSize = 11.sp)
                                }
                                if (task.priority == "High") Text("HIGH", color = Navy, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun WeeklyTimetable(
    dates: List<java.time.LocalDate>,
    tasksByDate: Map<java.time.LocalDate, List<TaskEntity>>,
    selectedDate: java.time.LocalDate,
    onSelectDate: (java.time.LocalDate) -> Unit,
    zone: java.time.ZoneId
) {
    val horizontalState = rememberScrollState()
    val verticalState = rememberScrollState()
    val today = java.time.LocalDate.now(zone)
    val dayFormatter = java.time.format.DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
    val timeFormatter = java.time.format.DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

    Text("WEEK AT A GLANCE", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
    Text("Swipe sideways to see all 7 days", color = Muted, fontSize = 11.sp)
    Spacer(Modifier.height(8.dp))
    Surface(
        modifier = Modifier.fillMaxWidth().height(560.dp),
        color = Color.White,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE7EBF0))
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(verticalState)) {
            Row(Modifier.horizontalScroll(horizontalState)) {
                Column {
                    Box(Modifier.width(54.dp).height(58.dp), contentAlignment = Alignment.Center) {
                        Text(PlannerTime.utcOffsetLabel(), color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    PlannerTime.dayHours.forEach { hour ->
                        Box(
                            Modifier.width(54.dp).height(72.dp).padding(end = 6.dp),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            Text(
                                java.time.LocalTime.of(hour, 0).format(timeFormatter),
                                color = Muted,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(top = 3.dp)
                            )
                        }
                    }
                }
                dates.forEach { date ->
                    val isToday = date == today
                    val isSelected = date == selectedDate
                    Column(Modifier.width(108.dp)) {
                        Surface(
                            onClick = { onSelectDate(date) },
                            modifier = Modifier.fillMaxWidth().height(58.dp).padding(end = 1.dp, bottom = 4.dp),
                            color = when {
                                isSelected -> Lime
                                isToday -> Color(0xFFEAF2D4)
                                else -> Color(0xFFF4F6F9)
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                Modifier.fillMaxSize().padding(vertical = 5.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(date.format(dayFormatter).uppercase(), color = Navy, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text(date.dayOfMonth.toString(), color = Navy, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        PlannerTime.dayHours.forEach { hour ->
                            val hourTasks = tasksByDate[date].orEmpty().filter { task ->
                                java.time.Instant.ofEpochMilli(task.scheduledAtMillis!!).atZone(zone).hour == hour
                            }
                            Box(
                                Modifier.fillMaxWidth().height(72.dp)
                                    .padding(end = 1.dp)
                                    .background(if (isToday) Color(0xFFFBFDF7) else Color.White)
                                    .then(Modifier)
                            ) {
                                Column(
                                    Modifier.fillMaxSize().padding(horizontal = 3.dp, vertical = 3.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    hourTasks.take(2).forEach { task ->
                                        Surface(
                                            modifier = Modifier.fillMaxWidth().height(30.dp),
                                            color = if (task.isCompleted) Color(0xFFF0F2F4) else Color(0xFFEAF2D4),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Column(Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
                                                val taskTime = java.time.Instant.ofEpochMilli(task.scheduledAtMillis!!).atZone(zone).toLocalTime()
                                                Text(taskTime.format(timeFormatter), color = Muted, fontSize = 7.sp, maxLines = 1)
                                                Text(
                                                    task.title,
                                                    color = Navy,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                                                )
                                            }
                                        }
                                    }
                                    if (hourTasks.size > 2) {
                                        Text("+${hourTasks.size - 2} more", color = Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            androidx.compose.foundation.Canvas(
                                Modifier.fillMaxWidth().height(1.dp)
                            ) {
                                drawLine(
                                    color = Color(0xFFE9EDF2),
                                    start = androidx.compose.ui.geometry.Offset.Zero,
                                    end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    if (dates.all { tasksByDate[it].orEmpty().isEmpty() }) {
        Text("No tasks scheduled this week yet.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
    } else {
        Text("Tasks appear in their scheduled hour. Times use your device timezone.", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun InsightsScreen(tasks: List<TaskEntity>, tracking: TimeTrackingSessionEntity?, entries: List<TimeTrackingEntryEntity>, nowMillis: Long, modifier: Modifier = Modifier) {
    val done = tasks.count { it.isCompleted }
    val percent = if (tasks.isEmpty()) 0 else done * 100 / tasks.size
    val trackedTotal = tasks.sumOf { it.timeSpentMillis } + if (tracking?.startedAtMillis != null) TimeTrackingRules.elapsedMillis(tracking.startedAtMillis, nowMillis) else 0L
    val reportZone = PlannerTime.zoneId()
    val today = java.time.LocalDate.now(reportZone)
    val weekStart = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
    val intervals = entries.map { com.haskatech.myday.focus.TimeInterval(it.startedAtMillis, it.endedAtMillis) } +
        listOfNotNull(tracking?.startedAtMillis?.let { com.haskatech.myday.focus.TimeInterval(it, nowMillis) })
    val dailyTotals = TimeTrackingRules.durationByDate(intervals, reportZone)
    val todayTracked = dailyTotals[today] ?: 0L
    val weekTracked = (0L..6L).sumOf { dailyTotals[weekStart.plusDays(it)] ?: 0L }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("YOUR PROGRESS", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
        Text("Insights", color = Navy, fontSize = 29.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.6).sp)
        Text("See where your time and effort go.", color = Muted, fontSize = 14.sp)
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text("Time tracked", color = Navy, fontWeight = FontWeight.Bold)
                Text(TimeTrackingRules.formatDuration(trackedTotal), color = Navy, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Lifetime total across tasks", color = Muted, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("Today", color = Muted, fontSize = 12.sp)
                        Text(TimeTrackingRules.formatDuration(todayTracked), color = Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("This week · Mon–Sun", color = Muted, fontSize = 12.sp)
                        Text(TimeTrackingRules.formatDuration(weekTracked), color = Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(18.dp)) {
                Text("Task completion", color = Navy, fontWeight = FontWeight.Bold)
                Text("$done of ${tasks.size} tasks completed", color = Muted)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxWidth(), color = Lime, trackColor = Paper)
                Text("$percent%", color = Navy, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, message: String) {
    Card(Modifier.fillMaxWidth().padding(top = 8.dp), colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(22.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFFEAF2D4), modifier = Modifier.size(58.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = Navy, modifier = Modifier.size(28.dp)) }
            }
            Text(title, color = Navy, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(message, color = Muted, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun SettingsScreen(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("MAKE IT YOURS", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
            Text("Settings", color = Navy, fontSize = 29.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.6).sp)
            Text("A calmer way to plan, built around you.", color = Muted, fontSize = 14.sp)
        }
        SettingsGroup("YOUR EXPERIENCE") {
            SettingsRow(Icons.Default.Palette, "Appearance", "Light theme · Navy, white and lime")
            SettingsRow(Icons.Default.NotificationsActive, "Reminders", "Choose a reminder when creating each task")
            SettingsRow(Icons.Default.CalendarMonth, "Calendar", "Week begins on Monday")
        }
        SettingsGroup("YOUR PRODUCTIVITY") {
            SettingsRow(Icons.Default.Timer, "Focus sessions", "Pomodoro timer · 25 / 5 / 15 minutes")
            SettingsRow(Icons.Default.Insights, "Time tracking", "Track time spent on individual tasks")
            SettingsRow(Icons.Default.OfflineBolt, "Offline-first", "Your tasks are stored on this device")
        }
        SettingsGroup("ABOUT") {
            SettingsRow(Icons.Default.AutoAwesome, "MyDay", "Plan with purpose")
            SettingsRow(Icons.Default.Info, "Version", "0.2.0 · Early access")
        }
        Text("MYDAY  ·  PLAN WITH PURPOSE", modifier = Modifier.fillMaxWidth(), color = Muted, fontSize = 9.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
            Column(content = content, modifier = Modifier.padding(horizontal = 13.dp, vertical = 5.dp))
        }
    }
}

@Composable
private fun SettingsRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(11.dp), color = Color(0xFFF0F3F6), modifier = Modifier.size(37.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = Navy, modifier = Modifier.size(19.dp)) }
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(title, color = Navy, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Muted, fontSize = 10.sp)
        }
    }
}
