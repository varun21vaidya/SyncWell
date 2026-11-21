package com.example.domain

enum class WeekMark { COUNTED, TODAY_IN_PROGRESS, EMPTY }

/** One Monday-to-Sunday strip for the Home streak card. */
object WeekBar {
    /**
     * Seven marks, Monday first. A day that reached 80% is COUNTED; today below 80% is TODAY_IN_PROGRESS;
     * everything else (missed, vacation and future days) is EMPTY, with no failure styling.
     * [countedDates] are the day keys that reached 80%.
     */
    fun of(todayKey: String, countedDates: Set<String>): List<WeekMark> {
        val monday = WeekKey.of(todayKey)
        return (0..6).map { i ->
            val d = WeekKey.addDays(monday, i)
            when {
                d in countedDates && d <= todayKey -> WeekMark.COUNTED
                d == todayKey -> WeekMark.TODAY_IN_PROGRESS
                else -> WeekMark.EMPTY
            }
        }
    }
}
