package com.haskatech.myday

import android.os.Bundle
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
    val tasks by tasksViewModel.tasks.collectAsStateWithLifecycle()
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
            0 -> TodayScreen(tasks, { task, done -> tasksViewModel.setCompleted(task, done) }, toggleTop, Modifier.padding(padding))
            1 -> TasksScreen(tasks, { task, done -> tasksViewModel.setCompleted(task, done) }, toggleTop,
                { tasksViewModel.deleteTask(it) }, Modifier.padding(padding))
            2 -> PlaceholderScreen("Calendar", "Calendar planning is the next feature to implement.", Modifier.padding(padding))
            3 -> InsightsScreen(tasks, Modifier.padding(padding))
            else -> PlaceholderScreen("Settings", "Personalise your planning experience.", Modifier.padding(padding))
        }
    }

    if (showAddDialog) {
        AddTaskDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { title, priority ->
                tasksViewModel.addTask(title, priority)
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
    onToggle: (TaskEntity, Boolean) -> Unit,
    onTopPriority: (TaskEntity, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
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
            items(tasks, key = { it.id }) { task -> TaskRow(task, onToggle, onTopPriority) }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun TasksScreen(
    tasks: List<TaskEntity>,
    onToggle: (TaskEntity, Boolean) -> Unit,
    onTopPriority: (TaskEntity, Boolean) -> Unit,
    onDelete: (TaskEntity) -> Unit,
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
                items(tasks, key = { it.id }) { task -> TaskRow(task, onToggle, onTopPriority, onDelete) }
            }
        }
    }
}

@Composable
private fun TaskRow(
    task: TaskEntity,
    onToggle: (TaskEntity, Boolean) -> Unit,
    onTopPriority: (TaskEntity, Boolean) -> Unit,
    onDelete: ((TaskEntity) -> Unit)? = null
) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = task.isCompleted, onCheckedChange = { onToggle(task, it) },
                colors = CheckboxDefaults.colors(checkedColor = Navy))
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text(task.title, color = Navy, fontWeight = FontWeight.SemiBold,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null)
                Text(task.priority, color = Muted, fontSize = 12.sp)
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

@Composable
private fun AddTaskDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Normal") }
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
            }
        },
        confirmButton = { TextButton(onClick = { onAdd(title, priority) }, enabled = title.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun InsightsScreen(tasks: List<TaskEntity>, modifier: Modifier = Modifier) {
    val done = tasks.count { it.isCompleted }
    val percent = if (tasks.isEmpty()) 0 else done * 100 / tasks.size
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Insights", color = Navy, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("A simple view of today's progress.", color = Muted)
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
