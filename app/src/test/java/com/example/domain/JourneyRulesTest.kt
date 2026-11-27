package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JourneyRulesTest {
    // 2026-11-23 is a Monday
    private fun good(d: String) = JourneyDay(d, qualifies = true)
    private fun bad(d: String) = JourneyDay(d, qualifies = false)
    private fun at(region: Int, step: Int, freezeWeek: String? = null) = JourneyPosition(region, step, freezeWeek)

    @Test fun routeAddsUpToSixtySixAtTheSummitGate() {
        assertEquals(JourneyRoute.SUMMIT_STEPS, (0..JourneyRoute.SUMMIT_REGION).sumOf { JourneyRoute.stepsIn(it) })
        assertEquals(66, JourneyRoute.totalSteps(at(5, 14)))
        assertEquals(10, JourneyRoute.totalSteps(at(1, 7)))
    }

    @Test fun aQualifyingDayAddsOneStep() {
        val r = JourneyRules.advance(at(1, 2), good("2026-11-23"))
        assertEquals(3, r.position.stepInRegion)
        assertEquals(listOf<JourneyEvent>(JourneyEvent.Step("2026-11-23", 1, 3)), r.events)
    }

    @Test fun reachingTheLastStepReportsTheGateAndMoreGoodDaysWaitThere() {
        val r = JourneyRules.advance(at(0, 2), good("2026-11-23"))
        assertTrue(r.position.atGate)
        assertTrue(r.events.contains(JourneyEvent.GateReached("2026-11-23", 0)))
        val again = JourneyRules.advance(r.position, good("2026-11-24"))
        assertEquals(r.position, again.position)
        assertTrue(again.events.isEmpty())
    }

    @Test fun aVacationOrFrozenDayIsSkippedWithNoPenalty() {
        val p = at(2, 9)
        assertEquals(p, JourneyRules.advance(p, JourneyDay("2026-11-23", false, vacation = true)).position)
        assertEquals(p, JourneyRules.advance(p, JourneyDay("2026-11-23", false, covered = true)).position)
    }

    @Test fun aQualifyingDayBeatsAVacationFlag() {
        assertEquals(10, JourneyRules.advance(at(2, 9), JourneyDay("2026-11-23", true, vacation = true)).position.stepInRegion)
    }

    @Test fun theFirstMissedDayOfAWeekUsesTheFreeze() {
        val r = JourneyRules.advance(at(2, 9), bad("2026-11-25"))
        assertEquals(9, r.position.stepInRegion)
        assertEquals("2026-11-23", r.position.freezeWeek)
        assertEquals(listOf<JourneyEvent>(JourneyEvent.FreezeUsed("2026-11-25")), r.events)
    }

    @Test fun withNoFreezeLeftAMissedDayGoesBackToTheCheckpoint() {
        val r = JourneyRules.advance(at(2, 9, freezeWeek = "2026-11-23"), bad("2026-11-26"))
        assertEquals(7, r.position.stepInRegion)
        assertEquals(listOf<JourneyEvent>(JourneyEvent.Setback("2026-11-26", 2, 9, 7)), r.events)
    }

    @Test fun beforeTheCheckpointAMissedDayGoesBackToTheRegionStart() {
        assertEquals(0, JourneyRules.advance(at(2, 5, "2026-11-23"), bad("2026-11-26")).position.stepInRegion)
    }

    @Test fun standingOnTheCheckpointOrStartThereIsNothingToLose() {
        val r = JourneyRules.advance(at(2, 7, "2026-11-23"), bad("2026-11-26"))
        assertEquals(7, r.position.stepInRegion)
        assertTrue(r.events.isEmpty())
        assertTrue(JourneyRules.advance(at(2, 0, "2026-11-23"), bad("2026-11-26")).events.isEmpty())
    }

    @Test fun verdantsCheckpointIsOnStepThree() {
        assertEquals(3, JourneyRules.advance(at(1, 5, "2026-11-23"), bad("2026-11-26")).position.stepInRegion)
    }

    @Test fun homeStartHasNoPenalties() {
        assertEquals(2, JourneyRules.advance(at(0, 2, "2026-11-23"), bad("2026-11-26")).position.stepInRegion)
    }

    @Test fun aMissedDayAtTheGateStillGoesBackToTheCheckpoint() {
        assertEquals(7, JourneyRules.advance(at(2, 14, "2026-11-23"), bad("2026-11-26")).position.stepInRegion)
    }

    @Test fun theFreezeComesBackOnTheNextMonday() {
        val used = at(2, 9, freezeWeek = "2026-11-23")
        val r = JourneyRules.advance(used, bad("2026-11-30"))
        assertEquals(9, r.position.stepInRegion)
        assertEquals("2026-11-30", r.position.freezeWeek)
    }

    @Test fun catchUpNeverProcessesToday() {
        val days = mapOf("2026-11-24" to good("2026-11-24"))
        val (p, _, last) = JourneyRules.catchUp(at(1, 0), "2026-11-23", "2026-11-25") { days[it] }
        assertEquals(1, p.stepInRegion)
        assertEquals("2026-11-24", last)
        // today is not stored yet and does not count as missed
        val (p2, _, _) = JourneyRules.catchUp(p, last, "2026-11-25") { days[it] }
        assertEquals(p, p2)
    }

    @Test fun catchUpOverSeveralMissedDaysCountsEachOneInOrder() {
        // Mon 23 .. Thu 26 all missing: the freeze covers Mon, then Tue falls to the checkpoint, the rest have nothing left to lose
        val (p, events, last) = JourneyRules.catchUp(at(2, 9), "2026-11-22", "2026-11-27") { null }
        assertEquals(7, p.stepInRegion)
        assertEquals("2026-11-26", last)
        assertEquals(listOf<JourneyEvent>(JourneyEvent.FreezeUsed("2026-11-23"), JourneyEvent.Setback("2026-11-24", 2, 9, 7)), events)
    }

    @Test fun processingInOneGoOrInStagesGivesTheSamePosition() {
        val days = mapOf("2026-11-23" to good("2026-11-23"), "2026-11-24" to good("2026-11-24"))
        val oneGo = JourneyRules.catchUp(at(1, 0), "2026-11-22", "2026-11-27") { days[it] }
        val first = JourneyRules.catchUp(at(1, 0), "2026-11-22", "2026-11-25") { days[it] }
        val staged = JourneyRules.catchUp(first.first, first.third, "2026-11-27") { days[it] }
        assertEquals(oneGo.first, staged.first)
        assertEquals(oneGo.third, staged.third)
        // Wed freezes, Thu goes from step 2 back to the region start (the checkpoint is on step 3)
        assertEquals(0, oneGo.first.stepInRegion)
    }

    @Test fun crossingTheGateNeedsEveryCommitmentMet() {
        assertNull(JourneyRules.crossGate(at(2, 14), commitmentsMet = false))
        assertNull(JourneyRules.crossGate(at(2, 13), commitmentsMet = true))
        assertEquals(at(3, 0), JourneyRules.crossGate(at(2, 14), commitmentsMet = true))
    }

    @Test fun theSummitIsTheGateOfRegionSix() {
        val atSummit = at(5, 14)
        assertFalse(JourneyRules.summitReached(atSummit))
        assertTrue(JourneyRules.summitReached(JourneyRules.crossGate(atSummit, true)!!))
        assertEquals(14, JourneyRoute.stepsIn(8)) // weekly regions after the Summit
    }

    @Test fun aMissedCommitmentAndAPauseBothGoToTheRegionStart() {
        assertEquals(at(3, 0, "x"), JourneyRules.commitmentMissed(at(3, 12, "x")))
        assertEquals(at(3, 0), JourneyRules.resume(at(3, 9)))
    }

    // ---- goal review ----
    private fun goal(measure: GoalMeasure, target: Double, status: GoalStatus = GoalStatus.PENDING, end: String = "2026-11-29") =
        GoalSpec("g", measure, target, "2026-11-23", end, status)

    @Test fun countAndDurationAddUpInsideTheWindowOnly() {
        val e = listOf(GoalEntry("2026-11-22", 5.0), GoalEntry("2026-11-24", 2.0), GoalEntry("2026-11-26", 1.5), GoalEntry("2026-11-30", 9.0))
        assertEquals(3.5, GoalReview.progress(goal(GoalMeasure.COUNT, 3.0), e), 1e-9)
        assertEquals(GoalStatus.MET, GoalReview.status(goal(GoalMeasure.DURATION, 3.0), e, "2026-11-27"))
    }

    @Test fun percentUsesTheLatestEntryAndMilestoneAnyEntry() {
        val e = listOf(GoalEntry("2026-11-24", 80.0), GoalEntry("2026-11-26", 60.0))
        assertEquals(60.0, GoalReview.progress(goal(GoalMeasure.PERCENT, 70.0), e), 1e-9)
        assertEquals(80.0, GoalReview.progress(goal(GoalMeasure.MILESTONE, 80.0), e), 1e-9)
    }

    @Test fun pendingUntilTheWindowEndsThenMissedIfShort() {
        val g = goal(GoalMeasure.COUNT, 5.0)
        val e = listOf(GoalEntry("2026-11-24", 2.0))
        assertEquals(GoalStatus.PENDING, GoalReview.status(g, e, "2026-11-29"))
        assertEquals(GoalStatus.MISSED, GoalReview.status(g, e, "2026-11-30"))
    }

    @Test fun aLateOpenGivesTheSameVerdictAsOpeningOnTheLastDay() {
        val g = goal(GoalMeasure.COUNT, 2.0)
        val e = listOf(GoalEntry("2026-11-24", 1.0), GoalEntry("2026-11-29", 1.0))
        assertEquals(GoalReview.status(g, e, "2026-11-30"), GoalReview.status(g, e, "2026-12-03"))
        assertEquals(GoalStatus.MET, GoalReview.status(g, e, "2026-12-03"))
    }

    @Test fun reviewQueueHoldsEndedPendingGoalsOldestFirst() {
        val a = goal(GoalMeasure.COUNT, 1.0, end = "2026-11-29").copy(id = "a")
        val b = goal(GoalMeasure.COUNT, 1.0, end = "2026-11-22").copy(id = "b")
        val done = goal(GoalMeasure.COUNT, 1.0, GoalStatus.MET, "2026-11-20").copy(id = "c")
        val open = goal(GoalMeasure.COUNT, 1.0, end = "2026-12-06").copy(id = "d")
        val q = GoalReview.reviewQueue(listOf(a, b, done, open), { emptyList() }, "2026-12-01")
        assertEquals(listOf("b", "a"), q.map { it.first.id })
        assertTrue(q.all { it.second == GoalStatus.MISSED })
    }

    @Test fun aTargetOfZeroNeverDecidesAnything() {
        assertEquals(GoalStatus.PENDING, GoalReview.status(goal(GoalMeasure.COUNT, 0.0), emptyList(), "2027-01-01"))
    }
}
