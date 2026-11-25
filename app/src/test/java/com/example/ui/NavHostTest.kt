package com.example.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Back and tab taps around the full screens opened from Home, with stand-in screens. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w324dp-h760dp-xxhdpi")
class NavHostTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val ticks = mutableIntStateOf(0)
    private var handled = 0

    private fun show(deepLink: Destination? = null) = compose.setContent {
        MyApplicationTheme {
            AppNavHost(
                deepLink = deepLink, onDeepLinkHandled = { handled++ },
                home = { nav ->
                    Column {
                        Text("Home", Modifier.testTag("home_content"))
                        Button({ nav(Destination(Tab.HOME, Overlay.STATS)) }, Modifier.testTag("home_stats_btn")) { Text("Stats") }
                        Button({ ticks.intValue++ }, Modifier.testTag("home_tick")) { Text("Tick") }
                    }
                },
                routine = { Text("Routine", Modifier.testTag("routine_content")) },
                workout = {
                    var history by remember { mutableStateOf(false) }
                    BackHandler(enabled = history) { history = false } // the screen's own Back, like workout history
                    Column {
                        Button({ history = true }, Modifier.testTag("open_history")) { Text("History") }
                        if (history) Text("History", Modifier.testTag("workout_history")) else Text("Workout", Modifier.testTag("workout_content"))
                    }
                },
                focus = { Text("Focus", Modifier.testTag("focus_content")) },
                settings = { Text("Settings", Modifier.testTag("settings_content")) },
                overlayContent = { o, back ->
                    Column(Modifier.testTag("overlay_${o.name}")) { Button(back, Modifier.testTag("overlay_back")) { Text("Back") } }
                }
            )
        }
    }

    private fun pressBack() {
        compose.waitForIdle()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        compose.waitForIdle()
    }

    @Test fun aFullScreenReplacesHomeSoNothingBehindItCanBeTapped() {
        show()
        compose.onNodeWithTag("home_stats_btn").performClick()
        compose.onNodeWithTag("overlay_STATS").assertIsDisplayed()
        compose.onNodeWithTag("home_content").assertDoesNotExist()
        compose.onNodeWithTag("home_tick").assertDoesNotExist() // it used to stay composed underneath and could be hit by a stray tap
        assertEquals(0, ticks.intValue)
    }

    @Test fun backClosesTheFullScreenAndShowsHome() {
        show()
        compose.onNodeWithTag("home_stats_btn").performClick()
        pressBack()
        compose.onNodeWithTag("overlay_STATS").assertDoesNotExist()
        compose.onNodeWithTag("home_content").assertIsDisplayed()
    }

    @Test fun theBackArrowAndATabTapAlsoCloseIt() {
        show()
        compose.onNodeWithTag("home_stats_btn").performClick()
        compose.onNodeWithTag("overlay_back").performClick()
        compose.onNodeWithTag("overlay_STATS").assertDoesNotExist()
        compose.onNodeWithTag("home_stats_btn").performClick()
        compose.onNodeWithTag("nav_tab_1").performClick() // Routine
        compose.onNodeWithTag("overlay_STATS").assertDoesNotExist()
        compose.onNodeWithTag("routine_content").assertIsDisplayed()
    }

    @Test fun backFromAnotherTabGoesHome() {
        show()
        compose.onNodeWithTag("nav_tab_1").performClick()
        pressBack()
        compose.onNodeWithTag("home_content").assertIsDisplayed()
    }

    @Test fun aScreensOwnBackWinsOverTheAppWideBack() {
        show()
        compose.onNodeWithTag("nav_tab_2").performClick() // Workout
        compose.onNodeWithTag("open_history").performClick()
        compose.onNodeWithTag("workout_history").assertIsDisplayed()
        pressBack() // closes the history, does not jump Home
        compose.onNodeWithTag("workout_content").assertIsDisplayed()
        compose.onNodeWithTag("home_content").assertDoesNotExist()
        pressBack() // nothing deeper now: this one goes Home
        compose.onNodeWithTag("home_content").assertIsDisplayed()
    }

    @Test fun settingsIsATabAndBackFromItGoesHome() {
        show()
        compose.onNodeWithTag("nav_tab_4").performClick()
        compose.onNodeWithTag("settings_content").assertExists()
        compose.onNodeWithTag("home_content").assertDoesNotExist()
        pressBack()
        compose.onNodeWithTag("home_content").assertExists()
    }

    @Test fun aDeepLinkOpensTheFullScreenOnceAndIsMarkedHandled() {
        show(deepLink = Destination(Tab.HOME, Overlay.GOALS))
        compose.waitForIdle()
        compose.onNodeWithTag("overlay_GOALS").assertIsDisplayed()
        assertEquals(1, handled)
    }
}
