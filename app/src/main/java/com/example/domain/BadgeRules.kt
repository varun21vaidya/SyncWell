package com.example.domain

import java.text.SimpleDateFormat
import java.util.Locale

/** Colour family of a medal. The palette itself lives in the UI (`BadgeMedal.kt`); the rule only names the tier. */
enum class BadgeTier { BRONZE, SILVER, GOLD, TIDE, MINT, ROSE, AQUA, VIOLET }

enum class BadgeIcon { CALENDAR_CHECK, STAR, LEAF, BOLT, DROP, CHECK, SUN, STONES, DUMBBELL, HOURGLASS, SPROUT }

enum class BadgeGroup(val title: String) {
    FIRST_STEPS("First steps"), STREAKS("Streaks"), GOOD_DAYS("Good days"), HABIT_WEEKS("Habit weeks"), KINDNESS("Kindness")
}

/**
 * One medal of the catalogue. [target] is the number to reach in [unit] ("25 good days"), [banner] the milestone text
 * drawn across the medal (the only uppercase text), [doubleRosette] marks the 100-day medals.
 */
data class Badge(
    val id: String, val title: String, val how: String, val group: BadgeGroup,
    val tier: BadgeTier, val icon: BadgeIcon, val banner: String?, val unit: String, val target: Int,
    val doubleRosette: Boolean = false
)

data class BadgeDay(val dateKey: String, val percent: Double, val dietCompleted: Boolean, val waterMl: Int)

/** Items done and total for one category on one day. */
data class CategoryDay(val dateKey: String, val category: String, val done: Int, val total: Int)

/** The longest focus session saved on one day. */
data class FocusDay(val dateKey: String, val maxMinutes: Int)

data class BadgeInput(
    val days: List<BadgeDay>,
    val categoryDays: List<CategoryDay>,
    val waterGoalMl: Int,
    val tickedItems: Int = 0,
    val bestStreak: Int = 0,
    val finishedWorkouts: Int = 0,
    val focusDays: List<FocusDay> = emptyList(),
    val gymBestWeeks: Int = 0,
    val vacationDates: Set<String> = emptySet()
)

/** Progress of one medal: [current] is capped at [target], [count] is how many times a repeatable medal was earned. */
data class BadgeState(val badge: Badge, val current: Int, val target: Int, val count: Int = 0) {
    val earned: Boolean get() = current >= target
    val fraction: Float get() = if (target <= 0) 1f else (current.toFloat() / target).coerceIn(0f, 1f)
    val progressText: String get() = "$current/$target"
}

