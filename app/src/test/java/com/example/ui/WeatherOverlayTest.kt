package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.example.domain.Weather
import com.example.ui.theme.LocalReducedMotion
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w324dp-h760dp-xxhdpi")
class WeatherOverlayTest {
    @get:Rule val compose = createComposeRule()

    // An infinite animation never lets Compose go idle, so the running overlay is covered by the particle tests; here the two cases that draw nothing
    @Test fun clearWeatherStartsNoAnimation() {
        compose.setContent { Box(Modifier.size(100.dp).testTag("box")) { WeatherOverlay(Weather.SUN) } }
        compose.waitForIdle()
        compose.onNodeWithTag("box").assertExists()
    }

    @Test fun reduceMotionSwitchesTheWeatherOff() {
        compose.setContent { CompositionLocalProvider(LocalReducedMotion provides true) { Box(Modifier.size(100.dp).testTag("box")) { WeatherOverlay(Weather.RAIN) } } }
        compose.waitForIdle() // would never return if a rain animation had started
        compose.onNodeWithTag("box").assertExists()
    }
}
