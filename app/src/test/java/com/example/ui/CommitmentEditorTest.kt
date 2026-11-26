package com.example.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.domain.CommitmentDraft
import com.example.domain.CommitmentRules
import com.example.domain.DraftError
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w324dp-h760dp-xxhdpi")
class CommitmentEditorTest {
    @get:Rule val compose = createComposeRule()

    private var saved: CommitmentDraft? = null
    private var closed = false

    private fun show() = compose.setContent {
        MyApplicationTheme {
            CommitmentEditor(region = 2, previous = emptyList(), onSave = { d ->
                CommitmentRules.validate(d, 2, 0).also { if (it.isEmpty()) saved = d }
            }, onClose = { closed = true })
        }
    }

    @Test fun anEmptyFormShowsWhatIsMissingAndDoesNotClose() {
        show()
        compose.onNodeWithTag("commitment_save").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Give the commitment a name.").assertExists()
        compose.onNodeWithText("The target must be above 0 (and at most 100 for a percentage).").assertExists()
        assertTrue(saved == null && !closed)
    }

    @Test fun aFilledFormSavesAndCloses() {
        show()
        compose.onNodeWithTag("commitment_title").performTextInput("DSA questions")
        compose.onNodeWithTag("commitment_target").performTextInput("10")
        compose.onNodeWithTag("commitment_save").performClick()
        compose.waitForIdle()
        assertEquals("DSA questions", saved?.title)
        assertEquals(10.0, saved?.target ?: 0.0, 0.0)
        assertTrue(closed)
    }

    @Test fun moreRevealsTheRewardAndPlanFields() {
        show()
        compose.onNodeWithText("Reward when met (optional)").assertDoesNotExist()
        compose.onNodeWithTag("commitment_more").performClick()
        compose.onNodeWithText("Reward when met (optional)").assertExists()
        compose.onNodeWithText("If I can't, then I will…").assertExists()
    }
}
