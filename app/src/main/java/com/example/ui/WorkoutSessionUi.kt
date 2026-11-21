package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ExerciseLibraryItem
import com.example.data.WorkoutSession
import com.example.data.WorkoutSessionExercise
import com.example.data.WorkoutSetLog
import com.example.domain.NextSplitRule
import com.example.domain.PersonalBestRule
import com.example.domain.SessionClock
import com.example.domain.SetRef
import com.example.ui.theme.*
import com.example.viewmodel.ExerciseHistory
import com.example.viewmodel.RestState
import com.example.viewmodel.WorkoutSummary
import com.example.viewmodel.formatClock
import com.example.viewmodel.formatKg
import kotlinx.coroutines.delay

private const val WEIGHT_STEP = 2.5

/** Presentation only: state comes in as parameters and every action goes out as a callback. */
@Composable
fun ActiveSessionContent(
    session: WorkoutSession,
    exercises: List<WorkoutSessionExercise>,
    logs: Map<String, List<WorkoutSetLog>>,
    library: Map<String, ExerciseLibraryItem>,
    history: Map<String, ExerciseHistory>,
    clock: SessionClock,
    rest: RestState?,
    onBack: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
    onSkipRest: () -> Unit,
    onAdjustRest: (Int) -> Unit,
    onCompleteSet: (logId: String, seId: String, reps: Int, kg: Double) -> Unit,
    onUncompleteSet: (logId: String, seId: String) -> Unit,
    onSetValues: (seId: String, logId: String, reps: Int, kg: Double) -> Unit,
    onAddSet: (seId: String) -> Unit,
    onDeleteSet: (seId: String, logId: String) -> Unit,
    onRemoveExercise: (WorkoutSessionExercise) -> Unit,
    onNotes: (WorkoutSessionExercise) -> Unit,
    onAddExercise: () -> Unit,
    modifier: Modifier = Modifier
) {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(clock) { while (true) { nowMs = System.currentTimeMillis(); delay(1000) } }
    val elapsed = clock.elapsedSeconds(nowMs)

    val allLogs = exercises.flatMap { logs[it.id].orEmpty() }
    val setsDone = allLogs.count { it.isCompleted }
    val currentIdx = exercises.indexOfFirst { e -> logs[e.id].orEmpty().any { !it.isCompleted } }.let { if (it == -1) exercises.lastIndex else it }
    var expandedId by remember { mutableStateOf<String?>(null) } // null = follow the current exercise, "" = all collapsed
    val openId = expandedId ?: exercises.getOrNull(currentIdx)?.id

    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack, modifier = Modifier.height(48.dp).testTag("workout_back_button")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to workouts", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp)); Text("Workouts", fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onDiscard, colors = ButtonDefaults.textButtonColors(contentColor = RoseCrimson), modifier = Modifier.height(48.dp)) {
                Text("Discard", fontWeight = FontWeight.Bold)
            }
        }

        // Header: timer, pause, finish and progress stay visible while the list scrolls
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, OledBorder)
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(NextSplitRule.splitOf(session.dayType)?.title ?: session.dayType, color = Slate400, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(formatClock(elapsed), fontSize = 30.sp, fontWeight = FontWeight.Black, color = if (clock.isPaused) TextSecondary else MaterialTheme.colorScheme.onSurface, modifier = Modifier.testTag("session_timer"))
                            if (clock.isPaused) Text("Paused", color = AmberFlame, fontWeight = FontWeight.Black, fontSize = 12.sp, modifier = Modifier.testTag("paused_label"))
                        }
                    }
                    OutlinedButton(
                        onClick = if (clock.isPaused) onResume else onPause, shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp).testTag("pause_resume_button")
                    ) { Text(if (clock.isPaused) "Resume" else "Pause", fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onFinish, shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow),
                        modifier = Modifier.height(48.dp).testTag("finish_workout_button")
                    ) { Text("Finish", fontWeight = FontWeight.Black, color = OledBlack) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (exercises.isEmpty()) "No exercises yet" else "Exercise ${currentIdx + 1} of ${exercises.size}", color = TextSecondary, fontSize = 12.sp)
                    Text("$setsDone / ${allLogs.size} sets", color = TextSecondary, fontSize = 12.sp)
                }
                LinearProgressIndicator(
                    progress = { if (allLogs.isEmpty()) 0f else setsDone.toFloat() / allLogs.size },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = EmeraldGlow, trackColor = SurfaceTier3
                )
            }
        }

        if (rest != null) {
            Spacer(Modifier.height(8.dp))
            RestCard(rest, onAdjustRest, onSkipRest)
        }

        LazyColumn(Modifier.weight(1f).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(exercises, key = { it.id }) { se ->
                val lib = library[se.exerciseId]
                ExerciseCard(
                    se = se, name = lib?.name ?: se.exerciseId, muscle = lib?.category.orEmpty(),
                    sets = logs[se.id].orEmpty(), hist = history[se.exerciseId], expanded = openId == se.id,
                    onToggle = { expandedId = if (openId == se.id) "" else se.id },
                    onCompleteSet = onCompleteSet, onUncompleteSet = onUncompleteSet, onSetValues = onSetValues,
                    onAddSet = onAddSet, onDeleteSet = onDeleteSet, onRemove = { onRemoveExercise(se) }, onNotes = { onNotes(se) }
                )
            }
            item {
                OutlinedButton(
                    onClick = onAddExercise, shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGlow), border = BorderStroke(1.dp, EmeraldGlow),
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("add_exercise_button")
                ) { Text("Add exercise", fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun RestCard(rest: RestState, onAdjust: (Int) -> Unit, onSkip: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(rest.remaining == 0) { if (rest.remaining == 0) haptic.performHapticFeedback(HapticFeedbackType.LongPress) } // buzzes at zero
    Card(
        modifier = Modifier.fillMaxWidth().testTag("rest_timer_card"),
        colors = CardDefaults.cardColors(containerColor = SurfaceTier2), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, OledBorder)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (rest.remaining == 0) "Rest done" else formatClock(rest.remaining), fontWeight = FontWeight.Black, fontSize = 22.sp, color = EmeraldGlow)
                    Text(rest.nextLabel, color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                TextButton(onClick = { onAdjust(-15) }, modifier = Modifier.height(48.dp)) { Text("−15", fontWeight = FontWeight.Bold) }
                TextButton(onClick = { onAdjust(15) }, modifier = Modifier.height(48.dp)) { Text("+15", fontWeight = FontWeight.Bold) }
                TextButton(onClick = onSkip, modifier = Modifier.height(48.dp)) { Text("Skip", fontWeight = FontWeight.Bold, color = Slate400) }
            }
            LinearProgressIndicator(
                progress = { if (rest.total <= 0) 0f else (rest.remaining.toFloat() / rest.total).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)), color = EmeraldGlow, trackColor = SurfaceTier3
            )
        }
    }
}

