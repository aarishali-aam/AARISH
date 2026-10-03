package com.workalarm.app.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.workalarm.app.R
import com.workalarm.app.WorkAlarmApp
import com.workalarm.app.data.model.BreakPreset
import com.workalarm.app.data.model.BreakPresets
import com.workalarm.app.data.model.TimerState
import com.workalarm.app.data.model.TimerStatus
import com.workalarm.app.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BreakTimerService : Service() {

    companion object {
        const val ACTION_START_TIMER = "com.workalarm.app.ACTION_START_TIMER"
        const val ACTION_PAUSE_TIMER = "com.workalarm.app.ACTION_PAUSE_TIMER"
        const val ACTION_RESUME_TIMER = "com.workalarm.app.ACTION_RESUME_TIMER"
        const val ACTION_STOP_TIMER = "com.workalarm.app.ACTION_STOP_TIMER"

        const val EXTRA_PRESET_ID = "extra_preset_id"
        const val EXTRA_CUSTOM_MINUTES = "extra_custom_minutes"

        private const val NOTIFICATION_ID = 3001

        private val _timerState = MutableStateFlow(TimerState())
        val timerState: StateFlow<TimerState> = _timerState.asStateFlow()
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var countdownJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_START_TIMER -> {
                val presetId = intent.getStringExtra(EXTRA_PRESET_ID)
                val customMinutes = intent.getIntExtra(EXTRA_CUSTOM_MINUTES, -1)

                val preset = BreakPresets.presets.find { it.id == presetId }
                    ?: BreakPreset(
                        id = "custom",
                        name = "Custom Timer",
                        durationMinutes = if (customMinutes > 0) customMinutes else 15,
                        iconName = "Timer",
                        description = "Custom workplace countdown"
                    )

                val totalSec = preset.durationMinutes * 60L
                _timerState.value = TimerState(
                    preset = preset,
                    totalSeconds = totalSec,
                    remainingSeconds = totalSec,
                    status = TimerStatus.RUNNING
                )

                startForeground(NOTIFICATION_ID, buildTimerNotification())
                startCountdown()
            }

            ACTION_PAUSE_TIMER -> {
                countdownJob?.cancel()
                _timerState.value = _timerState.value.copy(status = TimerStatus.PAUSED)
                updateNotification()
            }

            ACTION_RESUME_TIMER -> {
                if (_timerState.value.status == TimerStatus.PAUSED) {
                    _timerState.value = _timerState.value.copy(status = TimerStatus.RUNNING)
                    startCountdown()
                }
            }

            ACTION_STOP_TIMER -> {
                countdownJob?.cancel()
                _timerState.value = _timerState.value.copy(
                    remainingSeconds = _timerState.value.totalSeconds,
                    status = TimerStatus.IDLE
                )
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = serviceScope.launch {
            while (_timerState.value.remainingSeconds > 0 && _timerState.value.status == TimerStatus.RUNNING) {
                delay(1000L)
                val nextSec = _timerState.value.remainingSeconds - 1
                _timerState.value = _timerState.value.copy(remainingSeconds = nextSec)
                updateNotification()
            }

            if (_timerState.value.remainingSeconds <= 0) {
                onTimerFinished()
            }
        }
    }

    private fun onTimerFinished() {
        _timerState.value = _timerState.value.copy(status = TimerStatus.COMPLETED)
        playCompletionAlert()
        showCompletionNotification()
        stopForeground(STOP_FOREGROUND_DETACH)
    }

    private fun playCompletionAlert() {
        try {
            val alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(applicationContext, alert)
            ringtone?.play()
        } catch (_: Exception) {}

        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(800, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(800)
            }
        } catch (_: Exception) {}
    }

    private fun buildTimerNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            201,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, BreakTimerService::class.java).apply {
            action = ACTION_STOP_TIMER
        }
        val stopPending = PendingIntent.getService(
            this,
            202,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val state = _timerState.value
        return NotificationCompat.Builder(this, WorkAlarmApp.CHANNEL_BREAK_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("${state.preset.name}: ${state.formattedRemaining()}")
            .setContentText("Tap to open timer controls")
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(0, "Stop", stopPending)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildTimerNotification())
    }

    private fun showCompletionNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            203,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(this, WorkAlarmApp.CHANNEL_BREAK_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("${_timerState.value.preset.name} Complete!")
            .setContentText("Your break is done. Time to get back to work!")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        manager.notify(NOTIFICATION_ID, notif)
    }

    override fun onDestroy() {
        countdownJob?.cancel()
        super.onDestroy()
    }
}
