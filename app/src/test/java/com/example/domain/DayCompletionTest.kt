package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DayCompletionTest {
    @Test fun eightyPercentQualifies() {
        assertTrue(DayCompletion.of(4, 5).qualifies)
        assertTrue(DayCompletion.of(12, 15).qualifies)
        assertFalse(DayCompletion.of(11, 15).qualifies)
    }

    @Test fun emptyDayDoesNotQualify() {
        val r = DayCompletion.of(0, 0)
        assertFalse(r.qualifies)
        assertEquals(0.0, r.percent, 0.0)
    }

    @Test fun itemsToQualifyCountsTheTicksStillNeeded() {
        assertEquals(4, DayCompletion.itemsToQualify(0, 5))
        assertEquals(2, DayCompletion.itemsToQualify(7, 11)) // 9 of 11 needed
        assertEquals(0, DayCompletion.itemsToQualify(9, 11))
        assertEquals(0, DayCompletion.itemsToQualify(11, 11))
        assertEquals(0, DayCompletion.itemsToQualify(0, 0))
        (1..40).forEach { t -> (0..t).forEach { d ->
            assertEquals("done=$d total=$t", DayCompletion.of(d, t).qualifies, DayCompletion.itemsToQualify(d, t) == 0)
        } }
    }

    @Test fun storedPercentUsesSameBar() {
        assertTrue(DayCompletion.qualifies(4.0 / 5.0))
        assertTrue(DayCompletion.qualifies(12.0 / 15.0))
        assertFalse(DayCompletion.qualifies(0.79))
    }
}