object BadgeRules {
    val ALL = listOf(
        Badge("FIRST_TICK", "First tick", "Tick any routine item", BadgeGroup.FIRST_STEPS, BadgeTier.MINT, BadgeIcon.CHECK, null, "tick", 1),
        Badge("DAY_ONE", "Day one", "Reach 80% on a day", BadgeGroup.FIRST_STEPS, BadgeTier.MINT, BadgeIcon.SUN, "DAY 1", "day", 1),
        Badge("STREAK_3", "Three in a row", "Keep a 3-day streak", BadgeGroup.FIRST_STEPS, BadgeTier.BRONZE, BadgeIcon.STONES, "3 DAYS", "day streak", 3),
        Badge("STREAK_7", "First week", "Keep a 7-day streak", BadgeGroup.FIRST_STEPS, BadgeTier.SILVER, BadgeIcon.STONES, "7 DAYS", "day streak", 7),
        Badge("FIRST_WORKOUT", "First workout", "Finish a workout", BadgeGroup.FIRST_STEPS, BadgeTier.ROSE, BadgeIcon.DUMBBELL, null, "workout", 1),
        Badge("FIRST_FOCUS", "First focus", "Save a focus session of 25 minutes or more", BadgeGroup.FIRST_STEPS, BadgeTier.VIOLET, BadgeIcon.HOURGLASS, null, "minutes", 25),
        Badge("WELL_WATERED", "Well watered", "Reach your water goal once", BadgeGroup.FIRST_STEPS, BadgeTier.AQUA, BadgeIcon.DROP, null, "day", 1),

        Badge("STREAK_14", "Two weeks", "Keep a 14-day streak", BadgeGroup.STREAKS, BadgeTier.GOLD, BadgeIcon.STONES, "14 DAYS", "day streak", 14),
        Badge("STREAK_30", "One month", "Keep a 30-day streak", BadgeGroup.STREAKS, BadgeTier.TIDE, BadgeIcon.STONES, "30 DAYS", "day streak", 30),
        Badge("STREAK_60", "Two months", "Keep a 60-day streak", BadgeGroup.STREAKS, BadgeTier.VIOLET, BadgeIcon.STONES, "60 DAYS", "day streak", 60),
        Badge("STREAK_100", "Hundred", "Keep a 100-day streak", BadgeGroup.STREAKS, BadgeTier.GOLD, BadgeIcon.STONES, "100 DAYS", "day streak", 100, doubleRosette = true),

        Badge("DAYS_25", "25 good days", "Reach 80% on 25 days, gaps allowed", BadgeGroup.GOOD_DAYS, BadgeTier.BRONZE, BadgeIcon.CALENDAR_CHECK, "25 DAYS", "good days", 25),
        Badge("DAYS_50", "50 good days", "Reach 80% on 50 days, gaps allowed", BadgeGroup.GOOD_DAYS, BadgeTier.SILVER, BadgeIcon.CALENDAR_CHECK, "50 DAYS", "good days", 50),
        Badge("DAYS_75", "75 good days", "Reach 80% on 75 days, gaps allowed", BadgeGroup.GOOD_DAYS, BadgeTier.GOLD, BadgeIcon.CALENDAR_CHECK, "75 DAYS", "good days", 75),
        Badge("DAYS_100", "100 good days", "Reach 80% on 100 days, gaps allowed", BadgeGroup.GOOD_DAYS, BadgeTier.TIDE, BadgeIcon.CALENDAR_CHECK, "100 DAYS", "good days", 100, doubleRosette = true),

        Badge("PERFECT_DAY", "Perfect day", "Tick every item in one day", BadgeGroup.HABIT_WEEKS, BadgeTier.MINT, BadgeIcon.STAR, null, "day", 1),
        Badge("PROTEIN_WEEK", "Protein week", "Meet the diet rule 7 days in a row", BadgeGroup.HABIT_WEEKS, BadgeTier.GOLD, BadgeIcon.BOLT, null, "days in a row", 7),
        Badge("SKINCARE_7", "Skincare week", "Finish all skincare items 7 days in a row", BadgeGroup.HABIT_WEEKS, BadgeTier.ROSE, BadgeIcon.LEAF, null, "days in a row", 7),
        Badge("HYDRATION_HERO", "Hydration hero", "Reach your water goal 7 days in a row", BadgeGroup.HABIT_WEEKS, BadgeTier.AQUA, BadgeIcon.DROP, "7 DAYS", "days in a row", 7),
        Badge("FOCUS_WEEK", "Focus week", "Save a focus session 7 days in a row", BadgeGroup.HABIT_WEEKS, BadgeTier.VIOLET, BadgeIcon.HOURGLASS, "7 DAYS", "days in a row", 7),
        Badge("GYM_MONTH", "Gym month", "Meet your weekly workout target 4 weeks in a row", BadgeGroup.HABIT_WEEKS, BadgeTier.ROSE, BadgeIcon.DUMBBELL, "4 WEEKS", "weeks", 4),

        Badge("FRESH_START", "Fresh start", "Come back with a counted day after 2 or more missed days", BadgeGroup.KINDNESS, BadgeTier.MINT, BadgeIcon.SPROUT, null, "restart", 1)
    )

