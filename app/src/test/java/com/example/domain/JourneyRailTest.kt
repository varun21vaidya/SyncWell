package com.example.domain

import com.example.data.Goal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JourneyRailTest {
    private val regions = JourneyMap.parse(File("src/main/res/raw/adventure_route.json").readText())
    private val ironwood = regions[2] // 14 days, camp on 7
    private val homeStart = regions[0] // 3 days, no camp
    private fun goal(title: String, target: Double, status: String = "PENDING", reward: String? = null) =
        Goal(title, title, "COMMITMENT", true, "COUNT", target, windowStart = "a", windowEnd = "b", status = status, regionIndex = 2, rewardText = reward)

    @Test fun oneDotPerDayWithTheWalkedOnesDone() {
        val pips = JourneyRail.pips(ironwood, 9)
        assertEquals(14, pips.size)
        assertEquals(9, pips.count { it.done })
        assertEquals((1..14).toList(), pips.map { it.day })
    }

    @Test fun theCampAndTheGateAreMarkedOnceEach() {
        val pips = JourneyRail.pips(ironwood, 0)
        assertEquals(listOf(7), pips.filter { it.camp }.map { it.day })
        assertEquals(listOf(14), pips.filter { it.gate }.map { it.day })
        assertTrue(JourneyRail.pips(homeStart, 0).none { it.camp }) // Home Start has no camp
    }

    @Test fun daysToTheGateNeverGoNegative() {
        assertEquals(5, JourneyRail.daysToGate(ironwood, 9))
        assertEquals(0, JourneyRail.daysToGate(ironwood, 14))
        assertEquals(0, JourneyRail.daysToGate(ironwood, 20))
    }

    @Test fun theGateWordsHandleOneAndZero() {
        assertEquals("Gate in 5 days", JourneyRail.gateText(ironwood, 9))
        assertEquals("Gate in 1 day", JourneyRail.gateText(ironwood, 13))
        assertEquals("At the gate", JourneyRail.gateText(ironwood, 14))
    }

    @Test fun theLabelsSkipTheCampWhereThereIsNone() {
        assertEquals(listOf("Day 1", "Camp · day 7", "Gate · day 14"), JourneyRail.labels(ironwood))
        assertEquals(listOf("Day 1", "Gate · day 3"), JourneyRail.labels(homeStart))
    }

    @Test fun todayIsTheNextDayToWalkNeverPastTheGate() {
        assertEquals(10, JourneyRail.todayDay(ironwood, 9))
        assertEquals(14, JourneyRail.todayDay(ironwood, 14))
        assertEquals(1, JourneyRail.todayDay(ironwood, 0))
    }

    @Test fun theMotivatorNamesTheFirstWaitingRewardOnly() {
        val goals = listOf(goal("DSA", 10.0, reward = "Burger"), goal("Course", 30.0, reward = "Movie night"))
        assertEquals("5 days to the gate. Burger unlocks with DSA 10.", JourneyRail.motivator(ironwood, 9, goals))
    }

    @Test fun theMotivatorIgnoresMetOrRewardlessCommitments() {
        val goals = listOf(goal("A", 5.0, status = "MET", reward = "Burger"), goal("B", 5.0))
        assertEquals("5 days to the gate.", JourneyRail.motivator(ironwood, 9, goals))
        assertEquals("3 days to the gate.", JourneyRail.motivator(homeStart, 0, emptyList()))
    }

    @Test fun theMotivatorAtTheGateAndOneDayOut() {
        assertEquals("You are at the gate.", JourneyRail.motivator(ironwood, 14, emptyList()))
        assertEquals("1 day to the gate.", JourneyRail.motivator(ironwood, 13, emptyList()))
    }

    @Test fun noRailWordSaysStepOrPressure() {
        val text = listOf(JourneyRail.gateText(ironwood, 9), JourneyRail.motivator(ironwood, 9, listOf(goal("DSA", 10.0, reward = "Burger")))) + JourneyRail.labels(ironwood)
        assertTrue(text.none { it.contains("step", ignoreCase = true) })
        assertFalse(text.any { it.contains("hurry", ignoreCase = true) || it.contains("lose", ignoreCase = true) })
    }
}
