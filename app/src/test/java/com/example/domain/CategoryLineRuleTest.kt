package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryLineRuleTest {
    @Test fun countsDonePerCategory() {
        val items = listOf(
            DayItem("1", "a", "SKINCARE", null, true), DayItem("2", "b", "SKINCARE", null, true),
            DayItem("3", "c", "SKINCARE", null, false), DayItem("4", "d", "DIET", null, false)
        )
        assertEquals(listOf("Skincare 2 of 3", "Diet 0 of 1"), CategoryLineRule.lines(items))
    }
}
