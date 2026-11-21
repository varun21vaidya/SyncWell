package com.example.domain

data class DietMeal(val isDone: Boolean, val proteinAnswer: String?)

data class DietDay(val points: Int, val required: Int, val qualifies: Boolean)

/** A meal counts toward the diet streak only when it is ticked and the protein answer is Yes. */
object DietRule {
    const val YES = "YES"
    const val NO = "NO"
    private const val MAX_REQUIRED = 4

    fun evaluate(meals: List<DietMeal>): DietDay {
        val points = meals.count { it.isDone && it.proteinAnswer == YES }
        val required = minOf(MAX_REQUIRED, meals.size)
        return DietDay(points, required, meals.isNotEmpty() && points >= required)
    }
}
