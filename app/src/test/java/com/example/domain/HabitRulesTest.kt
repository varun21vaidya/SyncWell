package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitRulesTest {
    private fun days(id: String, vararg done: Boolean, start: String = "2026-11-16") =
        done.mapIndexed { i, d -> HabitDay(WeekKey.addDays(start, i), id, d) }

    private fun routines(start: String, n: Int) = (0 until n).map { WeekKey.addDays(start, it) }.toSet()

    @Test fun streakCountsConsecutiveDoneDaysUpToToday() {
        val rows = days("sunscreen", true, true, true)
        assertEquals(3, HabitStreaks.current(rows, routines("2026-11-16", 3), emptySet(), "2026-11-18")["sunscreen"])
    }

    @Test fun todayStillInProgressKeepsYesterdaysRun() {
        val rows = days("sunscreen", true, true, false)
        assertEquals(2, HabitStreaks.current(rows, routines("2026-11-16", 3), emptySet(), "2026-11-18")["sunscreen"])
    }

    @Test fun aMissedDayResetsTheRunAndAVacationDayDoesNot() {
        val missed = days("minoxidil", true, false, true, true)
        assertEquals(2, HabitStreaks.current(missed, routines("2026-11-16", 4), emptySet(), "2026-11-19")["minoxidil"])
        assertEquals(3, HabitStreaks.current(missed, routines("2026-11-16", 4), setOf("2026-11-17"), "2026-11-19")["minoxidil"])
    }

    @Test fun aDayTheHabitWasNotScheduledPausesInsteadOfBreaking() {
        // done Mon, not scheduled Tue (routine exists, no item), done Wed
        val rows = listOf(HabitDay("2026-11-16", "wk", true), HabitDay("2026-11-18", "wk", true))
        assertEquals(2, HabitStreaks.current(rows, routines("2026-11-16", 3), emptySet(), "2026-11-18")["wk"])
    }

    @Test fun aDayWithNoRoutineAtAllIsAMiss() {
        val rows = listOf(HabitDay("2026-11-16", "x", true), HabitDay("2026-11-18", "x", true))
        assertEquals(1, HabitStreaks.current(rows, setOf("2026-11-16", "2026-11-18"), emptySet(), "2026-11-18")["x"])
    }

    @Test fun eachHabitHasItsOwnStreak() {
        val rows = days("a", true, true) + days("b", false, true)
        val out = HabitStreaks.current(rows, routines("2026-11-16", 2), emptySet(), "2026-11-17")
        assertEquals(2, out["a"]); assertEquals(1, out["b"])
    }

    @Test fun oneFreezePerWeekOnly() {
        assertTrue(FreezeRule.canFreeze("2026-11-18", emptyList()))
        assertFalse(FreezeRule.canFreeze("2026-11-20", listOf("2026-11-16"))) // same Monday-to-Sunday week
        assertTrue(FreezeRule.canFreeze("2026-11-23", listOf("2026-11-22"))) // Sunday of the previous week
        assertTrue(FreezeRule.canFreeze("2026-11-18", listOf("2026-11-10")))
    }

    @Test fun aFreezeDayKeepsTheStreakLikeAVacationDay() {
        val streakDays = listOf(
            StreakDay("2026-11-16", true), StreakDay("2026-11-17", false, vacation = true), StreakDay("2026-11-18", true)
        )
        assertEquals(2, StreakRule.compute(streakDays, "2026-11-18").current)
    }
}
