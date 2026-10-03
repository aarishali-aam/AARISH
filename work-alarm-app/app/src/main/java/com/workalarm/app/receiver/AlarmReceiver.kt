package com.workalarm.app.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.workalarm.app.R
import com.workalarm.app.WorkAlarmApp
import com.workalarm.app.service.AlarmService
import com.workalarm.app.ui.screens.AlarmRingingActivity

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TRIGGER_ALARM = "com.workalarm.app.ACTION_TRIGGER_ALARM"
        const val ACTION_DISMISS_ALARM = "com.workalarm.app.ACTION_DISMISS_ALARM"
        const val ACTION_SNOOZE_ALARM = "com.workalarm.app.ACTION_SNOOZE_ALARM"

        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_ALARM_LABEL = "extra_alarm_label"
        const val EXTRA_SHIFT_NAME = "extra_shift_name"
        const val EXTRA_SHIFT_COLOR = "extra_shift_color"
        const val EXTRA_WORK_LOCATION = "extra_work_location"
        const val EXTRA_WORK_TIME = "extra_work_time"
        const val EXTRA_WORK_NOTES = "extra_work_notes"
        const val EXTRA_VIBRATE = "extra_vibrate"

        const val NOTIFICATION_ID = 1001
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        when (action) {
            ACTION_TRIGGER_ALARM -> {
                val alarmId = intent.getStringExtra(EXTRA_ALARM_ID) ?: ""
                val label = intent.getStringExtra(EXTRA_ALARM_LABEL) ?: "Work Alarm"
                val shiftName = intent.getStringExtra(EXTRA_SHIFT_NAME) ?: "Work"
                val location = intent.getStringExtra(EXTRA_WORK_LOCATION) ?: ""
                val workTime = intent.getStringExtra(EXTRA_WORK_TIME) ?: ""
                val notes = intent.getStringExtra(EXTRA_WORK_NOTES) ?: ""
                val shiftColor = intent.getLongExtra(EXTRA_SHIFT_COLOR, 0xFF3B82F6)
                val vibrate = intent.getBooleanExtra(EXTRA_VIBRATE, true)

                // 1. Start AlarmService (Foreground Audio playback)
                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    this.action = AlarmService.ACTION_START_ALARM
                    putExtra(EXTRA_ALARM_ID, alarmId)
                    putExtra(EXTRA_ALARM_LABEL, label)
                    putExtra(EXTRA_SHIFT_NAME, shiftName)
                    putExtra(EXTRA_VIBRATE, vibrate)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }

                // 2. Fullscreen Ringing Intent
                val fullScreenIntent = Intent(context, AlarmRingingActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(EXTRA_ALARM_ID, alarmId)
                    putExtra(EXTRA_ALARM_LABEL, label)
                    putExtra(EXTRA_SHIFT_NAME, shiftName)
                    putExtra(EXTRA_WORK_LOCATION, location)
                    putExtra(EXTRA_WORK_TIME, workTime)
                    putExtra(EXTRA_WORK_NOTES, notes)
                    putExtra(EXTRA_SHIFT_COLOR, shiftColor)
                }
                val fullScreenPendingIntent = PendingIntent.getActivity(
                    context,
                    alarmId.hashCode(),
                    fullScreenIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // 3. Notification Actions (Dismiss & Snooze)
                val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
                    this.action = ACTION_DISMISS_ALARM
                }
                val dismissPendingIntent = PendingIntent.getBroadcast(
                    context,
                    1,
                    dismissIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
                    this.action = ACTION_SNOOZE_ALARM
                }
                val snoozePendingIntent = PendingIntent.getBroadcast(
                    context,
                    2,
                    snoozeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // 4. Build Heads-Up Notification
                val notification = NotificationCompat.Builder(context, WorkAlarmApp.CHANNEL_ALARM_ID)
                    .setSmallIcon(R.drawable.ic_alarm)
                    .setContentTitle("Work Alarm: $label")
                    .setContentText(if (location.isNotBlank()) "Location: $location • $workTime" else "Time for $label!")
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .setFullScreenIntent(fullScreenPendingIntent, true)
                    .addAction(0, context.getString(R.string.action_dismiss), dismissPendingIntent)
                    .addAction(0, context.getString(R.string.action_snooze), snoozePendingIntent)
                    .build()

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, notification)
            }

            ACTION_DISMISS_ALARM -> {
                val stopIntent = Intent(context, AlarmService::class.java).apply {
                    this.action = AlarmService.ACTION_STOP_ALARM
                }
                context.startService(stopIntent)

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(NOTIFICATION_ID)
            }

            ACTION_SNOOZE_ALARM -> {
                val stopIntent = Intent(context, AlarmService::class.java).apply {
                    this.action = AlarmService.ACTION_STOP_ALARM
                }
                context.startService(stopIntent)

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(NOTIFICATION_ID)

                AlarmService.scheduleSnooze(context)
            }
        }
    }
}
