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

    /** For a stored percent; the epsilon absorbs floating-point division. */
    fun qualifies(percent: Double) = percent >= THRESHOLD - 1e-9
}
