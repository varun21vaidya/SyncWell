package com.example.domain

import com.example.data.Goal
import com.example.data.JourneyState

/** A one-off moment that takes over the screen once and then returns to the page. */
sealed class Moment {
    /** The oldest decided commitment not read yet; [remaining] counts it and the ones behind it. */
    data class Review(val goal: Goal, val remaining: Int) : Moment()
    data class BackToCamp(val note: String) : Moment()
    data class Ticket(val goal: Goal) : Moment()
    data class Camp(val regionIndex: Int, val regionName: String) : Moment()
    data class Gate(val regionIndex: Int) : Moment()
    object Summit : Moment()
}

/** Decides which moment, if any, shows now. Pure: what has been seen comes in as sets, so it is tested without a screen. */
object MomentQueue {

    /** Order: the letters first, then the walk back, a reward ticket, the camp, gate day and finally the Summit. */
    fun next(s: JourneyState, region: RegionDef?, goals: List<Goal>, seenReviews: Set<String>, seenMoments: Set<String>, setback: String?): Moment? {
        val unseen = ReviewQueue.unseen(goals, seenReviews)
        return when {
            unseen.isNotEmpty() -> Moment.Review(unseen.first(), unseen.size)
            setback != null -> Moment.BackToCamp(setback)
            else -> ticket(goals, seenMoments) ?: place(s, region, seenMoments)
        }
    }

    private fun ticket(goals: List<Goal>, seen: Set<String>): Moment? =
        goals.firstOrNull { it.status == "MET" && it.rewardText != null && it.rewardClaimedAt == null && "ticket-${it.id}" !in seen }?.let { Moment.Ticket(it) }

    /** The moments tied to where the character stands: the camp, the gate, the Summit. */
    private fun place(s: JourneyState, region: RegionDef?, seen: Set<String>): Moment? {
        if (region == null) return Moment.Summit.takeIf { "summit" !in seen }
        val camp = TravelLog.campDay(region)
        return when {
            camp != null && s.stepInRegion >= camp && "camp-${s.regionIndex}" !in seen -> Moment.Camp(s.regionIndex, region.name)
            s.stepInRegion >= region.steps && "gate-${s.regionIndex}" !in seen -> Moment.Gate(s.regionIndex)
            else -> null
        }
    }

    /** What to remember once a moment has been shown. Letters and the walk back are remembered elsewhere, so they have none. */
    fun key(m: Moment): String? = when (m) {
        is Moment.Ticket -> "ticket-${m.goal.id}"
        is Moment.Camp -> "camp-${m.regionIndex}"
        is Moment.Gate -> "gate-${m.regionIndex}"
        Moment.Summit -> "summit"
        is Moment.Review, is Moment.BackToCamp -> null
    }
}
