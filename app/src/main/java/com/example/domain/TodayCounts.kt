package com.example.domain

/** The small celebration when a day reaches 80%: a toast, once a day, never during vacation. */
object TodayCounts {
    const val MESSAGE = "Today counts. Streak safe."

    /** [lastShownDate] is the day key the toast last showed on, or null. */
    fun shouldShow(done: Int, total: Int, lastShownDate: String?, today: String, vacation: Boolean): Boolean =
        !vacation && total > 0 && lastShownDate != today && DayCompletion.of(done, total).qualifies
}
