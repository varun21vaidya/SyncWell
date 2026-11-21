package com.example.domain

data class Badge(val id: String, val title: String, val how: String)

data class BadgeDay(val dateKey: String, val percent: Double, val dietCompleted: Boolean, val waterMl: Int)

/** Items done and total for one category on one day. */
data class CategoryDay(val dateKey: String, val category: String, val done: Int, val total: Int)

data class BadgeInput(val days: List<BadgeDay>, val categoryDays: List<CategoryDay>, val waterGoalMl: Int)

object BadgeRules {
    val ALL = listOf(
        Badge("DAYS_25", "25 good days", "25 days at 80% or more"),
        Badge("DAYS_50", "50 good days", "50 days at 80% or more"),
        Badge("DAYS_75", "75 good days", "75 days at 80% or more"),
        Badge("DAYS_100", "100 good days", "100 days at 80% or more"),
        Badge("PERFECT_DAY", "Perfect day", "Every routine item done in one day"),
        Badge("SKINCARE_7", "Skincare week", "All skincare items done 7 days in a row"),
        Badge("PROTEIN_WEEK", "Protein week", "The diet rule met 7 days in a row"),
        Badge("HYDRATION_HERO", "Hydration hero", "Water goal reached 7 days in a row")
    )

    fun earned(input: BadgeInput): Set<String> {
        val good = input.days.count { DayCompletion.qualifies(it.percent) }
        val out = mutableSetOf<String>()
        listOf(25, 50, 75, 100).forEach { if (good >= it) out += "DAYS_$it" }
        if (input.days.any { it.percent >= 1.0 - 1e-9 }) out += "PERFECT_DAY"
        val skincareDays = input.categoryDays.filter { it.category == "SKINCARE" && it.total > 0 && it.done == it.total }.map { it.dateKey }
        if (longestRun(skincareDays) >= 7) out += "SKINCARE_7"
        if (longestRun(input.days.filter { it.dietCompleted }.map { it.dateKey }) >= 7) out += "PROTEIN_WEEK"
        if (input.waterGoalMl > 0 && longestRun(input.days.filter { it.waterMl >= input.waterGoalMl }.map { it.dateKey }) >= 7) out += "HYDRATION_HERO"
        return out
    }

    /** Longest run of consecutive calendar days in [dates]. */
    internal fun longestRun(dates: Collection<String>): Int {
        val set = dates.toSet()
        var best = 0
        set.forEach { d ->
            if (WeekKey.addDays(d, -1) !in set) { // start of a run
                var n = 1
                while (WeekKey.addDays(d, n) in set) n++
                best = maxOf(best, n)
            }
        }
        return best
    }
}
