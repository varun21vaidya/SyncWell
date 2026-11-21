package com.example.domain

data class StreakDay(val date: String, val qualifies: Boolean, val vacation: Boolean = false)

/** [current] is yesterday's run while today is still [inProgress]; it grows by one once today qualifies. */
data class StreakResult(val current: Int, val best: Int, val inProgress: Boolean)

object StreakRule {
    /**
     * [days] are the stored days up to [today]. A qualifying day adds one, a vacation day pauses,
     * and any other past day (including one with no entry) resets the run. Today never resets it.
     */
    fun compute(days: List<StreakDay>, today: String): StreakResult {
        val byDate = days.associateBy { it.date }
        val first = days.minOfOrNull { it.date } ?: return StreakResult(0, 0, true)
        var run = 0
        var best = 0
        for (date in WeekKey.range(first, today)) {
            val d = byDate[date]
            when {
                d?.qualifies == true -> run++
                d?.vacation == true -> Unit
                date != today -> run = 0
            }
            best = maxOf(best, run)
        }
        val t = byDate[today]
        return StreakResult(run, best, inProgress = t?.qualifies != true && t?.vacation != true)
    }
}
