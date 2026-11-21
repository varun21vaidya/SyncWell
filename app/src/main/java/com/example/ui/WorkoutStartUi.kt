package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WorkoutSession
import com.example.domain.NextSplitRule
import com.example.domain.Split
import com.example.ui.theme.*
import com.example.viewmodel.WorkoutHistoryItem
import com.example.viewmodel.WorkoutStartState
import java.text.SimpleDateFormat
import java.util.Locale

/** "Today", "Yesterday" or "Mon 12 Jan" for a yyyy-MM-dd key. */
fun friendlyDate(dateKey: String, today: String): String {
    if (dateKey == today) return "Today"
    return try {
        val f = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val d = f.parse(dateKey)!!
        val t = f.parse(today)
        if (t != null && (t.time - d.time) == 86_400_000L) "Yesterday" else SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(d)
    } catch (e: Exception) { dateKey }
}

fun formatMinutes(seconds: Int): String = "${maxOf(1, (seconds + 30) / 60)} min"

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, color = Slate400, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = modifier)
}

@Composable
fun PrBadge(count: Int = 0) {
    Text(
        text = if (count > 1) "PR ×$count" else "PR",
        color = AmberFlame, fontSize = 11.sp, fontWeight = FontWeight.Black,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(AmberDim).padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
private fun WorkoutCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, OledBorder)
    ) { Column(modifier = Modifier.padding(16.dp), content = content) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutStartContent(
    start: WorkoutStartState,
    activeSession: WorkoutSession?,
    location: String,
    onSetLocation: (String) -> Unit,
    onStart: (Split) -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
    onSetTarget: (Int) -> Unit,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (activeSession != null) item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("active_workout_resume_banner"),
                colors = CardDefaults.cardColors(containerColor = EmeraldDim),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, EmeraldGlow)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("WORKOUT IN PROGRESS", fontSize = 10.sp, fontWeight = FontWeight.Black, color = EmeraldGlow)
                    Text(NextSplitRule.splitOf(activeSession.dayType)?.title ?: activeSession.dayType, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = onResume, shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow),
                            modifier = Modifier.height(48.dp)
                        ) { Text("Resume", fontWeight = FontWeight.Bold, color = OledBlack) }
                        OutlinedButton(
                            onClick = onFinish, shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGlow),
                            border = BorderStroke(1.dp, EmeraldGlow), modifier = Modifier.height(48.dp)
                        ) { Text("Finish", fontWeight = FontWeight.Bold) }
                        TextButton(
                            onClick = onDiscard, colors = ButtonDefaults.textButtonColors(contentColor = RoseCrimson),
                            modifier = Modifier.height(48.dp)
                        ) { Text("Discard", fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }

        // Up next
        item {
            WorkoutCard {
                SectionLabel("UP NEXT")
                Text(start.next.title, fontSize = 22.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
                Text(start.next.muscles, fontSize = 12.sp, color = Slate400)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (start.nextLastDate == null) "First time on this split" else
                        "Last time ${friendlyDate(start.nextLastDate, start.today)}" + (start.nextLastBest?.let { " · $it" } ?: ""),
                    fontSize = 12.sp, color = TextSecondary
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { onStart(start.next) },
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("workout_preset_${start.next.key}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow)
                ) { Text("Start ${start.next.title}", fontWeight = FontWeight.Black, color = OledBlack) }
            }
        }

        // This week
        item {
            WorkoutCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        SectionLabel("THIS WEEK")
                        Text("${start.thisWeekCount} of ${start.weeklyTarget} workouts", fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onSetTarget(start.weeklyTarget - 1) }, enabled = start.weeklyTarget > 1,
                            modifier = Modifier.size(48.dp).testTag("weekly_target_minus")
                        ) { Text("−", fontSize = 22.sp, color = TextPrimary) }
                        Text("Goal ${start.weeklyTarget}", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                        IconButton(
                            onClick = { onSetTarget(start.weeklyTarget + 1) }, enabled = start.weeklyTarget < 7,
                            modifier = Modifier.size(48.dp).testTag("weekly_target_plus")
                        ) { Text("+", fontSize = 22.sp, color = TextPrimary) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (start.thisWeekCount.toFloat() / start.weeklyTarget.coerceAtLeast(1)).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = EmeraldGlow, trackColor = SurfaceTier3
                )
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    start.weekDates.forEachIndexed { i, date ->
                        val done = date in start.weekDone
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier.size(30.dp).clip(CircleShape)
                                    .background(if (done) EmeraldGlow else SurfaceTier2)
                                    .border(1.dp, if (date == start.today) EmeraldGlow else OledBorder, CircleShape)
                                    .semantics { contentDescription = if (done) "Worked out on ${friendlyDate(date, start.today)}" else "No workout on ${friendlyDate(date, start.today)}" },
                                contentAlignment = Alignment.Center
                            ) { Text("MTWTFSS"[i].toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (done) OledBlack else TextSecondary) }
                        }
                    }
                }
            }
        }

        // Location
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("GYM" to "Gym", "HOME" to "Home").forEachIndexed { i, (key, label) ->
                    SegmentedButton(
                        selected = location == key, onClick = { onSetLocation(key) },
                        shape = SegmentedButtonDefaults.itemShape(i, 2),
                        modifier = Modifier.height(48.dp),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = EmeraldDim, activeContentColor = EmeraldGlow, activeBorderColor = EmeraldGlow,
                            inactiveContainerColor = SurfaceTier2, inactiveContentColor = TextSecondary, inactiveBorderColor = OledBorder
                        )
                    ) { Text(label, fontWeight = FontWeight.Bold) }
                }
            }
        }

        // Other splits
        item { SectionLabel("OTHER SPLITS", Modifier.padding(top = 4.dp)) }
        items(Split.values().filter { it != start.next }) { split ->
            Card(
                modifier = Modifier.fillMaxWidth().testTag("workout_preset_${split.key}").clickable { onStart(split) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, OledBorder)
            ) {
                Row(Modifier.fillMaxWidth().defaultMinSize(minHeight = 64.dp).padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(split.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            start.lastDone[split]?.let { "Last done ${friendlyDate(it, start.today)}" } ?: "Not done yet",
                            color = Slate400, fontSize = 12.sp
                        )
                    }
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Start ${split.title}", tint = Slate400)
                }
            }
        }

        // Recent
        item {
            WorkoutCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("RECENT")
                    TextButton(onClick = onSeeAll, modifier = Modifier.height(48.dp).testTag("workout_see_all")) { Text("See all", color = EmeraldGlow, fontWeight = FontWeight.Bold) }
                }
                if (start.recent.isEmpty()) {
                    Text("Finished workouts show up here.", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(bottom = 4.dp))
                } else {
                    start.recent.forEach { HistoryRow(it, start.today) }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
fun HistoryRow(item: WorkoutHistoryItem, today: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                "${friendlyDate(item.session.date, today)} · ${NextSplitRule.splitOf(item.session.dayType)?.title ?: item.session.dayType}",
                fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                "${formatMinutes(item.activeSeconds)} · ${item.sets} sets · ${item.exercises} exercises",
                color = Slate400, fontSize = 12.sp
            )
            item.cardio.forEach { c ->
                Text(
                    "Cardio: ${c.type.lowercase().replaceFirstChar { it.uppercase() }} ${c.minutes} min" + (c.distanceKm?.let { " · ${com.example.viewmodel.formatKg(it)} km" } ?: ""),
                    color = Slate400, fontSize = 12.sp
                )
            }
        }
        if (item.newBests > 0) PrBadge(item.newBests)
    }
}

@Composable
fun WorkoutHistoryContent(items: List<WorkoutHistoryItem>, today: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("workout_history_back")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Workout history", fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
        }
        if (items.isEmpty()) {
            Text("No finished workouts yet.", color = TextSecondary, modifier = Modifier.padding(16.dp))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items, key = { it.session.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, OledBorder)
                    ) { Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) { HistoryRow(item, today) } }
                }
            }
        }
    }
}
