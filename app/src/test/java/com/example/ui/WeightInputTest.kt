package com.example.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeightInputTest {
    @Test fun parsesDotAndComma() {
        assertEquals(72.4, parseWeightKg("72.4")!!, 1e-9)
        assertEquals(72.4, parseWeightKg(" 72,4 ")!!, 1e-9)
    }
    @Test fun rejectsEmptyJunkAndOutOfRange() {
        assertNull(parseWeightKg(""))
        assertNull(parseWeightKg("abc"))
        assertNull(parseWeightKg("5"))
        assertNull(parseWeightKg("900"))
    }
}
