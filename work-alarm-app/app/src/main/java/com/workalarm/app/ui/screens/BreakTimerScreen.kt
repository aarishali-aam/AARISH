package com.workalarm.app.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.workalarm.app.data.model.BreakPreset
import com.workalarm.app.data.model.BreakPresets
import com.workalarm.app.data.model.TimerState
import com.workalarm.app.data.model.TimerStatus
import com.workalarm.app.service.BreakTimerService

@Composable
fun BreakTimerScreen() {
    val context = LocalContext.current
    val timerState by BreakTimerService.timerState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Workplace Break & Focus Timers",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Track meal breaks, Pomodoro focus sessions, and stretch intervals",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Circular Timer Display
        item {
            CircularTimerWidget(timerState = timerState)
        }

        // Controls (Play / Pause / Reset)
        item {
            TimerControlButtons(
                timerState = timerState,
                onStart = {
                    startTimer(context, timerState.preset.id)
                },
                onPause = {
                    sendTimerAction(context, BreakTimerService.ACTION_PAUSE_TIMER)
                },
                onResume = {
                    sendTimerAction(context, BreakTimerService.ACTION_RESUME_TIMER)
                },
                onStop = {
                    sendTimerAction(context, BreakTimerService.ACTION_STOP_TIMER)
                }
            )
        }

        // Section Title: Quick Presets
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Break Presets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Preset items
        items(BreakPresets.presets, key = { it.id }) { preset ->
            val isSelected = timerState.preset.id == preset.id
            BreakPresetCard(
                preset = preset,
                isSelected = isSelected,
                onClick = {
                    startTimer(context, preset.id)
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun CircularTimerWidget(timerState: TimerState) {
    val animatedProgress by animateFloatAsState(
        targetValue = timerState.progress,
        label = "progress_animation"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = Modifier
            .size(260.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Track circle
            drawCircle(
                color = trackColor,
                style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round)
            )

            // Dynamic progress arc
            drawArc(
                color = primaryColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
            ) {
                Text(
                    text = timerState.preset.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = timerState.formattedRemaining(),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 50.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = when (timerState.status) {
                    TimerStatus.IDLE -> "Ready"
                    TimerStatus.RUNNING -> "In Progress"
                    TimerStatus.PAUSED -> "Paused"
                    TimerStatus.COMPLETED -> "Finished!"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TimerControlButtons(
    timerState: TimerState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Stop / Reset
        if (timerState.status != TimerStatus.IDLE) {
            FilledTonalIconButton(
                onClick = onStop,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.Stop, contentDescription = "Stop Timer", modifier = Modifier.size(28.dp))
            }
        }

        // Play / Pause Main Button
        FloatingActionButton(
            onClick = {
                when (timerState.status) {
                    TimerStatus.IDLE, TimerStatus.COMPLETED -> onStart()
                    TimerStatus.RUNNING -> onPause()
                    TimerStatus.PAUSED -> onResume()
                }
            },
            modifier = Modifier.size(68.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(
                imageVector = when (timerState.status) {
                    TimerStatus.RUNNING -> Icons.Default.Pause
                    else -> Icons.Default.PlayArrow
                },
                contentDescription = "Toggle Timer",
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
fun BreakPresetCard(
    preset: BreakPreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "${preset.durationMinutes}m",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = preset.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text("Start")
            }
        }
    }
}

private fun startTimer(context: Context, presetId: String) {
    val intent = Intent(context, BreakTimerService::class.java).apply {
        action = BreakTimerService.ACTION_START_TIMER
        putExtra(BreakTimerService.EXTRA_PRESET_ID, presetId)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}

private fun sendTimerAction(context: Context, actionStr: String) {
    val intent = Intent(context, BreakTimerService::class.java).apply {
        action = actionStr
    }
    context.startService(intent)
}