    /** State of every medal, in catalogue order. Streak medals use the best streak ever, so a medal is never taken away. */
    fun evaluate(input: BadgeInput): List<BadgeState> {
        val goodDates = input.days.filter { DayCompletion.qualifies(it.percent) }.map { it.dateKey }
        val goal = input.waterGoalMl
        val skincareRun = longestRun(input.categoryDays.filter { it.category == "SKINCARE" && it.total > 0 && it.done == it.total }.map { it.dateKey })
        val fresh = freshStarts(goodDates, input.vacationDates)
        val current = mapOf(
            "FIRST_TICK" to input.tickedItems,
            "DAY_ONE" to goodDates.size,
            "FIRST_WORKOUT" to input.finishedWorkouts,
            "FIRST_FOCUS" to (input.focusDays.maxOfOrNull { it.maxMinutes } ?: 0),
            "WELL_WATERED" to if (goal > 0 && input.days.any { it.waterMl >= goal }) 1 else 0,
            "DAYS_25" to goodDates.size, "DAYS_50" to goodDates.size, "DAYS_75" to goodDates.size, "DAYS_100" to goodDates.size,
            "PERFECT_DAY" to if (input.days.any { it.percent >= 1.0 - 1e-9 }) 1 else 0,
            "PROTEIN_WEEK" to longestRun(input.days.filter { it.dietCompleted }.map { it.dateKey }),
            "SKINCARE_7" to skincareRun,
            "HYDRATION_HERO" to if (goal > 0) longestRun(input.days.filter { it.waterMl >= goal }.map { it.dateKey }) else 0,
            "FOCUS_WEEK" to longestRun(input.focusDays.map { it.dateKey }),
            "GYM_MONTH" to input.gymBestWeeks,
            "FRESH_START" to fresh
        ) + listOf("STREAK_3", "STREAK_7", "STREAK_14", "STREAK_30", "STREAK_60", "STREAK_100").associateWith { input.bestStreak }
        return ALL.map { b ->
            val value = current[b.id] ?: 0
            BadgeState(b, minOf(value, b.target), b.target, count = if (b.id == "FRESH_START") fresh else if (value >= b.target) 1 else 0)
        }
    }

    fun earned(input: BadgeInput): Set<String> = evaluate(input).filter { it.earned }.map { it.badge.id }.toSet()

    /** Current and target per medal id. */
    fun progress(input: BadgeInput): Map<String, Pair<Int, Int>> = evaluate(input).associate { it.badge.id to (it.current to it.target) }

    /** The medal to show as "Next stamp": the unearned one closest to done, the first in the catalogue when none has progress. */
    fun nextStamp(states: List<BadgeState>): BadgeState? = states.filter { !it.earned }.maxWithOrNull(compareBy { it.fraction })

    /** Popup and share-image wording. */
    fun stampTitle(b: Badge) = "Stamped: ${b.title}"

    fun stampBody(b: Badge) =
        if (b.group == BadgeGroup.GOOD_DAYS) "You showed up for yourself ${b.target} times." else "${b.how}. That is worth a stamp."

    /** "12 / 25 good days" for the Home line and the medal sheet. */
    fun progressLine(s: BadgeState) = "${s.current} / ${s.target} ${s.badge.unit}"

    /** "Earned 14 Nov", or "Earned" when the day was not recorded (history recorded silently). */
    fun earnedLabel(dateKey: String?): String {
        if (dateKey.isNullOrBlank()) return "Earned"
        return try {
            "Earned " + SimpleDateFormat("d MMM", Locale.getDefault()).format(SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateKey)!!)
        } catch (e: Exception) { "Earned" }
    }

    /**
     * Times a counted day (80%) came right after 2 or more missed days. Vacation days are not missed days.
     * The first counted day ever is not a restart.
     */
    fun freshStarts(goodDates: Collection<String>, vacationDates: Set<String>): Int =
        goodDates.toSortedSet().toList().zipWithNext().count { (a, b) ->
            WeekKey.range(WeekKey.addDays(a, 1), WeekKey.addDays(b, -1)).count { it !in vacationDates } >= 2
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

data class CelebrationPlan(val queue: List<String>, val celebrated: Set<String>)

/** Which unlocked medals get the "stamp" popup, and what counts as celebrated afterwards. */
object BadgeCelebration {
    const val MAX_IN_A_ROW = 3

    /**
     * Each medal pops up at most once. [silent] records everything without a popup: the first run, and the first run after
     * the catalogue grew, so existing history never triggers a burst. When more than [MAX_IN_A_ROW] unlock together only the
     * first ones pop up (in catalogue [order]); all of them are celebrated and stay in the Stats grid.
     */
    fun plan(earned: Set<String>, celebrated: Set<String>, silent: Boolean, order: List<String>): CelebrationPlan {
        val fresh = order.filter { it in earned && it !in celebrated }
        return CelebrationPlan(if (silent) emptyList() else fresh.take(MAX_IN_A_ROW), celebrated + earned)
    }
}
