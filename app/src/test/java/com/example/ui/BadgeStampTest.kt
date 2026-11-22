package com.example.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.domain.Badge
import com.example.domain.BadgeCelebration
import com.example.domain.BadgeRules
import com.example.ui.theme.LocalReducedMotion
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Shown once, and a queue of several one after another (at most 3 in a row). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w324dp-h760dp-xxhdpi")
class BadgeStampTest {
    @get:Rule val compose = createComposeRule()

    private fun badge(id: String) = BadgeRules.ALL.single { it.id == id }

    @Test fun stampShowsTheTitleAndCopyAndKeepGoingDismisses() {
        var dismissed = 0
        compose.setContent {
            MyApplicationTheme { CompositionLocalProvider(LocalReducedMotion provides true) { BadgeStampOverlay(badge("DAYS_25"), onShare = {}, onKeepGoing = { dismissed++ }) } }
        }
        compose.onNodeWithContentDescription("Stamped: 25 good days").assertIsDisplayed()
        compose.onNodeWithTag("stamp_keep_going").performClick()
        assertEquals(1, dismissed)
    }

    @Test fun shareButtonCallsShareWithoutDismissing() {
        var shared: Badge? = null; var dismissed = 0
        compose.setContent {
            MyApplicationTheme { CompositionLocalProvider(LocalReducedMotion provides true) { BadgeStampOverlay(badge("PERFECT_DAY"), onShare = { shared = badge("PERFECT_DAY") }, onKeepGoing = { dismissed++ }) } }
        }
        compose.onNodeWithTag("stamp_share").performClick()
        assertEquals("PERFECT_DAY", shared?.id)
        assertEquals(0, dismissed)
    }

    @Test fun aQueueOfSeveralShowsOneAfterAnotherThenNothing() {
        val queue = mutableStateListOf(badge("FIRST_TICK"), badge("DAY_ONE"), badge("STREAK_3"))
        compose.setContent {
            MyApplicationTheme { CompositionLocalProvider(LocalReducedMotion provides true) { StampQueueHost(queue, onShare = {}, onKeepGoing = { queue.removeAt(0) }) } }
        }
        listOf("First tick", "Day one", "Three in a row").forEach { title ->
            compose.onNodeWithContentDescription("Stamped: $title").assertIsDisplayed()
            compose.onNodeWithTag("stamp_keep_going").performClick()
        }
        compose.onNodeWithTag("stamp_overlay").assertDoesNotExistCompat()
    }

    @Test fun animatedPathRendersToo() {
        compose.setContent { MyApplicationTheme { BadgeStampOverlay(badge("STREAK_100"), onShare = {}, onKeepGoing = {}) } }
        compose.onNodeWithTag("stamp_overlay").assertIsDisplayed()
    }

    @Test fun medalShownOnceAcrossRestartsAndOnlyThreeInARow() {
        val order = BadgeRules.ALL.map { it.id }
        val earned = setOf("FIRST_TICK", "DAY_ONE", "STREAK_3", "PERFECT_DAY")
        val first = BadgeCelebration.plan(earned, emptySet(), silent = false, order = order)
        assertEquals(3, first.queue.size)
        // the app restarts: the celebrated set was stored, so none of them pops up again
        assertTrue(BadgeCelebration.plan(earned, first.celebrated, silent = false, order = order).queue.isEmpty())
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertDoesNotExistCompat() = this.assertDoesNotExist()
}
