package com.workalarm.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.workalarm.app.data.model.ShiftType
import com.workalarm.app.data.model.WorkAlarm
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftAlarmScreen(
    alarms: List<WorkAlarm>,
    onToggleAlarm: (String, Boolean) -> Unit,
    onDeleteAlarm: (String) -> Unit,
    onSaveAlarm: (WorkAlarm) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingAlarm by remember { mutableStateOf<WorkAlarm?>(null) }

    val nextActiveAlarm = remember(alarms) {
        alarms.firstOrNull { it.isEnabled }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingAlarm = null
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Shift Alarm")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                // Upcoming shift summary card
                NextShiftHeaderCard(nextActiveAlarm)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Shift Alarms",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${alarms.count { it.isEnabled }} Active",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (alarms.isEmpty()) {
                item {
                    EmptyAlarmsPlaceholder {
                        showAddDialog = true
                    }
                }
            } else {
                items(alarms, key = { it.id }) { alarm ->
                    AlarmItemCard(
                        alarm = alarm,
                        onToggle = { isChecked -> onToggleAlarm(alarm.id, isChecked) },
                        onEdit = {
                            editingAlarm = alarm
                            showAddDialog = true
                        },
                        onDelete = { onDeleteAlarm(alarm.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    if (showAddDialog) {
        AlarmEditDialog(
            initialAlarm = editingAlarm,
            onDismiss = { showAddDialog = false },
            onConfirm = { savedAlarm ->
                onSaveAlarm(savedAlarm)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun NextShiftHeaderCard(nextAlarm: WorkAlarm?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(nextAlarm?.shiftType?.getColor() ?: MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Alarm,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (nextAlarm != null) "NEXT WORK ALARM" else "NO ALARM SCHEDULED",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                if (nextAlarm != null) {
                    Text(
                        text = nextAlarm.formattedAlarmTime(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${nextAlarm.shiftType.displayName} starts at ${nextAlarm.formattedShiftStartTime()} (${nextAlarm.leadTimeMinutes}m prep)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "All alarms are paused",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun AlarmItemCard(
    alarm: WorkAlarm,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shift Type Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = alarm.shiftType.getColor().copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(alarm.shiftType.getColor())
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = alarm.shiftType.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = alarm.shiftType.getColor()
                        )
                    }
                }

                // Switch
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = alarm.shiftType.getColor()
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Alarm Ring Time
            Text(
                text = alarm.formattedAlarmTime(),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 38.sp),
                fontWeight = FontWeight.Bold,
                color = if (alarm.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )

            // Shift Context & Lead Time
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Icon(
                    Icons.Default.DirectionsBus,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Shift starts: ${alarm.formattedShiftStartTime()} • ${alarm.leadTimeMinutes} min commute & prep",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Days of Week & Delete Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = alarm.formattedDays(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Medium
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Alarm",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyAlarmsPlaceholder(onAdd: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Schedule,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "No Work Alarms Set",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Create an alarm tailored to your shift schedule with built-in travel and wake-up prep time.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAdd) {
                Text("Create First Shift Alarm")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditDialog(
    initialAlarm: WorkAlarm?,
    onDismiss: () -> Unit,
    onConfirm: (WorkAlarm) -> Unit
) {
    var selectedShift by remember { mutableStateOf(initialAlarm?.shiftType ?: ShiftType.MORNING) }
    var shiftHour by remember { mutableIntStateOf(initialAlarm?.shiftStartHour ?: selectedShift.defaultStartHour) }
    var shiftMinute by remember { mutableIntStateOf(initialAlarm?.shiftStartMinute ?: selectedShift.defaultStartMinute) }
    var leadTime by remember { mutableIntStateOf(initialAlarm?.leadTimeMinutes ?: 90) }
    var label by remember { mutableStateOf(initialAlarm?.label ?: "${selectedShift.displayName} Alarm") }
    var selectedDays by remember {
        mutableStateOf(
            initialAlarm?.daysOfWeek ?: setOf(
                Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                Calendar.THURSDAY, Calendar.FRIDAY
            )
        )
    }

    // Auto update defaults when shift changes if creating new
    val onShiftTypeSelected = { shift: ShiftType ->
        selectedShift = shift
        if (initialAlarm == null) {
            shiftHour = shift.defaultStartHour
            shiftMinute = shift.defaultStartMinute
            label = "${shift.displayName} Alarm"
        }
    }

    // Calculated alarm wake up time
    val dummyAlarm = WorkAlarm(
        shiftStartHour = shiftHour,
        shiftStartMinute = shiftMinute,
        leadTimeMinutes = leadTime
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialAlarm != null) "Edit Shift Alarm" else "New Shift Alarm")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Shift Type Selector Chips
                Text("Shift Type", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(ShiftType.MORNING, ShiftType.DAY, ShiftType.EVENING, ShiftType.NIGHT).forEach { shift ->
                        val isSelected = selectedShift == shift
                        FilterChip(
                            selected = isSelected,
                            onClick = { onShiftTypeSelected(shift) },
                            label = { Text(shift.displayName.split(" ")[0]) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = shift.getColor(),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Shift Start Time
                Text("Shift Starts At", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            shiftHour = (shiftHour + 1) % 24
                        }
                    ) {
                        Text(String.format("%02d:00", shiftHour))
                    }
                    Text("Minutes:")
                    OutlinedButton(
                        onClick = {
                            shiftMinute = if (shiftMinute == 0) 30 else 0
                        }
                    ) {
                        Text(String.format(":%02d", shiftMinute))
                    }
                }

                // Lead Time Selector (Commute + Prep)
                Text(
                    "Commute & Prep Time ($leadTime minutes)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(30, 60, 90, 120).forEach { mins ->
                        FilterChip(
                            selected = leadTime == mins,
                            onClick = { leadTime = mins },
                            label = { Text("${mins}m") }
                        )
                    }
                }

                // Live Alarm Preview Banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Alarm Rings At:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = dummyAlarm.formattedAlarmTime(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Days of week selector
                Text("Repeat Days", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val daysMap = listOf(
                        Calendar.MONDAY to "M",
                        Calendar.TUESDAY to "T",
                        Calendar.WEDNESDAY to "W",
                        Calendar.THURSDAY to "T",
                        Calendar.FRIDAY to "F",
                        Calendar.SATURDAY to "S",
                        Calendar.SUNDAY to "S"
                    )
                    daysMap.forEach { (dayInt, labelText) ->
                        val isSelected = selectedDays.contains(dayInt)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable {
                                    selectedDays = if (isSelected) {
                                        selectedDays - dayInt
                                    } else {
                                        selectedDays + dayInt
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = labelText,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalAlarm = (initialAlarm ?: WorkAlarm()).copy(
                        label = label,
                        shiftType = selectedShift,
                        shiftStartHour = shiftHour,
                        shiftStartMinute = shiftMinute,
                        leadTimeMinutes = leadTime,
                        daysOfWeek = selectedDays,
                        isEnabled = true
                    )
                    onConfirm(finalAlarm)
                }
            ) {
                Text("Save Alarm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
