package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Goal
import com.example.domain.CommitmentRules
import com.example.ui.theme.*
import com.example.viewmodel.SyncWellViewModel

/** This region's commitments with their progress, rewards and the buttons to log progress or set a new one. */
@Composable
fun CommitmentsCard(viewModel: SyncWellViewModel, commitments: List<Goal>, canAdd: Boolean, onAdd: () -> Unit) {
    var logging by remember { mutableStateOf<Goal?>(null) }
    var refresh by remember { mutableIntStateOf(0) } // bumped after a log so the bars read the new total
    val open = commitments.count { it.status != "MISSED" }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface1).padding(16.dp).testTag("journey_commitments"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Commitments", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        if (!canAdd) Text("Home Start has no commitments. Walk the three days and the trail opens.", color = TextMuted, fontSize = 13.sp)
        else if (commitments.isEmpty()) Text("Set a commitment to pass this region's gate. It takes about 20 seconds.", color = TextMuted, fontSize = 13.sp)
        commitments.forEach { g -> CommitmentRow(viewModel, g, refresh) { logging = g } }
        if (canAdd && open < CommitmentRules.MAX_PER_REGION) {
            OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth().testTag("journey_add_commitment")) { Text(if (commitments.isEmpty()) "Set a commitment" else "Add another") }
        }
        if (commitments.isNotEmpty()) Text("The gate opens once every commitment is met.", color = TextMuted, fontSize = 12.sp)
    }
    logging?.let { g ->
        val progress by produceState(0.0, g, refresh) { value = viewModel.goalProgress(g) }
        GoalProgressDialog(g, progress, onAdd = { amount, note -> viewModel.addGoalProgress(g.id, amount, note); refresh++ }, onClose = { logging = null })
    }
}

@Composable
private fun CommitmentRow(viewModel: SyncWellViewModel, g: Goal, refresh: Int, onLog: () -> Unit) {
    val progress by produceState(0.0, g, refresh) { value = viewModel.goalProgress(g) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(g.title, color = TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(statusText(g), color = if (g.status == "MET") Progress else if (g.status == "MISSED") Danger else TextMuted, fontSize = 12.sp)
        }
        LinearProgressIndicator(progress = { (progress / g.target).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = Progress, trackColor = Surface3)
        Text("${trim(progress)} of ${trim(g.target)}${g.targetLabel?.let { " $it" } ?: ""}", color = TextSecondary, fontSize = 12.sp)
        g.rewardText?.let { RewardLine(viewModel, g, it) }
        if (g.status == "PENDING") TextButton(onClick = onLog, modifier = Modifier.testTag("journey_log_${g.id}")) { Text("Log progress") }
    }
}

@Composable
private fun RewardLine(viewModel: SyncWellViewModel, g: Goal, reward: String) {
    when {
        g.rewardClaimedAt != null -> Text("Reward claimed: $reward", color = Streak, fontSize = 12.sp)
        g.status == "MET" -> TextButton(onClick = { viewModel.claimJourneyReward(g.id) }, modifier = Modifier.testTag("journey_claim_${g.id}")) { Text("Reward unlocked: claim $reward", color = Streak) }
        else -> Text("Unlocks when met: $reward", color = TextMuted, fontSize = 12.sp)
    }
}

private fun statusText(g: Goal) = when (g.status) {
    "MET" -> "Met"
    "MISSED" -> "Missed, back to the start of the region"
    else -> "By ${g.windowEnd}"
}

private fun trim(d: Double) = if (d % 1.0 == 0.0) d.toLong().toString() else "%.1f".format(java.util.Locale.US, d)
