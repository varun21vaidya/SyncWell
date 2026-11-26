package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JourneyRemindersTest {
    private fun input(
        slot: JourneySlot = JourneySlot.THREE, journey: Boolean = true, settings: JourneyReminderSettings = JourneyReminderSettings(master = true),
        vacation: Boolean = false, now: Int = 15 * 60, done: Int = 2, total: Int = 10, sent: Set<JourneySlot> = emptySet()
    ) = JourneyReminderInput(slot, journey, settings, vacation, now, 23 * 60, 7 * 60, done, total, sent)

    @Test fun fiveFixedSlotsAtTheAgreedHours() {
        assertEquals(listOf(9, 12, 15, 18, 21), JourneySlot.values().map { it.hour })
        assertEquals(listOf("9 AM", "12 PM", "3 PM", "6 PM", "9 PM"), JourneySlot.values().map { it.label })
        assertTrue(JourneySlot.values().all { it.body.isNotBlank() })
        assertTrue(JourneySlot.values().none { it.body.contains("step", ignoreCase = true) }) // the Journey says day, never step
    }

    @Test fun theWordingIsCalmInformation() {
        val banned = listOf("lose", "fail", "last chance", "hurry", "don't", "missed", "behind")
        JourneySlot.values().forEach { s -> banned.forEach { assertFalse("${s.body} / $it", s.body.lowercase().contains(it)) } }
    }

    @Test fun aReminderGoesOutWhenEveryRuleAllowsIt() = assertTrue(JourneyReminderPolicy.shouldPost(input()))

    @Test fun offUntilTheUserTurnsItOnAndOnlyWithTheJourney() {
        assertFalse(JourneyReminderPolicy.shouldPost(input(settings = JourneyReminderSettings())))
        assertFalse(JourneyReminderPolicy.shouldPost(input(journey = false)))
    }

    @Test fun eachSlotCanBeSwitchedOff() {
        val s = JourneyReminderSettings(master = true, slots = JourneySlot.values().toSet() - JourneySlot.THREE)
        assertFalse(JourneyReminderPolicy.shouldPost(input(slot = JourneySlot.THREE, settings = s)))
        assertTrue(JourneyReminderPolicy.shouldPost(input(slot = JourneySlot.SIX, settings = s, now = 18 * 60)))
    }

    @Test fun silentOnVacationAndOncePerSlotPerDay() {
        assertFalse(JourneyReminderPolicy.shouldPost(input(vacation = true)))
        assertFalse(JourneyReminderPolicy.shouldPost(input(sent = setOf(JourneySlot.THREE))))
        assertTrue(JourneyReminderPolicy.shouldPost(input(sent = setOf(JourneySlot.NINE))))
    }

    @Test fun skippedOnceTheDayAlreadyCountsOrThereIsNothingToDo() {
        assertFalse(JourneyReminderPolicy.shouldPost(input(done = 8, total = 10))) // exactly 80%
        assertTrue(JourneyReminderPolicy.shouldPost(input(done = 7, total = 10)))
        assertFalse(JourneyReminderPolicy.shouldPost(input(total = 0, done = 0)))
    }

    @Test fun quietHoursUseBedtimeToWakeUnlessTheUserSetTheirOwn() {
        assertFalse(JourneyReminderPolicy.shouldPost(input(slot = JourneySlot.NINE_PM, now = 23 * 60 + 30)))
        val own = JourneyReminderSettings(master = true, quietStart = 20 * 60, quietEnd = 8 * 60)
        assertFalse(JourneyReminderPolicy.shouldPost(input(slot = JourneySlot.NINE_PM, settings = own, now = 21 * 60)))
        assertTrue(JourneyReminderPolicy.shouldPost(input(slot = JourneySlot.NINE_PM, now = 21 * 60)))
        assertFalse(JourneyReminderPolicy.shouldPost(input(slot = JourneySlot.NINE, settings = own, now = 6 * 60 + 59)))
    }

    @Test fun timesParseAndFormat() {
        assertEquals(22 * 60 + 30, JourneyReminderPolicy.parseTime(" 22:30 "))
        assertEquals(7 * 60, JourneyReminderPolicy.parseTime("7:00"))
        assertNull(JourneyReminderPolicy.parseTime("24:00")); assertNull(JourneyReminderPolicy.parseTime("7")); assertNull(JourneyReminderPolicy.parseTime("07:60"))
        assertEquals("07:05", JourneyReminderPolicy.formatTime(7 * 60 + 5))
    }
}
