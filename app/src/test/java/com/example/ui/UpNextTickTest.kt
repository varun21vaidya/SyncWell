package com.example.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.domain.DayItem
import com.example.ui.theme.LocalReducedMotion
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** One tap on Done ticks the item: at once under reduced motion, after the short check animation otherwise. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w324dp-h760dp-xxhdpi")
class UpNextTickTest {
    @get:Rule val compose = createComposeRule()
    private val item = DayItem("i1", "Sunscreen", "SKINCARE", "08:00", false)

    @Test fun reducedMotionTicksImmediately() {
        val ticked = mutableListOf<String>()
        compose.setContent { MyApplicationTheme { CompositionLocalProvider(LocalReducedMotion provides true) { UpNextCard(item) { ticked += it.id } } } }
        compose.onNodeWithTag("up_next_done").performClick()
        assertEquals(listOf("i1"), ticked)
    }

    @Test fun animatedTickShowsTheCheckThenTicksOnce() {
        val ticked = mutableListOf<String>()
        compose.setContent { MyApplicationTheme { UpNextCard(item) { ticked += it.id } } }
        compose.onNodeWithTag("up_next_done").performClick()
        compose.mainClock.advanceTimeBy(600)
        compose.waitForIdle()
        assertEquals(listOf("i1"), ticked)
        compose.onNodeWithTag("up_next_card").assertIsDisplayed()
    }

    @Test fun nothingLeftShowsTheDoneMessageAndNoButton() {
        compose.setContent { MyApplicationTheme { UpNextCard(null) {} } }
        compose.onNodeWithTag("up_next_card").assertIsDisplayed()
        compose.onNodeWithTag("up_next_done").assertDoesNotExist()
    }
}
