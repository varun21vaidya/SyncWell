package com.example.domain

import com.example.data.Goal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommitmentRulesTest {
    private fun ok() = CommitmentDraft(title = "DSA", target = 10.0)
    private fun goal(id: String, title: String, end: String, status: String = "PENDING", reward: String? = null) =
        Goal(id, title, "COMMITMENT", true, "COUNT", 5.0, windowStart = "2026-11-01", windowEnd = end, status = status, rewardText = reward, planText = "After lunch", planTime = "13:30", ifThenText = "do 2")

    @Test fun aSevenDayRegionOffersOnlyTheWholeAndFourteenOffersThree() {
        assertEquals(listOf(CommitmentWindow.WHOLE), CommitmentRules.windows(1))
        assertEquals(3, CommitmentRules.windows(2).size)
    }

    @Test fun windowsAreCountedFromTheRegionStart() {
        assertEquals("2026-11-23" to "2026-11-29", CommitmentRules.dates(2, CommitmentWindow.SEGMENT_1, "2026-11-23"))
        assertEquals("2026-11-30" to "2026-12-06", CommitmentRules.dates(2, CommitmentWindow.SEGMENT_2, "2026-11-23"))
        assertEquals("2026-11-23" to "2026-12-06", CommitmentRules.dates(2, CommitmentWindow.WHOLE, "2026-11-23"))
        assertEquals("2026-11-23" to "2026-11-29", CommitmentRules.dates(1, CommitmentWindow.WHOLE, "2026-11-23"))
    }

    @Test fun aGoodDraftHasNoErrors() = assertTrue(CommitmentRules.validate(ok(), 2, 0).isEmpty())

    @Test fun eachRuleBlocksSaving() {
        assertEquals(listOf(DraftError.TITLE), CommitmentRules.validate(ok().copy(title = " "), 2, 0))
        assertEquals(listOf(DraftError.TARGET), CommitmentRules.validate(ok().copy(target = 0.0), 2, 0))
        assertEquals(listOf(DraftError.TARGET), CommitmentRules.validate(ok().copy(measure = GoalMeasure.PERCENT, target = 120.0), 2, 0))
        assertEquals(listOf(DraftError.TOO_MANY), CommitmentRules.validate(ok(), 2, 3))
        assertEquals(listOf(DraftError.WINDOW), CommitmentRules.validate(ok().copy(window = CommitmentWindow.SEGMENT_1), 1, 0))
        assertEquals(listOf(DraftError.TIME), CommitmentRules.validate(ok().copy(planTime = "25:00"), 2, 0))
        assertEquals(listOf(DraftError.TOO_LONG), CommitmentRules.validate(ok().copy(title = "x".repeat(61)), 2, 0))
    }

    @Test fun savingBuildsATrimmedCommitmentInItsRegion() {
        val g = CommitmentRules.toGoal(ok().copy(title = " DSA ", reward = " Burger ", planText = "", focusLinked = true), "id", 2, "2026-11-23")
        assertEquals("DSA", g.title); assertEquals("Burger", g.rewardText); assertNull(g.planText)
        assertEquals("COMMITMENT", g.kind); assertEquals(2, g.regionIndex); assertEquals("2026-12-06", g.windowEnd)
        assertFalse(g.focusLinked) // the focus link is for duration goals only
        assertTrue(CommitmentRules.toGoal(ok().copy(measure = GoalMeasure.DURATION, focusLinked = true), "i", 2, "2026-11-23").focusLinked)
    }

    @Test fun suggestionsAreTheUsersOwnGoalsNewestFirstOnePerTitle() {
        val s = CommitmentRules.suggestions(listOf(goal("a", "DSA", "2026-11-10"), goal("b", "dsa", "2026-11-20"), goal("c", "Read", "2026-11-15")), 5)
        assertEquals(listOf("b", "c"), s.map { it.id })
        assertEquals(1, CommitmentRules.suggestions(listOf(goal("a", "A", "1"), goal("b", "B", "2")), 1).size)
    }

    @Test fun pickingASuggestionFillsEverythingAndCarriesOnlyAMissedReward() {
        val missed = CommitmentRules.fromGoal(goal("a", "DSA", "x", "MISSED", "Burger"), 2)
        val met = CommitmentRules.fromGoal(goal("a", "DSA", "x", "MET", "Burger"), 2)
        assertEquals("Burger", missed.reward); assertNull(met.reward)
        assertEquals("DSA", missed.title); assertEquals(5.0, missed.target, 0.0); assertEquals("13:30", missed.planTime)
        assertEquals(CommitmentWindow.WHOLE, CommitmentRules.fromGoal(goal("a", "DSA", "x"), 1).window)
    }

    @Test fun theMoreFieldsAreCopiedFromTheLastCommitment() {
        val d = CommitmentRules.prefillMore(goal("a", "DSA", "x"))
        assertEquals("After lunch", d.planText); assertEquals("do 2", d.ifThen); assertEquals("", d.title)
        assertNull(CommitmentRules.prefillMore(null).planText)
    }

    @Test fun theTimerOffersOnlyPendingLinkedGoalsWhoseWindowIncludesToday() {
        val linked = goal("a", "Hours", "2026-11-29").copy(focusLinked = true, windowStart = "2026-11-23")
        val done = linked.copy(id = "b", status = "MET")
        val notLinked = linked.copy(id = "c", focusLinked = false)
        val later = linked.copy(id = "d", windowStart = "2026-12-01", windowEnd = "2026-12-07")
        val past = linked.copy(id = "e", windowEnd = "2026-11-24")
        assertEquals(listOf("a"), CommitmentRules.focusChoices(listOf(linked, done, notLinked, later, past), "2026-11-26").map { it.id })
    }

    @Test fun amountsAcceptCommasAndRejectNonsense() {
        assertEquals(1.5, GoalReview.parseAmount(" 1,5 ")!!, 0.0)
        assertEquals(2.0, GoalReview.parseAmount("2")!!, 0.0)
        assertNull(GoalReview.parseAmount("")); assertNull(GoalReview.parseAmount("abc")); assertNull(GoalReview.parseAmount("0"))
        assertNull(GoalReview.parseAmount("-3")); assertNull(GoalReview.parseAmount("999999")); assertNull(GoalReview.parseAmount("NaN"))
    }
}
