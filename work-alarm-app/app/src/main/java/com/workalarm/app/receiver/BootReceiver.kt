package com.workalarm.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.workalarm.app.data.repository.AlarmRepository

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d("BootReceiver", "Device reboot detected. Rescheduling all active work alarms...")
            try {
                val repository = AlarmRepository(context)
                repository.loadAlarms() // reloads and registers with AlarmManager
            } catch (e: Exception) {
                Log.e("BootReceiver", "Error rescheduling alarms on boot", e)
            }
        }
    }
}
