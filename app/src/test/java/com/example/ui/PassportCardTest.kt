package com.example.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.example.data.PassportStamp
import com.example.domain.JourneyMap
import com.example.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w324dp-h760dp-xxhdpi")
class PassportCardTest {
    @get:Rule val compose = createComposeRule()
    private val regions = JourneyMap.parse(File("src/main/res/raw/adventure_route.json").readText())

    @Test fun anEmptyPassportInvitesTheFirstStamp() {
        compose.setContent { MyApplicationTheme { PassportCard(emptyList(), regions) } }
        compose.onNodeWithText("Cross a region or meet a commitment to earn your first stamp.").assertExists()
    }

    @Test fun eachStampIsDrawnWithItsLabelForEveryWeather() {
        val stamps = listOf(
            PassportStamp("region-0", "REGION", 0, "2026-11-10", "Home Start"),
            PassportStamp("region-3", "REGION", 3, "2026-11-24", "Tidewater Coast"), // rain
            PassportStamp("region-4", "REGION", 4, "2026-12-01", "Obsidian Peak"), // snow
            PassportStamp("commit-a", "COMMITMENT", 2, "2026-11-20", "DSA questions")
        )
        compose.setContent { MyApplicationTheme { PassportCard(stamps, regions) } }
        stamps.forEach { compose.onNodeWithTag("stamp_${it.id}").assertExists() }
        compose.onNodeWithText("Tidewater Coast").assertExists()
        compose.onNodeWithText("DSA questions").assertExists()
    }
}
