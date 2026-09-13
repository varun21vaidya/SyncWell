package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.SyncWellViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerScreen(viewModel: SyncWellViewModel) {
    val isActiveTimerRunning by viewModel.isActiveTimerRunning.collectAsState()
    val accumulatedSeconds by viewModel.accumulatedSeconds.collectAsState()
    val activeContextType by viewModel.activeContextType.collectAsState()
    val activeTodoTag by viewModel.activeTodoTag.collectAsState()
    val todayFocusSessions by viewModel.todayFocusSessions.collectAsState()
    val todoItems by viewModel.todoCollectionList.collectAsState()
    val isPausedByAttention by viewModel.isSessionPausedByAttentionCheck.collectAsState()

    DisposableEffect(Unit) {
        viewModel.setTrackerScreenFocused(true)
        onDispose {
            viewModel.setTrackerScreenFocused(false)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "MULTI-CATEGORY FOCUS ENGINE",
                color = Slate400,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "Focus Stopwatch",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black
            )
        }

        // Auto-pause banner
        if (isPausedByAttention) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFFCD34D))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Session auto-paused",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                        Text(
                            text = "Your session was saved after no response was detected.",
                            fontSize = 12.sp,
                            color = Color(0xFFB45309)
                        )
                    }
                    IconButton(onClick = { viewModel.clearPauseState() }, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF92400E))
                    }
                }
            }
        }

        // Digital Focus Clock Card representation
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Focus Type Badge
                if (activeContextType != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(PurpleAccent.copy(alpha = 0.15f))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "FOCUSING ON: $activeContextType",
                                color = PurpleAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }

                        if (activeTodoTag != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Target: $activeTodoTag",
                                color = PurpleAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceTier2)
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Select a focus block to begin.",
                            color = Slate400,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Digital Timer Text Display
                val hours = accumulatedSeconds / 3600
                val minutes = (accumulatedSeconds % 3600) / 60
                val seconds = accumulatedSeconds % 60
                val formattedTime = String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)

                // Responsive timer: dynamically scales font size to fit available width
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Divide available width by 7.5 character slots (HH:MM:SS = 8 chars + spacing)
                    val scaledFontSize = minOf(58f, (maxWidth.value / 7.5f)).sp
                    Text(
                        text = formattedTime,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = if (isActiveTimerRunning) EmeraldGlow else MaterialTheme.colorScheme.onSurface,
                        fontSize = scaledFontSize,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Visible,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("digital_clock_timer")
                    )
                }

                // Optional Task Association selection chips before starting
                if (activeContextType == null) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "TAG A TASK (OPTIONAL)",
                            color = Slate400,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Clear tag option
                            FilterChip(
                                selected = activeTodoTag == null,
                                onClick = { viewModel.setActiveTodoTag(null) },
                                label = { Text("No tag", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Emerald500,
                                    selectedLabelColor = Color.White
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )

                            // Task items as tags
                            todoItems.forEach { item ->
                                FilterChip(
                                    selected = activeTodoTag == item.title,
                                    onClick = { viewModel.setActiveTodoTag(item.title) },
                                    label = {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            if (item.isCompleted) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp))
                                            }
                                            Text(item.title, fontSize = 12.sp, maxLines = 1)
                                        }
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Emerald500,
                                        selectedLabelColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.alpha(if (item.isCompleted) 0.6f else 1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Choose concentration block below:",
                        color = Slate400,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    // ── Focus Category Selection ──────────────────────────────────────────────
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val isNarrow = maxWidth < 340.dp
                        if (isNarrow) {
                            // Stacked 2-column grid for very small screens
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FocusCategoryButton(
                                        label = "STUDY",
                                        containerColor = PurpleAccent,
                                        modifier = Modifier.weight(1f),
                                        testTag = "boot_study_timer",
                                        onClick = { viewModel.bootTimer("STUDY") }
                                    )
                                    FocusCategoryButton(
                                        label = "WORK",
                                        containerColor = Slate700,
                                        modifier = Modifier.weight(1f),
                                        testTag = "boot_work_timer",
                                        onClick = { viewModel.bootTimer("WORK") }
                                    )
                                }
                                FocusCategoryButton(
                                    label = "TIMEPASS",
                                    containerColor = Slate400,
                                    modifier = Modifier.fillMaxWidth(),
                                    testTag = "boot_timepass_timer",
                                    onClick = { viewModel.bootTimer("TIMEPASS") }
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FocusCategoryButton(
                                    label = "STUDY",
                                    containerColor = PurpleAccent,
                                    modifier = Modifier.weight(1f),
                                    testTag = "boot_study_timer",
                                    onClick = { viewModel.bootTimer("STUDY") }
                                )
                                FocusCategoryButton(
                                    label = "WORK",
                                    containerColor = Slate700,
                                    modifier = Modifier.weight(1f),
                                    testTag = "boot_work_timer",
                                    onClick = { viewModel.bootTimer("WORK") }
                                )
                                FocusCategoryButton(
                                    label = "TIMEPASS",
                                    containerColor = Slate400,
                                    modifier = Modifier.weight(1f),
                                    testTag = "boot_timepass_timer",
                                    onClick = { viewModel.bootTimer("TIMEPASS") }
                                )
                            }
                        }
                    }
                } else {
                    // ── Active Timer Controls ─────────────────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pause / Resume — takes weight
                        if (isActiveTimerRunning) {
                            Button(
                                onClick = { viewModel.pauseTimer() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("pause_timer_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Slate700),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Pause",
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        } else {
                            Button(
                                onClick = { viewModel.bootTimer(activeContextType!!) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("resume_timer_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Resume",
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        // Save Block
                        Button(
                            onClick = { viewModel.saveFocusSession() },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("save_focus_session_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Save Block",
                                fontSize = 11.sp,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        // Reset (icon only, fixed width = 48 dp)
                        IconButton(
                            onClick = { viewModel.resetTimer() },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("reset_timer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset Clock",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 2. DAILY FOCUS HISTORIC TIMELINE LOGS
        Card(
            modifier = Modifier.fillMaxWidth().testTag("focus_history_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "TODAY'S CONSECRATED FOCUS LOGS",
                    color = Slate400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                if (todayFocusSessions.isEmpty()) {
                    Text(
                        text = "No focus records generated yet today.",
                        color = Slate400,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    )
                } else {
                    val totalFocusMin = todayFocusSessions.sumOf { it.durationMin ?: 0 }
                    val hrs = totalFocusMin / 60
                    val mins = totalFocusMin % 60

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total focus time completed:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (hrs > 0) "${hrs}h ${mins}m" else "$mins min",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = PurpleAccent
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), thickness = 1.dp)

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        todayFocusSessions.forEach { record ->
                            val durationText = (record.durationMin ?: 0).let { min ->
                                if (min >= 60) {
                                    val h = min / 60
                                    val m = min % 60
                                    if (m > 0) "${h}h ${m}m" else "${h} hrs"
                                } else {
                                    "$min min"
                                }
                            }

                            val sessionTag = if (record.source.contains("|Tag: ")) {
                                record.source.substringAfter("|Tag: ")
                            } else null

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                when (record.activityType) {
                                                    "STUDY" -> PurpleAccent
                                                    "WORK" -> MaterialTheme.colorScheme.secondary
                                                    "TIMEPASS" -> Slate400
                                                    else -> Emerald500
                                                }
                                            )
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(record.activityType, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                        if (sessionTag != null) {
                                            Text(
                                                text = sessionTag,
                                                color = PurpleAccent,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = durationText,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.wrapContentWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FocusCategoryButton(
    label: String,
    containerColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    testTag: String = "",
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(44.dp)
            .testTag(testTag),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
    }
}