@Composable
private fun ExerciseCard(
    se: WorkoutSessionExercise, name: String, muscle: String, sets: List<WorkoutSetLog>, hist: ExerciseHistory?,
    expanded: Boolean, onToggle: () -> Unit,
    onCompleteSet: (String, String, Int, Double) -> Unit, onUncompleteSet: (String, String) -> Unit,
    onSetValues: (String, String, Int, Double) -> Unit, onAddSet: (String) -> Unit, onDeleteSet: (String, String) -> Unit,
    onRemove: () -> Unit, onNotes: () -> Unit
) {
    val done = sets.count { it.isCompleted }
    val shownKg = sets.lastOrNull { it.isCompleted }?.weightKg ?: sets.firstOrNull()?.weightKg ?: 0.0
    Card(
        modifier = Modifier.fillMaxWidth().testTag("active_ex_card_${se.orderIndex}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, OledBorder)
    ) {
        if (!expanded) {
            Row(
                Modifier.fillMaxWidth().clickable { onToggle() }.defaultMinSize(minHeight = 56.dp).padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text("$done/${sets.size} sets · ${formatKg(shownKg)} kg", color = TextSecondary, fontSize = 12.sp)
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Expand $name", tint = Slate400)
            }
            return@Card
        }
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).clickable { onToggle() }) {
                    Text(name, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    if (muscle.isNotBlank()) Text(muscle.lowercase().replaceFirstChar { it.uppercase() }, color = Slate400, fontSize = 11.sp)
                }
                var menu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { menu = true }, modifier = Modifier.size(48.dp).testTag("exercise_menu_${se.orderIndex}")) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More for $name", tint = Slate400)
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Notes") }, onClick = { menu = false; onNotes() })
                        DropdownMenuItem(text = { Text("Add set") }, onClick = { menu = false; onAddSet(se.id) })
                        if (sets.size > 1) DropdownMenuItem(text = { Text("Delete last set") }, onClick = { menu = false; sets.lastOrNull()?.let { onDeleteSet(se.id, it.id) } })
                        DropdownMenuItem(text = { Text("Remove exercise", color = RoseCrimson) }, onClick = { menu = false; onRemove() })
                    }
                }
                IconButton(onClick = onToggle, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Collapse $name", tint = Slate400) }
            }

            val completed = sets.filter { it.isCompleted }
            val prIdx = PersonalBestRule.newBestIndex(hist?.best, completed.map { SetRef(it.weightKg, it.reps) })
            val prLogId = prIdx?.let { completed.getOrNull(it)?.id }
            sets.forEachIndexed { i, log ->
                SetRow(
                    log = log, previous = hist?.previous?.getOrNull(i)?.let { "${formatKg(it.weightKg)} × ${it.reps}" } ?: "—",
                    isPr = log.id == prLogId,
                    onCheck = { if (log.isCompleted) onUncompleteSet(log.id, se.id) else onCompleteSet(log.id, se.id, log.reps, log.weightKg) },
                    onKg = { onSetValues(se.id, log.id, log.reps, (log.weightKg + it).coerceAtLeast(0.0)) },
                    onReps = { onSetValues(se.id, log.id, (log.reps + it).coerceAtLeast(0), log.weightKg) }
                )
            }
        }
    }
}

