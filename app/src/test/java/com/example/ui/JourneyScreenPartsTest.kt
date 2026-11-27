package com.example.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.JourneyState
import com.example.domain.JourneyCharacters
import com.example.domain.JourneyMap
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w324dp-h760dp-xxhdpi")
class JourneyScreenPartsTest {
    @get:Rule val compose = createComposeRule()
    private val regions = JourneyMap.parse(File("src/main/res/raw/adventure_route.json").readText())

    @Test fun resumingExplainsTheRestartAndStartsWithoutQuestions() {
        var resumed = 0
        compose.setContent { MyApplicationTheme { JourneyResume { resumed++ } } }
        compose.onNodeWithText("Welcome back").assertExists()
        compose.onNodeWithText("Resume the Journey").performClick()
        compose.waitForIdle()
        assertEquals(1, resumed)
    }

    @Test fun theMapShowsTheCharacterOnAPaintedRegion() {
        compose.setContent { MyApplicationTheme { JourneyMapCard(regions[1], 3, JourneyCharacters.byId("ivy")) } }
        compose.onNodeWithTag("journey_character").assertExists()
    }

    @Test fun theSummitHasNoArtYetAndStillDrawsTheCharacter() {
        compose.setContent { MyApplicationTheme { JourneyMapCard(regions.last(), 0, JourneyCharacters.byId("max")) } }
        compose.onNodeWithTag("journey_character").assertExists()
    }

    @Test fun theStripTellsWhereYouAreOrInvitesYouAndNeverSaysStep() {
        assertTrue(JourneyMap.stripText(null, regions).startsWith("Try the Journey"))
        assertEquals("Journey paused. Tap to resume", JourneyMap.stripText(JourneyState(enabled = false), regions))
        val text = JourneyMap.stripText(JourneyState(enabled = true, regionIndex = 2, stepInRegion = 9), regions)
        assertEquals("Ironwood Outpost · Day 9 of 14 · Gate on day 14", text)
        assertFalse(text.contains("step", ignoreCase = true))
        assertEquals("Ironwood Outpost · Day 3 of 14 · Camp on day 7", JourneyMap.stripText(JourneyState(enabled = true, regionIndex = 2, stepInRegion = 3), regions))
    }
}
