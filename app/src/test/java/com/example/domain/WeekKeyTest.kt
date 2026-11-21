package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class WeekKeyTest {
    @Test fun weekStartsOnMonday() {
        assertEquals("2026-11-16", WeekKey.of("2026-11-16")) // Monday
        assertEquals("2026-11-16", WeekKey.of("2026-11-21")) // Saturday
        assertEquals("2026-11-16", WeekKey.of("2026-11-22")) // Sunday belongs to the week that began on Monday
        assertEquals("2026-11-23", WeekKey.of("2026-11-23"))
    }

    @Test fun addDaysCrossesMonthAndYear() {
        assertEquals("2026-12-01", WeekKey.addDays("2026-11-30", 1))
        assertEquals("2026-12-31", WeekKey.addDays("2027-01-01", -1))
    }

    @Test fun rangeIsInclusiveAndEmptyWhenReversed() {
        assertEquals(listOf("2026-11-29", "2026-11-30", "2026-12-01"), WeekKey.range("2026-11-29", "2026-12-01"))
        assertEquals(emptyList<String>(), WeekKey.range("2026-12-02", "2026-12-01"))
    }
}