@Composable
private fun SetRow(log: WorkoutSetLog, previous: String, isPr: Boolean, onCheck: () -> Unit, onKg: (Double) -> Unit, onReps: (Int) -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (log.isCompleted) EmeraldDim.copy(alpha = 0.35f) else SurfaceTier2).padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Set ${log.setIndex}", fontWeight = FontWeight.Black, fontSize = 13.sp)
                Text("Previous $previous", color = Slate400, fontSize = 11.sp)
                if (isPr) PrBadge()
            }
            IconButton(onClick = onCheck, modifier = Modifier.size(48.dp).testTag("set_check_${log.setIndex}")) {
                Icon(Icons.Default.CheckCircle, contentDescription = if (log.isCompleted) "Set ${log.setIndex} logged, tap to undo" else "Log set ${log.setIndex}",
                    tint = if (log.isCompleted) EmeraldGlow else TextMuted, modifier = Modifier.size(28.dp))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Stepper("kg", formatKg(log.weightKg), { onKg(-WEIGHT_STEP) }, { onKg(WEIGHT_STEP) })
            Stepper("reps", log.reps.toString(), { onReps(-1) }, { onReps(1) })
        }
    }
}

@Composable
private fun Stepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onMinus, modifier = Modifier.size(48.dp)) { Text("−", fontSize = 22.sp, color = TextPrimary, modifier = Modifier.semantics { contentDescription = "Decrease $label" }) }
        Column(Modifier.widthIn(min = 52.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.Black, fontSize = 17.sp)
            Text(label, color = Slate400, fontSize = 10.sp)
        }
        IconButton(onClick = onPlus, modifier = Modifier.size(48.dp)) { Text("+", fontSize = 22.sp, color = TextPrimary, modifier = Modifier.semantics { contentDescription = "Increase $label" }) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExerciseSheet(
    library: List<ExerciseLibraryItem>, inSession: Set<String>, environment: String,
    onAdd: (String) -> Unit, onCreate: (name: String, muscle: String) -> Unit, onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf<String?>(null) }
    var newName by remember { mutableStateOf("") }
    val pool = library.filter { if (environment == "HOME") it.environmentType == "HOME" else it.environmentType == null }
    val muscles = pool.map { it.category }.distinct().sorted()
    val candidates = pool.filter { it.id !in inSession && (muscle == null || it.category == muscle) && it.name.contains(query.trim(), ignoreCase = true) }.sortedBy { it.name }
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = EmeraldDim, selectedLabelColor = EmeraldGlow, containerColor = SurfaceTier2, labelColor = TextSecondary
    )
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(horizontal = 16.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Add exercise", fontWeight = FontWeight.Black, fontSize = 18.sp)
            OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Search") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("exercise_search"))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = muscle == null, onClick = { muscle = null }, label = { Text("All") }, colors = chipColors)
                muscles.forEach { m -> FilterChip(selected = muscle == m, onClick = { muscle = m }, label = { Text(m.lowercase().replaceFirstChar { it.uppercase() }) }, colors = chipColors) }
            }
            LazyColumn(Modifier.heightIn(max = 300.dp)) {
                items(candidates, key = { it.id }) { ex ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onAdd(ex.id); onDismiss() }.defaultMinSize(minHeight = 48.dp).padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(ex.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(ex.category.lowercase(), color = Slate400, fontSize = 11.sp)
                    }
                }
                if (candidates.isEmpty()) item { Text("Nothing matches. Create it below.", color = Slate400, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp)) }
            }
            HorizontalDivider(color = OledBorder)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("New exercise${muscle?.let { " (${it.lowercase()})" } ?: ""}") }, singleLine = true, modifier = Modifier.weight(1f))
                Button(
                    onClick = { onCreate(newName.trim(), muscle ?: "CHEST"); onDismiss() }, enabled = newName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow), modifier = Modifier.height(48.dp)
                ) { Text("Create", fontWeight = FontWeight.Black, color = OledBlack) }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
