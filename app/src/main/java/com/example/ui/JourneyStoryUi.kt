package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Goal
import com.example.domain.GateScript
import com.example.domain.JourneyMap
import com.example.domain.RegionDef
import com.example.domain.ReviewQueue
import com.example.ui.theme.*
import com.example.viewmodel.SyncWellViewModel

@Composable
private fun StoryCard(tag: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface1).padding(16.dp).testTag(tag), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
}

/** The oldest decided commitment the user has not read yet, one at a time: pass or fail and what changed. */
@Composable
fun ReviewCard(viewModel: SyncWellViewModel, goals: List<Goal>, seen: Set<String>) {
    val next = remember(goals, seen) { ReviewQueue.unseen(goals, seen).firstOrNull() } ?: return
    val logged by produceState(0.0, next) { value = viewModel.goalProgress(next) }
    StoryCard("journey_review") {
        Text(if (next.status == "MET") "Commitment met" else "Commitment not met", color = if (next.status == "MET") Progress else Streak, fontWeight = FontWeight.Black, fontSize = 16.sp)
        Text(next.title, color = TextPrimary, fontWeight = FontWeight.Bold)
        Text(ReviewQueue.describe(next, logged), color = TextSecondary, fontSize = 13.sp)
        TextButton(onClick = { viewModel.markReviewSeen(next.id) }, modifier = Modifier.testTag("journey_review_ok")) { Text("Got it") }
    }
}

/** Why the character moved back, in calm words. */
@Composable
fun SetbackBanner(text: String) {
    StoryCard("journey_setback") { Text(text, color = TextSecondary, fontSize = 13.sp, lineHeight = 19.sp) }
}

/** On gate day the region's Warden presents the commitments as the challenge; each lights up when met, then the gate opens. */
@Composable
fun WardenCard(region: RegionDef, commitments: List<Goal>, ambition: String?, identity: String?, onCross: () -> Unit) {
    val allMet = commitments.all { it.status == "MET" }
    StoryCard("journey_warden") {
        Text("${region.warden.name}, at the gate of ${region.name}", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
        Text(region.warden.personality.replaceFirstChar { it.uppercase() }, color = TextMuted, fontSize = 12.sp)
        GateScript.opening(region.warden, ambition, identity).forEach { Text(it, color = TextSecondary, fontSize = 13.sp, lineHeight = 19.sp) }
        commitments.forEach { Text((if (it.status == "MET") "✓ " else "○ ") + it.title, color = if (it.status == "MET") Progress else TextMuted, fontSize = 13.sp) }
        Text(GateScript.verdict(region.warden, allMet), color = if (allMet) Progress else TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Button(
            onClick = onCross, enabled = allMet, modifier = Modifier.fillMaxWidth().height(48.dp).testTag("journey_cross"), shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Progress, contentColor = OnProgress)
        ) { Text("Cross the gate", fontWeight = FontWeight.Black) }
    }
}

/** After the Summit gate: the habit-formed milestone, and the weekly regions that follow. */
@Composable
fun SummitCard() {
    StoryCard("journey_summit") {
        Text("The Summit", color = Streak, fontWeight = FontWeight.Black, fontSize = 22.sp)
        Text("Sixty-six days on the trail. Research puts the average time to form a habit at about 66 days, and you walked every one of them.", color = TextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
        Text("New regions arrive weekly. Keep walking, and set a new ambition when you are ready.", color = TextMuted, fontSize = 13.sp)
    }
}

/** Header words for the current region. */
@Composable
fun JourneyHeader(region: RegionDef, step: Int) {
    Column {
        Text(region.name, color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Black, modifier = Modifier.testTag("journey_region"))
        Text(
            JourneyMap.dayLabel(region, step) + (JourneyMap.nextMilestone(region, step)?.let { " · $it" } ?: ""),
            color = TextSecondary, fontSize = 14.sp, modifier = Modifier.testTag("journey_day")
        )
        if (region.intro.isNotBlank()) Text(region.intro, color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
    }
}
