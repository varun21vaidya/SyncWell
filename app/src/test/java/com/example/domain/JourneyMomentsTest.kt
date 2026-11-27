package com.example.domain

import com.example.data.Goal
import com.example.data.JourneyState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JourneyMomentsTest {
    private val regions = JourneyMap.parse(File("src/main/res/raw/adventure_route.json").readText())
    private val ironwood = regions[2] // 14 days, camp on day 7
    private fun state(step: Int, region: Int = 2) = JourneyState(enabled = true, regionIndex = region, stepInRegion = step)
    private fun goal(id: String, status: String, reward: String? = null, claimed: String? = null, end: String = "2026-11-29") =
        Goal(id, id, "COMMITMENT", true, "COUNT", 10.0, windowStart = "2026-11-23", windowEnd = end, status = status, regionIndex = 2, rewardText = reward, rewardClaimedAt = claimed)

    private fun next(s: JourneyState, region: RegionDef? = ironwood, goals: List<Goal> = emptyList(), reviews: Set<String> = emptySet(), moments: Set<String> = emptySet(), setback: String? = null) =
        MomentQueue.next(s, region, goals, reviews, moments, setback)

    @Test fun nothingIsDueOnAnOrdinaryDay() = assertNull(next(state(3)))

    @Test fun decidedLettersComeFirstOldestFirstAndCountWhatIsWaiting() {
        val goals = listOf(goal("a", "MET", end = "2026-11-29"), goal("b", "MISSED", end = "2026-11-22"))
        val m = next(state(14), goals = goals, setback = "back") as Moment.Review
        assertEquals("b", m.goal.id); assertEquals(2, m.remaining)
        assertEquals(1, (next(state(14), goals = goals, reviews = setOf("b")) as Moment.Review).remaining)
    }

    @Test fun afterTheLettersTheWalkBackShows() {
        assertEquals(Moment.BackToCamp("Back to camp"), next(state(7), setback = "Back to camp"))
    }

    @Test fun aMetCommitmentWithAnUnclaimedRewardGivesATicketOnce() {
        val goals = listOf(goal("a", "MET", reward = "Burger"))
        val t = next(state(3), goals = goals, reviews = setOf("a")) as Moment.Ticket
        assertEquals("a", t.goal.id)
        assertNull(next(state(3), goals = goals, reviews = setOf("a"), moments = setOf("ticket-a")))
        assertNull(next(state(3), goals = listOf(goal("a", "MET", reward = "Burger", claimed = "2026-11-30")), reviews = setOf("a"))) // already claimed: no ticket
        assertNull(next(state(3), goals = listOf(goal("a", "MET")), reviews = setOf("a"))) // no reward, no ticket
    }

    @Test fun theCampShowsOnceWhenTheCharacterReachesIt() {
        assertNull(next(state(6)))
        assertEquals(Moment.Camp(2, "Ironwood Outpost"), next(state(7)))
        assertNull(next(state(9), moments = setOf("camp-2")))
    }

    @Test fun homeStartHasNoCampButHasAGate() {
        val home = regions[0]
        assertNull(next(state(2, region = 0), region = home))
        assertEquals(Moment.Gate(0), next(state(3, region = 0), region = home))
    }

    @Test fun gateDayShowsOnceAndTheCampComesBeforeIt() {
        assertEquals(Moment.Camp(2, "Ironwood Outpost"), next(state(14)))
        assertEquals(Moment.Gate(2), next(state(14), moments = setOf("camp-2")))
        assertNull(next(state(14), moments = setOf("camp-2", "gate-2")))
    }

    @Test fun pastTheLastRegionTheSummitShowsOnce() {
        assertEquals(Moment.Summit, next(state(0, region = 6), region = null))
        assertNull(next(state(0, region = 6), region = null, moments = setOf("summit")))
    }

    @Test fun eachMomentKeepsItsOwnKeyAndLettersAndTheWalkBackHaveNone() {
        assertEquals("ticket-a", MomentQueue.key(Moment.Ticket(goal("a", "MET"))))
        assertEquals("camp-2", MomentQueue.key(Moment.Camp(2, "x")))
        assertEquals("gate-2", MomentQueue.key(Moment.Gate(2)))
        assertEquals("summit", MomentQueue.key(Moment.Summit))
        assertNull(MomentQueue.key(Moment.Review(goal("a", "MET"), 1))); assertNull(MomentQueue.key(Moment.BackToCamp("x")))
        assertTrue(true)
    }
}
