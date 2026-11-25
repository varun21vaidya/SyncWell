package com.example.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class TodayStatsFormatTest {
    @Test fun focusLabelFormats() {
        assertEquals("0m", focusLabel(0))
        assertEquals("45m", focusLabel(45))
        assertEquals("1h", focusLabel(60))
        assertEquals("1h 20m", focusLabel(80))
    }
}
