package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.CategoryRings
import com.example.domain.DayCompletion
import com.example.domain.DayItem
import com.example.domain.UpNextRule
import com.example.domain.dateHeader
import com.example.domain.greetingFor
import com.example.service.DayBoundaryManager
import com.example.ui.theme.*
import com.example.viewmodel.SyncWellViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: SyncWellViewModel, onNavigateToTab: (Int) -> Unit) {
    val activeRoutineItems by viewModel.activeRoutineItems.collectAsState()
    val todayWaterLogs by viewModel.todayWaterLogs.collectAsState()
    val todayAnalytics by viewModel.todayAnalytics.collectAsState()
    val streaks by viewModel.streaks.collectAsState()
    val weightLogs by viewModel.weightLogs.collectAsState(initial = emptyList())
    val targetWeightKg by viewModel.targetWeightKg.collectAsState()
    val waterGoalMl by viewModel.waterGoalMl.collectAsState()
    val latestWeight = weightLogs.firstOrNull()?.weightKg

    val totalWaterMl = todayWaterLogs.sumOf { it.amountMl }
    val activeStreak = streaks.routine
    val studyMins = todayAnalytics?.totalStudyMinutes ?: 0

    val completedCount = activeRoutineItems.count { it.isDone }
    val isVacationActive by viewModel.isVacationActive.collectAsState()
    val level by viewModel.level.collectAsState()
    val weekMarks by viewModel.weekMarks.collectAsState()
    val nextStamp by viewModel.nextStamp.collectAsState()
    val goals by viewModel.shortTermGoals.collectAsState()
    val goalWeekCounts by viewModel.goalWeekCounts.collectAsState()
    val dayItems = remember(activeRoutineItems) {
        activeRoutineItems.map { DayItem(it.id, it.title, it.category, it.scheduledTime, it.isDone) }
    }
    var nowMinutes by remember { mutableIntStateOf(currentMinutes()) }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(60_000); nowMinutes = currentMinutes() } }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Bar / Streak Badge Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val todayKey by DayBoundaryManager.currentTargetDate.collectAsState()
                Text(
                    text = greetingFor(nowMinutes / 60),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("home_greeting")
                )
                Text(text = dateHeader(todayKey), color = Slate400, fontSize = 12.sp)
            }

        }

        if (activeRoutineItems.isEmpty()) {
            EmptyDayCard { onNavigateToTab(1) }
        } else {
            DayRingCard(
                percent = DayCompletion.of(completedCount, activeRoutineItems.size).percent,
                done = completedCount, total = activeRoutineItems.size,
                categories = CategoryRings.of(dayItems), waterMl = totalWaterMl, waterGoalMl = waterGoalMl,
                onClick = { onNavigateToTab(1) }
            )
        }
        StreakChip(activeStreak, completedCount, activeRoutineItems.size, isVacationActive, level, weekMarks)
        NextStampLine(nextStamp)
        if (activeRoutineItems.isNotEmpty()) {
            UpNextCard(UpNextRule.next(dayItems, nowMinutes)) { next ->
                activeRoutineItems.firstOrNull { it.id == next.id }?.let { viewModel.toggleRoutineItem(it) }
            }
        }
        GoalsStrip(goals, goalWeekCounts) { onNavigateToTab(4) }

        // Two Column Split: Hydration & Study Time
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hydration Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 155.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, OledBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "HYDRATION",
                            color = Slate400,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            maxLines = 1
                        )
                        if (totalWaterMl > 0) {
                            Text(
                                text = "Undo",
                                color = RoseAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier
                                    .clickable { viewModel.reduceWaterLog() }
                                    .testTag("undo_hydration_button")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f", totalWaterMl / 1000f),
                            color = CyanLaser,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.testTag("water_total_text")
                        )
                        Text(
                            text = String.format(Locale.getDefault(), "/ %.1f L", waterGoalMl / 1000f),
                            color = Slate400,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.addWaterLog(250) },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("add_water_250"),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanLaser),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("+250ml", fontSize = 11.sp, fontWeight = FontWeight.Black, color = OledBlack)
                        }

                        Button(
                            onClick = { viewModel.addWaterLog(500) },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("add_water_500"),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanDim),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("+500ml", fontSize = 11.sp, fontWeight = FontWeight.Black, color = CyanLaser)
                        }
                    }
                }
            }

            // Study Focus Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 155.dp)
                    .clickable { onNavigateToTab(3) }, // Navigates to Focus Tab
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, OledBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "STUDY FOCUS",
                        color = Slate400,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val focusHours = studyMins / 60
                    val focusMinsLeft = studyMins % 60
                    Text(
                        text = "${focusHours}h ${focusMinsLeft}m",
                        color = VioletPulse,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )

                    Text(
                        text = "Daily block logs",
                        color = Slate400,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.clickable { onNavigateToTab(3) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Start Timer",
                            tint = VioletPulse,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Start block",
                            color = VioletPulse,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Bodyweight Target metrics info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceTier2),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, OledBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "TARGET BODY COMPOSITION",
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                val latestWeightText = latestWeight?.let { " | Latest Logged: ${String.format(Locale.US, "%.1f", it)} kg" } ?: ""
                val targetStr = String.format(Locale.US, "%.1f", targetWeightKg)
                Text(
                    text = "Current metric goal is set to $targetStr kg$latestWeightText. Ensure you register scale weights within Analytics logs tab regularly.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

private fun currentMinutes(): Int = java.util.Calendar.getInstance().let { it.get(java.util.Calendar.HOUR_OF_DAY) * 60 + it.get(java.util.Calendar.MINUTE) }
