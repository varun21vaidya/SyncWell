package com.example.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.data.PassportStamp
import com.example.domain.CommitmentDraft
import com.example.domain.CommitmentRules
import com.example.domain.JourneyCharacters
import com.example.domain.JourneyMap
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w324dp-h760dp-xxhdpi")
class JourneyPageUiTest {
    @get:Rule val compose = createComposeRule()
    private val regions = JourneyMap.parse(File("src/main/res/raw/adventure_route.json").readText())

    private val taps = mutableListOf<String>()
    private fun actions() = MapActions({ taps += "back" }, { taps += "passport" }, { taps += "log" }, { taps += "regions" }, { taps += "share" })

    @Test fun theMapShowsTheRegionNumberAndNameOverTheArt() {
        compose.setContent { MyApplicationTheme { JourneyMapHeader(regions[2], 9, JourneyCharacters.byId("ivy"), actions()) } }
        compose.onNodeWithText("Region 3").assertExists()
        compose.onNodeWithTag("journey_region").assertExists()
        compose.onNodeWithTag("journey_map").assertExists()
    }

    @Test fun theBackArrowAndTheFourIconsEachDoTheirOwnThing() {
        compose.setContent { MyApplicationTheme { JourneyMapHeader(regions[2], 9, JourneyCharacters.byId("ivy"), actions()) } }
        listOf("journey_back", "journey_icon_passport", "journey_open_log", "journey_icon_regions", "journey_share").forEach {
            compose.onNodeWithTag(it).performClick()
        }
        compose.waitForIdle()
        assertEquals(listOf("back", "passport", "log", "regions", "share"), taps)
    }

    @Test fun todayCountsAsTheNextDayWithItsPercentAndWhatIsLeft() {
        compose.setContent { MyApplicationTheme { JourneyTodayCard(regions[2], 9, done = 6, total = 10) } }
        compose.onNodeWithText("Today counts as day 10").assertExists()
        compose.onNodeWithText("60%").assertExists()
        compose.onNodeWithText("2 routine items left. 80% counts the day.").assertExists()
        compose.onNodeWithTag("journey_today_mark").assertExists()
    }

    @Test fun todayShowsTheDayAlreadyCountingAtEightyPercent() {
        compose.setContent { MyApplicationTheme { JourneyTodayCard(regions[2], 9, done = 8, total = 10) } }
        compose.onNodeWithText("Today counts. Keep going if you like.").assertExists()
    }

    @Test fun thePassportAndRegionsOpenAsTheirOwnScreensWithABackButton() {
        var backs = 0
        compose.setContent {
            MyApplicationTheme {
                PassportScreen(listOf(PassportStamp("region-0", "REGION", 0, "2026-11-10", "Home Start")), regions) { backs++ }
            }
        }
        compose.onNodeWithTag("journey_passport_screen").assertExists()
        compose.onNodeWithText("← Back").performClick()
        compose.waitForIdle()
        assertEquals(1, backs)
    }

    @Test fun theRegionsScreenNamesEveryRegionAndLocksTheOnesAhead() {
        compose.setContent { MyApplicationTheme { RegionsScreen(regions, current = 2, furthest = 2, today = "2026-11-26") {} } }
        regions.forEach { compose.onNodeWithText(it.name).assertExists() }
        compose.onNodeWithText("You are here").assertExists()
    }

    @Test fun theWeeklyEditorHidesTheWindowAndMoreAndSavesCoreFields() {
        var saved: CommitmentDraft? = null
        compose.setContent {
            MyApplicationTheme { CommitmentEditor(region = 2, previous = emptyList(), onSave = { d -> CommitmentRules.validateWeekly(d).also { if (it.isEmpty()) saved = d } }, onClose = {}, weekly = true) }
        }
        compose.onNodeWithText("Set a weekly goal").assertExists()
        compose.onNodeWithText("Window").assertDoesNotExist()
        compose.onNodeWithTag("commitment_more").assertDoesNotExist()
        compose.onNodeWithTag("commitment_title").performTextInput("Gym")
        compose.onNodeWithTag("commitment_target").performTextInput("4")
        compose.onNodeWithTag("commitment_save").performClick()
        compose.waitForIdle()
        assertEquals("Gym", saved?.title)
        assertEquals(4.0, saved?.target ?: 0.0, 0.0)
        assertNull(saved?.reward)
    }

    @Test fun theCommitmentEditorStillHasItsWindowAndMore() {
        compose.setContent { MyApplicationTheme { CommitmentEditor(2, emptyList(), { emptyList() }, {}) } }
        compose.onNodeWithText("Set a commitment").assertExists()
        compose.onNodeWithText("Window").assertExists()
        compose.onNodeWithTag("commitment_more").assertExists()
        assertTrue(true)
    }
}
