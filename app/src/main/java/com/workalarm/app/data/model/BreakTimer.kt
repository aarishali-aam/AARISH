package com.workalarm.app.data.model

data class BreakPreset(
    val id: String,
    val name: String,
    val durationMinutes: Int,
    val iconName: String,
    val description: String
)

object BreakPresets {
    val presets = listOf(
        BreakPreset(
            id = "pomodoro_focus",
            name = "Deep Focus Work",
            durationMinutes = 25,
            iconName = "Psychology",
            description = "Intense distraction-free work block"
        ),
        BreakPreset(
            id = "short_break",
            name = "Micro Break",
            durationMinutes = 5,
            iconName = "LocalCafe",
            description = "Rest eyes, stand up, take deep breaths"
        ),
        BreakPreset(
            id = "tea_break",
            name = "Tea / Coffee Break",
            durationMinutes = 15,
            iconName = "FreeBreakfast",
            description = "Quick recharge & hydration"
        ),
        BreakPreset(
            id = "lunch_break",
            name = "Meal / Lunch Break",
            durationMinutes = 45,
            iconName = "Restaurant",
            description = "Clock out, eat, and return on time"
        ),
        BreakPreset(
            id = "stretch_hourly",
            name = "Hourly Stretch",
            durationMinutes = 60,
            iconName = "AccessibilityNew",
            description = "Ergonomic check: stretch spine and neck"
        )
    )
}

enum class TimerStatus {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED
}

data class TimerState(
    val preset: BreakPreset = BreakPresets.presets[0],
    val totalSeconds: Long = 25 * 60L,
    val remainingSeconds: Long = 25 * 60L,
    val status: TimerStatus = TimerStatus.IDLE
) {
    val progress: Float
        get() = if (totalSeconds > 0) (remainingSeconds.toFloat() / totalSeconds.toFloat()) else 0f

    fun formattedRemaining(): String {
        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}
