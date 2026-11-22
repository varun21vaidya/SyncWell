package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WorkoutSession
import com.example.data.WorkoutSessionExercise
import com.example.data.WorkoutSetLog
import com.example.domain.Split
import com.example.ui.theme.*
import com.example.viewmodel.SyncWellViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(viewModel: SyncWellViewModel) {
    val session by viewModel.currentActiveSession.collectAsState()
    val exercises by viewModel.activeSessionExercises.collectAsState()
    val logs by viewModel.workoutSetLogs.collectAsState()
    val location by viewModel.workoutLocationMode.collectAsState()
    val library by viewModel.exerciseLibrary.collectAsState()
    val libraryMap = remember(library) { library.associateBy { it.id } }
    val notes by viewModel.exerciseCustomNotes.collectAsState()
    val start by viewModel.workoutStart.collectAsState()
    val history by viewModel.workoutHistory.collectAsState()
    val clock by viewModel.sessionClock.collectAsState()
    val rest by viewModel.rest.collectAsState()
    val exerciseHistory by viewModel.exerciseHistory.collectAsState()
    val summary by viewModel.workoutSummary.collectAsState()
    val cardioDraft by viewModel.cardioDraft.collectAsState()

    var viewingSession by remember { mutableStateOf(true) }
    var showHistory by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var pendingSplit by remember { mutableStateOf<Split?>(null) } // a different split was tapped while one is in progress
    var removing by remember { mutableStateOf<WorkoutSessionExercise?>(null) }
    var notesFor by remember { mutableStateOf<WorkoutSessionExercise?>(null) }
    var showAdd by remember { mutableStateOf(false) }

    LaunchedEffect(session) { if (session != null) viewingSession = true }
    BackHandler(enabled = session != null && viewingSession && summary == null) { viewingSession = false }
    BackHandler(enabled = showHistory) { showHistory = false }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard this workout?", fontWeight = FontWeight.Black) },
            text = { Text("The sets you logged will be thrown away and it will not count as a workout.") },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; viewModel.discardWorkout() }) { Text("Discard", color = RoseCrimson, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep going", color = Slate400) } }
        )
    }
    pendingSplit?.let { split ->
        AlertDialog(
            onDismissRequest = { pendingSplit = null },
            title = { Text("Switch workout?", fontWeight = FontWeight.Black) },
            text = { Text("Your ${NextSplitRuleTitle(session?.dayType)} workout is still in progress. Starting ${split.title} discards it and it will not count.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingSplit = null
                    viewModel.discardWorkout { viewModel.startWorkoutSession(split.label, location) }
                }) { Text("Discard & Switch", color = RoseCrimson, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { pendingSplit = null }) { Text("Keep it", color = Slate400) } }
        )
    }
    removing?.let { se ->
        val name = libraryMap[se.exerciseId]?.name ?: "this exercise"
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text("Remove $name?", fontWeight = FontWeight.Black) },
            text = { Text("Its sets in this workout will be removed.") },
            confirmButton = { TextButton(onClick = { removing = null; viewModel.deleteExerciseFromActiveSession(se.id) }) { Text("Remove", color = RoseCrimson, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { removing = null }) { Text("Keep", color = Slate400) } }
        )
    }
    notesFor?.let { se ->
        val ex = libraryMap[se.exerciseId]
        ExerciseNotesDialog(ex?.name ?: se.exerciseId, se.exerciseId, notes[se.exerciseId], viewModel) { notesFor = null }
    }
    if (showAdd) {
        AddExerciseSheet(
            library = library, inSession = exercises.map { it.exerciseId }.toSet(), environment = session?.environment ?: location,
            onAdd = { viewModel.addExerciseToActiveSession(it) }, onCreate = { name, muscle -> viewModel.createNewCustomExerciseAndAdd(name, muscle) },
            onDismiss = { showAdd = false }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)
    ) {
        val activeSession = session
        when {
            summary != null -> WorkoutSummaryContent(summary!!, onDone = { viewModel.dismissWorkoutSummary() }, modifier = Modifier.weight(1f))
            showHistory -> WorkoutHistoryContent(history, start.today, onBack = { showHistory = false }, modifier = Modifier.weight(1f))
            activeSession != null && viewingSession -> ActiveSessionContent(
                session = activeSession, exercises = exercises, logs = logs, library = libraryMap, history = exerciseHistory,
                clock = clock, rest = rest,
                onBack = { viewingSession = false }, onPause = { viewModel.pauseWorkout() }, onResume = { viewModel.resumeWorkout() },
                onFinish = { viewModel.finishWorkout() }, onDiscard = { confirmDiscard = true },
                onSkipRest = { viewModel.skipRest() }, onAdjustRest = { viewModel.adjustRest(it) },
                onCompleteSet = { logId, seId, reps, kg -> viewModel.completeSet(logId, seId, reps, kg) },
                onUncompleteSet = { logId, seId -> viewModel.uncompleteSet(logId, seId) },
                onSetValues = { seId, logId, reps, kg -> viewModel.setSetValues(seId, logId, reps, kg) },
                onAddSet = { viewModel.addSetToSessionExercise(it) }, onDeleteSet = { seId, logId -> viewModel.deleteSetLogFromSessionExercise(seId, logId) },
                onRemoveExercise = { removing = it }, onNotes = { notesFor = it }, onAddExercise = { showAdd = true },
                cardio = cardioDraft, onCardioChange = { viewModel.setCardioDraft(it) },
                modifier = Modifier.weight(1f)
            )
            else -> {
                Column(Modifier.padding(bottom = 12.dp)) {
                    Text("Workout", color = Slate400, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("Lifting", color = MaterialTheme.colorScheme.onBackground, fontSize = 26.sp, fontWeight = FontWeight.Black)
                }
                WorkoutStartContent(
                    start = start, activeSession = activeSession, location = location,
                    onSetLocation = { viewModel.setWorkoutLocationMode(it) },
                    onStart = { split -> if (activeSession != null) pendingSplit = split else viewModel.startWorkoutSession(split.label, location) },
                    onResume = { viewingSession = true }, onFinish = { viewModel.finishWorkout() }, onDiscard = { confirmDiscard = true },
                    onSetTarget = { viewModel.setWeeklyGymTarget(it) }, onSeeAll = { showHistory = true },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun NextSplitRuleTitle(dayType: String?): String = dayType?.let { com.example.domain.NextSplitRule.splitOf(it)?.title } ?: "current"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseNotesDialog(
    exerciseName: String,
    exerciseId: String,
    customNote: String?,
    viewModel: SyncWellViewModel,
    onDismiss: () -> Unit
) {
    // Default spec notes per exercise (simplified mapping)
    val defaultNotes = mapOf(
        "ex_flat_bench" to "Retract scapulae before unracking. Bar path slightly arched. Touch sternum lightly. Full lockout at top.",
        "ex_overhead_press" to "Stand, core braced, bar at clavicle. Press straight up, slight lean back at top. Full lockout.",
        "ex_incline_press" to "30–45° incline, elbows at 75°, controlled descent, explosive press. Focus on upper chest.",
        "ex_lateral_raises" to "Slight forward lean, elbows slightly bent. Raise to ear level. Pause at top, slow lower.",
        "ex_lat_pulldown" to "Wide pronated grip. Lean back 10°, pull bar to upper chest. Squeeze lats at bottom.",
        "ex_deadlift" to "Hip hinge pattern. Bar over mid-foot. Neutral spine. Drive floor away. Lock hips at top.",
        "ex_weighted_squat" to "Bar on traps, stance shoulder-width. Break hips first. Knees track toes. Full depth.",
        "ex_leg_press" to "Feet mid-platform, shoulder-width. Descend until 90°. Do not lock knees at top.",
        "ex_bicep_curl" to "Elbows pinned to sides. Supinate at top. Lower in 3 seconds. No swinging.",
        "ex_pullups" to "Dead hang start. Pull until chin clears bar. Control the descent fully.",
    )
    val defaultNote = defaultNotes[exerciseId] ?: "Maintain proper form. Control eccentric phase. Full range of motion."

    var isEditing by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf(customNote ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Emerald500)
                Text(exerciseName, fontWeight = FontWeight.Black, fontSize = 15.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (customNote != null && !isEditing) {
                    Text("Custom note", color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(customNote, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    TextButton(onClick = {
                        viewModel.resetCustomNoteForExercise(exerciseId)
                        editText = ""
                    }) { Text("Reset to Default", color = Color.Red, fontSize = 11.sp) }
                } else if (!isEditing) {
                    Text("Form notes", color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(defaultNote, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 20.sp)
                }

                if (isEditing) {
                    Text("Edit note", color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = editText,
                        onValueChange = { editText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 100.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldGlow,
                            unfocusedBorderColor = OledBorder,
                            focusedContainerColor = SurfaceTier2,
                            unfocusedContainerColor = SurfaceTier2
                        )
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            if (editText.isNotBlank()) viewModel.saveCustomNoteForExercise(exerciseId, editText)
                            isEditing = false
                            onDismiss()
                        }, colors = ButtonDefaults.buttonColors(containerColor = Emerald600)) {
                            Text("Save", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = { isEditing = false }) { Text("Cancel") }
                    }
                }
            }
        },
        confirmButton = {
            if (!isEditing) {
                Button(
                    onClick = { isEditing = true; editText = customNote ?: defaultNote },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                ) { Text("Edit Note", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
