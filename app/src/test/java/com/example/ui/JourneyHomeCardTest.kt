package com.example.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.domain.JourneyCharacters
import com.example.domain.JourneyMap
import com.example.domain.JourneyRail
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w324dp-h760dp-xxhdpi")
class JourneyHomeCardTest {
    @get:Rule val compose = createComposeRule()
    private val regions = JourneyMap.parse(File("src/main/res/raw/adventure_route.json").readText())
    private val ironwood = regions[2]

    private fun show(step: Int, onClick: () -> Unit = {}) = compose.setContent {
        MyApplicationTheme { JourneyHomeCard(ironwood, step, JourneyCharacters.byId("ivy"), JourneyRail.motivator(ironwood, step, emptyList()), onClick) }
    }

    @Test fun theCardShowsTheRegionTheDayAndTheMotivator() {
        show(9)
        compose.onNodeWithTag("journey_card_region", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("journey_card_day", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("journey_card_motivator", useUnmergedTree = true).assertExists()
    }

    @Test fun theRailHasOnePipPerDayAndNamesTheWalkedCampAndGateOnes() {
        show(9)
        (1..14).forEach { compose.onNodeWithTag("journey_pip_$it", useUnmergedTree = true).assertExists() }
        compose.onNodeWithTag("journey_pip_9", useUnmergedTree = true).assertContentDescriptionEquals("Day 9, walked")
        compose.onNodeWithTag("journey_pip_10", useUnmergedTree = true).assertContentDescriptionEquals("Day 10")
        compose.onNodeWithTag("journey_pip_7", useUnmergedTree = true).assertContentDescriptionEquals("Day 7, walked, camp")
        compose.onNodeWithTag("journey_pip_14", useUnmergedTree = true).assertContentDescriptionEquals("Day 14, gate")
    }

    @Test fun tappingTheCardOpensTheJourney() {
        var opened = 0
        show(3) { opened++ }
        compose.onNodeWithTag("journey_strip").performClick()
        compose.waitForIdle()
        assertEquals(1, opened)
    }

    @Test fun theInvitationRowStillOpensTheJourney() {
        var opened = 0
        compose.setContent { MyApplicationTheme { JourneyStripRow("Try the Journey", null) { opened++ } } }
        compose.onNodeWithTag("journey_strip").performClick()
        compose.waitForIdle()
        assertEquals(1, opened)
    }
}
