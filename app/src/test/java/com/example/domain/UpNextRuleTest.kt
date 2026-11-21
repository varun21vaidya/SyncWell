package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpNextRuleTest {
    private fun item(id: String, time: String?, done: Boolean = false, category: String = "HEALTH") = DayItem(id, id, category, time, done)

    @Test fun picksTheEarliestUntickedItemDueNowOrLater() {
        val items = listOf(item("a", "07:00", done = true), item("b", "09:00"), item("c", "13:00"))
        assertEquals("b", UpNextRule.next(items, 8 * 60)?.id)
        assertEquals("c", UpNextRule.next(items, 10 * 60)?.id)
    }

    @Test fun whenEverythingLeftIsOverdueTheEarliestOverdueComes() {
        val items = listOf(item("a", "07:00"), item("b", "08:00"))
        assertEquals("a", UpNextRule.next(items, 22 * 60)?.id)
    }

    @Test fun itemsWithoutATimeComeLast() {
        val items = listOf(item("x", null), item("b", "18:00"))
        assertEquals("b", UpNextRule.next(items, 9 * 60)?.id)
        assertEquals("x", UpNextRule.next(listOf(item("x", null)), 9 * 60)?.id)
    }

    @Test fun nothingLeftGivesNull() {
        assertNull(UpNextRule.next(listOf(item("a", "07:00", done = true)), 8 * 60))
        assertNull(UpNextRule.next(emptyList(), 8 * 60))
    }

    @Test fun categoryRingsCountDoneAndSkipEmptyCategories() {
        val items = listOf(
            item("m1", "08:00", done = true, category = "DIET"), item("m2", "13:00", category = "DIET"),
            item("w", "17:00", category = "WORKOUT"), item("h", "07:00", category = "HYGIENE")
        )
        val rings = CategoryRings.of(items)
        assertEquals(listOf("DIET", "WORKOUT"), rings.map { it.category })
        assertEquals(CategoryProgress("DIET", 1, 2), rings.first())
        assertEquals(0.5f, rings.first().fraction, 0f)
    }
}
