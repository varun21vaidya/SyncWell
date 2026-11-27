package com.example.domain

import com.example.data.Goal
import com.example.data.JourneyState

/** What a share card says: a title, body lines, and the footer that marks it as self-reported. */
data class ShareLines(val title: String, val lines: List<String>, val footer: String = JourneyShare.FOOTER)

/** The words of the three Journey share cards. Pure, so the wording is tested without drawing anything. */
object JourneyShare {
    const val FOOTER = "Self-reported, made with SyncWell"

    fun journey(region: RegionDef, step: Int, position: JourneyPosition, stamps: Int, characterName: String): ShareLines = ShareLines(
        "My Journey",
        listOf(
            region.name,
            JourneyMap.dayLabel(region, step),
            "${JourneyRoute.totalSteps(position)} of ${JourneyRoute.SUMMIT_STEPS} days to the Summit",
            "${plural(stamps, "stamp")} in my passport",
            "Walking with $characterName"
        )
    )

    /** A promise to a friend: the commitment and its deadline. The reward is hidden unless [showReward] is set. */
    fun promise(g: Goal, showReward: Boolean): ShareLines = ShareLines(
        "My promise",
        listOfNotNull(
            g.title,
            "${trim(g.target)}${g.targetLabel?.let { " $it" } ?: ""} by ${g.windowEnd}",
            g.rewardText?.takeIf { showReward }?.let { "Reward: $it" }
        )
    )

    /** The ambition card: the ambition, the identity line, the countdown to a target date, and the big reward when it is shown. */
    fun ambition(s: JourneyState, today: String, daysWalked: Int, showReward: Boolean): ShareLines = ShareLines(
        "My ambition",
        listOfNotNull(
            s.ambitionText,
            s.identityText,
            countdown(s.targetDate, today)?.let { if (it == 0) "Today is the day" else "$it days to go" },
            "${plural(daysWalked, "day")} walked on the trail",
            s.bigRewardText?.takeIf { showReward }?.let { if (s.bigRewardUnlockedAt != null) "Earned it back: $it" else "Giving up until then: $it" }
        )
    )

    /** Whole days from [today] to [target], or null when there is no target or it has passed. */
    fun countdown(target: String?, today: String): Int? =
        target?.takeIf { it >= today }?.let { WeekKey.range(today, it).size - 1 }

    private fun plural(n: Int, word: String) = "$n $word${if (n == 1) "" else "s"}"

    private fun trim(d: Double) = if (d % 1.0 == 0.0) d.toLong().toString() else "%.1f".format(java.util.Locale.US, d)
}
