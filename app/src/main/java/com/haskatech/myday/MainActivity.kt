package com.haskatech.myday

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

private data class PlannerTask(val title: String, val time: String, val priority: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyDayApp() {
    var tab by remember { mutableIntStateOf(0) }
    val tasks = remember {
        mutableStateListOf(
            PlannerTask("Review today's priorities", "8:00 AM", "High"),
            PlannerTask("Work on an important task", "10:00 AM", "High"),
            PlannerTask("Take a short break", "11:30 AM", "Normal")
        )
    }
    val labels = listOf("My Day", "Tasks", "Calendar", "Insights", "Settings")
    val icons = listOf(Icons.Default.Today, Icons.Default.CheckCircle, Icons.Default.CalendarMonth, Icons.Default.Insights, Icons.Default.Settings)
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
            FloatingActionButton(onClick = { tasks.add(PlannerTask("New task", "Unscheduled", "Normal")); tab = 1 },
                containerColor = Lime, contentColor = Navy) {
                Icon(Icons.Default.Add, contentDescription = "Add task")
            }
        }
    ) { padding ->
        when (tab) {
            0 -> TodayScreen(tasks, Modifier.padding(padding))
            1 -> TasksScreen(tasks, Modifier.padding(padding))
            2 -> PlaceholderScreen("Calendar", "Your time blocks and deadlines will appear here.", Modifier.padding(padding))
            3 -> PlaceholderScreen("Insights", "Daily progress and weekly reports will appear here.", Modifier.padding(padding))
            else -> PlaceholderScreen("Settings", "Personalise your planning experience.", Modifier.padding(padding))
        }
    }
}

@Composable
private fun TodayScreen(tasks: List<PlannerTask>, modifier: Modifier = Modifier) {
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
                    Text("TODAY'S TOP 3", color = Lime, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Choose the three things that matter most.", color = Color.White, fontSize = 17.sp)
                    Text("Your priorities guide the rest of your schedule.", color = Color.LightGray, fontSize = 13.sp)
                }
            }
        }
        item { Text("Today's timetable", color = Navy, fontSize = 19.sp, fontWeight = FontWeight.Bold) }
        items(tasks) { task -> TaskCard(task) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Daily progress", color = Navy, fontWeight = FontWeight.Bold)
                    Text("Completed tasks and focus time will be tracked here.", color = Muted, fontSize = 13.sp)
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun TasksScreen(tasks: List<PlannerTask>, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(18.dp)) {
        Text("Tasks", color = Navy, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("Capture tasks now. Organise them when ready.", color = Muted)
        Spacer(Modifier.height(16.dp))
        tasks.forEach { TaskCard(it) }
    }
}

@Composable
private fun TaskCard(task: PlannerTask) {
    Card(Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.background(Lime.copy(alpha = 0.35f), RoundedCornerShape(8.dp)).padding(8.dp)) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Navy)
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(task.title, color = Navy, fontWeight = FontWeight.SemiBold)
                Text(task.time, color = Muted, fontSize = 12.sp)
            }
            Text(task.priority, color = Navy, fontSize = 11.sp)
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
