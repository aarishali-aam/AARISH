package com.workalarm.app.data.model

import java.util.Calendar
import java.util.UUID

data class WorkAlarm(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "Shift Wake-up",
    val shiftType: ShiftType = ShiftType.MORNING,
    val shiftStartHour: Int = 6,
    val shiftStartMinute: Int = 0,
    val leadTimeMinutes: Int = 90, // Prep + commute time before shift
    val daysOfWeek: Set<Int> = setOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY),
    val isEnabled: Boolean = true,
    val isVibrate: Boolean = true,
    val ringtoneUri: String? = null
) {
    /**
     * Calculates the actual alarm wake-up hour and minute based on shift start time minus lead time.
     */
    fun calculateAlarmTime(): Pair<Int, Int> {
        val totalShiftMinutes = shiftStartHour * 60 + shiftStartMinute
        var alarmMinutes = totalShiftMinutes - leadTimeMinutes
        if (alarmMinutes < 0) {
            alarmMinutes += 24 * 60 // Wrap to previous day
        }
        val hour = (alarmMinutes / 60) % 24
        val minute = alarmMinutes % 60
        return Pair(hour, minute)
    }

    fun formattedAlarmTime(): String {
        val (h, m) = calculateAlarmTime()
        return formatTime12h(h, m)
    }

    fun formattedShiftStartTime(): String {
        return formatTime12h(shiftStartHour, shiftStartMinute)
    }

    fun formattedDays(): String {
        if (daysOfWeek.size == 7) return "Every day"
        if (daysOfWeek.size == 5 &&
            daysOfWeek.contains(Calendar.MONDAY) &&
            daysOfWeek.contains(Calendar.TUESDAY) &&
            daysOfWeek.contains(Calendar.WEDNESDAY) &&
            daysOfWeek.contains(Calendar.THURSDAY) &&
            daysOfWeek.contains(Calendar.FRIDAY)
        ) {
            return "Weekdays (Mon-Fri)"
        }
        if (daysOfWeek.isEmpty()) return "Once (Tomorrow / Today)"

        val dayNames = mapOf(
            Calendar.MONDAY to "Mon",
            Calendar.TUESDAY to "Tue",
            Calendar.WEDNESDAY to "Wed",
            Calendar.THURSDAY to "Thu",
            Calendar.FRIDAY to "Fri",
            Calendar.SATURDAY to "Sat",
            Calendar.SUNDAY to "Sun"
        )
        val orderedDays = listOf(
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
        )
        return orderedDays.filter { daysOfWeek.contains(it) }.joinToString(" ") { dayNames[it] ?: "" }
    }

    private fun formatTime12h(hour: Int, minute: Int): String {
        val amPm = if (hour >= 12) "PM" else "AM"
        val hour12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format("%02d:%02d %s", hour12, minute, amPm)
    }
}
