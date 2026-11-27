package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Goal
import com.example.domain.CommitmentRules
import com.example.domain.JourneyRail
import com.example.domain.JourneyShare
import com.example.domain.RegionDef
import com.example.domain.formatAmount
import com.example.ui.theme.*
import com.example.viewmodel.SyncWellViewModel

/**
 * The gate challenge: this region's commitments with progress bars and locked rewards, the Warden who checks them at the gate,
 * and the buttons to log progress or add one.
 */
@Composable
fun CommitmentsCard(viewModel: SyncWellViewModel, commitments: List<Goal>, region: RegionDef, canAdd: Boolean, onAdd: () -> Unit) {
    var logging by remember { mutableStateOf<Goal?>(null) }
    var refresh by remember { mutableIntStateOf(0) } // bumped after a log so the bars read the new total
    val open = commitments.count { it.status != "MISSED" }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface1).padding(16.dp).testTag("journey_commitments"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GateHeader(region, commitments.size)
        GateHint(canAdd, commitments.isEmpty())
        commitments.forEach { g -> CommitmentRow(viewModel, g, refresh) { logging = g } }
        if (canAdd) {
            WardenRow(region, commitments.size)
            AddOneButton(first = commitments.isEmpty(), room = open < CommitmentRules.MAX_PER_REGION, onAdd = onAdd)
        }
        if (commitments.isNotEmpty()) Text("The gate opens once every commitment is met.", color = TextMuted, fontSize = 12.sp)
    }
    logging?.let { g ->
        val progress by produceState(0.0, g, refresh) { value = viewModel.goalProgress(g) }
        GoalProgressDialog(g, progress, onAdd = { amount, note -> viewModel.addGoalProgress(g.id, amount, note); refresh++ }, onClose = { logging = null })
    }
}

/** The region's Warden: their initial, name and when they check the pledges. */
@Composable
private fun WardenRow(region: RegionDef, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.testTag("journey_warden_row")) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(Surface3), contentAlignment = Alignment.Center) {
            Text(region.warden.name.take(1), color = Streak, fontWeight = FontWeight.Black, fontSize = 14.sp)
        }
        Column {
            Text("Warden ${region.warden.name}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(JourneyRail.wardenLine(count, region.steps), color = TextMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun CommitmentRow(viewModel: SyncWellViewModel, g: Goal, refresh: Int, onLog: () -> Unit) {
    val progress by produceState(0.0, g, refresh) { value = viewModel.goalProgress(g) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(g.title, color = TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(statusText(g, progress), color = if (g.status == "MET") Progress else if (g.status == "MISSED") Danger else TextSecondary, fontSize = 12.sp)
        }
        LinearProgressIndicator(progress = { (progress / g.target).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = Progress, trackColor = Surface3)
        g.planText?.let { Text(it + (g.planTime?.let { t -> " · $t" } ?: ""), color = TextMuted, fontSize = 12.sp) }
        g.rewardText?.let { RewardLine(viewModel, g, it) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (g.status == "PENDING") TextButton(onClick = onLog, modifier = Modifier.testTag("journey_log_${g.id}")) { Text("Log progress") }
            PromiseShare(g)
        }
    }
}

/** "6 / 10", or the outcome once the commitment is decided. */
private fun statusText(g: Goal, progress: Double) = when (g.status) {
    "MET" -> "Met"
    "MISSED" -> "Missed, back to the start of the region"
    else -> "${formatAmount(progress)} / ${formatAmount(g.target)}${g.targetLabel?.let { " $it" } ?: ""}"
}

@Composable
private fun RewardLine(viewModel: SyncWellViewModel, g: Goal, reward: String) {
    when {
        g.rewardClaimedAt != null -> Text("Reward claimed: $reward", color = Streak, fontSize = 12.sp)
        g.status == "MET" -> TextButton(onClick = { viewModel.claimJourneyReward(g.id) }, modifier = Modifier.testTag("journey_claim_${g.id}")) { Text("Reward unlocked: claim $reward", color = Streak) }
        else -> Text("Unlocks when met: $reward", color = TextMuted, fontSize = 12.sp)
    }
}

/** Shares a commitment and its deadline with a friend. The reward stays hidden unless the switch is turned on. */
@Composable
private fun RowScope.PromiseShare(g: Goal) {
    val context = LocalContext.current
    var showReward by remember { mutableStateOf(false) }
    TextButton(onClick = { JourneyShareCards.share(context, JourneyShare.promise(g, showReward), "my-promise.png", "Share my promise") }, modifier = Modifier.testTag("journey_promise_${g.id}")) { Text("Share as a promise") }
    if (g.rewardText != null) {
        Spacer(Modifier.weight(1f))
        Text("Show reward", color = TextMuted, fontSize = 12.sp)
        Switch(showReward, { showReward = it }, modifier = Modifier.padding(start = 8.dp).testTag("journey_promise_reward_${g.id}"))
    }
}

@Composable
private fun GateHeader(region: RegionDef, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Gate challenge" + if (count == 0) "" else " · $count ${if (count == 1) "commitment" else "commitments"}",
            color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f)
        )
        Text(JourneyRail.labels(region).last(), color = TextMuted, fontSize = 12.sp)
    }
}

/** What to do here: nothing at Home Start, or set the first commitment. Empty once there are commitments to show. */
@Composable
private fun GateHint(canAdd: Boolean, empty: Boolean) {
    when {
        !canAdd -> Text("Home Start has no commitments. Walk the three days and the trail opens.", color = TextMuted, fontSize = 13.sp)
        empty -> Text("Set a commitment to pass this region's gate. It takes about 20 seconds.", color = TextMuted, fontSize = 13.sp)
    }
}

@Composable
private fun AddOneButton(first: Boolean, room: Boolean, onAdd: () -> Unit) {
    if (!room) return
    OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth().testTag("journey_add_commitment")) { Text(if (first) "Set a commitment" else "Add one") }
}
