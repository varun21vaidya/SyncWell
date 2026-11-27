package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Goal
import com.example.domain.CommitmentDraft
import com.example.domain.CommitmentRules
import com.example.domain.CommitmentWindow
import com.example.domain.DraftError
import com.example.domain.GoalMeasure
import com.example.domain.formatAmount
import com.example.ui.theme.*
import kotlinx.coroutines.launch

/** The one-line research note behind the "why" fields, shared with the Journey ambition form. */
internal const val WHY_IDENTITY = "People who tie a habit to who they want to become stick with it longer. Each good day is a vote for that person. (James Clear, Atomic Habits)"

/** The one-line research notes behind the info buttons. */
private enum class Why(val title: String, val text: String) {
    IDENTITY("Why this matters", WHY_IDENTITY),
    WHEN_WHERE("When and where", "A review of 94 studies found that deciding when and where you'll act makes you much more likely to follow through. (Gollwitzer and Sheeran, 2006)"),
    IF_THEN("If-then backup plan", "Planning what you'll do when the obstacle you expect shows up helps you keep going on bad days. (Oettingen, WOOP)")
}

private val MEASURE_LABEL = mapOf(GoalMeasure.COUNT to "Count", GoalMeasure.DURATION to "Hours", GoalMeasure.MILESTONE to "Milestone", GoalMeasure.PERCENT to "Percent")

private fun DraftError.message() = when (this) {
    DraftError.TITLE -> "Give the commitment a name."
    DraftError.TARGET -> "The target must be above 0 (and at most 100 for a percentage)."
    DraftError.TOO_MANY -> "This region already has ${CommitmentRules.MAX_PER_REGION} commitments."
    DraftError.WINDOW -> "That window is not available in this region."
    DraftError.TIME -> "Write the time as 24-hour HH:MM, for example 13:30."
    DraftError.TOO_LONG -> "One of the texts is too long."
}

/**
 * Sets one commitment, or with [weekly] one weekly goal: core fields up front, the rest under More (commitments only).
 * A weekly goal covers this calendar week and has no window, reward or Warden. Presentation only: [onSave] validates and saves,
 * and returns what blocks it (empty once saved). A commitment is locked once set, so the screen says so before saving.
 */
@Composable
fun CommitmentEditor(
    region: Int,
    previous: List<Goal>,
    onSave: suspend (CommitmentDraft) -> List<DraftError>,
    onClose: () -> Unit,
    weekly: Boolean = false
) {
    val scope = rememberCoroutineScope()
    val suggestions = remember(previous) { CommitmentRules.suggestions(previous) }
    var draft by remember { mutableStateOf(CommitmentRules.prefillMore(previous.maxByOrNull { it.windowEnd })) }
    var targetText by rememberSaveable { mutableStateOf("") }
    var more by rememberSaveable { mutableStateOf(false) }
    var errors by remember { mutableStateOf<List<DraftError>>(emptyList()) }
    var saving by remember { mutableStateOf(false) }
    var why by remember { mutableStateOf<Why?>(null) }

    Column(Modifier.fillMaxSize().background(Background).padding(16.dp).testTag("commitment_editor")) {
        EditorTitle(weekly, enabled = !saving, onClose = onClose)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Suggestions(suggestions) { g -> draft = CommitmentRules.fromGoal(g, region); targetText = formatAmount(g.target) }
            OutlinedTextField(draft.title, { draft = draft.copy(title = it) }, Modifier.fillMaxWidth().testTag("commitment_title"), label = { Text("Goal") }, singleLine = true)
            Text("Measure", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GoalMeasure.values().forEach { m -> FilterChip(selected = draft.measure == m, onClick = { draft = draft.copy(measure = m) }, label = { Text(MEASURE_LABEL.getValue(m)) }) }
            }
            OutlinedTextField(
                targetText, { targetText = it; draft = draft.copy(target = it.replace(',', '.').toDoubleOrNull() ?: 0.0) },
                Modifier.fillMaxWidth().testTag("commitment_target"), label = { Text(if (draft.measure == GoalMeasure.PERCENT) "Target (%)" else "Target") }, singleLine = true
            )
            if (!weekly) WindowChips(region, draft) { draft = it }
            if (draft.measure == GoalMeasure.DURATION) FocusLink(draft.focusLinked) { draft = draft.copy(focusLinked = it) }
            if (!weekly) MoreFields(draft, more, onToggle = { more = !more }, onChange = { draft = it }, onWhy = { why = it })
            errors.forEach { Text(it.message(), color = Danger, fontSize = 12.sp) }
            EditorNote(weekly)
        }
        SaveButton(weekly, enabled = !saving) { saving = true; scope.launch { errors = onSave(draft); saving = false; if (errors.isEmpty()) onClose() } }
    }
    WhyDialog(why) { why = null }
}

