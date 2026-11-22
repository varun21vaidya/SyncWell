package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class BadgeRulesTest {
    private fun day(date: String, pct: Double = 0.0, diet: Boolean = false, water: Int = 0) = BadgeDay(date, pct, diet, water)
    private fun dates(start: String, n: Int) = (0 until n).map { WeekKey.addDays(start, it) }
    private fun input(
        days: List<BadgeDay> = emptyList(), cats: List<CategoryDay> = emptyList(), goal: Int = 2500, ticked: Int = 0, best: Int = 0,
        workouts: Int = 0, focus: List<FocusDay> = emptyList(), gym: Int = 0, vacation: Set<String> = emptySet()
    ) = BadgeInput(days, cats, goal, ticked, best, workouts, focus, gym, vacation)
    private fun state(i: BadgeInput, id: String) = BadgeRules.evaluate(i).single { it.badge.id == id }

    // ---- catalogue
    @Test fun catalogueHasTwentyTwoUniqueMedalsInFiveGroups() {
        assertEquals(22, BadgeRules.ALL.size)
        assertEquals(22, BadgeRules.ALL.map { it.id }.toSet().size)
        assertEquals(BadgeGroup.values().toSet(), BadgeRules.ALL.map { it.group }.toSet())
    }

    @Test fun onlyTheHundredDayMedalsHaveADoubleRosetteAndMilestonesCarryABanner() {
        assertEquals(setOf("STREAK_100", "DAYS_100"), BadgeRules.ALL.filter { it.doubleRosette }.map { it.id }.toSet())
        assertTrue(BadgeRules.ALL.filter { it.id.startsWith("DAYS_") || it.id.startsWith("STREAK_") }.all { it.banner != null })
        assertTrue(BadgeRules.ALL.filter { it.banner != null }.all { it.banner == it.banner!!.uppercase() })
    }

    @Test fun tiersFollowThePlan() {
        fun tier(id: String) = BadgeRules.ALL.single { it.id == id }.tier
        assertEquals(listOf(BadgeTier.BRONZE, BadgeTier.SILVER, BadgeTier.GOLD, BadgeTier.TIDE), listOf("DAYS_25", "DAYS_50", "DAYS_75", "DAYS_100").map(::tier))
        assertEquals(BadgeTier.MINT, tier("PERFECT_DAY")); assertEquals(BadgeTier.ROSE, tier("SKINCARE_7"))
        assertEquals(BadgeTier.GOLD, tier("PROTEIN_WEEK")); assertEquals(BadgeTier.AQUA, tier("HYDRATION_HERO"))
        assertEquals(BadgeTier.VIOLET, tier("FOCUS_WEEK"))
    }

    // ---- first steps, one test per rule
    @Test fun firstTickNeedsAnyTickedItem() {
        assertFalse(state(input(ticked = 0), "FIRST_TICK").earned)
        assertTrue(state(input(ticked = 1), "FIRST_TICK").earned)
    }

    @Test fun dayOneNeedsAnEightyPercentDay() {
        assertFalse(state(input(days = listOf(day("2026-11-01", 0.79))), "DAY_ONE").earned)
        assertTrue(state(input(days = listOf(day("2026-11-01", 0.8))), "DAY_ONE").earned)
    }

    @Test fun streakMedalsUseTheBestStreakAndAreNeverTakenAway() {
        val i = input(best = 14)
        listOf("STREAK_3", "STREAK_7", "STREAK_14").forEach { assertTrue(it, state(i, it).earned) }
        listOf("STREAK_30", "STREAK_60", "STREAK_100").forEach { assertFalse(it, state(i, it).earned) }
        assertEquals(14, state(i, "STREAK_30").current)
        assertEquals(30, state(i, "STREAK_30").target)
    }

    @Test fun firstWorkoutNeedsAFinishedSession() {
        assertFalse(state(input(workouts = 0), "FIRST_WORKOUT").earned)
        assertTrue(state(input(workouts = 1), "FIRST_WORKOUT").earned)
    }

    @Test fun firstFocusNeedsATwentyFiveMinuteSession() {
        assertFalse(state(input(focus = listOf(FocusDay("2026-11-01", 24))), "FIRST_FOCUS").earned)
        assertTrue(state(input(focus = listOf(FocusDay("2026-11-01", 25))), "FIRST_FOCUS").earned)
        assertEquals(24, state(input(focus = listOf(FocusDay("2026-11-01", 24))), "FIRST_FOCUS").current)
    }

    @Test fun wellWateredNeedsTheGoalOnceAndNoGoalMeansNever() {
        assertTrue(state(input(days = listOf(day("2026-11-01", water = 2500))), "WELL_WATERED").earned)
        assertFalse(state(input(days = listOf(day("2026-11-01", water = 2499))), "WELL_WATERED").earned)
        assertFalse(state(input(days = listOf(day("2026-11-01", water = 9999)), goal = 0), "WELL_WATERED").earned)
    }

    // ---- good days and habit weeks
    @Test fun goodDayMedalsUnlockAtTwentyFiveFiftySeventyFiveAndHundred() {
        fun earned(n: Int) = BadgeRules.earned(input(days = dates("2026-01-01", n).map { day(it, 0.8) }))
        assertFalse("DAYS_25" in earned(24)); assertTrue("DAYS_25" in earned(25)); assertFalse("DAYS_50" in earned(49))
        assertTrue("DAYS_50" in earned(50)); assertTrue("DAYS_75" in earned(75)); assertTrue("DAYS_100" in earned(100))
    }

    @Test fun perfectDayNeedsEveryItemDone() {
        assertTrue(state(input(days = listOf(day("2026-11-01", 1.0))), "PERFECT_DAY").earned)
        assertFalse(state(input(days = listOf(day("2026-11-01", 0.95))), "PERFECT_DAY").earned)
    }

    @Test fun proteinWeekNeedsSevenConsecutiveDietDays() {
        assertTrue(state(input(days = dates("2026-11-01", 7).map { day(it, diet = true) }), "PROTEIN_WEEK").earned)
        assertFalse(state(input(days = dates("2026-11-01", 7).mapIndexed { i, d -> day(d, diet = i != 3) }), "PROTEIN_WEEK").earned)
    }

    @Test fun skincareWeekNeedsSevenConsecutiveFullSkincareDays() {
        fun cats(gap: Int?) = dates("2026-11-01", 7).mapIndexed { i, d -> CategoryDay(d, "SKINCARE", if (i == gap) 1 else 2, 2) }
        assertTrue(state(input(cats = cats(null)), "SKINCARE_7").earned)
        assertFalse(state(input(cats = cats(3)), "SKINCARE_7").earned)
    }

    @Test fun hydrationHeroNeedsSevenDaysAtTheGoal() {
        assertTrue(state(input(days = dates("2026-11-01", 7).map { day(it, water = 2500) }), "HYDRATION_HERO").earned)
        assertFalse(state(input(days = dates("2026-11-01", 7).map { day(it, water = 2400) }), "HYDRATION_HERO").earned)
    }

    @Test fun focusWeekNeedsAFocusSessionOnSevenDaysInARow() {
        assertTrue(state(input(focus = dates("2026-11-01", 7).map { FocusDay(it, 5) }), "FOCUS_WEEK").earned)
        assertFalse(state(input(focus = dates("2026-11-01", 7).filter { it != "2026-11-04" }.map { FocusDay(it, 60) }), "FOCUS_WEEK").earned)
    }

    @Test fun gymMonthNeedsFourWeeksInARow() {
        assertFalse(state(input(gym = 3), "GYM_MONTH").earned)
        assertTrue(state(input(gym = 4), "GYM_MONTH").earned)
    }

    // ---- fresh start
    @Test fun freshStartCountsACounteDayAfterTwoOrMoreMissedDays() {
        assertEquals(1, BadgeRules.freshStarts(listOf("2026-11-01", "2026-11-04"), emptySet())) // 2nd and 3rd missed
        assertEquals(0, BadgeRules.freshStarts(listOf("2026-11-01", "2026-11-03"), emptySet())) // only one missed
        assertEquals(0, BadgeRules.freshStarts(listOf("2026-11-01", "2026-11-02", "2026-11-03"), emptySet()))
        assertEquals(0, BadgeRules.freshStarts(listOf("2026-11-05"), emptySet())) // the first counted day is not a restart
        assertEquals(2, BadgeRules.freshStarts(listOf("2026-11-01", "2026-11-05", "2026-11-09"), emptySet()))
    }

    @Test fun vacationDaysAreNotMissedDaysAndTheMedalShowsTheCount() {
        assertEquals(0, BadgeRules.freshStarts(listOf("2026-11-01", "2026-11-04"), setOf("2026-11-02", "2026-11-03")))
        val s = state(input(days = listOf("2026-11-01", "2026-11-05", "2026-11-09").map { day(it, 0.9) }), "FRESH_START")
        assertTrue(s.earned); assertEquals(2, s.count)
    }

    // ---- progress, next stamp, labels
    @Test fun progressReportsCurrentAndTargetPerMedal() {
        val p = BadgeRules.progress(input(days = dates("2026-01-01", 12).map { day(it, 0.8) }))
        assertEquals(12 to 25, p["DAYS_25"])
        assertEquals(1 to 1, p["DAY_ONE"])
        assertEquals(22, p.size)
    }

    @Test fun nextStampIsTheUnearnedMedalClosestToDone() {
        val states = BadgeRules.evaluate(input(days = dates("2026-01-01", 12).map { day(it, 0.8) }, ticked = 5))
        val next = BadgeRules.nextStamp(states)!!
        assertFalse(next.earned)
        assertEquals("DAYS_25", next.badge.id) // 12/25 is the best unearned fraction
        assertEquals("12 / 25 good days", BadgeRules.progressLine(next))
    }

    @Test fun nextStampFallsBackToTheFirstWhenNothingHasProgressAndIsNullWhenAllAreEarned() {
        assertEquals("FIRST_TICK", BadgeRules.nextStamp(BadgeRules.evaluate(input()))!!.badge.id)
        assertNull(BadgeRules.nextStamp(BadgeRules.ALL.map { BadgeState(it, it.target, it.target) }))
    }

    @Test fun earnedLabelShowsTheDateOrJustEarned() {
        val saved = Locale.getDefault(); Locale.setDefault(Locale.US)
        try {
            assertEquals("Earned 14 Nov", BadgeRules.earnedLabel("2026-11-14"))
            assertEquals("Earned", BadgeRules.earnedLabel(null))
            assertEquals("Earned", BadgeRules.earnedLabel("garbage"))
        } finally { Locale.setDefault(saved) }
    }

    @Test fun stampCopyNamesTheMedalAndNeverGuilts() {
        val d25 = BadgeRules.ALL.single { it.id == "DAYS_25" }
        assertEquals("Stamped: 25 good days", BadgeRules.stampTitle(d25))
        assertEquals("You showed up for yourself 25 times.", BadgeRules.stampBody(d25))
        BadgeRules.ALL.forEach {
            assertTrue(BadgeRules.stampTitle(it).startsWith("Stamped: "))
            assertFalse(BadgeRules.stampBody(it).contains("Locked", ignoreCase = true))
        }
    }

    @Test fun longestRunHandlesGapsAndDuplicates() {
        assertEquals(3, BadgeRules.longestRun(listOf("2026-11-01", "2026-11-02", "2026-11-03", "2026-11-05", "2026-11-05")))
        assertEquals(0, BadgeRules.longestRun(emptyList()))
    }

    // ---- celebration plan
    private val order = BadgeRules.ALL.map { it.id }

    @Test fun eachMedalIsCelebratedOnceAndNeverAgain() {
        val first = BadgeCelebration.plan(setOf("DAYS_25"), emptySet(), silent = false, order = order)
        assertEquals(listOf("DAYS_25"), first.queue)
        val again = BadgeCelebration.plan(setOf("DAYS_25"), first.celebrated, silent = false, order = order)
        assertTrue(again.queue.isEmpty())
    }

    @Test fun silentRunRecordsEverythingWithoutAPopup() {
        val p = BadgeCelebration.plan(setOf("FIRST_TICK", "DAYS_25", "STREAK_7"), emptySet(), silent = true, order = order)
        assertTrue(p.queue.isEmpty())
        assertEquals(setOf("FIRST_TICK", "DAYS_25", "STREAK_7"), p.celebrated)
    }

    @Test fun severalUnlocksShowOneAfterAnotherUpToThreeInCatalogueOrder() {
        val earned = setOf("DAYS_25", "FIRST_TICK", "DAY_ONE", "STREAK_3", "PERFECT_DAY")
        val p = BadgeCelebration.plan(earned, emptySet(), silent = false, order = order)
        assertEquals(listOf("FIRST_TICK", "DAY_ONE", "STREAK_3"), p.queue)
        assertEquals(earned, p.celebrated) // the others are kept silently, so nothing pops up later either
        assertTrue(BadgeCelebration.plan(earned, p.celebrated, silent = false, order = order).queue.isEmpty())
    }
}
