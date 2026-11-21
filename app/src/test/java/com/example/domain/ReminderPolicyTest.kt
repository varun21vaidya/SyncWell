package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderPolicyTest {
    private fun input(
        slot: ReminderSlot = ReminderSlot.EVENING, enabled: Boolean = true, vacation: Boolean = false, now: Int = 20 * 60,
        done: Int = 3, total: Int = 10, streak: Int = 5, idle: Int = 0, comebackSent: Int = 0, sent: Set<ReminderType> = emptySet()
    ) = ReminderInput(slot, enabled, vacation, now, 23 * 60, 7 * 60, done, total, streak, idle, comebackSent, sent)

    @Test fun eveningNudgeGoesOutWhenAStreakIsAtStake() =
        assertEquals(ReminderType.EVENING_STREAK, ReminderPolicy.decide(input()))

    @Test fun morningPlanGoesOutWhenThereIsAListForToday() =
        assertEquals(ReminderType.MORNING_PLAN, ReminderPolicy.decide(input(slot = ReminderSlot.MORNING, now = 8 * 60, done = 0)))

    @Test fun offSwitchSilencesEverything() {
        ReminderSlot.values().forEach { assertNull(ReminderPolicy.decide(input(slot = it, enabled = false, idle = 9))) }
    }

    @Test fun vacationSilencesEverythingIncludingComeback() {
        ReminderSlot.values().forEach { assertNull(ReminderPolicy.decide(input(slot = it, vacation = true, idle = 9))) }
    }

    @Test fun quietHoursSilenceEverythingAndWrapPastMidnight() {
        assertTrue(ReminderPolicy.isQuiet(23 * 60 + 30, 23 * 60, 7 * 60))
        assertTrue(ReminderPolicy.isQuiet(2 * 60, 23 * 60, 7 * 60))
        assertFalse(ReminderPolicy.isQuiet(7 * 60, 23 * 60, 7 * 60))
        assertFalse(ReminderPolicy.isQuiet(12 * 60, 23 * 60, 7 * 60))
        assertTrue(ReminderPolicy.isQuiet(3 * 60, 1 * 60, 9 * 60)) // same-day window
        assertNull(ReminderPolicy.decide(input(now = 23 * 60 + 15)))
        assertNull(ReminderPolicy.decide(input(slot = ReminderSlot.MORNING, now = 6 * 60, done = 0, idle = 9)))
    }

    @Test fun nothingIsSentOnceTheDayIsAlreadyAtEightyPercent() {
        assertNull(ReminderPolicy.decide(input(done = 8, total = 10)))
        assertNull(ReminderPolicy.decide(input(slot = ReminderSlot.MORNING, done = 8, total = 10, idle = 9)))
        assertEquals(ReminderType.EVENING_STREAK, ReminderPolicy.decide(input(done = 7, total = 10)))
    }

    @Test fun noStreakMeansNoStreakReminder() =
        assertNull(ReminderPolicy.decide(input(streak = 0)))

    @Test fun comebackAfterTwoIdleDaysThenOnceMoreAfterFiveThenNoMore() {
        val m = ReminderSlot.MORNING
        assertEquals(ReminderType.MORNING_PLAN, ReminderPolicy.decide(input(slot = m, done = 0, idle = 1)))
        assertEquals(ReminderType.COMEBACK, ReminderPolicy.decide(input(slot = m, done = 0, idle = 2)))
        assertEquals(ReminderType.MORNING_PLAN, ReminderPolicy.decide(input(slot = m, done = 0, idle = 4, comebackSent = 1)))
        assertEquals(ReminderType.COMEBACK, ReminderPolicy.decide(input(slot = m, done = 0, idle = 5, comebackSent = 1)))
        // capped: however long the break, a third one never comes
        listOf(8, 30, 365).forEach {
            assertEquals(ReminderType.MORNING_PLAN, ReminderPolicy.decide(input(slot = m, done = 0, idle = it, comebackSent = ReminderPolicy.COMEBACK_CAP)))
        }
    }

    @Test fun neverMoreThanTwoInADayWhateverTheSettings() {
        val types = ReminderType.values().toSet()
        val subsets = listOf(emptySet(), setOf(ReminderType.MORNING_PLAN), setOf(ReminderType.COMEBACK, ReminderType.MORNING_PLAN), types)
        for (slot in ReminderSlot.values()) for (enabled in listOf(true, false)) for (vac in listOf(true, false))
            for (now in listOf(0, 8 * 60, 12 * 60, 20 * 60, 23 * 60 + 59)) for (done in listOf(0, 5, 9, 10))
                for (idle in listOf(0, 2, 5, 40)) for (sent in subsets) {
                    val r = ReminderPolicy.decide(ReminderInput(slot, enabled, vac, now, 23 * 60, 7 * 60, done, 10, 4, idle, 0, sent))
                    if (r != null) assertTrue("sent=$sent -> $r", sent.size < ReminderPolicy.DAILY_CAP && r !in sent)
                }
    }

    @Test fun aWholeDayNeverSendsMoreThanTwo() {
        var sent = emptySet<ReminderType>()
        listOf(ReminderSlot.MORNING to 8 * 60, ReminderSlot.EVENING to 20 * 60, ReminderSlot.EVENING to 21 * 60, ReminderSlot.MORNING to 9 * 60).forEach { (slot, now) ->
            ReminderPolicy.decide(ReminderInput(slot, true, false, now, 23 * 60, 7 * 60, 0, 10, 4, 2, 0, sent))?.let { sent = sent + it }
        }
        assertTrue(sent.size <= ReminderPolicy.DAILY_CAP)
    }

    @Test fun copyHasNoGuiltOrAlarmWords() {
        val banned = Regex("lose|lost|don't|miss|fail|hurry|last chance|running out|disappoint", RegexOption.IGNORE_CASE)
        ReminderType.values().forEach {
            assertFalse(banned.containsMatchIn(ReminderCopy.title(it)))
            assertFalse(banned.containsMatchIn(ReminderCopy.body(it, 10, 2, 5)))
        }
        assertEquals("1 more tick keeps your 5-day streak going.", ReminderCopy.body(ReminderType.EVENING_STREAK, 10, 1, 5))
    }
}
