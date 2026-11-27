package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import com.example.data.Goal
import com.example.data.JourneyState
import com.example.domain.JourneyPosition
import com.example.domain.JourneyRoute
import com.example.domain.Moment
import com.example.domain.MomentQueue
import com.example.domain.RegionDef
import com.example.viewmodel.SyncWellViewModel

/**
 * Shows the one moment that is due, full screen. Every action remembers that it was seen, so it returns to the page and does not come back.
 * [dismissSetback] clears the walk-back note, which is remembered on the page rather than stored.
 */
@Composable
fun JourneyMomentHost(moment: Moment, viewModel: SyncWellViewModel, s: JourneyState, region: RegionDef?, regions: List<RegionDef>, goals: List<Goal>, dismissSetback: () -> Unit) {
    val passport by viewModel.journeyPassport.collectAsState()
    val notes by viewModel.journeyNotes.collectAsState()
    val seen = { MomentQueue.key(moment)?.let { viewModel.markMomentSeen(it) } ?: Unit }
    when (moment) {
        is Moment.Review -> ReviewHost(moment, viewModel)
        is Moment.BackToCamp -> BackToCampScreen(moment.note, passport.size, dismissSetback)
        is Moment.Ticket -> TicketScreen(moment.goal, onLater = seen, onClaim = { viewModel.claimJourneyReward(moment.goal.id); seen() })
        is Moment.Camp -> CampScreen(moment.regionName, notes.firstOrNull { it.id == "camp-${moment.regionIndex}" }, onWrite = { viewModel.saveCampReflection(moment.regionIndex, it); seen() }, onSkip = seen)
        is Moment.Gate -> if (region != null) {
            GateDayScreen(
                region, goals.filter { it.kind == "COMMITMENT" && it.regionIndex == moment.regionIndex }, s.ambitionText, s.identityText,
                nextName = regions.getOrNull(moment.regionIndex + 1)?.name ?: "the weekly regions",
                onCross = { viewModel.crossJourneyGate(region.name); seen() }, onLater = seen
            )
        }
        Moment.Summit -> SummitScreen(JourneyRoute.totalSteps(JourneyPosition(s.regionIndex, s.stepInRegion)), passport.size, s.bigRewardText, s.bigRewardUnlockedAt != null, seen)
    }
}

/** One letter of the review queue; "Review 2 of 3" counts from how many were waiting when the first opened. */
@Composable
private fun ReviewHost(moment: Moment.Review, viewModel: SyncWellViewModel) {
    val total = remember { moment.remaining }
    val logged by produceState(0.0, moment.goal) { value = viewModel.goalProgress(moment.goal) }
    ReviewLetterScreen(moment.goal, logged, (total - moment.remaining + 1).coerceIn(1, total), total) { viewModel.markReviewSeen(moment.goal.id) }
}
