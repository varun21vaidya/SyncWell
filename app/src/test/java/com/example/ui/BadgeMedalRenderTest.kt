package com.example.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.domain.BadgeInput
import com.example.domain.BadgeRules
import com.example.domain.BadgeDay
import com.example.domain.BadgeState
import com.example.ui.theme.LocalReducedMotion
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The medals must render (without throwing) in the earned, locked-with-progress and reduced-motion states. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w324dp-h760dp-xxhdpi")
class BadgeMedalRenderTest {
    @get:Rule val compose = createComposeRule()

    private val twentyFive = BadgeRules.ALL.single { it.id == "DAYS_25" }
    private val hundred = BadgeRules.ALL.single { it.id == "DAYS_100" }

    @Test fun earnedMedalRenders() {
        compose.setContent { MyApplicationTheme { BadgeMedal(twentyFive, earned = true, size = 84.dp) } }
        compose.onNodeWithContentDescription("25 good days, earned").assertIsDisplayed()
    }

    @Test fun lockedMedalRendersWithoutTheWordLocked() {
        compose.setContent { MyApplicationTheme { BadgeMedal(twentyFive, earned = false, size = 84.dp) } }
        compose.onNodeWithContentDescription("25 good days, not yet earned").assertIsDisplayed()
    }

    @Test fun reducedMotionRendersTheStaticShineForEarnedAndDoubleRosetteMedals() {
        compose.setContent {
            MyApplicationTheme { CompositionLocalProvider(LocalReducedMotion provides true) { BadgeMedal(hundred, earned = true, size = 140.dp) } }
        }
        compose.onNodeWithContentDescription("100 good days, earned").assertIsDisplayed()
    }

    @Test fun everyCatalogueMedalRendersInBothStates() {
        compose.setContent {
            MyApplicationTheme {
                androidx.compose.foundation.layout.Column {
                    BadgeRules.ALL.forEach { BadgeMedal(it, earned = true, size = 40.dp); BadgeMedal(it, earned = false, size = 40.dp) }
                }
            }
        }
        compose.onNodeWithContentDescription("First tick, earned").assertExists()
        compose.onNodeWithContentDescription("Fresh start, not yet earned").assertExists()
    }

    @Test fun medalGridListsEveryMedalGroupedAndTapSelectsOne() {
        val states = BadgeRules.evaluate(BadgeInput(listOf(BadgeDay("2026-11-01", 1.0, false, 0)), emptyList(), 2500, tickedItems = 3))
        var selected: BadgeState? = null
        compose.setContent { MyApplicationTheme { MedalGrid(states, emptyMap()) { selected = it } } }
        compose.onNodeWithTag("medal_grid").assertExists()
        compose.onNodeWithTag("medal_FIRST_TICK").performClick()
        assertEquals("FIRST_TICK", selected?.badge?.id)
    }
}
