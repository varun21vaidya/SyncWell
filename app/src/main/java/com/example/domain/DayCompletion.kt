package com.example.domain

data class DayResult(val done: Int, val total: Int, val percent: Double, val qualifies: Boolean)

/** One definition of "a day counts": 80% or more of the routine items are done. */
object DayCompletion {
    const val THRESHOLD = 0.8

    fun of(done: Int, total: Int) = DayResult(
        done, total,
        percent = if (total == 0) 0.0 else done.toDouble() / total,
        qualifies = total > 0 && done * 5 >= total * 4
    )

    /** How many more items must be ticked for the day to reach 80%. 0 once it qualifies or when there are no items. */
    fun itemsToQualify(done: Int, total: Int): Int = if (total == 0) 0 else maxOf(0, (total * 4 + 4) / 5 - done)

    /** For a stored percent; the epsilon absorbs floating-point division. */
    fun qualifies(percent: Double) = percent >= THRESHOLD - 1e-9
}
