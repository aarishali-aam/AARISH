package com.workalarm.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import com.workalarm.app.data.repository.AlarmRepository
import com.workalarm.app.data.repository.WorkRepository
import com.workalarm.app.ui.screens.BreakTimerScreen
import com.workalarm.app.ui.screens.ShiftScheduleScreen
import com.workalarm.app.ui.screens.WorkListScreen
import com.workalarm.app.ui.theme.WorkAlarmTheme

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Works : Screen("works", "My Works", Icons.Default.Work)
    object Breaks : Screen("breaks", "Break Timers", Icons.Default.Timer)
    object Schedule : Screen("schedule", "Shift Roster", Icons.Default.CalendarMonth)
}

class MainActivity : ComponentActivity() {

    private lateinit var workRepository: WorkRepository
    private lateinit var alarmRepository: AlarmRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workRepository = WorkRepository(applicationContext)
        alarmRepository = AlarmRepository(applicationContext)

        setContent {
            WorkAlarmTheme {
                MainAppContainer(
                    workRepository = workRepository,
                    alarmRepository = alarmRepository,
                    onRequestPermissions = { checkAndRequestPermissions() }
                )
            }
        }
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionStatus = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(android.app.AlarmManager::class.java)
            if (alarmManager?.canScheduleExactAlarms() == false) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer(
    workRepository: WorkRepository,
    alarmRepository: AlarmRepository,
    onRequestPermissions: () -> Unit
) {
    var selectedScreen by remember { mutableStateOf<Screen>(Screen.Works) }
    val works by workRepository.works.collectAsState()
    val alarms by alarmRepository.alarms.collectAsState()
    val schedule by alarmRepository.schedule.collectAsState()

    LaunchedEffect(Unit) {
        onRequestPermissions()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row {
                        Text(
                            text = "Work",
                            fontWeight = FontWeight.Light,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Alarm",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                val items = listOf(Screen.Works, Screen.Breaks, Screen.Schedule)
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = selectedScreen == screen,
                        onClick = { selectedScreen = screen },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedScreen) {
                Screen.Works -> {
                    WorkListScreen(
                        works = works,
                        onAddWork = { work -> workRepository.addWork(work) },
                        onUpdateWork = { work -> workRepository.updateWork(work) },
                        onDeleteWork = { id -> workRepository.deleteWork(id) },
                        onToggleAlarm = { id, isEnabled -> workRepository.toggleAlarm(id, isEnabled) },
                        onToggleCompleted = { id, isCompleted -> workRepository.toggleCompleted(id, isCompleted) }
                    )
                }
                Screen.Breaks -> {
                    BreakTimerScreen()
                }
                Screen.Schedule -> {
                    ShiftScheduleScreen(
                        schedule = schedule,
                        alarms = alarms,
                        onUpdateDayShift = { day, shift -> alarmRepository.updateDayShift(day, shift) },
                        onApplyPattern = { pattern -> alarmRepository.applyRotationPattern(pattern) },
                        onSyncAlarms = { alarmRepository.loadAlarms() }
                    )
                }
            }
        }
    }
}
