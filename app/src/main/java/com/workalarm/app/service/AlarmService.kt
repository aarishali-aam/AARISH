package com.workalarm.app.service

import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.workalarm.app.R
import com.workalarm.app.WorkAlarmApp
import com.workalarm.app.receiver.AlarmReceiver
import com.workalarm.app.ui.screens.AlarmRingingActivity

class AlarmService : Service() {

    companion object {
        const val ACTION_START_ALARM = "com.workalarm.app.ACTION_START_ALARM"
        const val ACTION_STOP_ALARM = "com.workalarm.app.ACTION_STOP_ALARM"
        private const val NOTIFICATION_ID = 2001

        fun scheduleSnooze(context: Context, snoozeMinutes: Int = 10) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val triggerTime = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_TRIGGER_ALARM
                putExtra(AlarmReceiver.EXTRA_ALARM_LABEL, "Snoozed Work Alarm")
                putExtra(AlarmReceiver.EXTRA_SHIFT_NAME, "Work Shift")
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                9999,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            Log.d("AlarmService", "Snoozed alarm scheduled for $snoozeMinutes minutes later")
        }
    }

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "WorkAlarm::AlarmServiceWakeLock"
        ).apply {
            setReferenceCounted(false)
        }

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_START_ALARM -> {
                wakeLock?.acquire(10 * 60 * 1000L) // Max 10 minutes timeout
                val label = intent.getStringExtra(AlarmReceiver.EXTRA_ALARM_LABEL) ?: "Work Alarm"
                val shiftName = intent.getStringExtra(AlarmReceiver.EXTRA_SHIFT_NAME) ?: "Shift"
                val shouldVibrate = intent.getBooleanExtra(AlarmReceiver.EXTRA_VIBRATE, true)

                startForeground(NOTIFICATION_ID, buildForegroundNotification(label, shiftName))
                startAlarmMedia()
                if (shouldVibrate) {
                    startVibration()
                }
            }
            ACTION_STOP_ALARM -> {
                stopAlarmMedia()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun startAlarmMedia() {
        try {
            stopAlarmMedia()
            var alertUri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            if (alertUri == null) {
                alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, alertUri!!)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setLegacyStreamType(AudioManager.STREAM_ALARM)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("AlarmService", "Failed to start media player", e)
        }
    }

    private fun startVibration() {
        try {
            val pattern = longArrayOf(0, 800, 400, 800, 400, 800)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(pattern, 0)
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e("AlarmService", "Failed to vibrate", e)
        }
    }

    private fun stopAlarmMedia() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e("AlarmService", "Error stopping media player", e)
        }

        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e("AlarmService", "Error cancelling vibration", e)
        }

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.e("AlarmService", "Error releasing wakelock", e)
        }
    }

    private fun buildForegroundNotification(label: String, shiftName: String): Notification {
        val dismissIntent = Intent(this, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_DISMISS_ALARM
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this,
            10,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(this, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_SNOOZE_ALARM
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            this,
            11,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, WorkAlarmApp.CHANNEL_ALARM_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("Work Alarm Ringing: $label")
            .setContentText("Shift: $shiftName - Wake up and prepare!")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .addAction(0, getString(R.string.action_dismiss), dismissPendingIntent)
            .addAction(0, getString(R.string.action_snooze), snoozePendingIntent)
            .build()
    }

    override fun onDestroy() {
        stopAlarmMedia()
        super.onDestroy()
    }
}
