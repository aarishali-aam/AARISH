package com.workalarm.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.workalarm.app.data.model.WorkCategory
import com.workalarm.app.data.model.WorkItem
import com.workalarm.app.data.model.WorkPriority
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWorkDialog(
    initialWork: WorkItem?,
    onDismiss: () -> Unit,
    onConfirm: (WorkItem) -> Unit
) {
    var title by remember { mutableStateOf(initialWork?.title ?: "") }
    var location by remember { mutableStateOf(initialWork?.workplaceOrLocation ?: "") }
    var category by remember { mutableStateOf(initialWork?.category ?: WorkCategory.GENERAL) }
    var startHour by remember { mutableIntStateOf(initialWork?.startHour ?: 9) }
    var startMinute by remember { mutableIntStateOf(initialWork?.startMinute ?: 0) }
    var endHour by remember { mutableIntStateOf(initialWork?.endHour ?: 17) }
    var endMinute by remember { mutableIntStateOf(initialWork?.endMinute ?: 0) }
    var leadTime by remember { mutableIntStateOf(initialWork?.leadTimeMinutes ?: 30) }
    var notes by remember { mutableStateOf(initialWork?.notes ?: "") }
    var selectedDays by remember {
        mutableStateOf(
            initialWork?.repeatDays ?: setOf(
                Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                Calendar.THURSDAY, Calendar.FRIDAY
            )
        )
    }

    val previewWork = WorkItem(
        title = title.ifBlank { "New Work" },
        category = category,
        startHour = startHour,
        startMinute = startMinute,
        leadTimeMinutes = leadTime
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialWork != null) "Edit Work" else "Add New Work", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Work Title (e.g. Warehouse Shift, Store Opening)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Location / Workplace Input
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location / Workplace (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Category Chips
                Text("Category", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(WorkCategory.values()) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = cat.getColor(),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Work Start & End Time
                Text("Work Hours", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { startHour = (startHour + 1) % 24 },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(String.format("Starts: %02d:%02d", startHour, startMinute), fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { endHour = (endHour + 1) % 24 },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(String.format("Ends: %02d:%02d", endHour, endMinute), fontSize = 12.sp)
                    }
                }

                // Lead Time / Alarm Timing
                Text("When should alarm ring?", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val presets = listOf(0, 15, 30, 45, 60, 90, 120)
                    items(presets) { mins ->
                        FilterChip(
                            selected = leadTime == mins,
                            onClick = { leadTime = mins },
                            label = {
                                Text(if (mins == 0) "Exact Start" else "${mins}m before")
                            }
                        )
                    }
                }

                // Dynamic Alarm Ringing Preview
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Alarm rings at:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            text = previewWork.formattedAlarmTime(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = if (leadTime > 0) "$leadTime minutes to wake up, get ready, and travel" else "Rings right when work starts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                // Repeat Days
                Text("Repeat On Days", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
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
                                    selectedDays = if (isSelected) selectedDays - dayInt else selectedDays + dayInt
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

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Checklist / Tools needed") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val newWork = (initialWork ?: WorkItem(title = title)).copy(
                            title = title.trim(),
                            workplaceOrLocation = location.trim(),
                            category = category,
                            startHour = startHour,
                            startMinute = startMinute,
                            endHour = endHour,
                            endMinute = endMinute,
                            leadTimeMinutes = leadTime,
                            repeatDays = selectedDays,
                            notes = notes.trim(),
                            isAlarmEnabled = true
                        )
                        onConfirm(newWork)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text(if (initialWork != null) "Save Changes" else "Add Work")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
