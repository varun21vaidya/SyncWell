package com.example.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.data.Goal
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
class GoalProgressFormTest {
    @get:Rule val compose = createComposeRule()

    private var added: Pair<Double, String?>? = null
    private var closed = false

    private fun goal(measure: String) = Goal("g", "DSA", "COMMITMENT", true, measure, 10.0, windowStart = "2026-11-23", windowEnd = "2026-11-29")
    private fun show(measure: String) = compose.setContent {
        MyApplicationTheme { GoalProgressForm(goal(measure), 3.0, { a, n -> added = a to n }, { closed = true }) }
    }

    @Test fun addingNeedsAValidAmountThenSavesWithTheNote() {
        show("COUNT")
        compose.onNodeWithTag("progress_add").performClick()
        compose.waitForIdle()
        assertNull(added) // disabled with nothing typed
        compose.onNodeWithTag("progress_amount").performTextInput("2,5")
        compose.onNodeWithTag("progress_note").performTextInput("two easy ones")
        compose.onNodeWithTag("progress_add").performClick()
        compose.waitForIdle()
        assertEquals(2.5 to "two easy ones", added)
        assertTrue(closed)
    }

    @Test fun aMilestoneIsOneTapAndRecordsTheTarget() {
        show("MILESTONE")
        compose.onNodeWithTag("progress_add").performClick()
        compose.waitForIdle()
        assertEquals(10.0 to null, added)
    }
}
