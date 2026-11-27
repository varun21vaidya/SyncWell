package com.example.domain

import com.example.data.Goal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JourneyPageRulesTest {
    private fun weekly(id: String, start: String, end: String, title: String = id) =
        Goal(id, title, "WEEKLY", false, "COUNT", 4.0, windowStart = start, windowEnd = end)

    @Test fun todayLineSaysHowManyItemsReach80() {
        assertEquals("2 routine items left. 80% counts the day.", JourneyRail.todayLine(6, 10)) // 8 of 10 needed
        assertEquals("1 routine item left. 80% counts the day.", JourneyRail.todayLine(7, 10))
        assertEquals("Today counts. Keep going if you like.", JourneyRail.todayLine(8, 10))
        assertEquals("Nothing is scheduled today.", JourneyRail.todayLine(0, 0))
    }

    @Test fun theWardenLineCountsThePledges() {
        assertEquals("Waits at the gate on day 14", JourneyRail.wardenLine(0, 14))
        assertEquals("Checks it at the gate on day 14", JourneyRail.wardenLine(1, 14))
        assertEquals("Checks both at the gate on day 14", JourneyRail.wardenLine(2, 14))
        assertEquals("Checks all 3 at the gate on day 14", JourneyRail.wardenLine(3, 14))
    }

    @Test fun amountsAreWrittenWithoutTrailingZeros() {
        assertEquals("10", formatAmount(10.0)); assertEquals("1.5", formatAmount(1.5)); assertEquals("0", formatAmount(0.0))
    }

    @Test fun aWeeklyGoalCoversThisWeekMondayToSundayWithNoRewardOrRegion() {
        val d = CommitmentDraft(title = " Read 2 hours ", measure = GoalMeasure.DURATION, target = 2.0, reward = "Burger", planText = "x", focusLinked = true)
        val g = CommitmentRules.toWeekly(d, "w", "2026-11-26") // a Thursday
        assertEquals("WEEKLY", g.kind); assertFalse(g.mandatory)
        assertEquals("2026-11-23", g.windowStart); assertEquals("2026-11-29", g.windowEnd) // Monday to Sunday
        assertEquals("Read 2 hours", g.title)
        assertEquals(null, g.regionIndex); assertEquals(null, g.rewardText); assertEquals(null, g.planText)
        assertTrue(g.focusLinked) // a duration goal can still count Focus hours
    }

    @Test fun aWeeklyGoalOnlyNeedsValidCoreFields() {
        assertTrue(CommitmentRules.validateWeekly(CommitmentDraft(title = "Gym", target = 4.0)).isEmpty())
        assertEquals(listOf(DraftError.TITLE, DraftError.TARGET), CommitmentRules.validateWeekly(CommitmentDraft()))
    }

    @Test fun thisWeekHoldsOnlyWeeklyGoalsWhoseWindowIncludesToday() {
        val goals = listOf(
            weekly("a", "2026-11-23", "2026-11-29", "Read"), weekly("b", "2026-11-16", "2026-11-22", "Last week"),
            weekly("c", "2026-11-23", "2026-11-29", "Gym"), weekly("d", "2026-11-30", "2026-12-06", "Next week"),
            weekly("e", "2026-11-23", "2026-11-29").copy(kind = "COMMITMENT")
        )
        assertEquals(listOf("c", "a"), CommitmentRules.thisWeek(goals, "2026-11-26").map { it.id }) // Gym before Read
    }
}
