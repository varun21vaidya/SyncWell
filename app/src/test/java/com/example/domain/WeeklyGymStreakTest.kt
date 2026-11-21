package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class WeeklyGymStreakTest {
    private val today = "2026-11-21" // Saturday; this week began Monday 2026-11-16

    private val lastWeek3 = listOf("2026-11-09", "2026-11-11", "2026-11-13")

    @Test fun threeSessionsWithTargetThreeAddsOneWeek() {
        val r = WeeklyGymStreak.compute(listOf("2026-11-16", "2026-11-18", "2026-11-20"), 3, today)
        assertEquals(1, r.currentWeeks)
        assertEquals(3, r.thisWeekCount)
    }

    @Test fun weekInProgressDoesNotBreakTheStreak() {
        val r = WeeklyGymStreak.compute(lastWeek3 + "2026-11-16", 3, today)
        assertEquals(1, r.currentWeeks)
        assertEquals(1, r.thisWeekCount)
    }

    @Test fun finishedWeekBelowTargetResets() {
        val twoWeeksAgo = listOf("2026-11-02", "2026-11-03", "2026-11-04")
        val r = WeeklyGymStreak.compute(twoWeeksAgo + listOf("2026-11-09", "2026-11-10") + "2026-11-16", 3, today)
        assertEquals(0, r.currentWeeks)
        assertEquals(1, r.bestWeeks)
    }

    @Test fun weekWithNoSessionsResets() {
        val r = WeeklyGymStreak.compute(listOf("2026-10-26", "2026-10-27", "2026-10-28"), 3, today)
        assertEquals(0, r.currentWeeks)
        assertEquals(1, r.bestWeeks)
    }

    @Test fun sundayBelongsToTheWeekThatStartedMonday() {
        val r = WeeklyGymStreak.compute(listOf("2026-11-09", "2026-11-10", "2026-11-15"), 3, today) // Sunday 15th ends that week
        assertEquals(1, r.currentWeeks)
    }

    @Test fun noSessions() {
        assertEquals(GymStreakResult(0, 0, 0, 3), WeeklyGymStreak.compute(emptyList(), 3, today))
    }

    @Test fun targetBelowOneIsTreatedAsOne() {
        assertEquals(1, WeeklyGymStreak.compute(listOf("2026-11-16"), 0, today).currentWeeks)
    }
}
