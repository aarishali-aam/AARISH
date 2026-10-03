package com.workalarm.app.data.model

import androidx.compose.ui.graphics.Color

enum class ShiftType(
    val displayName: String,
    val defaultStartHour: Int,
    val defaultStartMinute: Int,
    val defaultEndHour: Int,
    val defaultEndMinute: Int,
    val colorHex: Long,
    val badgeIconName: String
) {
    MORNING(
        displayName = "Morning Shift",
        defaultStartHour = 6,
        defaultStartMinute = 0,
        defaultEndHour = 14,
        defaultEndMinute = 30,
        colorHex = 0xFFF59E0B, // Amber
        badgeIconName = "WbSunny"
    ),
    DAY(
        displayName = "Day / Standard",
        defaultStartHour = 9,
        defaultStartMinute = 0,
        defaultEndHour = 17,
        defaultEndMinute = 30,
        colorHex = 0xFF3B82F6, // Blue
        badgeIconName = "Work"
    ),
    EVENING(
        displayName = "Evening Shift",
        defaultStartHour = 14,
        defaultStartMinute = 0,
        defaultEndHour = 22,
        defaultEndMinute = 30,
        colorHex = 0xFFEC4899, // Pink
        badgeIconName = "BrightnessMedium"
    ),
    NIGHT(
        displayName = "Night Shift",
        defaultStartHour = 22,
        defaultStartMinute = 0,
        defaultEndHour = 6,
        defaultEndMinute = 30,
        colorHex = 0xFF8B5CF6, // Purple
        badgeIconName = "NightsStay"
    ),
    OFF(
        displayName = "Rest / Off Day",
        defaultStartHour = 0,
        defaultStartMinute = 0,
        defaultEndHour = 0,
        defaultEndMinute = 0,
        colorHex = 0xFF64748B, // Slate Grey
        badgeIconName = "Weekend"
    );

    fun getColor(): Color = Color(colorHex)

    fun formattedDefaultTime(): String {
        if (this == OFF) return "No Work Scheduled"
        val startH = String.format("%02d", defaultStartHour)
        val startM = String.format("%02d", defaultStartMinute)
        val endH = String.format("%02d", defaultEndHour)
        val endM = String.format("%02d", defaultEndMinute)
        return "$startH:$startM - $endH:$endM"
    }
}
