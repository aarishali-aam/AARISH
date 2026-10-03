package com.workalarm.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.workalarm.app.data.model.WorkCategory
import com.workalarm.app.data.model.WorkItem
import com.workalarm.app.data.model.WorkPriority

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkListScreen(
    works: List<WorkItem>,
    onAddWork: (WorkItem) -> Unit,
    onUpdateWork: (WorkItem) -> Unit,
    onDeleteWork: (String) -> Unit,
    onToggleAlarm: (String, Boolean) -> Unit,
    onToggleCompleted: (String, Boolean) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingWork by remember { mutableStateOf<WorkItem?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf<WorkCategory?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Active Works, 1 = Completed

    val activeWorks = remember(works, selectedCategoryFilter) {
        works.filter { !it.isCompleted && (selectedCategoryFilter == null || it.category == selectedCategoryFilter) }
    }

    val completedWorks = remember(works, selectedCategoryFilter) {
        works.filter { it.isCompleted && (selectedCategoryFilter == null || it.category == selectedCategoryFilter) }
    }

    val nextUpcomingWork = remember(works) {
        works.firstOrNull { it.isAlarmEnabled && !it.isCompleted }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingWork = null
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Work", fontWeight = FontWeight.Bold) }
            )
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
                Spacer(modifier = Modifier.height(4.dp))
                // Next Work Alarm Hero Card
                NextWorkHeroCard(nextUpcomingWork)
            }

            // Category Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategoryFilter == null,
                            onClick = { selectedCategoryFilter = null },
                            label = { Text("All Works (${works.size})") }
                        )
                    }
                    items(WorkCategory.values()) { cat ->
                        FilterChip(
                            selected = selectedCategoryFilter == cat,
                            onClick = {
                                selectedCategoryFilter = if (selectedCategoryFilter == cat) null else cat
                            },
                            label = { Text(cat.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = cat.getColor(),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Tabs: Active vs Completed
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Active Works (${activeWorks.size})", fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Completed (${completedWorks.size})", fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            val displayList = if (selectedTab == 0) activeWorks else completedWorks

            if (displayList.isEmpty()) {
                item {
                    EmptyWorksCard(
                        isCompletedTab = selectedTab == 1,
                        onAddClick = { showAddDialog = true }
                    )
                }
            } else {
                items(displayList, key = { it.id }) { work ->
                    WorkItemCard(
                        work = work,
                        onToggleAlarm = { isEnabled -> onToggleAlarm(work.id, isEnabled) },
                        onToggleCompleted = { isDone -> onToggleCompleted(work.id, isDone) },
                        onEdit = {
                            editingWork = work
                            showAddDialog = true
                        },
                        onDelete = { onDeleteWork(work.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (showAddDialog) {
        AddWorkDialog(
            initialWork = editingWork,
            onDismiss = { showAddDialog = false },
            onConfirm = { savedWork ->
                if (editingWork != null) {
                    onUpdateWork(savedWork)
                } else {
                    onAddWork(savedWork)
                }
                showAddDialog = false
            }
        )
    }
}

@Composable
fun NextWorkHeroCard(nextWork: WorkItem?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(nextWork?.category?.getColor() ?: MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Alarm,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (nextWork != null) "NEXT WORK ALARM" else "NO UPCOMING ALARM",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                if (nextWork != null) {
                    Text(
                        text = nextWork.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Alarm rings at ${nextWork.formattedAlarmTime()} (${nextWork.leadTimeMinutes}m before work)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Work time: ${nextWork.formattedWorkTime()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                } else {
                    Text(
                        text = "Add a work to get scheduled alarms",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun WorkItemCard(
    work: WorkItem,
    onToggleAlarm: (Boolean) -> Unit,
    onToggleCompleted: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (work.isCompleted) MaterialTheme.colorScheme.surface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Category Badge + Checkbox + Alarm Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Chip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = work.category.getColor().copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(work.category.getColor())
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = work.category.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = work.category.getColor()
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Checkbox for completed
                    Checkbox(
                        checked = work.isCompleted,
                        onCheckedChange = onToggleCompleted
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Alarm switch
                    Switch(
                        checked = work.isAlarmEnabled && !work.isCompleted,
                        onCheckedChange = onToggleAlarm,
                        enabled = !work.isCompleted,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = work.category.getColor()
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Work Title
            Text(
                text = work.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (work.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (work.isCompleted) TextDecoration.LineThrough else TextDecoration.None
            )

            // Location
            if (work.workplaceOrLocation.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = work.workplaceOrLocation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Work Time & Alarm Time Banner
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = work.formattedWorkTime(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Alarm time
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = if (work.isAlarmEnabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Alarm: ${work.formattedAlarmTime()}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (work.isAlarmEnabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Notes if any
            if (work.notes.isNotBlank()) {
                Text(
                    text = "Note: ${work.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer: Days & Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = work.formattedRepeatDays(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Medium
                )

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyWorksCard(isCompletedTab: Boolean, onAddClick: () -> Unit) {
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
                if (isCompletedTab) Icons.Default.CheckCircle else Icons.Default.WorkOutline,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isCompletedTab) "No Completed Works Yet" else "No Works Added Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isCompletedTab) "Completed works will appear here." else "Tap 'Add Work' to add your shifts, jobs, meetings, or tasks with customized alarms!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (!isCompletedTab) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onAddClick) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Your First Work")
                }
            }
        }
    }
}
