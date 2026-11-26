package com.example.domain

/** The five fixed daily slots with fixed, calm wording: information, never guilt. */
enum class JourneySlot(val hour: Int, val body: String) {
    NINE(9, "Your routine is ready when you are"),
    TWELVE(12, "Midday check: how are today's tasks going?"),
    THREE(15, "A quick look at what's left today"),
    SIX(18, "Finish what's left to count today"),
    NINE_PM(21, "Tick what's done before the day ends");

    val minutes get() = hour * 60
    val label get() = when (hour) { 12 -> "12 PM"; in 13..23 -> "${hour - 12} PM"; else -> "$hour AM" }
}

/** What the user controls: a master switch, one toggle per slot and quiet hours (null = use bedtime to wake time). */
data class JourneyReminderSettings(
    val master: Boolean = false,
    val slots: Set<JourneySlot> = JourneySlot.values().toSet(),
    val quietStart: Int? = null,
    val quietEnd: Int? = null
)

data class JourneyReminderInput(
    val slot: JourneySlot,
    val journeyEnabled: Boolean,
    val settings: JourneyReminderSettings,
    val vacation: Boolean,
    val nowMinutes: Int,
    val defaultQuietStart: Int, // bedtime
    val defaultQuietEnd: Int, // wake time
    val done: Int,
    val total: Int,
    val sentToday: Set<JourneySlot>
)

object JourneyReminderPolicy {
    /** True when the reminder should be posted: every rule must allow it. */
    fun shouldPost(i: JourneyReminderInput): Boolean {
        val s = i.settings
        if (!i.journeyEnabled || !s.master || i.slot !in s.slots || i.vacation || i.slot in i.sentToday) return false
        if (ReminderPolicy.isQuiet(i.nowMinutes, s.quietStart ?: i.defaultQuietStart, s.quietEnd ?: i.defaultQuietEnd)) return false
        return i.total > 0 && !DayCompletion.of(i.done, i.total).qualifies // nothing to remind about, or the day already counts
    }

    /** "22:30" to minutes, or null when it is not a time. */
    fun parseTime(text: String): Int? {
        val m = Regex("^([01]?\\d|2[0-3]):([0-5]\\d)$").matchEntire(text.trim()) ?: return null
        return m.groupValues[1].toInt() * 60 + m.groupValues[2].toInt()
    }

    fun formatTime(minutes: Int) = "%02d:%02d".format(minutes / 60, minutes % 60)
}
