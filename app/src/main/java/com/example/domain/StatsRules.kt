package com.example.domain

import java.text.SimpleDateFormat
import java.util.Locale

/** One stored day, reduced to what the Stats rules need. [done] and [total] are routine item counts. */
data class HeatmapDay(val dateKey: String, val percent: Double, val done: Int, val total: Int, val vacation: Boolean) {
    val countedTowardStreak: Boolean get() = DayCompletion.qualifies(percent)

    /** The text shown when a tile is tapped and read out by screen readers, from the exact stored percent. */
    fun description(): String {
        val date = SimpleDateFormat("EEE d MMM", Locale.US).format(SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateKey)!!)
        val verdict = when {
            vacation -> "vacation day, streak paused"
            countedTowardStreak -> "counted toward your streak"
            else -> "below 80%, not counted toward the streak"
        }
        return "${Math.round(percent * 100)}% · $date · $done of $total done · $verdict"
    }
}

object HeatmapGrid {
    /** Date keys of the Monday-to-Sunday weeks shown, oldest week first; the last week contains [todayKey]. */
    fun weeks(todayKey: String, weekCount: Int = 12): List<List<String>> {
        val firstMonday = firstDay(todayKey, weekCount)
        return (0 until weekCount).map { w -> (0..6).map { d -> WeekKey.addDays(firstMonday, w * 7 + d) } }
    }

    fun firstDay(todayKey: String, weekCount: Int = 12): String = WeekKey.addDays(WeekKey.of(todayKey), -7 * (weekCount - 1))
}

data class DayStat(val dateKey: String, val percent: Double, val gymDone: Boolean, val focusMinutes: Int)

data class WeekStats(val avgPercent: Double, val qualifyingDays: Int, val workouts: Int, val focusMinutes: Int)

data class WeekComparison(val thisWeek: WeekStats, val lastWeek: WeekStats, val daysCompared: Int) {
    /** Change in the average percent, in whole percentage points. */
    val deltaPoints: Int get() = Math.round((thisWeek.avgPercent - lastWeek.avgPercent) * 100).toInt()
}

object WeekCompare {
    /**
     * This week so far (Monday to [todayKey]) against the same number of days of last week, so the two sides are fair.
     * A day with no stored row counts as 0%.
     */
    fun of(days: List<DayStat>, todayKey: String): WeekComparison {
        val byDate = days.associateBy { it.dateKey }
        val monday = WeekKey.of(todayKey)
        val n = WeekKey.range(monday, todayKey).size
        fun stats(start: String): WeekStats {
            val week = WeekKey.range(start, WeekKey.addDays(start, n - 1)).map { byDate[it] }
            return WeekStats(
                avgPercent = week.sumOf { it?.percent ?: 0.0 } / n,
                qualifyingDays = week.count { it != null && DayCompletion.qualifies(it.percent) },
                workouts = week.count { it?.gymDone == true },
                focusMinutes = week.sumOf { it?.focusMinutes ?: 0 }
            )
        }
        return WeekComparison(stats(monday), stats(WeekKey.addDays(monday, -7)), n)
    }
}

object WeeklyReview {
    /** The review shows on Sunday only, once, until dismissed. [dismissedWeek] is the WeekKey it was dismissed in. */
    fun shouldShow(todayKey: String, dismissedWeek: String?): Boolean {
        val week = WeekKey.of(todayKey)
        return todayKey == WeekKey.addDays(week, 6) && dismissedWeek != week
    }
}
