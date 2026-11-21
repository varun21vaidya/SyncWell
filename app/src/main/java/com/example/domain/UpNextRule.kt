package com.example.domain

/** A routine item reduced to what the Home rules need. [time] is "HH:MM" or null. */
data class DayItem(val id: String, val title: String, val category: String, val time: String?, val isDone: Boolean)

object UpNextRule {
    /**
     * The next unticked item by time. Items due now or later come first; when everything left is overdue,
     * the earliest overdue one is returned. Items without a time come last. Null when nothing is left.
     * Only today's items are passed in, so an item from a template not scheduled today can never appear.
     */
    fun next(items: List<DayItem>, nowMinutes: Int): DayItem? {
        val open = items.filter { !it.isDone }
        fun minutes(i: DayItem) = i.time?.let { toMinutes(it) }
        val timed = open.filter { minutes(it) != null }.sortedBy { minutes(it) }
        return timed.firstOrNull { minutes(it)!! >= nowMinutes } ?: timed.firstOrNull() ?: open.firstOrNull()
    }

    private fun toMinutes(hhmm: String): Int? {
        val p = hhmm.split(":")
        if (p.size != 2) return null
        val h = p[0].toIntOrNull() ?: return null
        val m = p[1].toIntOrNull() ?: return null
        return h * 60 + m
    }
}

data class CategoryProgress(val category: String, val done: Int, val total: Int) {
    val fraction: Float get() = if (total == 0) 0f else done.toFloat() / total
}

object CategoryRings {
    /** The categories shown as small rings on Home, in display order. A category with no item today is left out. */
    val SHOWN = listOf("DIET", "SUPPLEMENT", "SKINCARE", "WORKOUT", "STUDY")

    fun of(items: List<DayItem>): List<CategoryProgress> =
        SHOWN.mapNotNull { c ->
            val inCat = items.filter { it.category == c }
            if (inCat.isEmpty()) null else CategoryProgress(c, inCat.count { it.isDone }, inCat.size)
        }
}
