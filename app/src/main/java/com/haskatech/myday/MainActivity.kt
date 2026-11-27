package com.haskatech.myday

import android.os.Bundle
import android.app.Activity
import android.content.ActivityNotFoundException
import android.speech.RecognizerIntent
import android.widget.Toast
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.haskatech.myday.data.TaskEntity
import com.haskatech.myday.data.CategoryEntity
import com.haskatech.myday.data.MyDayBackupManager
import com.haskatech.myday.data.TaskConflictRules
import com.haskatech.myday.data.SmartScheduleRules
import com.haskatech.myday.data.PlannerTime
import com.haskatech.myday.data.TimeTrackingSessionEntity
import com.haskatech.myday.data.TimeTrackingEntryEntity
import com.haskatech.myday.focus.TimeTrackingRules
import com.haskatech.myday.focus.VoiceCaptureRules
import com.haskatech.myday.settings.SettingsRepository
import com.haskatech.myday.settings.MyDaySettings
import com.haskatech.myday.settings.ThemeMode
import com.haskatech.myday.settings.WeekStartDay
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Lime = MyDayColors.Lime

class MainActivity : ComponentActivity() {
    private val quickAddRequest = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra(EXTRA_OPEN_QUICK_ADD, false)) quickAddRequest.intValue++
        setContent {
            val settingsRepository = remember { SettingsRepository(applicationContext) }
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = MyDaySettings())
            MyDayTheme(themeMode = settings.themeMode) {
                MyDayApp(
                    openQuickAddRequest = quickAddRequest.intValue,
                    settings = settings,
                    settingsRepository = settingsRepository
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_QUICK_ADD, false)) quickAddRequest.intValue++
    }

    companion object {
        const val EXTRA_OPEN_QUICK_ADD = "com.haskatech.myday.widget.EXTRA_OPEN_QUICK_ADD"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyDayApp(
    openQuickAddRequest: Int,
    settings: MyDaySettings,
    settingsRepository: SettingsRepository,
    tasksViewModel: TasksViewModel = viewModel()
) {
    var tab by remember { mutableIntStateOf(0) }
    var tasksInitialFilter by remember { mutableStateOf("All") }
    var showAddDialog by remember { mutableStateOf(false) }
    var handledQuickAddRequest by remember { mutableIntStateOf(0) }
    LaunchedEffect(openQuickAddRequest) {
        if (openQuickAddRequest > handledQuickAddRequest) {
            showAddDialog = true
            handledQuickAddRequest = openQuickAddRequest
        }
    }
    var taskBeingEdited by remember { mutableStateOf<TaskEntity?>(null) }
    var showTopThreeLimit by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val tasks by tasksViewModel.tasks.collectAsStateWithLifecycle()
    val categories by tasksViewModel.categories.collectAsStateWithLifecycle()
    val activeTracking by tasksViewModel.activeTracking.collectAsStateWithLifecycle()
    val timeTrackingEntries by tasksViewModel.timeTrackingEntries.collectAsStateWithLifecycle()
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(activeTracking?.startedAtMillis) {
        if (activeTracking?.startedAtMillis != null) {
            while (true) { nowMillis = System.currentTimeMillis(); delay(1_000) }
        }
    }
    val labels = listOf("My Day", "Tasks", "Calendar", "Plan", "Insights", "Settings")
    val icons = listOf(Icons.Default.Today, Icons.Default.CheckCircle, Icons.Default.CalendarMonth, Icons.Default.AutoAwesome, Icons.Default.Insights, Icons.Default.Settings)
    val toggleTop: (TaskEntity, Boolean) -> Unit = { task, selected ->
        tasksViewModel.setTopPriority(task, selected) { showTopThreeLimit = true }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text("MyDay", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                        Text("PLAN WITH PURPOSE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    }
                },
                navigationIcon = {
                    Surface(
                        modifier = Modifier.padding(start = 16.dp, end = 10.dp).size(36.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Lime
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "MyDay", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(19.dp))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
                labels.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Icon(icons[index], contentDescription = label, modifier = Modifier.size(21.dp)) },
                        label = { Text(label, fontSize = 10.sp, fontWeight = if (tab == index) FontWeight.Bold else FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onSurface,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = Lime.copy(alpha = 0.38f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Lime,
                contentColor = MaterialTheme.colorScheme.onSecondary,
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
                { tasksViewModel.startTracking(it) }, { tasksViewModel.pauseTracking(it) }, { tasksViewModel.stopTracking(it) },
                onOpenTasks = { filter -> tasksInitialFilter = filter; tab = 1 }, modifier = Modifier.padding(padding))
            1 -> TasksScreen(tasks, activeTracking, nowMillis,
                { task, done -> tasksViewModel.setCompleted(task, done) }, toggleTop,
                { tasksViewModel.deleteTask(it) }, { taskBeingEdited = it }, { tasksViewModel.startTracking(it) },
                { tasksViewModel.pauseTracking(it) }, { tasksViewModel.stopTracking(it) }, Modifier.padding(padding), initialFilter = tasksInitialFilter,
                categories = categories, onSetCategory = { id, categoryId -> tasksViewModel.setTaskCategory(id, categoryId) },
                onCreateCategory = { name, color, icon -> tasksViewModel.createCategory(name, color, icon) },
                onRenameCategory = { id, name, color, icon -> tasksViewModel.renameCategory(id, name, color, icon) },
                onDeleteCategory = { id -> tasksViewModel.deleteCategory(id) })
            2 -> CalendarScreen(tasks, settings.weekStartDay, Modifier.padding(padding))
            3 -> SmartPlanScreen(
                tasks = tasks,
                nowMillis = nowMillis,
                onApprove = { taskId, start, duration -> tasksViewModel.approveSuggestedTime(taskId, start, duration) },
                onDurationCorrection = { taskId, minutes -> tasksViewModel.correctPlannedDuration(taskId, minutes) },
                onEditTask = { taskId ->
                    val selectedTask = tasks.firstOrNull { it.id == taskId }
                    if (selectedTask != null) taskBeingEdited = selectedTask
                },
                modifier = Modifier.padding(padding)
            )
            4 -> InsightsScreen(tasks, activeTracking, timeTrackingEntries, nowMillis, Modifier.padding(padding))
            else -> SettingsScreen(
                modifier = Modifier.padding(padding),
                settings = settings,
                onThemeChange = { value -> scope.launch { settingsRepository.setTheme(value) } },
                onWeekStartChange = { value -> scope.launch { settingsRepository.setWeekStart(value) } },
                onReminderChange = { value -> scope.launch { settingsRepository.setDefaultReminderMinutes(value) } },
                onOpenInsights = { tab = 4 }
            )
        }
    }

    if (taskBeingEdited != null) {
        AddTaskDialog(
            initialTask = taskBeingEdited,
            onDismiss = { taskBeingEdited = null },
            allTasks = tasks,
            defaultReminderMinutes = settings.defaultReminderMinutes,
            categories = categories,
            onAdd = { title, priority, scheduledAt, reminderAt, deadlineAt, _, durationMinutes, applyToSeries, urgency, categoryId ->
                taskBeingEdited?.let { tasksViewModel.updateTask(it, title, priority, scheduledAt, reminderAt, durationMinutes, deadlineAt, applyToSeries, urgency, categoryId) }
                taskBeingEdited = null
            }
        )
    }
    if (showAddDialog) {
        AddTaskDialog(
            onDismiss = { showAddDialog = false },
            allTasks = tasks,
            defaultReminderMinutes = settings.defaultReminderMinutes,
            categories = categories,
            onAdd = { title, priority, scheduledAt, reminderAt, deadlineAt, recurrenceRule, durationMinutes, _, urgency, categoryId ->
                tasksViewModel.addTask(title, priority, scheduledAt, reminderAt, recurrenceRule, durationMinutes, deadlineAt, urgency, categoryId)
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
    tasks: List<TaskEntity>, tracking: TimeTrackingSessionEntity?, nowMillis: Long,
    onToggle: (TaskEntity, Boolean) -> Unit, onTopPriority: (TaskEntity, Boolean) -> Unit,
    onStartTracking: (TaskEntity) -> Unit, onPauseTracking: (TaskEntity) -> Unit, onStopTracking: (TaskEntity) -> Unit,
    onOpenTasks: (String) -> Unit, modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val today = java.time.LocalDate.now(PlannerTime.zoneId())
    val todayTasks = tasks.filter { !it.isCompleted && (it.scheduledAtMillis?.let { t -> PlannerTime.localDateAt(t) == today } == true || it.deadlineAtMillis?.let { t -> PlannerTime.localDateAt(t) == today } == true) }
    val completedToday = tasks.count { it.isCompleted && (it.scheduledAtMillis?.let { t -> PlannerTime.localDateAt(t) == today } == true || it.deadlineAtMillis?.let { t -> PlannerTime.localDateAt(t) == today } == true) }
    val committed = todayTasks.size + completedToday
    val percent = if (committed == 0) 0 else completedToday * 100 / committed
    val overdue = tasks.filter { !it.isCompleted && it.deadlineAtMillis != null && it.deadlineAtMillis < nowMillis }
    val dueSoon = tasks.filter { !it.isCompleted && it.deadlineAtMillis?.let { t -> val d = PlannerTime.localDateAt(t); d >= today && d <= today.plusDays(3) } == true }
    val topTasks = tasks.filter { it.isTopPriority && !it.isCompleted }
    val nextTask = tasks.filter { !it.isCompleted && it.scheduledAtMillis != null && it.scheduledAtMillis!! >= nowMillis }.minByOrNull { it.scheduledAtMillis!! }
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        item {
            Spacer(Modifier.height(8.dp))
            Text(today.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())).uppercase(Locale.getDefault()), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
            Text("Your day, in focus.", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
            Text("A clear view of what matters next.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("DAILY PROGRESS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold); Text("$percent%", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) }
                    LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp), color = Lime, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                    Text("$completedToday of $committed committed tasks completed today", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard("Completed", completedToday, Icons.Default.CheckCircle, Modifier.weight(1f)) { onOpenTasks("Completed") }
            SummaryCard("Pending", todayTasks.size, Icons.Default.PendingActions, Modifier.weight(1f)) { onOpenTasks("Active") }
        } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard("Overdue", overdue.size, Icons.Default.Warning, Modifier.weight(1f)) { onOpenTasks("Overdue") }
            SummaryCard("Due soon", dueSoon.size, Icons.Default.Schedule, Modifier.weight(1f)) { onOpenTasks("Due soon") }
        } }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("TODAY'S TOP 3 · ${topTasks.size}/3", color = Lime, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    if (topTasks.isEmpty()) Text("Choose up to three tasks that matter most.", color = MyDayColors.White, fontSize = 14.sp) else topTasks.forEach { Text("• ${it.title}", color = MyDayColors.White, fontSize = 14.sp) }
                    Text("Manage priorities in Tasks.", color = MyDayColors.OnNavyMuted, fontSize = 11.sp)
                }
            }
        }
        if (overdue.isNotEmpty() || dueSoon.isNotEmpty()) item {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("DEADLINE ATTENTION", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                (overdue.take(3) + dueSoon.take(3)).distinctBy { it.id }.forEach { task ->
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp)) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(task.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold); Text(if (task in overdue) "OVERDUE" else "DUE SOON", color = MaterialTheme.colorScheme.error, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                            TextButton(onClick = { onToggle(task, true) }) { Text("Complete") }
                        }
                    }
                }
                Text("Deadlines are never moved automatically.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Text("NEXT ON YOUR SCHEDULE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(nextTask?.title ?: "No upcoming scheduled task", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { onOpenTasks("All") }) { Text("View tasks") }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("FOCUS & TIME", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold); Text(if (tracking != null) "A tracking session is active" else "Make time for what matters", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold) }
                    Button(onClick = { context.startActivity(Intent(context, com.haskatech.myday.focus.PomodoroActivity::class.java)) }, colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = MaterialTheme.colorScheme.onSecondary)) { Text("Focus") }
                }
            }
        }
        item { Spacer(Modifier.height(75.dp)) }
    }
}

