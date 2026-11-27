package com.example.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.data.Goal
import com.example.domain.JourneyMap
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w324dp-h760dp-xxhdpi")
class JourneyMomentScreensTest {
    @get:Rule val compose = createComposeRule()
    private val regions = JourneyMap.parse(File("src/main/res/raw/adventure_route.json").readText())
    private val ironwood = regions[2]
    private fun goal(id: String, status: String, reward: String? = null) =
        Goal(id, "DSA", "COMMITMENT", true, "COUNT", 10.0, windowStart = "2026-11-23", windowEnd = "2026-11-29", status = status, regionIndex = 2, rewardText = reward)

    @Test fun theLetterSaysPassOrFailAndWhatChangedAndOffersNextUntilTheLast() {
        var next = 0
        compose.setContent { MyApplicationTheme { ReviewLetterScreen(goal("a", "MISSED", "Burger"), logged = 6.0, position = 1, total = 2) { next++ } } }
        compose.onNodeWithText("While you were away").assertExists()
        compose.onNodeWithText("Review 1 of 2").assertExists()
        compose.onNodeWithText("Commitment not met").assertExists()
        compose.onNodeWithTag("moment_next").performClick()
        compose.waitForIdle()
        assertEquals(1, next)
        compose.onNodeWithText("Next").assertExists()
    }

    @Test fun theLastLetterSaysGotIt() {
        compose.setContent { MyApplicationTheme { ReviewLetterScreen(goal("a", "MET"), 10.0, position = 2, total = 2) {} } }
        compose.onNodeWithText("Got it").assertExists()
        compose.onNodeWithText("Commitment met").assertExists()
    }

    @Test fun backToCampTellsWhatStaysYours() {
        var done = 0
        compose.setContent { MyApplicationTheme { BackToCampScreen("Back to the camp on day 7.", stamps = 3) { done++ } } }
        compose.onNodeWithText("Still yours").assertExists()
        compose.onNodeWithText("3 stamps in your passport").assertExists()
        compose.onNodeWithTag("moment_done").performClick()
        compose.waitForIdle()
        assertEquals(1, done)
    }

    @Test fun theTicketCanBeClaimedOrSavedForLater() {
        var claimed = 0; var later = 0
        compose.setContent { MyApplicationTheme { TicketScreen(goal("a", "MET", "Burger"), { later++ }, { claimed++ }) } }
        compose.onNodeWithText("Reward unlocked").assertExists()
        compose.onNodeWithTag("moment_reward").assertExists()
        compose.onNodeWithTag("moment_later").performClick()
        compose.onNodeWithTag("moment_claim").performClick()
        compose.waitForIdle()
        assertEquals(1, later); assertEquals(1, claimed)
    }

    @Test fun theCampOffersOneLineAndAnEasySkip() {
        var written: String? = null; var skipped = 0
        compose.setContent { MyApplicationTheme { CampScreen("Ironwood Outpost", null, { written = it }, { skipped++ }) } }
        compose.onNodeWithTag("moment_skip").performClick()
        compose.waitForIdle()
        assertEquals(1, skipped); assertNull(written)
        compose.onNodeWithTag("camp_text").performTextInput("Mornings work better")
        compose.onNodeWithTag("camp_save").performClick()
        compose.waitForIdle()
        assertEquals("Mornings work better", written)
    }

    @Test fun gateDayLetsYouCrossOnlyWhenEveryPledgeIsMet() {
        var crossed = 0
        compose.setContent {
            MyApplicationTheme { GateDayScreen(ironwood, listOf(goal("a", "MET"), goal("b", "PENDING")), null, null, "Tidewater Coast", { crossed++ }, {}) }
        }
        compose.onNodeWithText("Warden ${ironwood.warden.name}").assertExists()
        compose.onNodeWithTag("moment_cross").performClick() // disabled: one pledge is open
        compose.waitForIdle()
        assertEquals(0, crossed)
    }

    @Test fun gateDayCrossesIntoTheNextRegionOnceAllAreMet() {
        var crossed = 0
        compose.setContent { MyApplicationTheme { GateDayScreen(ironwood, listOf(goal("a", "MET")), "Land a role", null, "Tidewater Coast", { crossed++ }, {}) } }
        compose.onNodeWithText("Cross into Tidewater Coast").assertExists()
        compose.onNodeWithTag("moment_cross").performClick()
        compose.waitForIdle()
        assertEquals(1, crossed)
    }

    @Test fun theSummitShowsTheNumbersAndTheBigRewardState() {
        compose.setContent { MyApplicationTheme { SummitScreen(66, stamps = 6, bigReward = "Gaming", rewardBack = true) {} } }
        compose.onNodeWithText("66 days on the trail").assertExists()
        compose.onNodeWithText("6 stamps in your passport").assertExists()
        compose.onNodeWithText("It is yours again: Gaming").assertExists()
    }
}
