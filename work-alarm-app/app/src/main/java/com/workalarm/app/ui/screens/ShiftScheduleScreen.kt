package com.workalarm.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
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

@Composable
fun ShiftScheduleScreen(
    schedule: Map<Int, ShiftType>,
    alarms: List<WorkAlarm>,
    onUpdateDayShift: (Int, ShiftType) -> Unit,
    onApplyPattern: (List<ShiftType>) -> Unit,
    onSyncAlarms: () -> Unit
) {
    var selectedDayForDialog by remember { mutableStateOf<Int?>(null) }
    var syncSuccessMsg by remember { mutableStateOf(false) }

    val daysOrder = listOf(
        Calendar.MONDAY to "Monday",
        Calendar.TUESDAY to "Tuesday",
        Calendar.WEDNESDAY to "Wednesday",
        Calendar.THURSDAY to "Thursday",
        Calendar.FRIDAY to "Friday",
        Calendar.SATURDAY to "Saturday",
        Calendar.SUNDAY to "Sunday"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Shift Schedule Roster",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Assign shifts per day or apply rotating shift patterns",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Quick Rotation Generator Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Quick Rotation Templates",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onApplyPattern(
                                    listOf(
                                        ShiftType.MORNING, ShiftType.MORNING, ShiftType.MORNING,
                                        ShiftType.MORNING, ShiftType.OFF, ShiftType.OFF
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("4-On / 2-Off", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                onApplyPattern(
                                    listOf(
                                        ShiftType.MORNING, ShiftType.MORNING,
                                        ShiftType.NIGHT, ShiftType.NIGHT,
                                        ShiftType.OFF, ShiftType.OFF
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("2D-2N-2Off", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            onSyncAlarms()
                            syncSuccessMsg = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Auto-Sync Alarms to This Roster")
                    }

                    if (syncSuccessMsg) {
                        Text(
                            text = "Alarms updated to match current roster!",
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        // Section: 7-Day Roster
        item {
            Text(
                text = "Weekly Roster (Tap any day to change)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        // List of days
        items(daysOrder.size) { index ->
            val (dayInt, dayName) = daysOrder[index]
            val shift = schedule[dayInt] ?: ShiftType.OFF
            DayRosterCard(
                dayName = dayName,
                shift = shift,
                onClick = { selectedDayForDialog = dayInt }
            )
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Shift picker dialog
    selectedDayForDialog?.let { dayInt ->
        val currentShift = schedule[dayInt] ?: ShiftType.OFF
        val dayName = daysOrder.find { it.first == dayInt }?.second ?: ""

        AlertDialog(
            onDismissRequest = { selectedDayForDialog = null },
            title = { Text("Select Shift for $dayName") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShiftType.values().forEach { shiftType ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUpdateDayShift(dayInt, shiftType)
                                    selectedDayForDialog = null
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (currentShift == shiftType) shiftType.getColor().copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(shiftType.getColor())
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = shiftType.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = shiftType.formattedDefaultTime(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedDayForDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DayRosterCard(
    dayName: String,
    shift: ShiftType,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = shift.formattedDefaultTime(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = shift.getColor().copy(alpha = 0.2f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(shift.getColor())
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = shift.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = shift.getColor(),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
