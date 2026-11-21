package com.example.domain

enum class ReminderSlot { MORNING, EVENING }

enum class ReminderType { MORNING_PLAN, EVENING_STREAK, COMEBACK }

data class ReminderInput(
    val slot: ReminderSlot,
    val enabled: Boolean,
    val vacation: Boolean,
    val nowMinutes: Int,
    val quietStartMinutes: Int, // bedtime
    val quietEndMinutes: Int, // wake time
    val done: Int,
    val total: Int,
    val streak: Int,
    val idleDays: Int, // days since the last day with any routine progress
    val comebackSent: Int, // comeback messages already sent in this break
    val sentToday: Set<ReminderType>
)

/** Decides whether a reminder goes out and which one. At most one per alarm slot, at most two a day. */
object ReminderPolicy {
    const val DAILY_CAP = 2
    const val COMEBACK_CAP = 2
    const val IDLE_DAYS_FOR_COMEBACK = 2
    const val COMEBACK_SPACING_DAYS = 3

    /** Quiet hours run from bedtime to wake time and may cross midnight. */
    fun isQuiet(now: Int, start: Int, end: Int): Boolean = when {
        start == end -> false
        start < end -> now in start until end
        else -> now >= start || now < end
    }

    fun decide(i: ReminderInput): ReminderType? {
        if (!i.enabled || i.vacation) return null
        if (isQuiet(i.nowMinutes, i.quietStartMinutes, i.quietEndMinutes)) return null
        if (i.sentToday.size >= DAILY_CAP) return null
        if (i.total > 0 && DayCompletion.of(i.done, i.total).qualifies) return null // the day already counts
        val candidate = when (i.slot) {
            ReminderSlot.MORNING -> if (comebackDue(i)) ReminderType.COMEBACK else if (i.total > 0) ReminderType.MORNING_PLAN else null
            ReminderSlot.EVENING -> if (i.total > 0 && i.streak >= 1) ReminderType.EVENING_STREAK else null
        }
        return candidate?.takeIf { it !in i.sentToday }
    }

    // First after 2 idle days, the next after 5, then no more until the user is back
    private fun comebackDue(i: ReminderInput) =
        i.comebackSent < COMEBACK_CAP && i.idleDays >= IDLE_DAYS_FOR_COMEBACK + COMEBACK_SPACING_DAYS * i.comebackSent
}

/** Notification wording: plain, no guilt, no countdown pressure. */
object ReminderCopy {
    fun title(type: ReminderType) = when (type) {
        ReminderType.MORNING_PLAN -> "Your day is ready"
        ReminderType.EVENING_STREAK -> "Your streak, if you want it"
        ReminderType.COMEBACK -> "Welcome back whenever you are ready"
    }

    fun body(type: ReminderType, total: Int, itemsToGo: Int, streak: Int) = when (type) {
        ReminderType.MORNING_PLAN -> "$total things on today's list. Start with whichever feels easy."
        ReminderType.EVENING_STREAK -> "$itemsToGo more tick${if (itemsToGo == 1) "" else "s"} keeps your $streak-day streak going."
        ReminderType.COMEBACK -> "Your level and best streak are safe. One small tick is a fine restart."
    }
}
