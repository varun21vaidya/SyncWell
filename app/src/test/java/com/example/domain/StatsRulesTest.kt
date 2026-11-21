package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsRulesTest {
    // ---- heatmap
    @Test fun tapTextUsesTheExactStoredPercentNotABand() {
        val d = HeatmapDay("2026-11-18", 9.0 / 11, 9, 11, vacation = false)
        assertEquals("82% · Wed 18 Nov · 9 of 11 done · counted toward your streak", d.description())
    }

    @Test fun belowEightyPercentIsNotCountedAndVacationSaysPaused() {
        assertEquals("73% · Tue 17 Nov · 8 of 11 done · below 80%, not counted toward the streak",
            HeatmapDay("2026-11-17", 8.0 / 11, 8, 11, false).description())
        assertTrue(HeatmapDay("2026-11-17", 0.0, 0, 0, true).description().endsWith("vacation day, streak paused"))
    }

    @Test fun gridHasTwelveMondayToSundayWeeksEndingWithTheCurrentOne() {
        val weeks = HeatmapGrid.weeks("2026-11-21") // a Saturday
        assertEquals(12, weeks.size)
        assertTrue(weeks.all { it.size == 7 })
        assertEquals("2026-11-16", weeks.last().first()) // Monday of the current week
        assertEquals("2026-11-22", weeks.last().last())
        assertEquals(HeatmapGrid.firstDay("2026-11-21"), weeks.first().first())
    }

    // ---- week comparison
    private fun stat(date: String, pct: Double, gym: Boolean = false, focus: Int = 0) = DayStat(date, pct, gym, focus)

    @Test fun comparesThisWeekSoFarWithTheSameDaysOfLastWeek() {
        // today = Wednesday 2026-11-18 -> 3 days compared: Mon-Wed vs 9-11 Nov
        val days = listOf(
            stat("2026-11-16", 1.0, gym = true, focus = 60), stat("2026-11-17", 0.8), stat("2026-11-18", 0.6),
            stat("2026-11-09", 0.5), stat("2026-11-10", 0.4), stat("2026-11-11", 0.9, gym = true, focus = 30),
            stat("2026-11-12", 1.0) // Thursday of last week is outside the comparison
        )
        val c = WeekCompare.of(days, "2026-11-18")
        assertEquals(3, c.daysCompared)
        assertEquals(0.8, c.thisWeek.avgPercent, 1e-9)
        assertEquals(2, c.thisWeek.qualifyingDays)
        assertEquals(1, c.thisWeek.workouts)
        assertEquals(0.6, c.lastWeek.avgPercent, 1e-9)
        assertEquals(1, c.lastWeek.qualifyingDays)
        assertEquals(30, c.lastWeek.focusMinutes)
        assertEquals(20, c.deltaPoints)
    }

    @Test fun missingDaysCountAsZero() {
        val c = WeekCompare.of(listOf(stat("2026-11-16", 1.0)), "2026-11-17")
        assertEquals(0.5, c.thisWeek.avgPercent, 1e-9)
        assertEquals(0.0, c.lastWeek.avgPercent, 1e-9)
    }

    // ---- weekly review
    @Test fun reviewShowsOnSundayOnlyAndOnlyUntilDismissed() {
        assertTrue(WeeklyReview.shouldShow("2026-11-22", null)) // Sunday
        assertFalse(WeeklyReview.shouldShow("2026-11-21", null)) // Saturday
        assertFalse(WeeklyReview.shouldShow("2026-11-23", null)) // Monday
        assertFalse(WeeklyReview.shouldShow("2026-11-22", "2026-11-16")) // dismissed this week
        assertTrue(WeeklyReview.shouldShow("2026-11-29", "2026-11-16")) // next Sunday, old dismissal
    }

    // ---- badges: one test per rule
    private fun day(date: String, pct: Double = 0.0, diet: Boolean = false, water: Int = 0) = BadgeDay(date, pct, diet, water)
    private fun dates(start: String, n: Int) = (0 until n).map { WeekKey.addDays(start, it) }

    @Test fun qualifyingDayBadgesUnlockAtTwentyFiveFiftySeventyFiveAndHundred() {
        fun earned(n: Int) = BadgeRules.earned(BadgeInput(dates("2026-01-01", n).map { day(it, 0.8) }, emptyList(), 2500))
        assertFalse("DAYS_25" in earned(24))
        assertTrue("DAYS_25" in earned(25)); assertFalse("DAYS_50" in earned(25))
        assertTrue("DAYS_50" in earned(50)); assertTrue("DAYS_75" in earned(75)); assertTrue("DAYS_100" in earned(100))
    }

    @Test fun belowEightyDaysDoNotCountTowardTheDayBadges() {
        assertFalse("DAYS_25" in BadgeRules.earned(BadgeInput(dates("2026-01-01", 40).map { day(it, 0.79) }, emptyList(), 2500)))
    }

    @Test fun perfectDayNeedsEveryItemDone() {
        assertTrue("PERFECT_DAY" in BadgeRules.earned(BadgeInput(listOf(day("2026-11-01", 1.0)), emptyList(), 2500)))
        assertFalse("PERFECT_DAY" in BadgeRules.earned(BadgeInput(listOf(day("2026-11-01", 0.95)), emptyList(), 2500)))
    }

    @Test fun skincareWeekNeedsSevenConsecutiveDaysWithAllSkincareDone() {
        fun input(vararg gapAt: Int) = BadgeInput(emptyList(), dates("2026-11-01", 7).mapIndexed { i, d ->
            CategoryDay(d, "SKINCARE", if (i in gapAt) 1 else 2, 2)
        }, 2500)
        assertTrue("SKINCARE_7" in BadgeRules.earned(input()))
        assertFalse("SKINCARE_7" in BadgeRules.earned(input(3))) // one day half done breaks the run
    }

    @Test fun proteinWeekNeedsSevenConsecutiveDietDays() {
        assertTrue("PROTEIN_WEEK" in BadgeRules.earned(BadgeInput(dates("2026-11-01", 7).map { day(it, diet = true) }, emptyList(), 2500)))
        val broken = dates("2026-11-01", 7).mapIndexed { i, d -> day(d, diet = i != 3) }
        assertFalse("PROTEIN_WEEK" in BadgeRules.earned(BadgeInput(broken, emptyList(), 2500)))
    }

    @Test fun hydrationHeroNeedsSevenConsecutiveDaysAtTheGoal() {
        assertTrue("HYDRATION_HERO" in BadgeRules.earned(BadgeInput(dates("2026-11-01", 7).map { day(it, water = 2500) }, emptyList(), 2500)))
        assertFalse("HYDRATION_HERO" in BadgeRules.earned(BadgeInput(dates("2026-11-01", 7).map { day(it, water = 2400) }, emptyList(), 2500)))
        assertFalse("HYDRATION_HERO" in BadgeRules.earned(BadgeInput(dates("2026-11-01", 7).map { day(it, water = 9999) }, emptyList(), 0)))
    }

    @Test fun longestRunHandlesGapsAndDuplicates() {
        assertEquals(3, BadgeRules.longestRun(listOf("2026-11-01", "2026-11-02", "2026-11-03", "2026-11-05", "2026-11-05")))
        assertEquals(0, BadgeRules.longestRun(emptyList()))
    }
}
