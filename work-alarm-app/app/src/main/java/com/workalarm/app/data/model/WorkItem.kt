package com.workalarm.app.data.model

import androidx.compose.ui.graphics.Color
import java.util.Calendar
import java.util.UUID

enum class WorkCategory(val displayName: String, val colorHex: Long, val iconName: String) {
    GENERAL("General Work", 0xFF3B82F6, "Work"),          // Blue
    OFFICE("Office / Corporate", 0xFF6366F1, "Business"), // Indigo
    SHIFT("Shift Job", 0xFFF59E0B, "Schedule"),           // Amber
    CONSTRUCTION("Field / Site", 0xFFF97316, "Build"),    // Orange
    MEETING("Meeting / Call", 0xFF8B5CF6, "Groups"),      // Purple
    DELIVERY("Delivery / Travel", 0xFF10B981, "LocalShipping"), // Emerald
    SIDE_GIG("Side Gig / Freelance", 0xFFEC4899, "Computer"); // Pink

    fun getColor(): Color = Color(colorHex)
}

enum class WorkPriority(val displayName: String, val colorHex: Long) {
    HIGH("High Priority", 0xFFEF4444),
    MEDIUM("Medium", 0xFFF59E0B),
    NORMAL("Normal", 0xFF10B981)
}

data class WorkItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,                               // e.g. "Morning Warehouse Shift", "Client Presentation"
    val workplaceOrLocation: String = "",            // e.g. "Building B", "Downtown Store"
    val category: WorkCategory = WorkCategory.GENERAL,
    val startHour: Int = 9,
    val startMinute: Int = 0,
    val endHour: Int = 17,
    val endMinute: Int = 0,
    val leadTimeMinutes: Int = 30,                   // Alarm rings X minutes before work starts (0 = exact start, 15, 30, 60, 90)
    val repeatDays: Set<Int> = setOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY),
    val isAlarmEnabled: Boolean = true,
    val isCompleted: Boolean = false,
    val priority: WorkPriority = WorkPriority.NORMAL,
    val notes: String = "",
    val checklistItems: List<String> = emptyList(),
    val isVibrate: Boolean = true
) {
    /**
     * Calculates the exact hour and minute the alarm will ring based on work start time minus lead time.
     */
    fun calculateAlarmTime(): Pair<Int, Int> {
        val totalWorkMinutes = startHour * 60 + startMinute
        var alarmMinutes = totalWorkMinutes - leadTimeMinutes
        if (alarmMinutes < 0) {
            alarmMinutes += 24 * 60 // Wraps to previous day
        }
        val hour = (alarmMinutes / 60) % 24
        val minute = alarmMinutes % 60
        return Pair(hour, minute)
    }

    fun formattedWorkTime(): String {
        return "${formatTime12h(startHour, startMinute)} - ${formatTime12h(endHour, endMinute)}"
    }

    fun formattedAlarmTime(): String {
        val (h, m) = calculateAlarmTime()
        return formatTime12h(h, m)
    }

    fun formattedRepeatDays(): String {
        if (repeatDays.isEmpty()) return "Once (Today / Upcoming)"
        if (repeatDays.size == 7) return "Every day"
        if (repeatDays.size == 5 &&
            repeatDays.contains(Calendar.MONDAY) &&
            repeatDays.contains(Calendar.TUESDAY) &&
            repeatDays.contains(Calendar.WEDNESDAY) &&
            repeatDays.contains(Calendar.THURSDAY) &&
            repeatDays.contains(Calendar.FRIDAY)
        ) {
            return "Mon - Fri"
        }
        val names = mapOf(
            Calendar.MONDAY to "Mon",
            Calendar.TUESDAY to "Tue",
            Calendar.WEDNESDAY to "Wed",
            Calendar.THURSDAY to "Thu",
            Calendar.FRIDAY to "Fri",
            Calendar.SATURDAY to "Sat",
            Calendar.SUNDAY to "Sun"
        )
        val ordered = listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY)
        return ordered.filter { repeatDays.contains(it) }.joinToString(" ") { names[it] ?: "" }
    }

    private fun formatTime12h(h: Int, m: Int): String {
        val amPm = if (h >= 12) "PM" else "AM"
        val h12 = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        return String.format("%02d:%02d %s", h12, m, amPm)
    }
}
