package com.example.domain

import com.example.data.Goal

/** Where in a region a commitment's window sits. A 7-day region only has [WHOLE]. */
enum class CommitmentWindow(val label: String) { SEGMENT_1("Days 1 to 7"), SEGMENT_2("Days 8 to 14"), WHOLE("Whole region") }

enum class DraftError { TITLE, TARGET, TOO_MANY, WINDOW, TIME, TOO_LONG }

/** What the commitment editor holds while the user types. Only the first five fields are shown up front; the rest sit under More. */
data class CommitmentDraft(
    val title: String = "",
    val measure: GoalMeasure = GoalMeasure.COUNT,
    val target: Double = 0.0,
    val targetLabel: String? = null,
    val window: CommitmentWindow = CommitmentWindow.WHOLE,
    val focusLinked: Boolean = false,
    val reward: String? = null,
    val planText: String? = null,
    val planTime: String? = null,
    val ifThen: String? = null
)

object CommitmentRules {
    const val MAX_PER_REGION = 3
    const val MAX_TITLE = 60
    const val MAX_SHORT = 80 // reward, when-and-where
    const val MAX_IF_THEN = 120
    private val TIME = Regex("([01]\\d|2[0-3]):[0-5]\\d")

    /** The windows a region offers: a 14-day region has two segments and the whole; a 7-day region only the whole. */
    fun windows(region: Int): List<CommitmentWindow> =
        if (JourneyRoute.stepsIn(region) <= 7) listOf(CommitmentWindow.WHOLE) else CommitmentWindow.values().toList()

    /** First and last date of the window, counted from the region's start date. */
    fun dates(region: Int, window: CommitmentWindow, regionStart: String): Pair<String, String> {
        val length = JourneyRoute.stepsIn(region)
        val (from, to) = when (window) {
            CommitmentWindow.SEGMENT_1 -> 0 to 6
            CommitmentWindow.SEGMENT_2 -> 7 to 13
            CommitmentWindow.WHOLE -> 0 to length - 1
        }
        return WeekKey.addDays(regionStart, from) to WeekKey.addDays(regionStart, to)
    }

    /** Everything that blocks saving. [existingInRegion] is how many commitments the region already has. */
    fun validate(d: CommitmentDraft, region: Int, existingInRegion: Int): List<DraftError> = listOfNotNull(
        DraftError.TITLE.takeIf { d.title.isBlank() },
        DraftError.TARGET.takeIf { !targetOk(d) },
        DraftError.TOO_MANY.takeIf { existingInRegion >= MAX_PER_REGION },
        DraftError.WINDOW.takeIf { d.window !in windows(region) },
        DraftError.TIME.takeIf { d.planTime != null && !TIME.matches(d.planTime) },
        DraftError.TOO_LONG.takeIf { tooLong(d) }
    )

    private fun targetOk(d: CommitmentDraft) = d.target > 0.0 && (d.measure != GoalMeasure.PERCENT || d.target <= 100.0)

    private fun tooLong(d: CommitmentDraft) =
        d.title.length > MAX_TITLE || listOf(d.reward, d.planText).any { (it?.length ?: 0) > MAX_SHORT } || (d.ifThen?.length ?: 0) > MAX_IF_THEN

    fun toGoal(d: CommitmentDraft, id: String, region: Int, regionStart: String): Goal {
        val (start, end) = dates(region, d.window, regionStart)
        return Goal(
            id = id, title = d.title.trim(), kind = "COMMITMENT", mandatory = true, measure = d.measure.name, target = d.target,
            targetLabel = d.targetLabel?.trim()?.ifEmpty { null }, focusLinked = d.focusLinked && d.measure == GoalMeasure.DURATION,
            windowStart = start, windowEnd = end, regionIndex = region, rewardText = d.reward?.trim()?.ifEmpty { null },
            planText = d.planText?.trim()?.ifEmpty { null }, planTime = d.planTime, ifThenText = d.ifThen?.trim()?.ifEmpty { null }
        )
    }

    /** Pending duration goals linked to the Focus timer whose window includes [today]: what the timer can count toward. */
    fun focusChoices(goals: List<Goal>, today: String): List<Goal> =
        goals.filter { it.focusLinked && it.status == "PENDING" && today >= it.windowStart && today <= it.windowEnd }

    /** The user's own earlier goals as editor suggestions: newest window first, one per title, at most [limit]. */
    fun suggestions(previous: List<Goal>, limit: Int = 5): List<Goal> =
        previous.sortedByDescending { it.windowEnd }.distinctBy { it.title.lowercase() }.take(limit)

    /** Picking a suggestion fills the editor; every field stays editable. The reward is carried over only from a missed commitment, so a met one is not repeated. */
    fun fromGoal(g: Goal, region: Int) = CommitmentDraft(
        title = g.title, measure = GoalMeasure.valueOf(g.measure), target = g.target, targetLabel = g.targetLabel,
        window = CommitmentWindow.WHOLE.takeIf { it in windows(region) } ?: CommitmentWindow.WHOLE, focusLinked = g.focusLinked,
        reward = g.rewardText.takeIf { g.status == "MISSED" }, planText = g.planText, planTime = g.planTime, ifThen = g.ifThenText
    )

    /** The More fields (plan and backup plan) copied from the user's last commitment, so a new one takes seconds. */
    fun prefillMore(last: Goal?) = CommitmentDraft(planText = last?.planText, planTime = last?.planTime, ifThen = last?.ifThenText)
}
