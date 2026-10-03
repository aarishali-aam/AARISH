package com.workalarm.app.data.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.workalarm.app.data.model.ShiftType
import com.workalarm.app.data.model.WorkAlarm
import com.workalarm.app.receiver.AlarmReceiver
import com.workalarm.app.ui.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

class AlarmRepository(private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences("work_alarm_prefs", Context.MODE_PRIVATE)
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    private val _alarms = MutableStateFlow<List<WorkAlarm>>(emptyList())
    val alarms: StateFlow<List<WorkAlarm>> = _alarms.asStateFlow()

    // 7-day schedule mapping: Day of week (Calendar.SUNDAY..SATURDAY) -> ShiftType
    private val _schedule = MutableStateFlow<Map<Int, ShiftType>>(defaultSchedule())
    val schedule: StateFlow<Map<Int, ShiftType>> = _schedule.asStateFlow()

    init {
        loadAlarms()
        loadSchedule()
    }

    private fun defaultSchedule(): Map<Int, ShiftType> {
        return mapOf(
            Calendar.MONDAY to ShiftType.MORNING,
            Calendar.TUESDAY to ShiftType.MORNING,
            Calendar.WEDNESDAY to ShiftType.MORNING,
            Calendar.THURSDAY to ShiftType.MORNING,
            Calendar.FRIDAY to ShiftType.MORNING,
            Calendar.SATURDAY to ShiftType.OFF,
            Calendar.SUNDAY to ShiftType.OFF
        )
    }

    fun loadAlarms() {
        val jsonString = sharedPrefs.getString("saved_alarms", null)
        val loaded = if (!jsonString.isNullOrEmpty()) {
            try {
                deserializeAlarms(jsonString)
            } catch (e: Exception) {
                Log.e("AlarmRepository", "Failed to deserialize alarms", e)
                createDefaultAlarms()
            }
        } else {
            createDefaultAlarms()
        }
        _alarms.value = loaded
        // Reschedule active alarms
        loaded.filter { it.isEnabled }.forEach { scheduleSystemAlarm(it) }
    }

    private fun createDefaultAlarms(): List<WorkAlarm> {
        val list = listOf(
            WorkAlarm(
                id = "morning_shift_default",
                label = "Morning Shift Wake-Up",
                shiftType = ShiftType.MORNING,
                shiftStartHour = 6,
                shiftStartMinute = 0,
                leadTimeMinutes = 90, // Alarm rings at 04:30 AM
                daysOfWeek = setOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY),
                isEnabled = true
            ),
            WorkAlarm(
                id = "day_shift_default",
                label = "Standard Day Shift Wake-Up",
                shiftType = ShiftType.DAY,
                shiftStartHour = 9,
                shiftStartMinute = 0,
                leadTimeMinutes = 60, // Alarm rings at 08:00 AM
                daysOfWeek = setOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY),
                isEnabled = false
            ),
            WorkAlarm(
                id = "evening_shift_default",
                label = "Evening Shift Wake-Up",
                shiftType = ShiftType.EVENING,
                shiftStartHour = 14,
                shiftStartMinute = 0,
                leadTimeMinutes = 60, // Alarm rings at 01:00 PM
                daysOfWeek = setOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY),
                isEnabled = false
            ),
            WorkAlarm(
                id = "night_shift_default",
                label = "Night Shift Wake-Up",
                shiftType = ShiftType.NIGHT,
                shiftStartHour = 22,
                shiftStartMinute = 0,
                leadTimeMinutes = 90, // Alarm rings at 08:30 PM
                daysOfWeek = setOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY),
                isEnabled = false
            )
        )
        saveAlarms(list)
        return list
    }

    fun addAlarm(alarm: WorkAlarm) {
        val current = _alarms.value.toMutableList()
        current.add(alarm)
        saveAlarms(current)
        if (alarm.isEnabled) {
            scheduleSystemAlarm(alarm)
        }
    }

    fun updateAlarm(alarm: WorkAlarm) {
        val current = _alarms.value.toMutableList()
        val index = current.indexOfFirst { it.id == alarm.id }
        if (index != -1) {
            current[index] = alarm
            saveAlarms(current)
            if (alarm.isEnabled) {
                scheduleSystemAlarm(alarm)
            } else {
                cancelSystemAlarm(alarm)
            }
        }
    }

    fun toggleAlarm(id: String, enabled: Boolean) {
        val current = _alarms.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            val updated = current[index].copy(isEnabled = enabled)
            current[index] = updated
            saveAlarms(current)
            if (enabled) {
                scheduleSystemAlarm(updated)
            } else {
                cancelSystemAlarm(updated)
            }
        }
    }

    fun deleteAlarm(id: String) {
        val current = _alarms.value.toMutableList()
        val alarm = current.find { it.id == id }
        if (alarm != null) {
            cancelSystemAlarm(alarm)
            current.remove(alarm)
            saveAlarms(current)
        }
    }

    fun updateDayShift(dayOfWeek: Int, shiftType: ShiftType) {
        val map = _schedule.value.toMutableMap()
        map[dayOfWeek] = shiftType
        _schedule.value = map
        saveSchedule(map)
    }

    fun applyRotationPattern(pattern: List<ShiftType>, startDay: Int = Calendar.MONDAY) {
        if (pattern.isEmpty()) return
        val map = _schedule.value.toMutableMap()
        val daysOrder = listOf(
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
        )
        for (i in daysOrder.indices) {
            val day = daysOrder[i]
            val shift = pattern[i % pattern.size]
            map[day] = shift
        }
        _schedule.value = map
        saveSchedule(map)
    }

    private fun saveAlarms(list: List<WorkAlarm>) {
        _alarms.value = list
        val json = serializeAlarms(list)
        sharedPrefs.edit().putString("saved_alarms", json).apply()
    }

    private fun saveSchedule(scheduleMap: Map<Int, ShiftType>) {
        val json = JSONObject()
        scheduleMap.forEach { (day, shift) ->
            json.put(day.toString(), shift.name)
        }
        sharedPrefs.edit().putString("saved_schedule", json.toString()).apply()
    }

    private fun loadSchedule() {
        val jsonStr = sharedPrefs.getString("saved_schedule", null) ?: return
        try {
            val json = JSONObject(jsonStr)
            val map = mutableMapOf<Int, ShiftType>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val day = key.toInt()
                val shiftName = json.getString(key)
                val shift = ShiftType.valueOf(shiftName)
                map[day] = shift
            }
            _schedule.value = map
        } catch (e: Exception) {
            Log.e("AlarmRepository", "Failed to load schedule", e)
        }
    }

    fun scheduleSystemAlarm(alarm: WorkAlarm) {
        if (alarmManager == null || !alarm.isEnabled) return

        val (alarmHour, alarmMinute) = alarm.calculateAlarmTime()
        val triggerTimeMs = calculateNextTriggerTime(alarmHour, alarmMinute, alarm.daysOfWeek)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmReceiver.EXTRA_ALARM_LABEL, alarm.label)
            putExtra(AlarmReceiver.EXTRA_SHIFT_NAME, alarm.shiftType.displayName)
            putExtra(AlarmReceiver.EXTRA_SHIFT_COLOR, alarm.shiftType.colorHex)
            putExtra(AlarmReceiver.EXTRA_VIBRATE, alarm.isVibrate)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            alarm.id.hashCode(),
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTimeMs, showPendingIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            Log.d("AlarmRepository", "Scheduled alarm ${alarm.label} at $triggerTimeMs")
        } catch (e: SecurityException) {
            // Android 12/13+ exact alarm permission fallback
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
            } catch (ex: Exception) {
                Log.e("AlarmRepository", "Cannot schedule alarm due to permissions", ex)
            }
        }
    }

    fun cancelSystemAlarm(alarm: WorkAlarm) {
        if (alarmManager == null) return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d("AlarmRepository", "Cancelled alarm ${alarm.label}")
    }

    private fun calculateNextTriggerTime(hour: Int, minute: Int, daysOfWeek: Set<Int>): Long {
        val now = Calendar.getInstance()
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (daysOfWeek.isEmpty()) {
            // One-off alarm: if time has passed today, schedule for tomorrow
            if (calendar.before(now)) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            return calendar.timeInMillis
        }

        // Repeating on days of week: find the next matching day
        for (i in 0..7) {
            val checkDay = calendar.get(Calendar.DAY_OF_WEEK)
            if (daysOfWeek.contains(checkDay) && calendar.after(now)) {
                return calendar.timeInMillis
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return calendar.timeInMillis
    }

    private fun serializeAlarms(alarms: List<WorkAlarm>): String {
        val array = JSONArray()
        for (alarm in alarms) {
            val obj = JSONObject().apply {
                put("id", alarm.id)
                put("label", alarm.label)
                put("shiftType", alarm.shiftType.name)
                put("shiftStartHour", alarm.shiftStartHour)
                put("shiftStartMinute", alarm.shiftStartMinute)
                put("leadTimeMinutes", alarm.leadTimeMinutes)
                put("isEnabled", alarm.isEnabled)
                put("isVibrate", alarm.isVibrate)
                val daysArr = JSONArray()
                alarm.daysOfWeek.forEach { daysArr.put(it) }
                put("daysOfWeek", daysArr)
            }
            array.put(obj)
        }
        return array.toString()
    }

    private fun deserializeAlarms(json: String): List<WorkAlarm> {
        val list = mutableListOf<WorkAlarm>()
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val daysSet = mutableSetOf<Int>()
            val daysArr = obj.optJSONArray("daysOfWeek")
            if (daysArr != null) {
                for (j in 0 until daysArr.length()) {
                    daysSet.add(daysArr.getInt(j))
                }
            }
            list.add(
                WorkAlarm(
                    id = obj.getString("id"),
                    label = obj.getString("label"),
                    shiftType = ShiftType.valueOf(obj.getString("shiftType")),
                    shiftStartHour = obj.getInt("shiftStartHour"),
                    shiftStartMinute = obj.getInt("shiftStartMinute"),
                    leadTimeMinutes = obj.getInt("leadTimeMinutes"),
                    isEnabled = obj.getBoolean("isEnabled"),
                    isVibrate = obj.optBoolean("isVibrate", true),
                    daysOfWeek = daysSet
                )
            )
        }
        return list
    }
}