@Composable
private fun EditorTitle(weekly: Boolean, enabled: Boolean, onClose: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onClose, enabled = enabled) { Text("← Close") }
        Text(if (weekly) "Set a weekly goal" else "Set a commitment", fontSize = 20.sp, fontWeight = FontWeight.Black, color = TextPrimary)
    }
}

@Composable
private fun EditorNote(weekly: Boolean) = Text(
    if (weekly) "Counts in your weekly goals. A week you miss costs nothing."
    else "Once set, a commitment is locked. A missed one sends you back to the start of the region.",
    color = TextMuted, fontSize = 12.sp
)

@Composable
private fun SaveButton(weekly: Boolean, enabled: Boolean, onClick: () -> Unit) = Button(
    onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().height(52.dp).testTag("commitment_save"), shape = RoundedCornerShape(14.dp),
    colors = ButtonDefaults.buttonColors(containerColor = Progress, contentColor = OnProgress)
) { Text(if (weekly) "Add weekly goal" else "Set commitment", fontWeight = FontWeight.Black) }

@Composable
private fun WhyDialog(why: Why?, onClose: () -> Unit) {
    if (why == null) return
    AlertDialog(onDismissRequest = onClose, title = { Text(why.title) }, text = { Text(why.text) }, confirmButton = { TextButton(onClick = onClose) { Text("OK") } })
}

/** The user's own earlier goals as chips; picking one fills the editor. */
@Composable
private fun Suggestions(suggestions: List<Goal>, onPick: (Goal) -> Unit) {
    if (suggestions.isEmpty()) return
    Text("Your earlier goals", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        suggestions.forEach { g -> AssistChip(onClick = { onPick(g) }, label = { Text(g.title, maxLines = 1) }) }
    }
}

@Composable
private fun WindowChips(region: Int, draft: CommitmentDraft, onChange: (CommitmentDraft) -> Unit) {
    Text("Window", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CommitmentRules.windows(region).forEach { w -> FilterChip(selected = draft.window == w, onClick = { onChange(draft.copy(window = w)) }, label = { Text(w.label) }) }
    }
}

@Composable
private fun FocusLink(linked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Switch(linked, onChange)
        Text("  Count Focus timer hours automatically", color = TextSecondary, fontSize = 13.sp)
    }
}

/** Reward, when-and-where plan with time, and the if-then backup plan, behind one toggle. */
@Composable
private fun MoreFields(draft: CommitmentDraft, open: Boolean, onToggle: () -> Unit, onChange: (CommitmentDraft) -> Unit, onWhy: (Why) -> Unit) {
    TextButton(onClick = onToggle, modifier = Modifier.testTag("commitment_more")) { Text(if (open) "Less" else "More: reward, when and where, backup plan") }
    if (!open) return
    OutlinedTextField(draft.reward.orEmpty(), { onChange(draft.copy(reward = it)) }, Modifier.fillMaxWidth(), label = { Text("Reward when met (optional)") }, singleLine = true)
    OutlinedTextField(
        draft.planText.orEmpty(), { onChange(draft.copy(planText = it)) }, Modifier.fillMaxWidth(),
        label = { Text("When and where") }, singleLine = true, trailingIcon = { InfoButton { onWhy(Why.WHEN_WHERE) } }
    )
    OutlinedTextField(
        draft.planTime.orEmpty(), { onChange(draft.copy(planTime = it.ifBlank { null })) }, Modifier.fillMaxWidth(),
        label = { Text("Time, HH:MM (shows in Up next)") }, singleLine = true
    )
    OutlinedTextField(
        draft.ifThen.orEmpty(), { onChange(draft.copy(ifThen = it)) }, Modifier.fillMaxWidth(),
        label = { Text("If I can't, then I will…") }, trailingIcon = { InfoButton { onWhy(Why.IF_THEN) } }
    )
}

@Composable
private fun InfoButton(onClick: () -> Unit) =
    IconButton(onClick = onClick) { Icon(Icons.Default.Info, contentDescription = "Why this helps", tint = TextMuted) }
