package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DietRuleTest {
    private fun meal(done: Boolean, answer: String?) = DietMeal(done, answer)

    @Test fun threeMealsAllYesQualifies() {
        val r = DietRule.evaluate(List(3) { meal(true, DietRule.YES) })
        assertEquals(3, r.points)
        assertTrue(r.qualifies)
    }

    @Test fun noAnswerDoesNotCountEvenWhenTicked() {
        val r = DietRule.evaluate(listOf(meal(true, DietRule.YES), meal(true, DietRule.NO), meal(true, DietRule.YES)))
        assertEquals(2, r.points)
        assertFalse(r.qualifies)
    }

    @Test fun moreThanFourMealsNeedsFour() {
        assertTrue(DietRule.evaluate(List(6) { meal(it < 4, DietRule.YES) }).qualifies)
        assertFalse(DietRule.evaluate(List(6) { meal(it < 3, DietRule.YES) }).qualifies)
    }

    @Test fun untickedYesAnswerDoesNotCount() {
        assertEquals(0, DietRule.evaluate(listOf(meal(false, DietRule.YES))).points)
    }

    @Test fun noMealsNeverQualifies() {
        assertFalse(DietRule.evaluate(emptyList()).qualifies)
    }
}
