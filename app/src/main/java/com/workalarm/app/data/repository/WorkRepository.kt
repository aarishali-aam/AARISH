package com.workalarm.app.data.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.workalarm.app.data.model.WorkCategory
import com.workalarm.app.data.model.WorkItem
import com.workalarm.app.data.model.WorkPriority
import com.workalarm.app.receiver.AlarmReceiver
import com.workalarm.app.ui.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

class WorkRepository(private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences("user_works_prefs", Context.MODE_PRIVATE)
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    private val _works = MutableStateFlow<List<WorkItem>>(emptyList())
    val works: StateFlow<List<WorkItem>> = _works.asStateFlow()

    init {
        loadWorks()
    }

    fun loadWorks() {
        val jsonString = sharedPrefs.getString("saved_works", null)
        val list = if (!jsonString.isNullOrEmpty()) {
            try {
                deserializeWorks(jsonString)
            } catch (e: Exception) {
                Log.e("WorkRepository", "Error deserializing works", e)
                createDefaultWorks()
            }
        } else {
            createDefaultWorks()
        }
        _works.value = list
        // Register active alarms with Android AlarmManager
        list.filter { it.isAlarmEnabled && !it.isCompleted }.forEach { scheduleSystemAlarm(it) }
    }

    private fun createDefaultWorks(): List<WorkItem> {
        val defaults = listOf(
            WorkItem(
                id = "default_work_1",
                title = "Morning Warehouse Shift",
                workplaceOrLocation = "Central Logistics Hub",
                category = WorkCategory.SHIFT,
                startHour = 6,
                startMinute = 0,
                endHour = 14,
                endMinute = 30,
                leadTimeMinutes = 60, // Alarm rings at 05:00 AM (1 hr before)
                repeatDays = setOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY),
                isAlarmEnabled = true,
                priority = WorkPriority.HIGH,
                notes = "Wear safety boots and bring ID badge"
            ),
            WorkItem(
                id = "default_work_2",
                title = "Client Progress Meeting",
                workplaceOrLocation = "Conference Room 3 / Online",
                category = WorkCategory.MEETING,
                startHour = 10,
                startMinute = 0,
                endHour = 11,
                endMinute = 0,
                leadTimeMinutes = 15, // Alarm rings at 09:45 AM (15 min before)
                repeatDays = setOf(Calendar.TUESDAY, Calendar.THURSDAY),
                isAlarmEnabled = true,
                priority = WorkPriority.MEDIUM,
                notes = "Review Q3 delivery milestone report"
            ),
            WorkItem(
                id = "default_work_3",
                title = "Inventory & Store Closing",
                workplaceOrLocation = "Retail Branch A",
                category = WorkCategory.OFFICE,
                startHour = 17,
                startMinute = 0,
                endHour = 20,
                endMinute = 0,
                leadTimeMinutes = 30, // Alarm rings at 04:30 PM (30 min before)
                repeatDays = setOf(Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.FRIDAY),
                isAlarmEnabled = false,
                priority = WorkPriority.NORMAL,
                notes = "Count registers and lock safe"
            )
        )
        saveWorks(defaults)
        return defaults
    }

    fun addWork(work: WorkItem) {
        val current = _works.value.toMutableList()
        current.add(0, work) // Insert at beginning
        saveWorks(current)
        if (work.isAlarmEnabled && !work.isCompleted) {
            scheduleSystemAlarm(work)
        }
    }

    fun updateWork(work: WorkItem) {
        val current = _works.value.toMutableList()
        val index = current.indexOfFirst { it.id == work.id }
        if (index != -1) {
            current[index] = work
            saveWorks(current)
            if (work.isAlarmEnabled && !work.isCompleted) {
                scheduleSystemAlarm(work)
            } else {
                cancelSystemAlarm(work)
            }
        }
    }

    fun deleteWork(workId: String) {
        val current = _works.value.toMutableList()
        val work = current.find { it.id == workId }
        if (work != null) {
            cancelSystemAlarm(work)
            current.remove(work)
            saveWorks(current)
        }
    }

    fun toggleAlarm(workId: String, isEnabled: Boolean) {
        val current = _works.value.toMutableList()
        val index = current.indexOfFirst { it.id == workId }
        if (index != -1) {
            val updated = current[index].copy(isAlarmEnabled = isEnabled)
            current[index] = updated
            saveWorks(current)
            if (isEnabled && !updated.isCompleted) {
                scheduleSystemAlarm(updated)
            } else {
                cancelSystemAlarm(updated)
            }
        }
    }

    fun toggleCompleted(workId: String, isCompleted: Boolean) {
        val current = _works.value.toMutableList()
        val index = current.indexOfFirst { it.id == workId }
        if (index != -1) {
            val updated = current[index].copy(isCompleted = isCompleted)
            current[index] = updated
            saveWorks(current)
            if (isCompleted) {
                cancelSystemAlarm(updated)
            } else if (updated.isAlarmEnabled) {
                scheduleSystemAlarm(updated)
            }
        }
    }

    private fun saveWorks(list: List<WorkItem>) {
        _works.value = list
        val json = serializeWorks(list)
        sharedPrefs.edit().putString("saved_works", json).apply()
    }

    fun scheduleSystemAlarm(work: WorkItem) {
        if (alarmManager == null || !work.isAlarmEnabled) return

        val (alarmHour, alarmMinute) = work.calculateAlarmTime()
        val triggerTimeMs = calculateNextTriggerTime(alarmHour, alarmMinute, work.repeatDays)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, work.id)
            putExtra(AlarmReceiver.EXTRA_ALARM_LABEL, work.title)
            putExtra(AlarmReceiver.EXTRA_SHIFT_NAME, work.category.displayName)
            putExtra(AlarmReceiver.EXTRA_WORK_LOCATION, work.workplaceOrLocation)
            putExtra(AlarmReceiver.EXTRA_WORK_TIME, work.formattedWorkTime())
            putExtra(AlarmReceiver.EXTRA_WORK_NOTES, work.notes)
            putExtra(AlarmReceiver.EXTRA_SHIFT_COLOR, work.category.colorHex)
            putExtra(AlarmReceiver.EXTRA_VIBRATE, work.isVibrate)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            work.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            work.id.hashCode(),
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTimeMs, showPendingIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            Log.d("WorkRepository", "Scheduled alarm for work '${work.title}' at $triggerTimeMs")
        } catch (e: SecurityException) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            } catch (ex: Exception) {
                Log.e("WorkRepository", "Error setting alarm", ex)
            }
        }
    }

    fun cancelSystemAlarm(work: WorkItem) {
        if (alarmManager == null) return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            work.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
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
            if (calendar.before(now)) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            return calendar.timeInMillis
        }

        for (i in 0..7) {
            val checkDay = calendar.get(Calendar.DAY_OF_WEEK)
            if (daysOfWeek.contains(checkDay) && calendar.after(now)) {
                return calendar.timeInMillis
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }

    private fun serializeWorks(works: List<WorkItem>): String {
        val array = JSONArray()
        for (w in works) {
            val obj = JSONObject().apply {
                put("id", w.id)
                put("title", w.title)
                put("workplaceOrLocation", w.workplaceOrLocation)
                put("category", w.category.name)
                put("startHour", w.startHour)
                put("startMinute", w.startMinute)
                put("endHour", w.endHour)
                put("endMinute", w.endMinute)
                put("leadTimeMinutes", w.leadTimeMinutes)
                put("isAlarmEnabled", w.isAlarmEnabled)
                put("isCompleted", w.isCompleted)
                put("priority", w.priority.name)
                put("notes", w.notes)
                val daysArr = JSONArray()
                w.repeatDays.forEach { daysArr.put(it) }
                put("repeatDays", daysArr)
            }
            array.put(obj)
        }
        return array.toString()
    }

    private fun deserializeWorks(json: String): List<WorkItem> {
        val list = mutableListOf<WorkItem>()
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val daysSet = mutableSetOf<Int>()
            val daysArr = obj.optJSONArray("repeatDays")
            if (daysArr != null) {
                for (j in 0 until daysArr.length()) {
                    daysSet.add(daysArr.getInt(j))
                }
            }
            list.add(
                WorkItem(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    workplaceOrLocation = obj.optString("workplaceOrLocation", ""),
                    category = WorkCategory.valueOf(obj.optString("category", WorkCategory.GENERAL.name)),
                    startHour = obj.getInt("startHour"),
                    startMinute = obj.getInt("startMinute"),
                    endHour = obj.getInt("endHour"),
                    endMinute = obj.getInt("endMinute"),
                    leadTimeMinutes = obj.getInt("leadTimeMinutes"),
                    isAlarmEnabled = obj.getBoolean("isAlarmEnabled"),
                    isCompleted = obj.optBoolean("isCompleted", false),
                    priority = WorkPriority.valueOf(obj.optString("priority", WorkPriority.NORMAL.name)),
                    notes = obj.optString("notes", ""),
                    repeatDays = daysSet
                )
            )
        }
        return list
    }
}
