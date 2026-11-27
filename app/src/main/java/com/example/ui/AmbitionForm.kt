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
import androidx.compose.ui.window.Dialog
import com.example.data.JourneyState
import com.example.domain.Ambition
import com.example.domain.BigReward
import com.example.domain.JourneyShare
import com.example.ui.theme.*

/** The Journey's "why", shown under the map: the user's own words, or a gentle invitation to write them. Everything is optional. */
@Composable
fun AmbitionCard(state: JourneyState, today: String, onEdit: () -> Unit, onShare: (() -> Unit)? = null) {
    val empty = state.ambitionText == null && state.identityText == null
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface1).padding(16.dp).testTag("journey_ambition"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Why you walk", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        if (empty) Text("Optional: name what this trail is preparing you for. The Wardens will quote it at the gates.", color = TextSecondary, fontSize = 13.sp)
        AmbitionLines(state, today)
        Row {
            TextButton(onClick = onEdit, modifier = Modifier.testTag("journey_ambition_edit")) { Text(if (empty) "Write it" else "Edit") }
            if (onShare != null && state.ambitionText != null) TextButton(onClick = onShare, modifier = Modifier.testTag("journey_ambition_share")) { Text("Share") }
        }
    }
}

/** The ambition, the identity line, the countdown and the favourite thing given up (or back). */
@Composable
private fun AmbitionLines(state: JourneyState, today: String) {
    state.ambitionText?.let { Text(it, color = TextPrimary, fontWeight = FontWeight.Bold) }
    state.identityText?.let { Text(it, color = TextSecondary, fontSize = 13.sp) }
    JourneyShare.countdown(state.targetDate, today)?.let { left ->
        Text(if (left == 0) "Today is the day" else "$left days to go", color = Streak, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("journey_countdown"))
    }
    state.bigRewardText?.let {
        Text(if (state.bigRewardUnlockedAt != null) "It is yours again: $it" else "Giving up until then: $it", color = TextMuted, fontSize = 12.sp, modifier = Modifier.testTag("journey_big_reward"))
    }
}

/** The dialog window around [AmbitionForm]. */
@Composable
fun AmbitionDialog(state: JourneyState, today: String, onSave: (String?, String?, String?, String?) -> Unit, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        Surface(shape = RoundedCornerShape(24.dp), color = Surface2) { AmbitionForm(state.ambitionText, state.identityText, state.targetDate, state.bigRewardText, today, onSave, onClose) }
    }
}

/**
 * Optional lines with the research note beside them. A target date is a countdown only; with one, the user may give up a favourite thing until then.
 * Presentation only: [onSave] gets the cleaned text, or null for an empty field.
 */
@Composable
fun AmbitionForm(
    ambition: String?, identity: String?, targetDate: String?, bigReward: String?, today: String,
    onSave: (String?, String?, String?, String?) -> Unit, onClose: () -> Unit,
    saveLabel: String = "Save", cancelLabel: String = "Cancel", showTitle: Boolean = true
) {
    var a by remember { mutableStateOf(ambition.orEmpty()) }
    var i by remember { mutableStateOf(identity.orEmpty()) }
    var date by remember { mutableStateOf(targetDate.orEmpty()) }
    var reward by remember { mutableStateOf(bigReward.orEmpty()) }
    val status = Ambition.dateStatus(date, targetDate, today)
    val hasDate = status == Ambition.DateStatus.VALID || status == Ambition.DateStatus.KEPT
    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (showTitle) Text("Why you walk", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(a, { a = it.take(Ambition.MAX_AMBITION) }, Modifier.fillMaxWidth().testTag("ambition_text"), label = { Text("Ambition (optional)") }, placeholder = { Text("Land a backend role") }, singleLine = true)
        OutlinedTextField(i, { i = it.take(Ambition.MAX_IDENTITY) }, Modifier.fillMaxWidth().testTag("identity_text"), label = { Text("Who you are becoming (optional)") }, placeholder = { Text("I'm becoming someone who practises daily") })
        Text(WHY_IDENTITY, color = TextMuted, fontSize = 11.sp, lineHeight = 16.sp)
        if (a.isNotBlank()) DateAndReward(date, { date = it.take(10) }, reward, { reward = it.take(Ambition.MAX_REWARD) }, status, today)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onClose, modifier = Modifier.testTag("ambition_cancel")) { Text(cancelLabel) }
            TextButton(onClick = {
                onSave(Ambition.clean(a, Ambition.MAX_AMBITION), Ambition.clean(i, Ambition.MAX_IDENTITY), date.trim().takeIf { hasDate }, Ambition.clean(reward, Ambition.MAX_REWARD))
                onClose()
            }, enabled = status != Ambition.DateStatus.INVALID, modifier = Modifier.testTag("ambition_save")) { Text(saveLabel) }
        }
    }
}

/** The target date, and once it is valid the favourite thing to give up until then, with when it comes back. */
@Composable
private fun DateAndReward(date: String, onDate: (String) -> Unit, reward: String, onReward: (String) -> Unit, status: Ambition.DateStatus, today: String) {
    val bad = status == Ambition.DateStatus.INVALID
    OutlinedTextField(date, onDate, Modifier.fillMaxWidth().testTag("ambition_date"), label = { Text("Target date, YYYY-MM-DD (optional)") }, singleLine = true, isError = bad,
        supportingText = { Text(if (bad) "Use a real date that is today or later." else "Only a countdown: the Summit on the map does not move.") })
    if (status == Ambition.DateStatus.VALID || status == Ambition.DateStatus.KEPT) {
        OutlinedTextField(reward, onReward, Modifier.fillMaxWidth().testTag("big_reward"), label = { Text("Give up a favourite thing until then (optional)") }, placeholder = { Text("Gaming") }, singleLine = true,
            supportingText = { Text(BigReward.explain(today, date.trim()) + " Honor system.") })
    }
}
