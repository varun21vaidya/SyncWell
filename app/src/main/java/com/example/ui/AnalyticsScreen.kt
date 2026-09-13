package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DailyAnalytics
import com.example.data.WeightLog
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import com.example.ui.theme.*
import com.example.viewmodel.SyncWellViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

data class ChartDataItem(
    val label: String,
    val studyMinutes: Int,
    val workMinutes: Int = 0,
    val timepassMinutes: Int = 0,
    val workoutMinutes: Int = 0,
    val steps: Int = 0,
    val routinePct: Double = 0.0,
    val waterMl: Int,
    val weightKg: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(viewModel: SyncWellViewModel) {
    val weeklyAnalytics by viewModel.weeklyAnalytics.collectAsState(initial = emptyList())
    val monthlyAnalytics by viewModel.monthlyAnalytics.collectAsState(initial = emptyList())
    val weightLogs by viewModel.weightLogs.collectAsState(initial = emptyList())
    val analyticsLastRefreshed by viewModel.analyticsLastRefreshed.collectAsState()
    val activeRoutineItems by viewModel.activeRoutineItems.collectAsState()
    val targetWeightKg by viewModel.targetWeightKg.collectAsState()
    val todayAnalytics by viewModel.todayAnalytics.collectAsState()
    
    val isGymDoneToday = remember(activeRoutineItems) {
        activeRoutineItems.any { (it.title.contains("Gym", ignoreCase = true) || it.title.contains("Workout", ignoreCase = true)) && it.isDone }
    }

    val proteinItems = remember(activeRoutineItems) {
        activeRoutineItems.filter {
            it.title.contains("Breakfast", ignoreCase = true) ||
            it.title.contains("Lunch", ignoreCase = true) ||
            it.title.contains("Dinner", ignoreCase = true) ||
            it.title.contains("Protein", ignoreCase = true)
        }
    }
    val proteinPointsToday = proteinItems.count { it.isDone }
    val isDietDoneToday = proteinPointsToday >= 4

    var selectedPeriod by remember { mutableStateOf("DAILY") }
    var weightInput by remember(targetWeightKg) {
        mutableStateOf(String.format(Locale.US, "%.1f", targetWeightKg))
    }
    val scope = rememberCoroutineScope()

    // Always refresh on screen mount
    LaunchedEffect(Unit) { viewModel.refreshAnalytics() }

    // Resolve analytics list: merge database values + weight logs with target weight fallback
    val resolvedList = remember(monthlyAnalytics, weightLogs, targetWeightKg) {
        getResolvedAnalytics(monthlyAnalytics, weightLogs, targetWeightKg)
    }

    val aggregatedData = remember(resolvedList, selectedPeriod, targetWeightKg) {
        aggregateAnalytics(resolvedList, selectedPeriod, targetWeightKg)
    }

    // Latest weight from logs or today's analytics
    val latestWeight = weightLogs.firstOrNull()?.weightKg ?: todayAnalytics?.weightKg

    // Summary calculations for KPI matrix
    val periodItems = remember(resolvedList, selectedPeriod) {
        when (selectedPeriod) {
            "DAILY" -> resolvedList.takeLast(7)
            "WEEKLY" -> resolvedList.takeLast(28)
            else -> resolvedList
        }
    }

    val avgConsistency = remember(periodItems) {
        if (periodItems.isNotEmpty()) periodItems.map { it.routineCompletionPct }.average() else 0.0
    }

    val totalStudyMins = remember(periodItems) { periodItems.sumOf { it.totalStudyMinutes } }
    val totalWorkMins = remember(periodItems) { periodItems.sumOf { it.workMin } }
    val totalTimepassMins = remember(periodItems) { periodItems.sumOf { it.timepassMin } }
    val totalFocusMins = totalStudyMins + totalWorkMins
    val totalTrackedTime = totalFocusMins + totalTimepassMins
    val productivityRate = if (totalTrackedTime > 0) (totalFocusMins * 100) / totalTrackedTime else 100

    val totalSteps = remember(periodItems) { periodItems.sumOf { it.totalSteps } }
    val avgStepsPerDay = if (periodItems.isNotEmpty()) totalSteps / periodItems.size else 0
    val totalWorkoutMins = remember(periodItems) { periodItems.sumOf { it.totalWorkoutMinutes } }
    val workoutsCompleted = remember(periodItems) { periodItems.count { it.isGymCompleted || it.totalWorkoutMinutes > 0 } }

    val streakCount = todayAnalytics?.streakValue ?: 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "CONSOLIDATED PERFORMANCE METRICS",
                    color = Slate400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Executive Statistics",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black
                )
                analyticsLastRefreshed?.let { ts ->
                    val time = SimpleDateFormat("HH:mm", Locale.US).format(Date(ts))
                    Text("Last updated: $time", color = Slate400, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }
            IconButton(
                onClick = { viewModel.refreshAnalytics() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh Analytics", tint = EmeraldGlow)
            }
        }

        // =========================================================================
        // EXECUTIVE 4-CARD KPI OVERVIEW MATRIX
        // =========================================================================
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Row 1: Habit Consistency & Focus Productivity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // KPI 1: Habit Consistency
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = SurfaceTier1),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, OledBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("CONSISTENCY", color = Slate400, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGlow, modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = "${(avgConsistency * 100).toInt()}%",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                        Surface(
                            color = EmeraldDim,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "🔥 $streakCount day streak",
                                color = EmeraldGlow,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // KPI 2: Focus & Productivity
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = SurfaceTier1),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, OledBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("FOCUS EFFORT", color = Slate400, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(16.dp))
                        }
                        val focusHours = totalFocusMins / 60
                        val focusRemMins = totalFocusMins % 60
                        Text(
                            text = if (focusHours > 0) "${focusHours}h ${focusRemMins}m" else "${focusRemMins}m",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                        Surface(
                            color = VioletDim,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "$productivityRate% productive",
                                color = PurpleAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Row 2: Physical Activity & Target Weight Delta
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // KPI 3: Physical Activity (Steps + Workouts)
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = SurfaceTier1),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, OledBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ACTIVITY & STEPS", color = Slate400, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = if (totalSteps >= 1000) String.format(Locale.US, "%,d", totalSteps) else "$totalSteps",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                        Surface(
                            color = AmberDim,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "$workoutsCompleted workouts (${totalWorkoutMins}m)",
                                color = GoldAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // KPI 4: Body Weight vs Target Delta
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = SurfaceTier1),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, OledBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("BODY WEIGHT", color = Slate400, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Icon(Icons.Default.Star, contentDescription = null, tint = CyanLaser, modifier = Modifier.size(16.dp))
                        }
                        val currentWtStr = latestWeight?.let { String.format(Locale.US, "%.1f kg", it) } ?: "Unlogged"
                        Text(
                            text = currentWtStr,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )

                        val weightDelta = latestWeight?.let { it - targetWeightKg }
                        val (deltaBg, deltaFg, deltaText) = when {
                            weightDelta == null -> Triple(SurfaceTier2, Slate400, "Goal: ${String.format(Locale.US, "%.1f", targetWeightKg)} kg")
                            Math.abs(weightDelta) < 0.1 -> Triple(EmeraldDim, EmeraldGlow, "🎯 On Target (${String.format(Locale.US, "%.1f", targetWeightKg)} kg)")
                            weightDelta > 0 -> Triple(AmberDim, GoldAccent, "+${String.format(Locale.US, "%.1f", weightDelta)} kg to goal")
                            else -> Triple(CyanDim, CyanLaser, "${String.format(Locale.US, "%.1f", weightDelta)} kg to goal")
                        }

                        Surface(
                            color = deltaBg,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = deltaText,
                                color = deltaFg,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // GYM & DIET DAILY STREAK STATUS
        // =========================================================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = if (isGymDoneToday) EmeraldDim else SurfaceTier1),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, OledBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Gym Streak",
                        tint = if (isGymDoneToday) EmeraldGlow else TextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "GYM STREAK",
                        color = Slate400,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (isGymDoneToday) "🔥 Active" else "Pending",
                        color = if (isGymDoneToday) EmeraldGlow else MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = if (isDietDoneToday) EmeraldDim else SurfaceTier1),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, OledBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Diet Streak",
                        tint = if (isDietDoneToday) EmeraldGlow else TextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "DIET STREAK",
                        color = Slate400,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (isDietDoneToday) "🔥 Active" else "$proteinPointsToday/4 Pts",
                        color = if (isDietDoneToday) EmeraldGlow else MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // =========================================================================
        // PERIOD AGGREGATION SELECTOR
        // =========================================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceTier1)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val periods = listOf("DAILY", "WEEKLY", "MONTHLY")
            periods.forEach { period ->
                val isSelected = selectedPeriod == period
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) EmeraldGlow else Color.Transparent)
                        .clickable { selectedPeriod = period }
                        .defaultMinSize(minHeight = 44.dp)
                        .padding(vertical = 10.dp)
                        .testTag("period_tab_$period"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = period,
                        color = if (isSelected) OledBlack else TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // =========================================================================
        // SECTION A: SCALE BODYWEIGHT LOGGING (DAILY)
        // =========================================================================
        if (selectedPeriod == "DAILY") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceTier1),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, OledBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Scales",
                            tint = EmeraldGlow,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "REGISTER BODYWEIGHT MEASUREMENT",
                            color = Slate400,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    val targetFormatted = String.format(Locale.US, "%.1f", targetWeightKg)
                    Text(
                        text = "Your target weight is set to $targetFormatted kg in Settings. Register scale measurements regularly to track progress vectors:",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    var isWeightLogging by remember { mutableStateOf(false) }
                    var weightLogSuccess by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        placeholder = { Text(targetFormatted, fontSize = 14.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("analytics_weight_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldGlow,
                            unfocusedBorderColor = OledBorder,
                            focusedContainerColor = SurfaceTier2,
                            unfocusedContainerColor = SurfaceTier2
                        ),
                        label = { Text("Body Weight (kg)", fontSize = 11.sp) },
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (!isWeightLogging && !weightLogSuccess) {
                                val wt = weightInput.toDoubleOrNull() ?: targetWeightKg
                                isWeightLogging = true
                                scope.launch {
                                    viewModel.logWeight(wt)
                                    delay(800)
                                    isWeightLogging = false
                                    weightLogSuccess = true
                                    delay(1500)
                                    weightLogSuccess = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (weightLogSuccess) Emerald700 else Emerald600
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("submit_weight_log_btn")
                    ) {
                        if (isWeightLogging) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else if (weightLogSuccess) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Success",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    "Weight Logged Successfully!",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = "Log", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                                Text("Submit Log ($targetFormatted kg Target)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SECTION B: MULTI-CATEGORY FOCUS TIME ALLOCATION (STUDY + WORK + TIMEPASS)
        // =========================================================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceTier1),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, OledBorder)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "FOCUS TIME & ALLOCATION",
                            color = Slate400,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (selectedPeriod == "DAILY") "Recent Daily Breakdown" else "$selectedPeriod Trends",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    // Legend
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(PurpleAccent))
                            Text("Study", color = Slate400, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(CyanLaser))
                            Text("Work", color = Slate400, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(RoseCrimson))
                            Text("Timepass", color = Slate400, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (selectedPeriod == "WEEKLY" || selectedPeriod == "MONTHLY") {
                    val studyChartData = remember(resolvedList, selectedPeriod) {
                        getChartData(resolvedList, selectedPeriod) { it.totalStudyMinutes }
                    }
                    VerticalColumnChart(title = "STUDY FOCUS (HOURS)", data = studyChartData, color = PurpleAccent, isHours = true)
                } else {
                    // Multi-Segment Focus Bars for Daily
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        aggregatedData.forEach { chartItem ->
                            MultiCategoryFocusRow(
                                label = chartItem.label,
                                studyMins = chartItem.studyMinutes,
                                workMins = chartItem.workMinutes,
                                timepassMins = chartItem.timepassMinutes,
                                maxMins = 480f // 8-hour scale
                            )
                        }
                    }
                }

                Text(
                    text = "Target Focus: ~480 mins (8 hours) productive daily block",
                    color = PurpleAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // =========================================================================
        // SECTION C: PHYSICAL ACTIVITY & DAILY STEP TREND
        // =========================================================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceTier1),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, OledBorder)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PHYSICAL ACTIVITY & STEPS",
                            color = Slate400,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Step Count Progression",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Surface(
                        color = AmberDim,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Avg: $avgStepsPerDay/day",
                            color = GoldAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    aggregatedData.forEach { chartItem ->
                        StepActivityBarRow(
                            label = chartItem.label,
                            steps = chartItem.steps,
                            workoutMins = chartItem.workoutMinutes,
                            maxSteps = 12000f
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Benchmark: 8,000 steps baseline",
                        color = GoldAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Stretch: 10,000 steps",
                        color = EmeraldGlow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // =========================================================================
        // SECTION D: HABIT & ROUTINE ADHERENCE TREND
        // =========================================================================
        if (selectedPeriod == "WEEKLY" || selectedPeriod == "MONTHLY") {
            val gymChartData = remember(resolvedList, selectedPeriod) {
                getChartData(resolvedList, selectedPeriod) { if (it.isGymCompleted) 100 else 0 }
            }
            val dietChartData = remember(resolvedList, selectedPeriod) {
                getChartData(resolvedList, selectedPeriod) { (it.dietProteinPoints * 25).coerceAtMost(100) }
            }

            VerticalColumnChart(title = "GYM ADHERENCE (%)", data = gymChartData, color = EmeraldGlow)
            VerticalColumnChart(title = "DIET PERFORMANCE (%)", data = dietChartData, color = GoldAccent)
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceTier1),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, OledBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "ROUTINE ADHERENCE RATE",
                        color = Slate400,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        aggregatedData.forEach { chartItem ->
                            RoutineAdherenceRow(
                                label = chartItem.label,
                                pct = chartItem.routinePct
                            )
                        }
                    }

                    Text(
                        text = "Consistency Target: ≥ 80% daily routine completion",
                        color = EmeraldGlow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // =========================================================================
        // SECTION E: WEIGHT HISTORY & PROGRESSION (WEEKLY / MONTHLY)
        // =========================================================================
        if (selectedPeriod == "WEEKLY" || selectedPeriod == "MONTHLY") {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("weight_history_card"),
                colors = CardDefaults.cardColors(containerColor = SurfaceTier1),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, OledBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "WEIGHT HISTORY ($selectedPeriod TRENDS)",
                                color = Slate400,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Body Composition Timeline",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Surface(
                            color = CyanDim,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Target: ${String.format(Locale.US, "%.1f", targetWeightKg)} kg",
                                color = CyanLaser,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    val weightData = remember(resolvedList, selectedPeriod, targetWeightKg) {
                        if (selectedPeriod == "WEEKLY") {
                            resolvedList.chunked(7).mapIndexed { index, chunk ->
                                val avg = chunk.mapNotNull { it.weightKg }.average()
                                "Week ${index + 1}" to if (avg.isNaN()) targetWeightKg else avg
                            }.takeLast(4)
                        } else {
                            resolvedList.chunked(7).mapIndexed { index, chunk ->
                                val avg = chunk.mapNotNull { it.weightKg }.average()
                                "Week ${index + 1}" to if (avg.isNaN()) targetWeightKg else avg
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        weightData.forEach { (label, weight) ->
                            val delta = weight - targetWeightKg
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = label,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "%.1f kg", weight),
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    val deltaStr = if (Math.abs(delta) < 0.1) "On target" else if (delta > 0) "+${String.format(Locale.US, "%.1f", delta)}" else String.format(Locale.US, "%.1f", delta)
                                    Text(
                                        text = "($deltaStr)",
                                        color = if (Math.abs(delta) < 0.1) EmeraldGlow else if (delta > 0) GoldAccent else CyanLaser,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SECTION F: HYDRATION METERS
        // =========================================================================
        val maxWater = when (selectedPeriod) {
            "DAILY" -> 4000.0f
            "WEEKLY" -> 28000.0f
            "MONTHLY" -> 120000.0f
            else -> 4000.0f
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceTier1),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, OledBorder)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "HYDRATION METERS ($selectedPeriod TRENDS)",
                    color = Slate400,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    aggregatedData.forEach { chartItem ->
                        ActualWaterMeter(
                            label = chartItem.label,
                            waterMl = chartItem.waterMl,
                            maxValue = maxWater
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// REUSABLE HIGH-CONTRAST CHART COMPONENTS
// =============================================================================

@Composable
fun VerticalColumnChart(
    title: String,
    data: List<Pair<String, Int>>,
    color: Color,
    isHours: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceTier1),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, OledBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = title,
                color = Slate400,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                val maxVal = Math.max(1, data.maxOfOrNull { it.second } ?: 1).toFloat()
                data.forEach { (label, value) ->
                    val fraction = (value.toFloat() / maxVal).coerceIn(0.05f, 1.0f)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val formattedValue = if (isHours) "${value / 60}h" else "$value"
                        Text(
                            text = formattedValue,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = color,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .fillMaxHeight(fraction * 0.7f)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(color)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MultiCategoryFocusRow(
    label: String,
    studyMins: Int,
    workMins: Int,
    timepassMins: Int,
    maxMins: Float
) {
    val totalProductive = studyMins + workMins
    val studyFraction = (studyMins / maxMins).coerceIn(0f, 1f)
    val workFraction = (workMins / maxMins).coerceIn(0f, 1f)
    val timepassFraction = (timepassMins / maxMins).coerceIn(0f, 1f)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(48.dp)
        )

        // Stacked Progress Bar
        Row(
            modifier = Modifier
                .weight(1f)
                .height(14.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(SurfaceTier2)
        ) {
            if (studyFraction > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(studyFraction)
                        .background(PurpleAccent)
                )
            }
            if (workFraction > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(workFraction)
                        .background(CyanLaser)
                )
            }
            if (timepassFraction > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(timepassFraction)
                        .background(RoseCrimson)
                )
            }
            val remaining = (1f - studyFraction - workFraction - timepassFraction).coerceAtLeast(0.01f)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(remaining)
                    .background(Color.Transparent)
            )
        }

        val totalText = if (totalProductive >= 60) "${totalProductive / 60}h ${totalProductive % 60}m" else "${totalProductive}m"
        Text(
            text = totalText,
            color = PurpleAccent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.width(62.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun StepActivityBarRow(
    label: String,
    steps: Int,
    workoutMins: Int,
    maxSteps: Float
) {
    val fraction = (steps.toFloat() / maxSteps).coerceIn(0.02f, 1.0f)
    val isTargetMet = steps >= 8000

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(48.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(SurfaceTier2)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isTargetMet) EmeraldGlow else GoldAccent)
            )
        }

        Text(
            text = if (steps >= 1000) String.format(Locale.US, "%,d", steps) else "$steps",
            color = if (isTargetMet) EmeraldGlow else GoldAccent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.width(55.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun RoutineAdherenceRow(
    label: String,
    pct: Double
) {
    val fraction = pct.toFloat().coerceIn(0.02f, 1.0f)
    val isTargetMet = pct >= 0.8

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(48.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(SurfaceTier2)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (isTargetMet) EmeraldGlow else Slate400)
            )
        }

        Text(
            text = "${(pct * 100).toInt()}%",
            color = if (isTargetMet) EmeraldGlow else Slate400,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.width(45.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun ActualWaterMeter(label: String, waterMl: Int, maxValue: Float) {
    val fraction = Math.max(0.02f, Math.min(1.0f, waterMl.toFloat() / maxValue))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(48.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(SurfaceTier2)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(RoundedCornerShape(5.dp))
                    .background(CyanLaser)
            )
        }

        Text(
            text = String.format(Locale.US, "%.1fL", waterMl / 1000f),
            color = CyanLaser,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.width(50.dp),
            textAlign = TextAlign.End
        )
    }
}

fun getResolvedAnalytics(
    dbAnalytics: List<DailyAnalytics>,
    dbWeightLogs: List<WeightLog>,
    targetWeightKg: Double = 70.0
): List<DailyAnalytics> {
    val weightMap = dbWeightLogs.associateBy { it.dateKey }

    return dbAnalytics.map { dbItem ->
        val loggedWeight = weightMap[dbItem.dateKey]?.weightKg
        dbItem.copy(
            weightKg = loggedWeight ?: dbItem.weightKg ?: targetWeightKg
        )
    }.sortedBy { it.dateKey }
}

fun getChartData(list: List<DailyAnalytics>, period: String, selector: (DailyAnalytics) -> Int): List<Pair<String, Int>> {
    return if (period == "WEEKLY") {
        val last7 = list.takeLast(7)
        val daysOfWeekOrder = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = java.util.Calendar.getInstance()

        val dayMap = last7.associate { item ->
            val dayName = try {
                val date = sdf.parse(item.dateKey) ?: Date()
                cal.time = date
                val dayNum = cal.get(java.util.Calendar.DAY_OF_WEEK)
                val labels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                labels[dayNum - 1]
            } catch (_: Exception) {
                "Mon"
            }
            dayName to selector(item)
        }

        daysOfWeekOrder.map { dayName ->
            dayName to (dayMap[dayName] ?: 0)
        }
    } else {
        if (list.size >= 28) {
            val w1 = list.subList(0, 7).sumOf { selector(it) }
            val w2 = list.subList(7, 14).sumOf { selector(it) }
            val w3 = list.subList(14, 21).sumOf { selector(it) }
            val w4 = list.subList(21, list.size).sumOf { selector(it) }
            listOf("Week 1" to w1, "Week 2" to w2, "Week 3" to w3, "Week 4" to w4)
        } else {
            listOf("Week 1" to 0, "Week 2" to 0, "Week 3" to 0, "Week 4" to 0)
        }
    }
}

fun aggregateAnalytics(
    dataList: List<DailyAnalytics>,
    period: String,
    targetWeightKg: Double = 70.0
): List<ChartDataItem> {
    if (dataList.isEmpty()) return emptyList()
    
    return when (period) {
        "DAILY" -> {
            dataList.takeLast(7).map {
                val dateParts = it.dateKey.split("-")
                val shortLabel = if (dateParts.size >= 3) "${dateParts[1]}/${dateParts[2]}" else it.dateKey
                ChartDataItem(
                    label = shortLabel,
                    studyMinutes = it.totalStudyMinutes,
                    workMinutes = it.workMin,
                    timepassMinutes = it.timepassMin,
                    workoutMinutes = it.totalWorkoutMinutes,
                    steps = it.totalSteps,
                    routinePct = it.routineCompletionPct,
                    waterMl = it.totalWaterMl,
                    weightKg = it.weightKg ?: targetWeightKg
                )
            }
        }
        "WEEKLY" -> {
            dataList.chunked(7).mapIndexed { index, chunk ->
                val avgWeight = chunk.mapNotNull { it.weightKg }.average()
                val avgRoutine = chunk.map { it.routineCompletionPct }.average()
                ChartDataItem(
                    label = "Week ${index + 1}",
                    studyMinutes = chunk.sumOf { it.totalStudyMinutes },
                    workMinutes = chunk.sumOf { it.workMin },
                    timepassMinutes = chunk.sumOf { it.timepassMin },
                    workoutMinutes = chunk.sumOf { it.totalWorkoutMinutes },
                    steps = chunk.sumOf { it.totalSteps },
                    routinePct = if (avgRoutine.isNaN()) 0.0 else avgRoutine,
                    waterMl = chunk.sumOf { it.totalWaterMl },
                    weightKg = if (avgWeight.isNaN()) targetWeightKg else avgWeight
                )
            }.takeLast(5)
        }
        "MONTHLY" -> {
            val grouped = dataList.groupBy { it.dateKey.substring(0, 7) }
            grouped.map { (monthKey, items) ->
                val monthParts = monthKey.split("-")
                val monthLabel = when (monthParts.getOrNull(1)) {
                    "01" -> "Jan"
                    "02" -> "Feb"
                    "03" -> "Mar"
                    "04" -> "Apr"
                    "05" -> "May"
                    "06" -> "Jun"
                    "07" -> "Jul"
                    "08" -> "Aug"
                    "09" -> "Sep"
                    "10" -> "Oct"
                    "11" -> "Nov"
                    "12" -> "Dec"
                    else -> monthKey
                }
                val avgWeight = items.mapNotNull { it.weightKg }.average()
                val avgRoutine = items.map { it.routineCompletionPct }.average()
                ChartDataItem(
                    label = monthLabel,
                    studyMinutes = items.sumOf { it.totalStudyMinutes },
                    workMinutes = items.sumOf { it.workMin },
                    timepassMinutes = items.sumOf { it.timepassMin },
                    workoutMinutes = items.sumOf { it.totalWorkoutMinutes },
                    steps = items.sumOf { it.totalSteps },
                    routinePct = if (avgRoutine.isNaN()) 0.0 else avgRoutine,
                    waterMl = items.sumOf { it.totalWaterMl },
                    weightKg = if (avgWeight.isNaN()) targetWeightKg else avgWeight
                )
            }.takeLast(4)
        }
        else -> emptyList()
    }
}
