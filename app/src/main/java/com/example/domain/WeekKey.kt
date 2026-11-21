package com.example.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Date-key arithmetic on "yyyy-MM-dd" strings. Weeks run Monday to Sunday. */
object WeekKey {
    private fun fmt() = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun calendarAt(dateKey: String) = Calendar.getInstance(Locale.US).apply { time = fmt().parse(dateKey)!! }

    /** Date key of the Monday that starts the week containing [dateKey]. */
    fun of(dateKey: String): String {
        val c = calendarAt(dateKey)
        c.add(Calendar.DAY_OF_YEAR, -((c.get(Calendar.DAY_OF_WEEK) + 5) % 7)) // Sunday=1 -> 6 days back, Monday=2 -> 0
        return fmt().format(c.time)
    }

    fun addDays(dateKey: String, days: Int): String {
        val c = calendarAt(dateKey)
        c.add(Calendar.DAY_OF_YEAR, days)
        return fmt().format(c.time)
    }

    /** Every date key from [from] to [to], inclusive. Empty when [from] is after [to]. */
    fun range(from: String, to: String): List<String> {
        if (from > to) return emptyList()
        val f = fmt()
        val c = calendarAt(from)
        return buildList {
            var d = from
            while (d <= to) {
                add(d)
                c.add(Calendar.DAY_OF_YEAR, 1)
                d = f.format(c.time)
            }
        }
    }
}
