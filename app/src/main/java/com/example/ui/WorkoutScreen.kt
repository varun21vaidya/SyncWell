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
import com.example.ui.theme.*
import com.example.viewmodel.SyncWellViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(viewModel: SyncWellViewModel) {
    val currentActiveSession by viewModel.currentActiveSession.collectAsState()
    val activeSessionExercises by viewModel.activeSessionExercises.collectAsState()
    val workoutSetLogs by viewModel.workoutSetLogs.collectAsState()
    val workoutLocationMode by viewModel.workoutLocationMode.collectAsState()
    val exerciseLibrary by viewModel.exerciseLibrary.collectAsState(initial = emptyList())
    val exerciseMap = remember(exerciseLibrary) { exerciseLibrary.associateBy { it.id } }
    val exerciseCustomNotes by viewModel.exerciseCustomNotes.collectAsState()

    var isViewingActiveSession by remember { mutableStateOf(true) }
    // FIX 6: Holds (dayType, environment) of the new session the user wants to switch to
    var pendingSwitchDayType by remember { mutableStateOf<Pair<String, String>?>(null) }

    BackHandler(enabled = currentActiveSession != null && isViewingActiveSession) {
        isViewingActiveSession = false
    }

    LaunchedEffect(currentActiveSession) {
        if (currentActiveSession != null) {
            isViewingActiveSession = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Top Back Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "WEIGHT ROOM & PROGRESSION",
                    color = Slate400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (currentActiveSession != null && isViewingActiveSession) "Active Workout Session" else "Lifting Schedules",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )
            }

            // Back button if viewing active workout, so users can toggle back to lifting preset selections
            if (currentActiveSession != null && isViewingActiveSession) {
                OutlinedButton(
                    onClick = { isViewingActiveSession = false },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),  // FIX 8: was vertical=6
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate700),
                    modifier = Modifier.height(48.dp).testTag("workout_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Schedules",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Dashboard", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (currentActiveSession == null || !isViewingActiveSession) {
            // STATE A: Lifting presets selections + Active Session Resume Banner
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // If a workout session is active in the background, show a resume card
                if (currentActiveSession != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("active_workout_resume_banner"),
                        colors = CardDefaults.cardColors(containerColor = EmeraldDim),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, EmeraldGlow)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "WORKOUT SESSION IN PROGRESS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = EmeraldGlow
                                )
                                Text(
                                    text = currentActiveSession?.dayType ?: "",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }

                            // FIX 7: Finish button visible from resume banner
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { isViewingActiveSession = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Resume", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OledBlack)
                                }

                                OutlinedButton(
                                    onClick = { viewModel.finishWorkout() },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGlow),
                                    border = BorderStroke(1.dp, EmeraldGlow)
                                ) {
                                    Text("Finish", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                TextButton(
                                    onClick = { viewModel.stopWorkout() },
                                    colors = ButtonDefaults.textButtonColors(contentColor = RoseCrimson),
                                ) {
                                    Text("Discard", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Gym/Home location toggle
                Card(
                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, OledBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "WORKOUT LOCATION ENVIRONMENT",
                            color = Slate400,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.0.sp
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val gymActive = workoutLocationMode == "GYM"
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.setWorkoutLocationMode("GYM") },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (gymActive) EmeraldDim else SurfaceTier2
                                ),
                                border = if (gymActive) BorderStroke(1.5.dp, EmeraldGlow) else BorderStroke(1.dp, OledBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Gym Mode",
                                        tint = if (gymActive) EmeraldGlow else TextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Commercial Gym",
                                        color = if (gymActive) EmeraldGlow else TextSecondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            val homeActive = workoutLocationMode == "HOME"
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.setWorkoutLocationMode("HOME") },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (homeActive) EmeraldDim else SurfaceTier2
                                ),
                                border = if (homeActive) BorderStroke(1.5.dp, EmeraldGlow) else BorderStroke(1.dp, OledBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Home,
                                        contentDescription = "Home Mode",
                                        tint = if (homeActive) EmeraldGlow else TextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Home Calisthenics",
                                        color = if (homeActive) EmeraldGlow else TextSecondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Choose lift day preset structure
                Text(
                    text = "SELECT WORKOUT PATTERN PRESET",
                    color = Slate400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )

                val daysPresets = listOf(
                    "Push Day — Chest + Shoulders + Triceps" to "PUSH",
                    "Pull Day — Back + Biceps + Rear Delts" to "PULL",
                    "Legs + Abs Day — Thighs + Calves + Core" to "LEGS"
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(daysPresets) { (label, category) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("workout_preset_$category")
                                // FIX 6: Guard switch when a session is already active
                                .clickable {
                                    if (currentActiveSession != null) {
                                        pendingSwitchDayType = label to workoutLocationMode
                                    } else {
                                        viewModel.startWorkoutSession(label, workoutLocationMode)
                                    }
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Start Workout",
                                        tint = Emerald500
                                    )
                                    Column {
                                        Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            "Target Split: $category Groups",
                                            color = Slate400,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowRight,
                                    contentDescription = "Pick preset",
                                    tint = Slate400,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // STATE B: Active Workout Panel (with dynamic custom exercises + set additions + edits)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            currentActiveSession?.dayType ?: "Active Workout",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                        Text(
                            "Environment: " + (currentActiveSession?.environment ?: "GYM"),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // FIX 5: Finish Workout - calls finishWorkout() for dual-reference save
                    Button(
                        onClick = { viewModel.finishWorkout() },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("finish_workout_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Finish", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }

                // List Exercises with Set Logging Inputs
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(activeSessionExercises) { se ->
                        val setsLogs = workoutSetLogs[se.id] ?: emptyList()
                        val libraryItem = exerciseMap[se.exerciseId]
                        val nameStr = libraryItem?.name ?: se.exerciseId
                        val categoryStr = libraryItem?.category ?: "Strength"

                        ActiveExerciseCard(
                            sessionExercise = se,
                            exerciseName = nameStr,
                            category = categoryStr,
                            setLogs = setsLogs,
                            viewModel = viewModel,
                            exerciseId = se.exerciseId,
                            customNote = exerciseCustomNotes[se.exerciseId]
                        )
                    }

                    // FIX 5: Bottom "Finish Workout" CTA — accessible even mid-scroll on long sessions
                    item {
                        Button(
                            onClick = { viewModel.finishWorkout() },
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(top = 8.dp, bottom = 8.dp)
                                .testTag("finish_workout_bottom_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("FINISH WORKOUT & SAVE", fontSize = 13.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 16.dp),
                            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(16.dp),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "ADD CUSTOM EXERCISE FOR TODAY'S SESSION",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate400,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                val alreadyInSessionIds = activeSessionExercises.map { it.exerciseId }.toSet()
                                val candidates = exerciseLibrary.filter { it.id !in alreadyInSessionIds }

                                if (candidates.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        candidates.forEach { candidate ->
                                            Button(
                                                onClick = { viewModel.addExerciseToActiveSession(candidate.id) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Emerald100,
                                                    contentColor = Emerald700
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(candidate.name, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                }

                                Text(
                                    "OR CREATE COMPLETELY NEW CUSTOM EXERCISE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate400,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                var customName by remember { mutableStateOf("") }
                                var customCategory by remember { mutableStateOf("CHEST") }
                                var showCategoryMenu by remember { mutableStateOf(false) }

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = customName,
                                            onValueChange = { customName = it },
                                            placeholder = { Text("e.g. Incline DB Flyes", fontSize = 12.sp, maxLines = 1) },
                                            modifier = Modifier
                                                .weight(1.5f)
                                                .testTag("custom_exercise_input_name"),
                                            singleLine = true,
                                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = EmeraldGlow,
                                                unfocusedBorderColor = OledBorder,
                                                focusedContainerColor = SurfaceTier2,
                                                unfocusedContainerColor = SurfaceTier2
                                            )
                                        )

                                        Box(modifier = Modifier.weight(1f)) {
                                            OutlinedButton(
                                                onClick = { showCategoryMenu = true },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(54.dp)
                                                    .testTag("custom_exercise_cat_button"),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 6.dp)
                                            ) {
                                                Text(customCategory, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                            }
                                            DropdownMenu(
                                                expanded = showCategoryMenu,
                                                onDismissRequest = { showCategoryMenu = false }
                                            ) {
                                                listOf("CHEST", "SHOULDERS", "TRICEPS", "BACK", "BICEPS", "FOREARMS", "LEGS", "ABS", "CALVES").forEach { cat ->
                                                    DropdownMenuItem(
                                                        text = { Text(cat, fontSize = 11.sp) },
                                                        onClick = {
                                                            customCategory = cat
                                                            showCategoryMenu = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            if (customName.isNotBlank()) {
                                                viewModel.createNewCustomExerciseAndAdd(customName.trim(), customCategory)
                                                customName = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(40.dp)
                                            .testTag("custom_exercise_submit_button")
                                    ) {
                                        Text("Create & Insert to Routine", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // FIX 6: Day-type switch confirmation dialog
    pendingSwitchDayType?.let { (newDayType, env) ->
        AlertDialog(
            onDismissRequest = { pendingSwitchDayType = null },
            title = {
                Text("Workout In Progress", fontWeight = FontWeight.Black, fontSize = 16.sp)
            },
            text = {
                Text(
                    "You have an active ${currentActiveSession?.dayType} session.\nWhat would you like to do?",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.finishWorkout()
                            viewModel.startWorkoutSession(newDayType, env)
                            pendingSwitchDayType = null
                            isViewingActiveSession = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Finish & Switch", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                    OutlinedButton(
                        onClick = {
                            viewModel.stopWorkout()
                            viewModel.startWorkoutSession(newDayType, env)
                            pendingSwitchDayType = null
                            isViewingActiveSession = true
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Discard & Switch", fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = { pendingSwitchDayType = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel", color = Slate400)
                    }
                }
            },
            dismissButton = {}
        )
    }
}

@Composable
fun ActiveExerciseCard(
    sessionExercise: WorkoutSessionExercise,
    exerciseName: String,
    category: String,
    setLogs: List<WorkoutSetLog>,
    viewModel: SyncWellViewModel,
    exerciseId: String = sessionExercise.exerciseId,     // FEATURE 10
    customNote: String? = null                           // FEATURE 10
) {
    val haptic = LocalHapticFeedback.current
    var repsInput by remember(sessionExercise) { mutableStateOf(sessionExercise.targetRepsMax.toString()) }
    var weightInput by remember(sessionExercise) { mutableStateOf(String.format(Locale.getDefault(), "%.1f", sessionExercise.targetWeightKg ?: 0.0)) }
    var selectedSetId by remember { mutableStateOf<String?>(null) }

    // FEATURE 10: Notes popup state
    var showNotesDialog by remember { mutableStateOf(false) }
    LaunchedEffect(exerciseId) { viewModel.loadCustomNoteForExercise(exerciseId) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_ex_card_${sessionExercise.orderIndex}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Exercise Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Exercise #${sessionExercise.orderIndex}: $exerciseName",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                
                // BUG 2 FIX: All icon buttons now meet 48dp minimum tap target
                Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                    // Add Set icon-button
                    IconButton(
                        onClick = { viewModel.addSetToSessionExercise(sessionExercise.id) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Set", tint = Emerald600, modifier = Modifier.size(20.dp))
                    }

                    // Remove last Set icon-button
                    IconButton(
                        onClick = { viewModel.deleteLastSetFromSessionExercise(sessionExercise.id) },
                        modifier = Modifier.size(48.dp),
                        enabled = setLogs.size > 1
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Set", tint = Slate400, modifier = Modifier.size(20.dp))
                    }

                    // Delete Exercise button
                    IconButton(
                        onClick = { viewModel.deleteExerciseFromActiveSession(sessionExercise.id) },
                        modifier = Modifier.size(48.dp).testTag("delete_exercise_${sessionExercise.orderIndex}")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Delete Exercise", tint = Color.Red, modifier = Modifier.size(20.dp))
                    }

                    // FEATURE 10: Info/Notes icon
                    IconButton(
                        onClick = { showNotesDialog = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "Exercise Notes", tint = Slate400, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Responsive targets adjustments: horizontally scrollable if screen width is narrow
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "TARGETS:",
                    color = Slate400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
                
                // Weight Adjust
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(SurfaceTier2, RoundedCornerShape(8.dp))) {
                    IconButton(onClick = { viewModel.updateExerciseTargets(sessionExercise.id, sessionExercise.targetRepsMax, maxOf(0.0, (sessionExercise.targetWeightKg ?: 0.0) - 2.5)) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextSecondary)
                    }
                    Text("${sessionExercise.targetWeightKg ?: 0.0}kg", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    IconButton(onClick = { viewModel.updateExerciseTargets(sessionExercise.id, sessionExercise.targetRepsMax, (sessionExercise.targetWeightKg ?: 0.0) + 2.5) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextSecondary)
                    }
                }

                // Reps Adjust
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(SurfaceTier2, RoundedCornerShape(8.dp))) {
                    IconButton(onClick = { viewModel.updateExerciseTargets(sessionExercise.id, sessionExercise.targetRepsMax - 1, sessionExercise.targetWeightKg ?: 0.0) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextSecondary)
                    }
                    Text("${sessionExercise.targetRepsMax} reps", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    IconButton(onClick = { viewModel.updateExerciseTargets(sessionExercise.id, sessionExercise.targetRepsMax + 1, sessionExercise.targetWeightKg ?: 0.0) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextSecondary)
                    }
                }
            }

            Text(
                text = "Category: $category | Session Target: ${sessionExercise.targetSets} Sets x ${sessionExercise.targetRepsMax} Reps @ ${sessionExercise.targetWeightKg ?: 0.0} kg",
                color = Slate400,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            // Dynamic Form for logging/editing clicked sets
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = repsInput,
                    onValueChange = { repsInput = it },
                    label = { Text("Reps", fontSize = 10.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1.1f),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGlow,
                        unfocusedBorderColor = OledBorder,
                        focusedContainerColor = SurfaceTier2,
                        unfocusedContainerColor = SurfaceTier2
                    )
                )

                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    label = { Text("Wt (kg)", fontSize = 10.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1.3f),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGlow,
                        unfocusedBorderColor = OledBorder,
                        focusedContainerColor = SurfaceTier2,
                        unfocusedContainerColor = SurfaceTier2
                    )
                )

                // BUG 2 FIX: Haptic feedback on log/update set
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val reps = repsInput.toIntOrNull() ?: 12
                        val wt = weightInput.toDoubleOrNull() ?: 0.0
                        
                        val curSelectedId = selectedSetId
                        if (curSelectedId != null) {
                            // Update the specific highlighted set
                            viewModel.updateWorkoutSetLogDirectly(curSelectedId, sessionExercise.id, reps, wt, isCompleted = true)
                            selectedSetId = null
                        } else {
                            // Log the first incomplete set
                            val firstIncomplete = setLogs.find { !it.isCompleted }
                            if (firstIncomplete != null) {
                                viewModel.updateWorkoutSetLogDirectly(firstIncomplete.id, sessionExercise.id, reps, wt, isCompleted = true)
                            } else {
                                // If all are complete, add a new set on the fly and log it completed
                                viewModel.addSetToSessionExercise(sessionExercise.id)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow),
                    modifier = Modifier
                        .weight(1.6f)
                        .height(54.dp)
                        .testTag("log_set_submit"),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text(
                        text = if (selectedSetId != null) "Update Set" else "Log Set",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = OledBlack,
                        maxLines = 1
                    )
                }

                if (selectedSetId != null) {
                    TextButton(
                        onClick = { selectedSetId = null },
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text("X", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // List Sets Logs Checkboxes/Rows
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                setLogs.forEach { log ->
                    val isEditingThisSet = selectedSetId == log.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    isEditingThisSet -> Emerald500.copy(alpha = 0.25f)
                                    log.isCompleted -> Emerald500.copy(alpha = 0.08f)
                                    else -> SurfaceTier2
                                }
                            )
                            .clickable {
                                selectedSetId = log.id
                                repsInput = log.reps.toString()
                                weightInput = String.format(Locale.getDefault(), "%.1f", log.weightKg)
                            }
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Set #${log.setIndex}",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            if (isEditingThisSet) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("(Editing)", color = EmeraldGlow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(
                            text = if (log.isCompleted) "${log.weightKg} kg x ${log.reps} reps (Done)" else "Pending Target: ${sessionExercise.targetWeightKg ?: 0.0} kg",
                            color = if (log.isCompleted) EmeraldGlow else TextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // BUG 2 FIX: 48dp minimum tap target for delete set
                            IconButton(
                                onClick = { viewModel.deleteSetLogFromSessionExercise(sessionExercise.id, log.id) },
                                modifier = Modifier.size(40.dp).testTag("delete_set_${log.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Set",
                                    tint = RoseCrimson,
                                    modifier = Modifier.size(18.dp)
                               )
                            }
                            Icon(
                                imageVector = if (log.isCompleted) Icons.Default.CheckCircle else Icons.Default.Check,
                                contentDescription = "Status",
                                tint = if (log.isCompleted) EmeraldGlow else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // FEATURE 10: Show notes dialog
    if (showNotesDialog) {
        ExerciseNotesDialog(
            exerciseName = exerciseName,
            exerciseId = exerciseId,
            customNote = customNote,
            viewModel = viewModel,
            onDismiss = { showNotesDialog = false }
        )
    }
}

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
                    Text("CUSTOM NOTE", color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(customNote, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    TextButton(onClick = {
                        viewModel.resetCustomNoteForExercise(exerciseId)
                        editText = ""
                    }) { Text("Reset to Default", color = Color.Red, fontSize = 11.sp) }
                } else if (!isEditing) {
                    Text("FORM NOTES", color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(defaultNote, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 20.sp)
                }

                if (isEditing) {
                    Text("EDIT NOTE", color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
