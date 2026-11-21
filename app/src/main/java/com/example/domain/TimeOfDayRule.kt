package com.example.domain

/** One block of the Routine screen: a heading and its items in order. */
data class TimeGroup(val label: String, val items: List<DayItem>)

object TimeOfDayRule {
    /**
     * Groups items by time of day, counted from the wake time so late-night items (e.g. 00:30) land in
     * Night after 23:00 rather than at the top. Morning is the first 6 hours after wake, Afternoon the next 5,
     * Evening the next 5, Night the rest. Items without a time go in a last "Anytime" group. Empty groups are left out.
     */
    fun group(items: List<DayItem>, wakeTime: String): List<TimeGroup> {
        val wake = minutes(wakeTime) ?: 420
        fun offset(i: DayItem) = i.time?.let { minutes(it) }?.let { (it - wake + 1440) % 1440 }
        val timed = items.filter { offset(it) != null }.sortedBy { offset(it) } // stable: keeps stored order for equal times
        fun label(o: Int) = when {
            o < 360 -> "Morning"
            o < 660 -> "Afternoon"
            o < 960 -> "Evening"
            else -> "Night"
        }
        val groups = timed.groupBy { label(offset(it)!!) }
        return listOf("Morning", "Afternoon", "Evening", "Night").mapNotNull { l -> groups[l]?.let { TimeGroup(l, it) } } +
            listOfNotNull(items.filter { offset(it) == null }.takeIf { it.isNotEmpty() }?.let { TimeGroup("Anytime", it) })
    }

    private fun minutes(hhmm: String): Int? {
        val p = hhmm.split(":")
        if (p.size != 2) return null
        val h = p[0].toIntOrNull() ?: return null
        val m = p[1].toIntOrNull() ?: return null
        return h * 60 + m
    }
}