@Composable
private fun SummaryCard(title: String, count: Int, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(modifier.clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Text(count.toString(), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
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
    onEdit: (TaskEntity) -> Unit,
    onStartTracking: (TaskEntity) -> Unit,
    onPauseTracking: (TaskEntity) -> Unit,
    onStopTracking: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier,
    initialFilter: String = "All",
    categories: List<CategoryEntity> = emptyList(),
    onSetCategory: (Long, Long?) -> Unit = { _, _ -> },
    onCreateCategory: (String, String, String) -> Unit = { _, _, _ -> },
    onRenameCategory: (Long, String, String, String) -> Unit = { _, _, _, _ -> },
    onDeleteCategory: (Long) -> Unit = {}
) {
    var taskFilter by remember(initialFilter) { mutableStateOf(initialFilter) }
    val filteredTasks = when (taskFilter) {
        "Overdue" -> tasks.filter { !it.isCompleted && it.deadlineAtMillis != null && it.deadlineAtMillis < nowMillis }
        "Due soon" -> {
            val today = java.time.LocalDate.now(PlannerTime.zoneId())
            tasks.filter { !it.isCompleted && it.deadlineAtMillis?.let { millis ->
                val date = PlannerTime.localDateAt(millis); date >= today && date <= today.plusDays(3)
            } == true }
        }
        else -> com.haskatech.myday.data.TaskGrouping.filterByStatus(
            tasks,
            when (taskFilter) {
                "Active" -> com.haskatech.myday.data.TaskStatusFilter.ACTIVE
                "Completed" -> com.haskatech.myday.data.TaskStatusFilter.COMPLETED
                else -> com.haskatech.myday.data.TaskStatusFilter.ALL
            }
        )
    }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var inboxOnly by remember { mutableStateOf(false) }
    val categoryFilteredTasks = when {
        inboxOnly -> filteredTasks.filter { it.categoryId == null }
        selectedCategoryId != null -> filteredTasks.filter { it.categoryId == selectedCategoryId }
        else -> filteredTasks
    }
    val (unscheduledTasks, scheduledTasks) = com.haskatech.myday.data.TaskGrouping.partition(categoryFilteredTasks)
    Column(modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 20.dp)) {
        Text("YOUR WORKSPACE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
        Text("Tasks", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineLarge)
        Text("Everything you need to move forward.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        var showCategoryManager by remember { mutableStateOf(false) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("CATEGORIES", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = { showCategoryManager = true }) { Text("Manage") }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = selectedCategoryId == null && !inboxOnly, onClick = { selectedCategoryId = null; inboxOnly = false }, label = { Text("All") })
            FilterChip(selected = inboxOnly, onClick = { selectedCategoryId = null; inboxOnly = true }, label = { Text("Inbox") })
            categories.forEach { category ->
                FilterChip(selected = selectedCategoryId == category.id, onClick = { selectedCategoryId = category.id; inboxOnly = false }, label = { Text(category.name) })
            }
        }
        if (showCategoryManager) CategoryManagerDialog(categories, onCreateCategory, onRenameCategory, onDeleteCategory) { showCategoryManager = false }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All", "Active", "Completed", "Overdue", "Due soon").forEach { option ->
                FilterChip(
                    selected = taskFilter == option,
                    onClick = { taskFilter = option },
                    label = {
                        val count = when (option) {
                            "Active" -> tasks.count { !it.isCompleted }
                            "Completed" -> tasks.count { it.isCompleted }
                            "Overdue" -> tasks.count { !it.isCompleted && it.deadlineAtMillis != null && it.deadlineAtMillis < nowMillis }
                            "Due soon" -> {
                                val today = java.time.LocalDate.now(PlannerTime.zoneId())
                                tasks.count { !it.isCompleted && it.deadlineAtMillis?.let { millis -> val d = PlannerTime.localDateAt(millis); d >= today && d <= today.plusDays(3) } == true }
                            }
                            else -> tasks.size
                        }
                        Text("$option · $count")
                    }
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        if (filteredTasks.isEmpty()) {
            val emptyTitle = when (taskFilter) {
                "Active" -> "No active tasks"
                "Completed" -> "Nothing completed yet"
                else -> "Your task list is clear"
            }
            val emptyMessage = when (taskFilter) {
                "Active" -> "All your tasks are complete. Add a new task when you're ready."
                "Completed" -> "Completed tasks will appear here."
                else -> "Capture your next action and keep moving forward."
            }
            EmptyState(Icons.Default.CheckCircleOutline, emptyTitle, emptyMessage)
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (unscheduledTasks.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("UNSCHEDULED · ${unscheduledTasks.size}", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Tasks you have captured but not assigned a date or time.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                    }
                    items(unscheduledTasks, key = { it.id }) { task ->
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            TaskRow(task, onToggle, onTopPriority, onDelete, tracking, nowMillis, onStartTracking, onPauseTracking, onStopTracking, onEdit)
                            CategoryAssignmentRow(task, categories, onSetCategory)
                        }
                    }
                }
                if (scheduledTasks.isNotEmpty()) {
                    item {
                        Text("SCHEDULED · ${scheduledTasks.size}", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    items(scheduledTasks, key = { it.id }) { task ->
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            TaskRow(task, onToggle, onTopPriority, onDelete, tracking, nowMillis, onStartTracking, onPauseTracking, onStopTracking)
                            CategoryAssignmentRow(task, categories, onSetCategory)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryAssignmentRow(task: TaskEntity, categories: List<CategoryEntity>, onSetCategory: (Long, Long?) -> Unit) {
    var expanded by remember(task.id) { mutableStateOf(false) }
    val selected = categories.firstOrNull { it.id == task.categoryId }
    Box {
        TextButton(onClick = { expanded = true }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
            Text(selected?.name ?: "Inbox · Uncategorized", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Change category", modifier = Modifier.size(15.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Inbox · Uncategorized") }, onClick = { onSetCategory(task.id, null); expanded = false })
            categories.forEach { category ->
                DropdownMenuItem(text = { Text(category.name) }, onClick = { onSetCategory(task.id, category.id); expanded = false })
            }
        }
    }
}

@Composable
private fun CategoryManagerDialog(
    categories: List<CategoryEntity>,
    onCreate: (String, String, String) -> Unit,
    onRename: (Long, String, String, String) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedId by remember { mutableStateOf<Long?>(null) }
    val selected = categories.firstOrNull { it.id == selectedId }
    var name by remember(selectedId) { mutableStateOf(selected?.name.orEmpty()) }
    var color by remember(selectedId) { mutableStateOf(selected?.colorHex ?: "#5B8DEF") }
    var iconKey by remember(selectedId) { mutableStateOf(selected?.iconKey ?: "folder") }
    var creating by remember { mutableStateOf(false) }
    val colors = listOf("#5B8DEF", "#7C6EE6", "#E58A4E", "#38A88A", "#C49A36", "#B36BC7", "#D65D73", "#607D8B")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage categories") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 440.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { category ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedId == category.id, onClick = { selectedId = category.id; creating = false })
                        Text(category.name, modifier = Modifier.weight(1f))
                        if (category.isDefault) Text("Default", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                OutlinedButton(onClick = { selectedId = null; creating = true; name = ""; color = colors.first(); iconKey = "folder" }, modifier = Modifier.fillMaxWidth()) { Text("+ Create category") }
                if (creating || selected != null) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Category name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("Icon", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf("folder", "work", "home", "favorite", "school", "wallet").forEach { key ->
                            FilterChip(selected = iconKey == key, onClick = { iconKey = key }, label = { Text(key.replaceFirstChar { it.uppercase() }) })
                        }
                    }
                    Text("Colour", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        colors.forEach { swatch ->
                            Surface(onClick = { color = swatch }, modifier = Modifier.size(27.dp), color = androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(swatch)), shape = RoundedCornerShape(50), border = if (color == swatch) androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else null) {}
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                if (selected != null) TextButton(onClick = { onDelete(selected.id); selectedId = null }) { Text("Delete") }
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        if (creating) onCreate(name, color, iconKey) else selected?.let { onRename(it.id, name, color, iconKey) }
                    }
                    onDismiss()
                }) { Text(if (creating) "Create" else "Save") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
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
    onStopTracking: (TaskEntity) -> Unit = {},
    onEdit: ((TaskEntity) -> Unit)? = null
) {
    val isTrackingThisTask = tracking?.taskId == task.id
    val elapsed = if (isTrackingThisTask && tracking?.startedAtMillis != null) {
        TimeTrackingRules.elapsedMillis(tracking.startedAtMillis, nowMillis)
    } else 0L
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = task.isCompleted, onCheckedChange = { onToggle(task, it) },
                    colors = CheckboxDefaults.colors(checkedColor = MyDayColors.Navy))
                Column(Modifier.weight(1f).padding(start = 4.dp)) {
                    Text(task.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null)
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(6.dp)) {
                            Text(task.priority, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                        }
                        Text("Tracked " + TimeTrackingRules.formatDuration(task.timeSpentMillis + elapsed),
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                }
                if (onEdit != null) {
                    IconButton(onClick = { onEdit(task) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit task", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (onDelete != null) {
                    IconButton(onClick = { onDelete(task) }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete task", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (!task.isCompleted) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
                Text("Completed", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp,
                    modifier = Modifier.padding(start = 52.dp, top = 2.dp))
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTaskDialog(
    initialTask: TaskEntity? = null,
    allTasks: List<TaskEntity>,
    defaultReminderMinutes: Int = 15,
    categories: List<CategoryEntity> = emptyList(),
    onDismiss: () -> Unit,
    onAdd: (String, String, Long?, Long?, Long?, String, Int?, Boolean, String, Long?) -> Unit
) {
    var title by remember(initialTask?.id) { mutableStateOf(initialTask?.title.orEmpty()) }
    var categoryId by remember(initialTask?.id) { mutableStateOf(initialTask?.categoryId) }
    var priority by remember(initialTask?.id) { mutableStateOf(initialTask?.priority ?: "Normal") }
    var urgency by remember(initialTask?.id) { mutableStateOf(initialTask?.urgency ?: "Normal") }
    var recurrenceRule by remember(initialTask?.id) { mutableStateOf(initialTask?.recurrenceRule ?: "None") }
    var applyToSeries by remember(initialTask?.id) { mutableStateOf(false) }
    var scheduledAt by remember(initialTask?.id) { mutableStateOf(initialTask?.scheduledAtMillis) }
    var plannedDurationMinutes by remember(initialTask?.id) { mutableStateOf(initialTask?.plannedDurationMinutes) }
    var deadlineAt by remember(initialTask?.id) { mutableStateOf(initialTask?.deadlineAtMillis) }
    val conflicts = remember(scheduledAt, plannedDurationMinutes, allTasks, initialTask?.id) {
        TaskConflictRules.findConflicts(scheduledAt, plannedDurationMinutes, allTasks, initialTask?.id)
    }
    val initialReminderOffset = initialTask?.let { task ->
        if (task.scheduledAtMillis != null && task.reminderAtMillis != null)
            ((task.scheduledAtMillis - task.reminderAtMillis) / 60_000L).toInt() else 0
    } ?: 0
    var reminderEnabled by remember(initialTask?.id, defaultReminderMinutes) {
        mutableStateOf(initialTask?.let { it.reminderAtMillis != null } ?: (defaultReminderMinutes >= 0))
    }
    var reminderOffsetMinutes by remember(initialTask?.id, defaultReminderMinutes) {
        mutableIntStateOf(if (initialTask == null) defaultReminderMinutes.coerceAtLeast(0) else initialReminderOffset)
    }
    val context = LocalContext.current
    val nowMillis = System.currentTimeMillis()
    val scheduledInFuture = scheduledAt == null || scheduledAt!! > nowMillis || scheduledAt == initialTask?.scheduledAtMillis
    val reminderInFuture = !reminderEnabled || scheduledAt == null ||
        scheduledAt!! - reminderOffsetMinutes * 60_000L > nowMillis ||
        (scheduledAt == initialTask?.scheduledAtMillis && reminderOffsetMinutes == initialReminderOffset)
    val deadlineValid = deadlineAt == null || (deadlineAt!! > nowMillis && (scheduledAt == null || deadlineAt!! >= scheduledAt!!)) || deadlineAt == initialTask?.deadlineAtMillis
    val canSave = title.isNotBlank() && scheduledInFuture && reminderInFuture && deadlineValid
    val dateLabel = scheduledAt?.let { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it)) }
        ?: "Not scheduled"

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (initialTask == null) "New task" else "Edit task", color = MaterialTheme.colorScheme.onSurface, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text(if (initialTask == null) "Capture it now. Organise it when you're ready." else "Update the details or move this task to a new time.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Normal)
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
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.onSurface, focusedLabelColor = MaterialTheme.colorScheme.onSurface, cursorColor = MaterialTheme.colorScheme.onSurface)
                    )
                    Surface(shape = RoundedCornerShape(13.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
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
                        Icon(Icons.Default.Mic, contentDescription = "Add task by voice", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    }
                }
                var categoryMenuExpanded by remember { mutableStateOf(false) }
                Text("CATEGORY", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Box {
                    OutlinedButton(onClick = { categoryMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(categories.firstOrNull { it.id == categoryId }?.name ?: "Inbox · Uncategorized")
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = categoryMenuExpanded, onDismissRequest = { categoryMenuExpanded = false }) {
                        DropdownMenuItem(text = { Text("Inbox · Uncategorized") }, onClick = { categoryId = null; categoryMenuExpanded = false })
                        categories.forEach { category -> DropdownMenuItem(text = { Text(category.name) }, onClick = { categoryId = category.id; categoryMenuExpanded = false }) }
                    }
                }
                Text("PRIORITY", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("High", "Normal", "Low").forEach { option ->
                        FilterChip(selected = priority == option, onClick = { priority = option }, label = { Text(option) })
                    }
                }
                Text("URGENCY", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Low", "Normal", "High", "Critical").forEach { option ->
                        FilterChip(selected = urgency == option, onClick = { urgency = option }, label = { Text(option, fontSize = 11.sp) })
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
                            deadlineAt = chosen.timeInMillis
                        }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), false).show()
                    }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).apply {
                        datePicker.minDate = System.currentTimeMillis()
                    }.show()
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Flag, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Deadline: ${deadlineAt?.let { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it)) } ?: "Not set"}")
                }
                if (!deadlineValid) {
                    Text("Deadline must be in the future and cannot be earlier than the scheduled start.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
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
                Text("ESTIMATED DURATION", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf<Int?>(null, 15, 30, 45, 60, 90, 120).forEach { minutes ->
                        FilterChip(
                            selected = plannedDurationMinutes == minutes,
                            onClick = { plannedDurationMinutes = minutes },
                            label = { Text(if (minutes == null) "Estimate later" else "${minutes}m", fontSize = 10.sp) }
                        )
                    }
                }
                if (scheduledAt != null) {
                    if (conflicts.isNotEmpty()) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), shape = RoundedCornerShape(12.dp)) {
                            Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Schedule conflict detected", color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                conflicts.forEach { conflict ->
                                    Text(
                                        "Overlaps with ${conflict.title} · " +
                                            DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(conflict.startMillis)) +
                                            "–" + DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(conflict.endMillis)),
                                        color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 11.sp
                                    )
                                }
                                Text("You can still save, but review the overlapping time blocks.", color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 10.sp)
                            }
                        }
                    }
                    if (initialTask == null) {
                        Text("REPEAT", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("None", "Daily", "Weekly", "Monthly").forEach { option ->
                                FilterChip(selected = recurrenceRule == option, onClick = { recurrenceRule = option }, label = { Text(option, fontSize = 11.sp) })
                            }
                        }
                    } else if (initialTask.recurrenceRule != "None") {
                        Text("Recurring task · ${initialTask.recurrenceRule}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        Text("APPLY CHANGES TO", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            FilterChip(selected = !applyToSeries, onClick = { applyToSeries = false }, label = { Text("This occurrence", fontSize = 10.sp) })
                            FilterChip(selected = applyToSeries, onClick = { applyToSeries = true }, label = { Text("This and future", fontSize = 10.sp) })
                        }
                        Text("Past occurrences stay unchanged.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
                        Text("Enable reminder", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                    }
                    if (reminderEnabled) {
                        Text("REMIND ME", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
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
                    if (initialTask?.recurrenceRule == null || initialTask.recurrenceRule == "None") {
                        TextButton(onClick = { scheduledAt = null; recurrenceRule = "None"; reminderOffsetMinutes = 0 }) { Text("Clear schedule") }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val reminder = if (reminderEnabled) scheduledAt?.minus(reminderOffsetMinutes * 60_000L) else null
                onAdd(title, priority, scheduledAt, reminder, deadlineAt, recurrenceRule, plannedDurationMinutes, applyToSeries, urgency, categoryId)
            }, enabled = canSave, shape = RoundedCornerShape(13.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = MaterialTheme.colorScheme.onSecondary)) {
                Text(if (initialTask == null) "Save task" else "Save changes", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(13.dp)) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
    )
}

@Composable
private fun CalendarScreen(tasks: List<TaskEntity>, weekStartDay: WeekStartDay, modifier: Modifier = Modifier) {
    val zone = PlannerTime.zoneId()
    var selectedDate by remember { mutableStateOf(java.time.LocalDate.now(zone)) }
    var view by remember { mutableStateOf("Week") }
    val scheduled = tasks.filter { it.scheduledAtMillis != null }
    val dateFormatter = remember { java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault()) }
    val monthFormatter = remember { java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()) }
    val firstDay = if (weekStartDay == WeekStartDay.SUNDAY) java.time.DayOfWeek.SUNDAY else java.time.DayOfWeek.MONDAY
    val weekStart = selectedDate.with(java.time.temporal.TemporalAdjusters.previousOrSame(firstDay))
    val monthStart = selectedDate.withDayOfMonth(1)
    val gridStart = monthStart.with(java.time.temporal.TemporalAdjusters.previousOrSame(firstDay))
    val weekdayLabels = if (weekStartDay == WeekStartDay.SUNDAY)
        listOf("S", "M", "T", "W", "T", "F", "S")
    else listOf("M", "T", "W", "T", "F", "S", "S")
    val visibleDates = when (view) {
        "Day" -> listOf(selectedDate)
        "Week" -> (0L..6L).map { weekStart.plusDays(it) }
        else -> (0L..41L).map { gridStart.plusDays(it) }
    }
    fun tasksFor(date: java.time.LocalDate): List<TaskEntity> = scheduled.filter { task ->
        PlannerTime.localDateAt(task.scheduledAtMillis!!) == date
    }.sortedBy { it.scheduledAtMillis }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 18.dp)) {
        Text("YOUR SCHEDULE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
        Text("Calendar", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineLarge)
        Text("Make space for what matters.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
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
                color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold
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
                weekdayLabels.forEach { day ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(day, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                                date == java.time.LocalDate.now(zone) -> MyDayColors.SoftLime
                                else -> MaterialTheme.colorScheme.surface
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = if (date == selectedDate) null else androidx.compose.foundation.BorderStroke(1.dp, MyDayColors.Divider)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center) {
                                Text(date.dayOfMonth.toString(), color = if (inMonth) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (date == selectedDate) FontWeight.Bold else FontWeight.Normal)
                                if (count > 0) Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    repeat(count.coerceAtMost(3)) {
                                        Box(Modifier.size(3.dp).background(if (date == selectedDate) MaterialTheme.colorScheme.onSurface else MyDayColors.LimeDeep, RoundedCornerShape(50)))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Selected day · ${selectedDate.format(dateFormatter)}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
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
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(17.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                            Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.width(76.dp)) {
                                    Text(date.format(java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM")),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                    Text(DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).apply {
                                        timeZone = java.util.TimeZone.getTimeZone(zone)
                                    }.format(Date(task.scheduledAtMillis!!)),
                                        color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(task.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold,
                                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null)
                                    val details = listOfNotNull(
                                        if (task.reminderAtMillis != null) "Reminder" else null,
                                        if (task.recurrenceRule != "None") "Repeats ${task.recurrenceRule.lowercase()}" else null,
                                        if (task.isCompleted) "Completed" else null
                                    )
                                    if (details.isNotEmpty()) Text(details.joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                }
                                if (task.priority == "High") Text("HIGH", color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
    LaunchedEffect(Unit) { verticalState.scrollTo(6 * 72) }
    val today = java.time.LocalDate.now(zone)
    val dayFormatter = java.time.format.DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
    val timeFormatter = java.time.format.DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

    Text("WEEK AT A GLANCE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
    Text("Swipe sideways to see all 7 days", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
    Spacer(Modifier.height(8.dp))
    Surface(
        modifier = Modifier.fillMaxWidth().height(560.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MyDayColors.BorderStrong)
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(verticalState)) {
            Row(Modifier.horizontalScroll(horizontalState)) {
                Column {
                    Box(Modifier.width(54.dp).height(58.dp), contentAlignment = Alignment.Center) {
                        Text(PlannerTime.utcOffsetLabel(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    PlannerTime.dayHours.forEach { hour ->
                        Box(
                            Modifier.width(54.dp).height(72.dp).padding(end = 6.dp),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            Text(
                                java.time.LocalTime.of(hour, 0).format(timeFormatter),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                isToday -> MyDayColors.SoftLime
                                else -> MaterialTheme.colorScheme.background
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                Modifier.fillMaxSize().padding(vertical = 5.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(date.format(dayFormatter).uppercase(), color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text(date.dayOfMonth.toString(), color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        PlannerTime.dayHours.forEach { hour ->
                            val hourTasks = tasksByDate[date].orEmpty().filter { task ->
                                java.time.Instant.ofEpochMilli(task.scheduledAtMillis!!).atZone(zone).hour == hour
                            }
                            Box(
                                Modifier.fillMaxWidth().height(72.dp)
                                    .padding(end = 1.dp)
                                    .background(if (isToday) MyDayColors.PaperTint else MaterialTheme.colorScheme.surface)
                                    .then(Modifier)
                            ) {
                                Column(
                                    Modifier.fillMaxSize().padding(horizontal = 3.dp, vertical = 3.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    hourTasks.take(2).forEach { task ->
                                        Surface(
                                            modifier = Modifier.fillMaxWidth().height(30.dp),
                                            color = if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant else MyDayColors.SoftLime,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Column(Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
                                                val taskTime = java.time.Instant.ofEpochMilli(task.scheduledAtMillis!!).atZone(zone).toLocalTime()
                                                Text(taskTime.format(timeFormatter), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 7.sp, maxLines = 1)
                                                Text(
                                                    task.title,
                                                    color = MaterialTheme.colorScheme.onSurface,
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
                                        Text("+${hourTasks.size - 2} more", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            androidx.compose.foundation.Canvas(
                                Modifier.fillMaxWidth().height(1.dp)
                            ) {
                                drawLine(
                                    color = MyDayColors.SurfaceSubtle,
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
        Text("No tasks scheduled this week yet.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
    } else {
        Text("Tasks appear in their scheduled hour. Times use your device timezone.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun InsightsScreen(tasks: List<TaskEntity>, tracking: TimeTrackingSessionEntity?, entries: List<TimeTrackingEntryEntity>, nowMillis: Long, modifier: Modifier = Modifier) {
    var selectedRange by remember { mutableStateOf(com.haskatech.myday.data.AnalyticsRange.WEEK) }
    val zone = PlannerTime.zoneId()
    val report = remember(tasks, entries, tracking?.startedAtMillis, nowMillis, selectedRange, zone) {
        com.haskatech.myday.data.ProductivityAnalyticsRules.calculate(tasks, entries, tracking?.startedAtMillis, nowMillis, zone, selectedRange)
    }
    val activeNow = tasks.count { !it.isCompleted }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("YOUR PROGRESS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
        Text("Insights", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineLarge)
        Text("A local view of your work and focus.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            com.haskatech.myday.data.AnalyticsRange.values().forEach { range ->
                FilterChip(selected = selectedRange == range, onClick = { selectedRange = range }, label = { Text(range.label, fontSize = 10.sp) })
            }
        }
        Text("${report.rangeStart.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy"))} – ${report.rangeEnd.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy"))}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            AnalyticsMetricCard("Tasks created", report.tasksCreated.toString(), Modifier.weight(1f))
            AnalyticsMetricCard("Cohort complete", "${report.completionRatePercent}%", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            AnalyticsMetricCard("Tracked time", TimeTrackingRules.formatDuration(report.trackedMillis), Modifier.weight(1f))
            AnalyticsMetricCard("Overdue active", report.overdueActive.toString(), Modifier.weight(1f))
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Daily focus trend", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Text("Tracked time per local calendar day", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                if (report.daily.none { it.trackedMillis > 0L }) Text("No tracked focus time in this period yet.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                else {
                    val maxValue = report.daily.maxOf { it.trackedMillis }.coerceAtLeast(1L)
                    report.daily.takeLast(14).forEach { point ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(point.date.format(java.time.format.DateTimeFormatter.ofPattern("EEE d")), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, modifier = Modifier.width(48.dp))
                            Box(Modifier.weight(1f).height(10.dp).background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))) {
                                Box(Modifier.fillMaxWidth(point.trackedMillis.toFloat() / maxValue).height(10.dp).background(Lime, RoundedCornerShape(8.dp)))
                            }
                            Text(TimeTrackingRules.formatDuration(point.trackedMillis), color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, modifier = Modifier.width(48.dp))
                        }
                    }
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("Task outcomes", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Text("${report.completedFromCohort} of ${report.tasksCreated} tasks created in this period are currently complete.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                LinearProgressIndicator(progress = { report.completionRatePercent / 100f }, modifier = Modifier.fillMaxWidth(), color = Lime, trackColor = MaterialTheme.colorScheme.background)
                Text("$activeNow active tasks across your workspace", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }
        Text("Completion rate compares current task status for tasks created in the selected period. MyDay does not yet store completion timestamps, so historical completion dates are not inferred. Tracked time is split using your device timezone.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
    }
}
@Composable
private fun AnalyticsMetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(17.dp)) {
        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, message: String) {
    Card(Modifier.fillMaxWidth().padding(top = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(58.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(28.dp)) }
            }
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun SettingsScreen(
    modifier: Modifier = Modifier,
    settings: MyDaySettings,
    onThemeChange: (ThemeMode) -> Unit,
    onWeekStartChange: (WeekStartDay) -> Unit,
    onReminderChange: (Int) -> Unit,
    onOpenInsights: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    var showRestoreConfirmation by remember { mutableStateOf(false) }
    var activeSetting by remember { mutableStateOf<String?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            runCatching { MyDayBackupManager.exportTo(context, uri) }
                .onSuccess { Toast.makeText(context, "MyDay backup exported.", Toast.LENGTH_LONG).show() }
                .onFailure { Toast.makeText(context, "Backup failed: ${it.localizedMessage ?: "storage error"}", Toast.LENGTH_LONG).show() }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        restoreUri = uri
        showRestoreConfirmation = uri != null
    }
    val reminderLabel = when (settings.defaultReminderMinutes) {
        -1 -> "Off by default"
        0 -> "At scheduled time"
        else -> "${settings.defaultReminderMinutes} minutes before"
    }
    val notificationsEnabled = Build.VERSION.SDK_INT < 33 ||
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("MAKE IT YOURS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
            Text("Settings", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineLarge)
            Text("A calmer way to plan, built around you.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }
        SettingsGroup("YOUR EXPERIENCE") {
            SettingsRow(Icons.Default.Palette, "Appearance", when (settings.themeMode) {
                ThemeMode.SYSTEM -> "Follow device theme"
                ThemeMode.LIGHT -> "Light theme"
                ThemeMode.DARK -> "Dark theme"
            }) { activeSetting = "Appearance" }
            SettingsRow(Icons.Default.NotificationsActive, "Reminders", reminderLabel) { activeSetting = "Reminders" }
            SettingsRow(Icons.Default.CalendarMonth, "Calendar", "Week begins on ${settings.weekStartDay.name.lowercase().replaceFirstChar { it.uppercase() }}") {
                activeSetting = "Calendar"
            }
        }
        SettingsGroup("YOUR PRODUCTIVITY") {
            SettingsRow(Icons.Default.Timer, "Focus sessions", "Configure focus and break durations") {
                context.startActivity(Intent(context, com.haskatech.myday.focus.PomodoroActivity::class.java))
            }
            SettingsRow(Icons.Default.Insights, "Time tracking", "Review tracked time and task sessions") { activeSetting = "Time tracking" }
            SettingsRow(Icons.Default.OfflineBolt, "Offline-first", "Your tasks are stored on this device") { activeSetting = "Offline-first" }
        }
        SettingsGroup("BACKUP & RESTORE") {
            Text("Export a portable copy of tasks and tracked time, or restore a previous MyDay backup. Files stay under your control; no cloud account is required.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(vertical = 8.dp))
            TextButton(onClick = {
                scope.launch {
                    runCatching { MyDayBackupManager.checkIntegrity(context) }
                        .onSuccess { ok ->
                            val message = if (ok) "Local database integrity check passed."
                                else "Database integrity check reported a problem. Export a backup if possible."
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                        .onFailure { Toast.makeText(context, "Database check failed: ${it.localizedMessage ?: "storage error"}", Toast.LENGTH_LONG).show() }
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null)
                Spacer(Modifier.width(5.dp))
                Text("Check local data integrity")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { exportLauncher.launch("MyDay-backup.json") }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.FileUpload, contentDescription = null)
                    Spacer(Modifier.width(5.dp))
                    Text("Export backup")
                }
                TextButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/*", "application/octet-stream")) },
                    modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                    Spacer(Modifier.width(5.dp))
                    Text("Restore backup")
                }
            }
        }
        SettingsGroup("ABOUT") {
            SettingsRow(Icons.Default.AutoAwesome, "MyDay", "Plan with purpose") { activeSetting = "MyDay" }
            SettingsRow(Icons.Default.Info, "Version", "${BuildConfig.VERSION_NAME} · Build ${BuildConfig.VERSION_CODE}") { activeSetting = "Version" }
        }
        Text("MYDAY  ·  PLAN WITH PURPOSE", modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }

    when (activeSetting) {
        "Appearance" -> AlertDialog(
            onDismissRequest = { activeSetting = null },
            title = { Text("Appearance") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ThemeMode.values().forEach { mode ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = settings.themeMode == mode, onClick = { onThemeChange(mode) })
                            Text(when (mode) {
                                ThemeMode.SYSTEM -> "System default"
                                ThemeMode.LIGHT -> "Light"
                                ThemeMode.DARK -> "Dark"
                            }, modifier = Modifier.weight(1f))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { activeSetting = null }) { Text("Done") } }
        )
        "Reminders" -> AlertDialog(
            onDismissRequest = { activeSetting = null },
            title = { Text("Default task reminder") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("New tasks will use this reminder lead time when you enable a reminder. You can still change it for each task.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Text(
                        if (notificationsEnabled) "Android notifications: enabled"
                        else "Android notifications: permission not granted. Scheduled reminders may not appear.",
                        color = if (notificationsEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                        fontSize = 11.sp
                    )
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent("android.settings.APP_NOTIFICATION_SETTINGS")
                                        .putExtra("android.provider.extra.APP_PACKAGE", context.packageName)
                                )
                            }.onFailure {
                                Toast.makeText(context, "Open MyDay notification settings from Android Settings.", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Android notification settings")
                    }
                    listOf(-1, 0, 5, 10, 15).forEach { minutes ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = settings.defaultReminderMinutes == minutes, onClick = { onReminderChange(minutes) })
                            Text(when (minutes) {
                                -1 -> "Off"
                                0 -> "At scheduled time"
                                else -> "$minutes minutes before"
                            }, modifier = Modifier.weight(1f))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { activeSetting = null }) { Text("Done") } }
        )
        "Calendar" -> AlertDialog(
            onDismissRequest = { activeSetting = null },
            title = { Text("Calendar week starts on") },
            text = {
                Column {
                    WeekStartDay.values().forEach { day ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = settings.weekStartDay == day, onClick = { onWeekStartChange(day) })
                            Text(if (day == WeekStartDay.MONDAY) "Monday" else "Sunday")
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { activeSetting = null }) { Text("Done") } }
        )
        "Time tracking" -> AlertDialog(
            onDismissRequest = { activeSetting = null },
            title = { Text("Time tracking") },
            text = { Text("Track time against individual tasks using Start timer, Pause and Stop. Recorded sessions remain stored locally and are included in Insights. Opening Insights does not change or delete any tracking records.") },
            confirmButton = {
                TextButton(onClick = { activeSetting = null; onOpenInsights() }) { Text("View Insights") }
            },
            dismissButton = { TextButton(onClick = { activeSetting = null }) { Text("Close") } }
        )
        "Offline-first" -> AlertDialog(
            onDismissRequest = { activeSetting = null },
            title = { Text("Offline-first storage") },
            text = { Text("MyDay stores tasks and tracked-time records in the on-device database. Core task planning and time tracking are available without an internet connection. Voice recognition may depend on the speech service and network availability. Back up your data regularly using Backup & Restore.") },
            confirmButton = { TextButton(onClick = { activeSetting = null }) { Text("Got it") } }
        )
        "MyDay" -> AlertDialog(
            onDismissRequest = { activeSetting = null },
            title = { Text("MyDay") },
            text = { Text("A personal productivity planner built to help you plan with purpose, stay focused and understand how you spend your time.") },
            confirmButton = { TextButton(onClick = { activeSetting = null }) { Text("Close") } }
        )
        "Version" -> AlertDialog(
            onDismissRequest = { activeSetting = null },
            title = { Text("MyDay version") },
            text = { Text("Version ${BuildConfig.VERSION_NAME}\nBuild ${BuildConfig.VERSION_CODE}\nApplication ID: ${BuildConfig.APPLICATION_ID}") },
            confirmButton = { TextButton(onClick = { activeSetting = null }) { Text("Close") } }
        )
    }

    if (showRestoreConfirmation) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmation = false; restoreUri = null },
            title = { Text("Replace this device's MyDay data?") },
            text = { Text("Restoring replaces all current tasks and tracked-time records with the selected backup. Export a backup of current data first if you may need it.") },
            confirmButton = {
                TextButton(onClick = {
                    val uri = restoreUri
                    showRestoreConfirmation = false
                    restoreUri = null
                    if (uri != null) scope.launch {
                        runCatching { MyDayBackupManager.importFrom(context, uri) }
                            .onSuccess { Toast.makeText(context, "MyDay data restored.", Toast.LENGTH_LONG).show() }
                            .onFailure { Toast.makeText(context, "Restore failed: ${it.localizedMessage ?: "invalid backup or storage error"}", Toast.LENGTH_LONG).show() }
                    }
                }) { Text("Replace data") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmation = false; restoreUri = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
            Column(content = content, modifier = Modifier.padding(horizontal = 13.dp, vertical = 5.dp))
        }
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 9.dp)
            .then(Modifier.clickable(onClick = onClick)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = RoundedCornerShape(11.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(37.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(19.dp)) }
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }
    }
}
