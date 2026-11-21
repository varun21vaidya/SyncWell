package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreakRuleTest {
    private val today = "2026-11-21"

    /** [n] qualifying days ending yesterday. */
    private fun run(n: Int) = (1..n).map { StreakDay(WeekKey.addDays(today, -it), true) }

    @Test fun morningShowsYesterdayStreak() {
        val r = StreakRule.compute(run(12) + StreakDay(today, false), today)
        assertEquals(12, r.current)
        assertTrue(r.inProgress)
    }

    @Test fun noRowForTodayYetStillShowsYesterdayStreak() {
        val r = StreakRule.compute(run(12), today)
        assertEquals(12, r.current)
        assertTrue(r.inProgress)
    }

    @Test fun reachingEightyPercentAddsOneAndUntickingReturns() {
        assertEquals(13, StreakRule.compute(run(12) + StreakDay(today, true), today).current)
        assertEquals(12, StreakRule.compute(run(12) + StreakDay(today, false), today).current)
        assertFalse(StreakRule.compute(run(12) + StreakDay(today, true), today).inProgress)
    }

    @Test fun missedDayResetsButVacationKeepsIt() {
        val missed = listOf(StreakDay("2026-11-17", true), StreakDay("2026-11-18", true), StreakDay("2026-11-19", false), StreakDay("2026-11-20", true))
        assertEquals(1, StreakRule.compute(missed, today).current)

        val vacation = listOf(StreakDay("2026-11-17", true), StreakDay("2026-11-18", true), StreakDay("2026-11-19", false, vacation = true), StreakDay("2026-11-20", true))
        assertEquals(3, StreakRule.compute(vacation, today).current)
    }

    @Test fun dayWithNoEntryBreaksTheRun() {
        val gap = listOf(StreakDay("2026-11-17", true), StreakDay("2026-11-18", true), StreakDay("2026-11-20", true))
        assertEquals(1, StreakRule.compute(gap, today).current)
    }

    @Test fun bestIsTheMaximumOverHistory() {
        val days = (1..5).map { StreakDay(WeekKey.addDays("2026-10-01", it), true) } + StreakDay("2026-11-20", true)
        val r = StreakRule.compute(days, today)
        assertEquals(1, r.current)
        assertEquals(5, r.best)
    }

    @Test fun noHistory() {
        assertEquals(StreakResult(0, 0, true), StreakRule.compute(emptyList(), today))
    }
}
