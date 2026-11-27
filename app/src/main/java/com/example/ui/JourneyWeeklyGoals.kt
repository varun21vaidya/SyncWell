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
import com.example.domain.formatAmount
import com.example.ui.theme.*
import com.example.viewmodel.SyncWellViewModel

/** The optional weekly goals of this calendar week: progress bars, a way to log, and an Add button. Never penalised. */
@Composable
fun WeeklyGoalsCard(viewModel: SyncWellViewModel, weekly: List<Goal>, onAdd: () -> Unit) {
    var logging by remember { mutableStateOf<Goal?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface1).padding(16.dp).testTag("journey_weekly"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Weekly goals · optional", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Text("Ends Sunday", color = TextMuted, fontSize = 12.sp)
        }
        if (weekly.isEmpty()) Text("Goals that count for the week. A week you miss costs nothing.", color = TextMuted, fontSize = 13.sp)
        weekly.forEach { g -> WeeklyRow(viewModel, g, refresh) { logging = g } }
        OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth().testTag("journey_add_weekly")) { Text(if (weekly.isEmpty()) "Add a weekly goal" else "Add one") }
    }
    logging?.let { g ->
        val progress by produceState(0.0, g, refresh) { value = viewModel.goalProgress(g) }
        GoalProgressDialog(g, progress, onAdd = { amount, note -> viewModel.addGoalProgress(g.id, amount, note); refresh++ }, onClose = { logging = null })
    }
}

@Composable
private fun WeeklyRow(viewModel: SyncWellViewModel, g: Goal, refresh: Int, onLog: () -> Unit) {
    val progress by produceState(0.0, g, refresh) { value = viewModel.goalProgress(g) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(g.title, color = TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(
                if (g.status == "MET") "Met" else "${formatAmount(progress)} / ${formatAmount(g.target)}${g.targetLabel?.let { " $it" } ?: ""}",
                color = if (g.status == "MET") Progress else TextSecondary, fontSize = 12.sp
            )
        }
        LinearProgressIndicator(progress = { (progress / g.target).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = Progress, trackColor = Surface3)
        if (g.status == "PENDING") TextButton(onClick = onLog, modifier = Modifier.testTag("journey_log_${g.id}")) { Text("Log progress") }
    }
}
