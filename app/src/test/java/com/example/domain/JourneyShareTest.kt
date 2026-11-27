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
class JourneyShareTest {
    private val regions = JourneyMap.parse(File("src/main/res/raw/adventure_route.json").readText())
    private val goal = Goal("g", "DSA questions", "COMMITMENT", true, "COUNT", 10.0, targetLabel = "questions", windowStart = "2026-11-23", windowEnd = "2026-11-29", rewardText = "Burger")

    @Test fun everyCardCarriesTheSelfReportedFooter() {
        val cards = listOf(
            JourneyShare.journey(regions[2], 9, JourneyPosition(2, 9), 3, "Ivy"),
            JourneyShare.promise(goal, false),
            JourneyShare.ambition(JourneyState(ambitionText = "Land a backend role"), "2026-11-26", 12, false)
        )
        assertTrue(cards.all { it.footer.startsWith("Self-reported") })
    }

    @Test fun theJourneyCardSaysWhereYouAreInDaysAndNeverSteps() {
        val c = JourneyShare.journey(regions[2], 9, JourneyPosition(2, 9), 1, "Ivy")
        assertEquals(listOf("Ironwood Outpost", "Day 9 of 14", "19 of 66 days to the Summit", "1 stamp in my passport", "Walking with Ivy"), c.lines)
        assertTrue(c.lines.none { it.contains("step", ignoreCase = true) })
    }

    @Test fun thePromiseHidesTheRewardUnlessAsked() {
        assertEquals(listOf("DSA questions", "10 questions by 2026-11-29"), JourneyShare.promise(goal, false).lines)
        assertEquals("Reward: Burger", JourneyShare.promise(goal, true).lines.last())
        assertEquals(2, JourneyShare.promise(goal.copy(rewardText = null), true).lines.size)
    }

    @Test fun theAmbitionCardShowsTheWordsTheCountdownAndTheBigRewardOnlyWhenShown() {
        val s = JourneyState(ambitionText = "Land a backend role", identityText = "I practise daily", targetDate = "2026-12-06", bigRewardText = "Gaming")
        val hidden = JourneyShare.ambition(s, "2026-11-26", 1, false)
        assertEquals(listOf("Land a backend role", "I practise daily", "10 days to go", "1 day walked on the trail"), hidden.lines)
        assertEquals("Waiting for it: Gaming", JourneyShare.ambition(s, "2026-11-26", 1, true).lines.last())
    }

    @Test fun theCountdownIsWholeDaysAndSilentOncePassed() {
        assertEquals(10, JourneyShare.countdown("2026-12-06", "2026-11-26"))
        assertEquals(0, JourneyShare.countdown("2026-11-26", "2026-11-26"))
        assertNull(JourneyShare.countdown("2026-11-25", "2026-11-26")); assertNull(JourneyShare.countdown(null, "2026-11-26"))
        assertTrue(JourneyShare.ambition(JourneyState(targetDate = "2026-11-20"), "2026-11-26", 0, false).lines.none { it.contains("to go") })
        assertEquals("Today is the day", JourneyShare.ambition(JourneyState(targetDate = "2026-11-26"), "2026-11-26", 0, false).lines.first())
    }
}
