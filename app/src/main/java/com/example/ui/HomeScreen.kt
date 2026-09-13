package com.example.ui

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
    val todayStepsLog by viewModel.todayStepsLog.collectAsState()
    val weightLogs by viewModel.weightLogs.collectAsState(initial = emptyList())
    val latestWeight = weightLogs.firstOrNull()?.weightKg

    var showManualStepsDialog by remember { mutableStateOf(false) }
    var manualStepsInput by remember { mutableStateOf("") }

    val totalWaterMl = todayWaterLogs.sumOf { it.amountMl }
    val activeStreak = todayAnalytics?.streakValue ?: 0
    val studyMins = todayAnalytics?.totalStudyMinutes ?: 0
    val totalSteps = todayStepsLog?.stepCount ?: 0

    val completedCount = activeRoutineItems.count { it.isDone }
    val totalCount = if (activeRoutineItems.isNotEmpty()) activeRoutineItems.size else 1
    val completionRatio = completedCount.toFloat() / totalCount

    val isSunday = DayBoundaryManager.isSunday()

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
                Text(
                    text = "EXECUTIVE OVERVIEW",
                    color = Slate400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "SyncWell Control",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Flame Streak Badge (Uses core Star icon)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(Color(0xFFFEF3C7))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Active Streak Star",
                    tint = GoldAccent,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "$activeStreak Days",
                    color = Color(0xFF92400E),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // Sunday Calibration warning Alert (Uses core DateRange icon)
        if (isSunday) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Emerald600),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Calendar Icon",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Weekly Focus Calibration Due",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Sunday at 21:00 requires allocation for the upcoming 55h 20m framework blocks.",
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Checklist Status Progress Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToTab(1) }, // Navigates to Routine Tab
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "TODAY'S ROUTINE",
                    color = Slate400,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "$completedCount / ${activeRoutineItems.size}",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.testTag("routine_completed_text")
                    )
                    Text(
                        text = "Checklist Tasks",
                        color = Slate400,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom Rounded Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(Slate100)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(if (completionRatio > 0f) completionRatio else 0.01f)
                            .clip(CircleShape)
                            .background(Emerald500)
                    )
                }
            }
        }

        // Two Column Split: Hydration & Study Time
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hydration Card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                            letterSpacing = 1.sp
                        )
                        if (totalWaterMl > 0) {
                            Text(
                                text = "Undo Last",
                                color = RoseAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
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
                            color = Emerald500,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.testTag("water_total_text")
                        )
                        Text(
                            text = "/ 4.0 L",
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
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("+250ml", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimary)
                        }

                        Button(
                            onClick = { viewModel.addWaterLog(500) },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("add_water_500"),
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("+500ml", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }

            // Study Focus Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToTab(3) }, // Navigates to Focus Tab
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "STUDY FOCUS",
                        color = Slate400,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val focusHours = studyMins / 60
                    val focusMinsLeft = studyMins % 60
                    Text(
                        text = "${focusHours}h ${focusMinsLeft}m",
                        color = PurpleAccent,
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
                            tint = PurpleAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Start block",
                            color = PurpleAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Steps Metrics Sync Panel (Uses core Refresh icon)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "STEPS COUNT",
                        color = Slate400,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "$totalSteps Steps",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                        IconButton(
                            onClick = {
                                manualStepsInput = totalSteps.toString()
                                showManualStepsDialog = true
                            },
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("steps_manual_edit_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit steps manually",
                                tint = Emerald500,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Button(
                    onClick = { viewModel.syncStepsWithHealthConnect() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("health_sync_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync steps Icon",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                        Text("Health Sync", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Bodyweight Target metrics info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "TARGET BODY COMPOSITION",
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                val latestWeightText = latestWeight?.let { " | Latest Logged: $it kg" } ?: ""
                Text(
                    text = "Current metric goal is strictly pinned to 70.0 kg$latestWeightText. Ensure you register scales weight within Analytics logs tab regularly.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                    lineHeight = 18.sp
                )
            }
        }

        if (showManualStepsDialog) {
            AlertDialog(
                onDismissRequest = { showManualStepsDialog = false },
                title = {
                    Text(
                        text = "MANUALLY LOG STEPS",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Customize your recorded active steps for today's physical activity index:",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                        OutlinedTextField(
                            value = manualStepsInput,
                            onValueChange = { manualStepsInput = it },
                            placeholder = { Text("e.g. 10420", fontSize = 14.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("steps_manual_input"),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Emerald500)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val steps = manualStepsInput.toIntOrNull() ?: 0
                            viewModel.saveManualSteps(steps)
                            showManualStepsDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                        modifier = Modifier.testTag("steps_manual_save_button")
                    ) {
                        Text("Save Steps", color = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showManualStepsDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                    }
                }
            )
        }
    }
}
