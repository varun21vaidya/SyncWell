package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Goal
import com.example.data.JourneyNote
import com.example.domain.GateScript
import com.example.domain.RegionDef
import com.example.domain.ReviewQueue
import com.example.domain.formatAmount
import com.example.ui.theme.*

/** The shared frame of a moment: it takes over the whole screen, with a title and room for the content. */
@Composable
private fun MomentFrame(tag: String, title: String, subtitle: String? = null, background: Brush? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().then(if (background != null) Modifier.background(background) else Modifier.background(Background)).verticalScroll(rememberScrollState()).padding(24.dp).testTag(tag),
        verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        subtitle?.let { Text(it, color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        Text(title, color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        content()
    }
}

@Composable
private fun MomentButton(label: String, tag: String, primary: Boolean = true, enabled: Boolean = true, onClick: () -> Unit) {
    if (primary) Button(
        onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().height(52.dp).testTag(tag), shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Progress, contentColor = OnProgress)
    ) { Text(label, fontWeight = FontWeight.Black) }
    else TextButton(onClick = onClick, modifier = Modifier.testTag(tag)) { Text(label, color = TextSecondary) }
}

/** "While you were away": the oldest decided commitment as a letter, pass or fail and what changed. */
@Composable
fun ReviewLetterScreen(goal: Goal, logged: Double, position: Int, total: Int, onNext: () -> Unit) =
    MomentFrame("moment_review", "While you were away", "Review $position of $total") {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface1).padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (goal.status == "MET") "Commitment met" else "Commitment not met", color = if (goal.status == "MET") Progress else Streak, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Text(goal.title, color = TextPrimary, fontWeight = FontWeight.Bold)
            Text(ReviewQueue.describe(goal, logged), color = TextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
        }
        MomentButton(if (position < total) "Next" else "Got it", "moment_next", onClick = onNext)
    }

/** The walk back to camp or the start, with what stays yours. */
@Composable
fun BackToCampScreen(note: String, stamps: Int, onDone: () -> Unit) =
    MomentFrame("moment_back", "Back to camp") {
        Text(note, color = TextSecondary, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface1).padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Still yours", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Your level and your badges", color = TextPrimary)
            Text("${if (stamps == 1) "1 stamp" else "$stamps stamps"} in your passport", color = TextPrimary)
            Text("Every region you crossed", color = TextPrimary)
        }
        MomentButton("Got it", "moment_done", onClick = onDone)
    }

/** The admit-one ticket for a reward earned by meeting a commitment. */
@Composable
fun TicketScreen(goal: Goal, onLater: () -> Unit, onClaim: () -> Unit) =
    MomentFrame("moment_ticket", "Reward unlocked") {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface2).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Admit one", color = Streak, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(goal.rewardText.orEmpty(), color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.testTag("moment_reward"))
            Text("For meeting ${goal.title}", color = TextSecondary, fontSize = 13.sp)
        }
        Text("You decide when. The app cannot stop an early treat, so it is on your honour.", color = TextMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
        MomentButton("Claim it", "moment_claim", onClick = onClaim)
        MomentButton("Save for later", "moment_later", primary = false, onClick = onLater)
    }

/** The camp by firelight: one optional line for the journal, or skip. */
@Composable
fun CampScreen(regionName: String, existing: JourneyNote?, onWrite: (String) -> Unit, onSkip: () -> Unit) =
    MomentFrame("moment_camp", "Camp", "Halfway through $regionName", background = Brush.verticalGradient(listOf(Color(0xFF2A1F16), Background))) {
        Text("Rest a moment. Nothing here is required.", color = TextSecondary, fontSize = 14.sp)
        CampReflectionCard(regionName, existing, onWrite)
        MomentButton("Skip", "moment_skip", primary = false, onClick = onSkip)
    }

/** Gate day: the Warden, their words, a seal for each pledge kept, and the way through once all are. */
@Composable
fun GateDayScreen(region: RegionDef, commitments: List<Goal>, ambition: String?, identity: String?, nextName: String, onCross: () -> Unit, onLater: () -> Unit) {
    val allMet = commitments.all { it.status == "MET" }
    MomentFrame("moment_gate", "The gate of ${region.name}", "Warden ${region.warden.name}") {
        GateScript.opening(region.warden, ambition, identity).forEach { Text(it, color = TextSecondary, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center) }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface1).padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            commitments.forEach { Text((if (it.status == "MET") "✓  " else "○  ") + it.title, color = if (it.status == "MET") Progress else TextMuted, fontWeight = FontWeight.Bold) }
            if (commitments.isEmpty()) Text("Nothing to check. The way is open.", color = TextSecondary)
        }
        Text(GateScript.verdict(region.warden, allMet), color = if (allMet) Progress else TextSecondary, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        MomentButton("Cross into $nextName", "moment_cross", enabled = allMet, onClick = onCross)
        MomentButton("Back to the trail", "moment_later", primary = false, onClick = onLater)
    }
}

/** The Summit: the habit-formed milestone with the numbers behind it. */
@Composable
fun SummitScreen(days: Int, stamps: Int, bigReward: String?, rewardBack: Boolean, onDone: () -> Unit) =
    MomentFrame("moment_summit", "The Summit") {
        Text("Research puts the average time to form a habit at about 66 days. You walked ${if (days == 1) "1 day" else "$days days"} of it.", color = TextSecondary, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface1).padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${formatAmount(days.toDouble())} days on the trail", color = TextPrimary, fontWeight = FontWeight.Bold)
            Text("${if (stamps == 1) "1 stamp" else "$stamps stamps"} in your passport", color = TextPrimary)
            bigReward?.let { Text(if (rewardBack) "It is yours again: $it" else "Giving up until then: $it", color = Streak, modifier = Modifier.testTag("moment_big_reward")) }
        }
        Text("New regions arrive weekly. Keep walking, and set a new ambition when you are ready.", color = TextMuted, fontSize = 13.sp, textAlign = TextAlign.Center)
        MomentButton("Keep walking", "moment_done", onClick = onDone)
    }
