package com.example.domain

data class GymStreakResult(val currentWeeks: Int, val bestWeeks: Int, val thisWeekCount: Int, val target: Int)

object WeeklyGymStreak {
    /**
     * Consecutive Monday-to-Sunday weeks with at least [weeklyTarget] finished sessions.
     * The current week adds one once it reaches the target and never breaks the streak while in progress.
     */
    fun compute(finishedSessionDates: List<String>, weeklyTarget: Int, today: String): GymStreakResult {
        val target = weeklyTarget.coerceAtLeast(1)
        val perWeek = finishedSessionDates.filter { it <= today }.groupingBy { WeekKey.of(it) }.eachCount()
        val thisWeek = WeekKey.of(today)
        var week = perWeek.keys.minOrNull() ?: return GymStreakResult(0, 0, 0, target)
        var run = 0
        var best = 0
        while (week <= thisWeek) {
            if ((perWeek[week] ?: 0) >= target) run++ else if (week != thisWeek) run = 0
            best = maxOf(best, run)
            week = WeekKey.addDays(week, 7)
        }
        return GymStreakResult(run, best, perWeek[thisWeek] ?: 0, target)
    }
}
