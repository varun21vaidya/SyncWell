package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Goal
import com.example.domain.GoalMeasure
import com.example.domain.GoalReview
import com.example.ui.theme.*

private fun unitOf(g: Goal) = when (GoalMeasure.valueOf(g.measure)) {
    GoalMeasure.COUNT -> "times"
    GoalMeasure.DURATION -> "hours"
    GoalMeasure.PERCENT -> "% now"
    GoalMeasure.MILESTONE -> "of the milestone"
}

/** The dialog window around [GoalProgressForm]. */
@Composable
fun GoalProgressDialog(goal: Goal, progress: Double, onAdd: (amount: Double, note: String?) -> Unit, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        Surface(shape = RoundedCornerShape(24.dp), color = Surface2) { GoalProgressForm(goal, progress, onAdd, onClose) }
    }
}

/**
 * Logs one dated progress entry for a goal, with an optional note. Presentation only: [onAdd] saves it.
 * A milestone is a single tap, since it is reached or it is not. [progress] is what is logged so far.
 */
@Composable
fun GoalProgressForm(goal: Goal, progress: Double, onAdd: (amount: Double, note: String?) -> Unit, onClose: () -> Unit) {
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val amount = GoalReview.parseAmount(amountText)
    val milestone = GoalMeasure.valueOf(goal.measure) == GoalMeasure.MILESTONE

    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(goal.title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("So far ${trimmed(progress)} of ${trimmed(goal.target)}", color = TextSecondary, fontSize = 13.sp)
        if (!milestone) {
            OutlinedTextField(
                amountText, { amountText = it }, Modifier.fillMaxWidth().testTag("progress_amount"),
                label = { Text("Add (${unitOf(goal)})") }, singleLine = true, isError = amountText.isNotBlank() && amount == null
            )
        }
        OutlinedTextField(note, { note = it.take(120) }, Modifier.fillMaxWidth().testTag("progress_note"), label = { Text("Note (optional)") })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onClose) { Text("Cancel") }
            if (milestone) TextButton(onClick = { onAdd(goal.target, note.ifBlank { null }); onClose() }, modifier = Modifier.testTag("progress_add")) { Text("Mark reached") }
            else TextButton(onClick = { amount?.let { onAdd(it, note.ifBlank { null }); onClose() } }, enabled = amount != null, modifier = Modifier.testTag("progress_add")) { Text("Add") }
        }
    }
}

private fun trimmed(d: Double) = if (d % 1.0 == 0.0) d.toLong().toString() else "%.1f".format(java.util.Locale.US, d)
