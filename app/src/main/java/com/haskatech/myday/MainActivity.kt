package com.haskatech.myday

import android.os.Bundle
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
import com.haskatech.myday.data.TimeTrackingSessionEntity
import com.haskatech.myday.focus.TimeTrackingRules
import kotlinx.coroutines.delay

private val Navy = Color(0xFF14243A)
private val Lime = Color(0xFFB7E445)
private val Paper = Color(0xFFF6F8F4)
private val Muted = Color(0xFF667085)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Navy, secondary = Lime, background = Paper)) {
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
        topBar = { TopAppBar(title = { Column {
            Text("MyDay", color = Navy, fontWeight = FontWeight.Bold)
            Text("Plan with purpose.", color = Muted, fontSize = 12.sp)
        } }) },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                labels.forEachIndexed { index, label ->
                    NavigationBarItem(selected = tab == index, onClick = { tab = index },
                        icon = { Icon(icons[index], contentDescription = label) },
                        label = { Text(label, fontSize = 10.sp) })
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = Lime, contentColor = Navy) {
                Icon(Icons.Default.Add, contentDescription = "Add task")
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
            3 -> InsightsScreen(tasks, activeTracking, nowMillis, Modifier.padding(padding))
            else -> PlaceholderScreen("Settings", "Personalise your planning experience.", Modifier.padding(padding))
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
            Text("YOUR DAY", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Make today count.", color = Navy, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Text("Your plan, at a glance.", color = Muted)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
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
            Card(colors = CardDefaults.cardColors(containerColor = Navy), shape = RoundedCornerShape(20.dp)) {
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
            item { Text("No tasks yet. Tap + to add your first task.", color = Muted) }
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
    Column(modifier.fillMaxSize().padding(18.dp)) {
        Text("Tasks", color = Navy, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("Your task list is saved on this device.", color = Muted)
        Spacer(Modifier.height(16.dp))
        if (tasks.isEmpty()) {
            Text("Nothing here yet. Tap + to create a task.", color = Muted)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tasks, key = { it.id }) { task -> TaskRow(task, onToggle, onTopPriority, onDelete, tracking, nowMillis, onStartTracking, onPauseTracking, onStopTracking) }
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
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = task.isCompleted, onCheckedChange = { onToggle(task, it) },
                colors = CheckboxDefaults.colors(checkedColor = Navy))
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text(task.title, color = Navy, fontWeight = FontWeight.SemiBold,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null)
                Text(task.priority, color = Muted, fontSize = 12.sp)
                Text("Tracked " + TimeTrackingRules.formatDuration(task.timeSpentMillis + if (tracking?.taskId == task.id && tracking.startedAtMillis != null) TimeTrackingRules.elapsedMillis(tracking.startedAtMillis, nowMillis) else 0L), color = Muted, fontSize = 12.sp)
                if (!task.isCompleted) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (tracking?.taskId == task.id) {
                            if (tracking.startedAtMillis == null) {
                                TextButton(onClick = { onStartTracking(task) }) { Text("Resume", fontSize = 11.sp) }
                            } else {
                                TextButton(onClick = { onPauseTracking(task) }) { Text("Pause", fontSize = 11.sp) }
                            }
                            TextButton(onClick = { onStopTracking(task) }) { Text("Stop", fontSize = 11.sp) }
                        } else {
                            TextButton(onClick = { onStartTracking(task) }, enabled = tracking == null) {
                                Text("Start timer", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
            if (!task.isCompleted) {
                TextButton(onClick = { onTopPriority(task, !task.isTopPriority) }) {
                    Text(if (task.isTopPriority) "TOP 3 ✓" else "TOP 3", fontSize = 10.sp)
                }
            }
            if (onDelete != null) {
                IconButton(onClick = { onDelete(task) }) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete task", tint = Muted)
                }
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
        title = { Text("Add a task", color = Navy) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it },
                    label = { Text("What do you need to do?") }, singleLine = true)
                Text("Priority", color = Navy, fontWeight = FontWeight.Medium)
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
                    Text("Repeat", color = Navy, fontWeight = FontWeight.Medium)
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
                        Text("Remind me", color = Navy, fontWeight = FontWeight.Medium)
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
            TextButton(onClick = {
                val reminder = if (reminderEnabled) scheduledAt?.minus(reminderOffsetMinutes * 60_000L) else null
                onAdd(title, priority, scheduledAt, reminder, recurrenceRule)
            }, enabled = canSave) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CalendarScreen(tasks: List<TaskEntity>, modifier: Modifier = Modifier) {
    val zone = remember { java.time.ZoneId.systemDefault() }
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
        java.time.Instant.ofEpochMilli(task.scheduledAtMillis!!).atZone(zone).toLocalDate() == date
    }.sortedBy { it.scheduledAtMillis }

    Column(modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text("Calendar", color = Navy, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("Plan by day, week or month.", color = Muted)
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
                            color = if (date == selectedDate) Lime else Color.White,
                            shape = RoundedCornerShape(9.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center) {
                                Text(date.dayOfMonth.toString(), color = if (inMonth) Navy else Muted,
                                    fontWeight = if (date == selectedDate) FontWeight.Bold else FontWeight.Normal)
                                if (count > 0) Text(count.toString(), color = Navy, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Selected day · ${selectedDate.format(dateFormatter)}", color = Navy, fontWeight = FontWeight.Bold)
        }
        val datesToShow = if (view == "Month") listOf(selectedDate) else visibleDates
        val entries = datesToShow.flatMap { date -> tasksFor(date).map { date to it } }
        if (entries.isEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("No scheduled tasks for this ${view.lowercase()}.", color = Muted)
            Text("Tap + to add a task and give it a date and time.", color = Muted, fontSize = 12.sp)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(entries, key = { "${it.second.id}-${it.first}" }) { (date, task) ->
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp)) {
                        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.width(76.dp)) {
                                Text(date.format(java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM")),
                                    color = Muted, fontSize = 11.sp)
                                Text(DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(task.scheduledAtMillis!!)),
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

@Composable
private fun InsightsScreen(tasks: List<TaskEntity>, tracking: TimeTrackingSessionEntity?, nowMillis: Long, modifier: Modifier = Modifier) {
    val done = tasks.count { it.isCompleted }
    val percent = if (tasks.isEmpty()) 0 else done * 100 / tasks.size
    val trackedTotal = tasks.sumOf { it.timeSpentMillis } + if (tracking?.startedAtMillis != null) TimeTrackingRules.elapsedMillis(tracking.startedAtMillis, nowMillis) else 0L
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Insights", color = Navy, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("A simple view of today's progress.", color = Muted)
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(18.dp)) {
                Text("Time tracked", color = Navy, fontWeight = FontWeight.Bold)
                Text(TimeTrackingRules.formatDuration(trackedTotal), color = Navy, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Across all tasks", color = Muted, fontSize = 12.sp)
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
private fun PlaceholderScreen(title: String, description: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(20.dp)) {
        Text(title, color = Navy, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(description, color = Muted)
    }
}
