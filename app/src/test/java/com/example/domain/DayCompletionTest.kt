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

    @Test fun storedPercentUsesSameBar() {
        assertTrue(DayCompletion.qualifies(4.0 / 5.0))
        assertTrue(DayCompletion.qualifies(12.0 / 15.0))
        assertFalse(DayCompletion.qualifies(0.79))
    }
}
