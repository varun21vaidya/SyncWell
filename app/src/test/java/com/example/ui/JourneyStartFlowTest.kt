package com.example.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.data.JourneyState
import com.example.domain.JourneyReminderSettings
import com.example.domain.WhyDraft
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w324dp-h760dp-xxhdpi")
class JourneyStartFlowTest {
    @get:Rule val compose = createComposeRule()

    private var finished: Pair<String, WhyDraft?>? = null
    private var closed = 0

    private fun flow() = compose.setContent { MyApplicationTheme { JourneyStartFlow("2026-11-28", { id, why -> finished = id to why }, { closed++ }) } }

    @Test fun theFlowStartsWithTheOptionalWhyAndAnyoneCanSkipIt() {
        flow()
        compose.onNodeWithText("Before you set off").assertExists()
        compose.onNodeWithTag("ambition_cancel").performClick() // Skip for now
        compose.waitForIdle()
        compose.onNodeWithTag("journey_choose").assertExists()
    }

    @Test fun skippingTheWhyStillStartsWithNothingWritten() {
        flow()
        compose.onNodeWithTag("ambition_cancel").performClick()
        compose.onNodeWithTag("journey_choose_confirm").performClick()
        compose.waitForIdle()
        assertEquals("max", finished?.first)
        assertNull(finished?.second)
    }

    @Test fun whatWasWrittenTravelsWithTheChosenTraveller() {
        flow()
        compose.onNodeWithTag("ambition_text").performTextInput("Land a backend role")
        compose.onNodeWithTag("ambition_save").performClick() // Set off
        compose.waitForIdle()
        compose.onNodeWithTag("journey_char_emma").performClick()
        compose.onNodeWithTag("journey_choose_confirm").performClick()
        compose.waitForIdle()
        assertEquals("emma", finished?.first)
        assertEquals("Land a backend role", finished?.second?.ambition)
    }

    @Test fun theBackArrowOnTheTravellerScreenReturnsToTheWhy() {
        flow()
        compose.onNodeWithTag("ambition_cancel").performClick()
        compose.onNodeWithText("← Back").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Before you set off").assertExists()
    }

    @Test fun theTravellerScreenShowsTheNameAndTheLineAboutThem() {
        var picked = "ivy"
        compose.setContent { MyApplicationTheme { ChooseTravellerScreen(picked, { picked = it }, { "Use ${it.name}" }, {}, {}) } }
        compose.onNodeWithTag("journey_choose_name").assertExists()
        compose.onNodeWithText("Use Ivy").assertExists()
        compose.onNodeWithTag("journey_pedestal").assertExists() // the avatar stands in for the five without full-body art
    }

    @Test fun maxStandsOnThePedestalInFullLengthAndEveryoneIsPickable() {
        compose.setContent { MyApplicationTheme { ChooseTravellerScreen("max", {}, { "Walk as ${it.name}" }, {}, {}) } }
        compose.onNodeWithText("Walk as Max").assertExists()
        listOf("max", "ivy", "lee", "emma", "charles", "georgia").forEach { compose.onNodeWithTag("journey_char_$it").assertExists() }
    }

    // ---- the Settings group ----
    private var toggled: Boolean? = null
    private var changeTaps = 0

    private fun settings(state: JourneyState?) = compose.setContent {
        MyApplicationTheme { SettingsJourneyContent(state, JourneyReminderSettings(), "2026-11-28", { toggled = it }, { changeTaps++ }, { _, _, _, _ -> }, {}) }
    }

    @Test fun beforeTheFirstStartOnlyTheSwitchShows() {
        settings(null)
        compose.onNodeWithTag("journey_switch").assertExists()
        compose.onNodeWithTag("journey_change_traveller").assertDoesNotExist()
        compose.onNodeWithTag("journey_reminders").assertDoesNotExist()
    }

    @Test fun onceStartedTheTravellerWhyAndRemindersAppear() {
        settings(JourneyState(enabled = true, characterId = "georgia", ambitionText = "Land a role"))
        compose.onNodeWithTag("settings_traveller_name").assertExists()
        compose.onNodeWithText("Land a role").assertExists()
        compose.onNodeWithTag("journey_reminders").assertExists()
        compose.onNodeWithTag("journey_change_traveller").performClick()
        compose.waitForIdle()
        assertEquals(1, changeTaps)
    }

    @Test fun pausedExplainsThatSwitchingOnRestartsTheRegion() {
        settings(JourneyState(enabled = false))
        compose.onNodeWithText("Paused. Switching it on again restarts your current region.").assertExists()
    }

    @Test fun theSwitchReportsTheNewValue() {
        settings(JourneyState(enabled = true))
        compose.onNodeWithTag("journey_switch").performClick()
        compose.waitForIdle()
        assertEquals(false, toggled)
        assertTrue(closed == 0)
    }
}
