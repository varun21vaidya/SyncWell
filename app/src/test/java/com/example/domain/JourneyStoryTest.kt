package com.example.domain

import com.example.data.Goal
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
class JourneyStoryTest {
    private val regions = JourneyMap.parse(File("src/main/res/raw/adventure_route.json").readText())
    private fun goal(id: String, status: String, end: String, reward: String? = null) =
        Goal(id, id, "COMMITMENT", true, "COUNT", 10.0, windowStart = "2026-11-23", windowEnd = end, status = status, regionIndex = 2, rewardText = reward)

    @Test fun everyRegionHasItsOwnWardenWithAllLines() {
        assertEquals(regions.size, regions.map { it.warden.name }.toSet().size)
        regions.forEach { r -> assertTrue(listOf(r.warden.name, r.warden.personality, r.warden.challenge, r.warden.open, r.warden.closed).all { it.isNotBlank() }) }
    }

    @Test fun wardenLinesAreCalmInformationNotGuilt() {
        val banned = listOf("fail", "lazy", "shame", "disappoint", "weak")
        regions.forEach { r -> listOf(r.warden.challenge, r.warden.open, r.warden.closed).forEach { line -> banned.forEach { assertTrue("$line / $it", !line.lowercase().contains(it)) } } }
    }

    @Test fun theWardenQuotesTheAmbitionAndIdentityOnlyWhenTheUserWroteThem() {
        val w = regions[2].warden
        assertEquals(listOf(w.challenge), GateScript.opening(w, null, null))
        assertEquals(listOf(w.challenge), GateScript.opening(w, "  ", ""))
        val full = GateScript.opening(w, "Land a backend role", "practises daily")
        assertEquals(3, full.size)
        assertTrue(full[1].contains("Land a backend role")); assertTrue(full[2].contains("practises daily"))
        assertTrue(GateScript.opening(w, null, "I'm becoming someone who practises daily")[1].contains("\"practises daily\""))
    }

    @Test fun theVerdictFollowsTheCommitments() {
        val w = regions[1].warden
        assertEquals(w.open, GateScript.verdict(w, true)); assertEquals(w.closed, GateScript.verdict(w, false))
    }

    @Test fun theQueueHoldsDecidedCommitmentsNotYetSeenOldestFirst() {
        val goals = listOf(goal("a", "MET", "2026-11-29"), goal("b", "MISSED", "2026-11-22"), goal("c", "PENDING", "2026-11-20"), goal("d", "MET", "2026-11-15"),
            goal("w", "MET", "2026-11-10").copy(kind = "WEEKLY"))
        assertEquals(listOf("b", "a"), ReviewQueue.unseen(goals, seen = setOf("d")).map { it.id })
        assertTrue(ReviewQueue.unseen(goals, seen = setOf("a", "b", "d")).isEmpty())
    }

    @Test fun theReviewSaysPassOrFailAndWhatChanged() {
        assertEquals("You logged 10 of 10 by 2026-11-29. Your reward is unlocked: Burger.", ReviewQueue.describe(goal("a", "MET", "2026-11-29", "Burger"), 10.0))
        val missed = ReviewQueue.describe(goal("b", "MISSED", "2026-11-29", "Burger"), 6.5)
        assertTrue(missed.startsWith("You logged 6.5 of 10")); assertTrue(missed.contains("start of the region")); assertTrue(missed.contains("carries over"))
        assertTrue(!ReviewQueue.describe(goal("c", "MISSED", "x"), 0.0).contains("carries over"))
    }

    @Test fun aSetbackIsExplainedByWhereTheCharacterLanded() {
        assertNull(SetbackNote.of("Ironwood Outpost", 5, 5, 7)); assertNull(SetbackNote.of("Ironwood Outpost", 5, 9, 7))
        assertTrue(SetbackNote.of("Ironwood Outpost", 9, 7, 7)!!.startsWith("Back to the camp on day 7"))
        assertTrue(SetbackNote.of("Ironwood Outpost", 9, 0, 7)!!.startsWith("Back to the start of Ironwood Outpost"))
        assertTrue(SetbackNote.of("Verdant Trail", 6, 2, 3)!!.startsWith("Back to day 2"))
        // the reassurance is always there
        assertTrue(SetbackNote.of("Verdant Trail", 6, 3, 3)!!.contains("only the map position changes"))
    }
}
