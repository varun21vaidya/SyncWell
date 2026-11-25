package com.example.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w324dp-h760dp-xxhdpi")
class NavigationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun barHasFiveTabsAndStatsAndGoalsAreNotAmongThem() {
        compose.setContent { MyApplicationTheme { AppNavigationBar(Tab.HOME) {} } }
        assertEquals(listOf("Home", "Routine", "Workout", "Focus", "Settings"), Tab.values().map { it.title })
        Tab.values().forEach { compose.onNodeWithTag("nav_tab_${it.ordinal}").assertIsDisplayed() }
        compose.onNodeWithTag("nav_tab_5").assertDoesNotExist()
    }

    @Test fun selectedTabIsMarkedAndTappingSelects() {
        val selected = mutableStateOf(Tab.HOME)
        compose.setContent { MyApplicationTheme { AppNavigationBar(selected.value) { selected.value = it } } }
        compose.onNodeWithTag("nav_tab_0").assertIsSelected()
        compose.onNodeWithTag("nav_tab_3").performClick()
        assertEquals(Tab.FOCUS, selected.value)
        compose.onNodeWithTag("nav_tab_3").assertIsSelected()
    }

    @Test fun deepLinksToGoalsAndStatsOpenThatScreenOnHomeAndSettingsIsATab() {
        assertEquals(Destination(Tab.HOME, Overlay.GOALS), Destination.parse("goals"))
        assertEquals(Destination(Tab.HOME, Overlay.GOALS), Destination.parse("TASKS")) // the old name
        assertEquals(Destination(Tab.HOME, Overlay.STATS), Destination.parse("Stats"))
        assertEquals(Destination(Tab.SETTINGS), Destination.parse(" config "))
        assertEquals(Destination(Tab.SETTINGS), Destination.parse("SETTINGS"))
        assertEquals(Destination(Tab.SETTINGS), Destination.parse("you"))
    }

    @Test fun otherTabsAndUnknownNames() {
        assertEquals(Destination(Tab.HOME), Destination.parse("HOME"))
        assertEquals(Destination(Tab.WORKOUT), Destination.parse("workout"))
        assertEquals(Destination(Tab.FOCUS), Destination.parse("FOCUS"))
        assertNull(Destination.parse("nonsense"))
        assertNull(Destination.parse(null))
    }

    @Test fun categoryCodesShowInSentenceCase() {
        assertEquals("Skincare", categoryLabel("SKINCARE"))
        assertEquals("Diet", categoryLabel("DIET"))
    }
}
