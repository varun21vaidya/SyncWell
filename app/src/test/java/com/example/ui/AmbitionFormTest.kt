package com.example.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.data.JourneyState
import com.example.domain.Ambition
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
class AmbitionFormTest {
    @get:Rule val compose = createComposeRule()

    private val today = "2026-11-26"
    private var saved: List<String?>? = null
    private var closed = false

    private fun show(ambition: String? = null, identity: String? = null, date: String? = null, reward: String? = null) = compose.setContent {
        MyApplicationTheme { AmbitionForm(ambition, identity, date, reward, today, { a, i, d, r -> saved = listOf(a, i, d, r) }, { closed = true }) }
    }

    @Test fun everythingIsOptionalAndAnEmptyFormSavesNulls() {
        show()
        compose.onNodeWithTag("ambition_save").performClick()
        compose.waitForIdle()
        assertEquals(listOf<String?>(null, null, null, null), saved)
        assertTrue(closed)
    }

    @Test fun typedLinesAreSavedTrimmed() {
        show()
        compose.onNodeWithTag("ambition_text").performTextInput("  Land a backend role ")
        compose.onNodeWithTag("identity_text").performTextInput("I'm becoming someone who practises daily")
        compose.onNodeWithTag("ambition_save").performClick()
        compose.waitForIdle()
        assertEquals("Land a backend role", saved?.get(0))
        assertEquals("I'm becoming someone who practises daily", saved?.get(1))
        assertNull(saved?.get(2))
    }

    @Test fun theDateAndTheRewardFieldsOnlyAppearStepByStep() {
        show()
        compose.onNodeWithTag("ambition_date").assertDoesNotExist() // no ambition yet
        compose.onNodeWithTag("ambition_text").performTextInput("Land a role")
        compose.onNodeWithTag("ambition_date").assertExists()
        compose.onNodeWithTag("big_reward").assertDoesNotExist() // no valid date yet
        compose.onNodeWithTag("ambition_date").performTextInput("2026-12-06")
        compose.onNodeWithTag("big_reward").assertExists()
    }

    @Test fun aDateAndARewardAreSavedTogether() {
        show()
        compose.onNodeWithTag("ambition_text").performTextInput("Land a role")
        compose.onNodeWithTag("ambition_date").performTextInput("2026-12-06")
        compose.onNodeWithTag("big_reward").performTextInput("Gaming")
        compose.onNodeWithTag("ambition_save").performClick()
        compose.waitForIdle()
        assertEquals(listOf<String?>("Land a role", null, "2026-12-06", "Gaming"), saved)
    }

    @Test fun aPastDateBlocksSavingUntilFixed() {
        show()
        compose.onNodeWithTag("ambition_text").performTextInput("Land a role")
        compose.onNodeWithTag("ambition_date").performTextInput("2026-11-01")
        compose.onNodeWithTag("ambition_save").performClick()
        compose.waitForIdle()
        assertNull(saved) // disabled: the date has passed
    }

    @Test fun aSavedDateThatHasPassedStaysEditableWithoutBlocking() {
        show(ambition = "Land a role", date = "2026-11-10", reward = "Gaming")
        compose.onNodeWithTag("ambition_save").performClick()
        compose.waitForIdle()
        assertEquals(listOf<String?>("Land a role", null, "2026-11-10", "Gaming"), saved)
    }

    @Test fun theCardShowsTheCountdownAndTheLockedReward() {
        compose.setContent {
            MyApplicationTheme { AmbitionCard(JourneyState(enabled = true, ambitionText = "Land a role", targetDate = "2026-12-06", bigRewardText = "Gaming"), today, onEdit = {}) }
        }
        compose.onNodeWithTag("journey_countdown").assertExists()
        compose.onNodeWithTag("journey_big_reward").assertExists()
    }

    @Test fun theCardInvitesWhenEmptyAndOpensTheEditor() {
        var edits = 0
        compose.setContent { MyApplicationTheme { AmbitionCard(JourneyState(enabled = true), today, onEdit = { edits++ }) } }
        compose.onNodeWithTag("journey_countdown").assertDoesNotExist()
        compose.onNodeWithTag("journey_ambition_edit").performClick()
        compose.waitForIdle()
        assertEquals(1, edits)
    }

    @Test fun cleaningTrimsCutsAndTurnsEmptyIntoNull() {
        assertNull(Ambition.clean("   ", 80)); assertNull(Ambition.clean(null, 80))
        assertEquals("abc", Ambition.clean("  abc  ", 80))
        assertEquals("ab", Ambition.clean("ab cd", 3)) // cut at 3 leaves "ab " and the trailing space goes
        assertEquals(Ambition.MAX_AMBITION, Ambition.clean("x".repeat(500), Ambition.MAX_AMBITION)!!.length)
    }
}
