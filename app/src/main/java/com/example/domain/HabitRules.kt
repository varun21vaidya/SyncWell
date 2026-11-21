package com.example.domain

/** One routine item on one day, linked to the template item it came from. */
data class HabitDay(val dateKey: String, val templateItemId: String, val done: Boolean)

object HabitStreaks {
    /**
     * Current streak per template item, so it survives a routine rebuild and a title edit.
     * A day the habit was not scheduled (the day has a routine but no item for it) pauses the streak,
     * like a vacation day; a day with no routine at all counts as a miss.
     */
    fun current(rows: List<HabitDay>, routineDates: Set<String>, vacationDates: Set<String>, today: String): Map<String, Int> =
        rows.groupBy { it.templateItemId }.mapValues { (_, habitRows) ->
            val byDate = habitRows.associateBy { it.dateKey }
            val first = habitRows.minOf { it.dateKey }
            val days = WeekKey.range(first, today).map { d ->
                val row = byDate[d]
                when {
                    d in vacationDates -> StreakDay(d, false, vacation = true)
                    row != null -> StreakDay(d, row.done)
                    d in routineDates -> StreakDay(d, false, vacation = true) // not scheduled that day
                    else -> StreakDay(d, false)
                }
            }
            StreakRule.compute(days, today).current
        }
}

object FreezeRule {
    /**
     * One freeze per Monday-to-Sunday week, and only for today: it never reaches back to change an older day.
     * [freezeDates] are the days already frozen.
     */
    fun canFreeze(todayKey: String, freezeDates: Collection<String>): Boolean {
        val week = WeekKey.of(todayKey)
        return freezeDates.none { WeekKey.of(it) == week }
    }
}
