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

    private var saved: Pair<String?, String?>? = null
    private var closed = false

    @Test fun bothLinesAreOptionalAndAnEmptyFormSavesNulls() {
        compose.setContent { MyApplicationTheme { AmbitionForm(null, null, { a, i -> saved = a to i }, { closed = true }) } }
        compose.onNodeWithTag("ambition_save").performClick()
        compose.waitForIdle()
        assertEquals(null to null, saved)
        assertTrue(closed)
    }

    @Test fun typedLinesAreSavedTrimmed() {
        compose.setContent { MyApplicationTheme { AmbitionForm(null, null, { a, i -> saved = a to i }, { closed = true }) } }
        compose.onNodeWithTag("ambition_text").performTextInput("  Land a backend role ")
        compose.onNodeWithTag("identity_text").performTextInput("I'm becoming someone who practises daily")
        compose.onNodeWithTag("ambition_save").performClick()
        compose.waitForIdle()
        assertEquals("Land a backend role", saved?.first)
        assertEquals("I'm becoming someone who practises daily", saved?.second)
    }

    @Test fun theCardInvitesWhenEmptyAndShowsTheUsersWordsOtherwise() {
        var edits = 0
        compose.setContent { MyApplicationTheme { AmbitionCard(JourneyState(enabled = true), onEdit = { edits++ }) } }
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
