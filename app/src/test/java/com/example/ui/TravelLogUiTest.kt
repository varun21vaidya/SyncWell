package com.example.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.data.JourneyNote
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w324dp-h760dp-xxhdpi")
class TravelLogUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun theReflectionIsOptionalAndSavesTheTypedLine() {
        var saved: String? = null
        compose.setContent { MyApplicationTheme { CampReflectionCard("Ironwood Outpost", null) { saved = it } } }
        compose.onNodeWithTag("camp_save").performClick() // empty: disabled, nothing saved
        compose.waitForIdle()
        assertEquals(null, saved)
        compose.onNodeWithTag("camp_text").performTextInput("Mornings work better")
        compose.onNodeWithTag("camp_save").performClick()
        compose.waitForIdle()
        assertEquals("Mornings work better", saved)
    }

    @Test fun anEmptyLogInvitesTheFirstDay() {
        compose.setContent { MyApplicationTheme { TravelLogScreen(emptyList()) {} } }
        compose.onNodeWithText("Nothing here yet. Your days on the trail will appear as you walk them.").assertExists()
    }

    @Test fun theLogShowsDatesAndBothKindsOfLine() {
        val notes = listOf(
            JourneyNote("a", "2026-11-26", "REFLECTION", "Mornings work better"),
            JourneyNote("b", "2026-11-25", "LOG", "Day 3 of Verdant Trail")
        )
        compose.setContent { MyApplicationTheme { TravelLogScreen(notes) {} } }
        compose.onNodeWithText("Mornings work better").assertExists()
        compose.onNodeWithText("Day 3 of Verdant Trail").assertExists()
        compose.onNodeWithText("2026-11-25").assertExists()
    }
}
