package com.example.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.example.domain.CommitmentRules
import com.example.service.DayBoundaryManager
import com.example.viewmodel.SyncWellViewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Goal
import com.example.ui.theme.*

/** Lets the Focus timer count toward a Journey goal before it starts. Shows nothing when there is no goal to count toward. */
@Composable
fun FocusGoalChips(viewModel: SyncWellViewModel) {
    val goals by viewModel.journeyGoals.collectAsState()
    val selectedId by viewModel.activeGoalId.collectAsState()
    val today by DayBoundaryManager.currentTargetDate.collectAsState()
    val choices = remember(goals, today) { CommitmentRules.focusChoices(goals, today) }
    FocusGoalChipRow(choices, selectedId) { viewModel.setActiveGoal(it) }
}

@Composable
private fun FocusGoalChipRow(choices: List<Goal>, selectedId: String?, onSelect: (String?) -> Unit) {
    if (choices.isEmpty()) return
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Count toward a Journey goal (optional)", color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = selectedId == null, onClick = { onSelect(null) }, label = { Text("None", fontSize = 12.sp) }, shape = RoundedCornerShape(20.dp))
            choices.forEach { g ->
                FilterChip(selected = selectedId == g.id, onClick = { onSelect(g.id) }, label = { Text(g.title, fontSize = 12.sp, maxLines = 1) }, shape = RoundedCornerShape(20.dp))
            }
        }
    }
}
