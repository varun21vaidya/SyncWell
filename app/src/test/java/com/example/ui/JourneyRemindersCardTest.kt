package com.example.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.domain.JourneyReminderSettings
import com.example.domain.JourneySlot
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w324dp-h760dp-xxhdpi")
class JourneyRemindersCardTest {
    @get:Rule val compose = createComposeRule()

    private var latest: JourneyReminderSettings? = null

    private fun show(s: JourneyReminderSettings) = compose.setContent { MyApplicationTheme { JourneyRemindersCard(s) { latest = it } } }

    @Test fun theSlotsAndQuietHoursStayHiddenUntilTheMasterSwitchIsOn() {
        show(JourneyReminderSettings())
        compose.onNodeWithTag("jrem_slot_NINE").assertDoesNotExist()
        compose.onNodeWithTag("jrem_master").performClick()
        compose.waitForIdle()
        assertTrue(latest!!.master)
    }

    @Test fun everySlotHasItsOwnSwitch() {
        show(JourneyReminderSettings(master = true))
        JourneySlot.values().forEach { compose.onNodeWithTag("jrem_slot_${it.name}").assertExists() }
        compose.onNodeWithTag("jrem_slot_SIX").performClick()
        compose.waitForIdle()
        assertFalse(JourneySlot.SIX in latest!!.slots)
        assertEquals(4, latest!!.slots.size)
    }

    @Test fun quietHoursAreSavedOnlyAsValidTimes() {
        show(JourneyReminderSettings(master = true))
        compose.onNodeWithTag("jrem_quiet_start").performTextInput("22:30")
        compose.waitForIdle()
        assertEquals(22 * 60 + 30, latest?.quietStart)
        latest = null
        compose.onNodeWithTag("jrem_quiet_end").performTextInput("25:99")
        compose.waitForIdle()
        assertEquals(null, latest) // not a time: nothing is saved
    }
}