fun WorkoutSummaryContent(s: WorkoutSummary, onDone: () -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier.testTag("workout_summary"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Workout complete", fontSize = 26.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
            Text(NextSplitRule.splitOf(s.dayType)?.title ?: s.dayType, color = Slate400, fontSize = 13.sp)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(formatMinutes(s.activeSeconds) to "time", s.sets.toString() to "sets", s.exercises.toString() to "exercises").forEach { (v, l) ->
                    SummaryCard(Modifier.weight(1f)) { Text(v, fontSize = 22.sp, fontWeight = FontWeight.Black); Text(l, color = Slate400, fontSize = 11.sp) }
                }
            }
        }
        s.cardio?.let { c ->
            item {
                SummaryCard {
                    SectionLabel("CARDIO")
                    Text("${c.type.lowercase().replaceFirstChar { it.uppercase() }} · ${c.minutes} min · ${c.effort.lowercase()}", fontWeight = FontWeight.Bold)
                    val extra = listOfNotNull(c.distanceKm?.let { "${formatKg(it)} km" }, c.inclinePct?.let { "${formatKg(it)}% incline" })
                    if (extra.isNotEmpty()) Text(extra.joinToString(" · "), color = Slate400, fontSize = 12.sp)
                }
            }
        }
        item {
            SummaryCard {
                SectionLabel("PERSONAL BESTS")
                if (s.personalBests.isEmpty()) Text("No new bests this time. Showing up counts.", color = TextSecondary, fontSize = 13.sp)
                s.personalBests.forEach { b ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(b.exerciseName, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text("${formatKg(b.weightKg)} kg × ${b.reps}", fontSize = 13.sp); Spacer(Modifier.width(8.dp)); PrBadge()
                    }
                }
            }
        }
        item {
            SummaryCard {
                SectionLabel("NEXT TIME")
                if (s.nextTargets.isEmpty()) Text("Log sets to get next-time targets.", color = TextSecondary, fontSize = 13.sp)
                s.nextTargets.forEach { t ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(t.exerciseName, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text("${formatKg(t.target.weightKg)} kg × ${t.target.reps}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
        item {
            SummaryCard {
                SectionLabel("GYM STREAK")
                Text("${s.streak.currentWeeks} wk streak · Best ${s.streak.bestWeeks} wk", fontWeight = FontWeight.Black, fontSize = 16.sp, color = AmberFlame)
                Text("This week ${s.streak.thisWeekCount} of ${s.streak.target}", color = Slate400, fontSize = 12.sp)
            }
        }
        item {
            Button(
                onClick = onDone, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow),
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("summary_done")
            ) { Text("Done", fontWeight = FontWeight.Black, color = OledBlack) }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SummaryCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, OledBorder)
    ) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp), content = content) }
}
