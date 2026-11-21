package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Todo
import com.example.domain.CategoryProgress
import com.example.domain.DayCompletion
import com.example.domain.DayItem
import com.example.domain.LevelResult
import com.example.domain.StreakResult
import com.example.domain.WeekMark
import com.example.ui.theme.*

/** Presentation only: every number comes in as a parameter, nothing here reads the database or the view model. */
@Composable
fun ProgressRing(fraction: Float, size: Dp, stroke: Dp, color: Color, modifier: Modifier = Modifier, content: @Composable () -> Unit = {}) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = stroke.toPx()
            val topLeft = Offset(w / 2, w / 2)
            val arc = Size(this.size.width - w, this.size.height - w)
            drawArc(SurfaceTier3, 0f, 360f, false, topLeft, arc, style = Stroke(w, cap = StrokeCap.Round))
            if (fraction > 0f) drawArc(color, -90f, 360f * fraction.coerceIn(0f, 1f), false, topLeft, arc, style = Stroke(w, cap = StrokeCap.Round))
        }
        content()
    }
}

private fun ringLabel(category: String) = when (category) {
    "DIET" -> "Diet"
    "SUPPLEMENT" -> "Supps"
    "SKINCARE" -> "Skin"
    "WORKOUT" -> "Workout"
    "STUDY" -> "Focus"
    else -> category.lowercase().replaceFirstChar { it.uppercase() }
}

// Colour means something: workout is the gym colour, focus is violet, the rest are progress
private fun ringColor(category: String) = when (category) {
    "WORKOUT" -> Gym
    "STUDY" -> Focus
    else -> Progress
}

@Composable
fun DayRingCard(
    percent: Double, done: Int, total: Int,
    categories: List<CategoryProgress>, waterMl: Int, waterGoalMl: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.testTag("day_ring_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, OledBorder)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ProgressRing(percent.toFloat(), 96.dp, 10.dp, Progress) {
                    Text("${(percent * 100).toInt()}%", fontSize = 22.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                }
                Column {
                    Text("Today's routine", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text("$done / $total", fontSize = 30.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("routine_completed_text"))
                    Text("tasks done", color = Slate400, fontSize = 12.sp)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                categories.forEach { c ->
                    SmallRing(ringLabel(c.category), c.fraction, "${c.done}/${c.total}", ringColor(c.category))
                }
                SmallRing("Water", if (waterGoalMl > 0) (waterMl.toFloat() / waterGoalMl) else 0f, "${waterMl * 100 / maxOf(waterGoalMl, 1)}%", CyanLaser)
            }
        }
    }
}

@Composable
private fun SmallRing(label: String, fraction: Float, centre: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ProgressRing(fraction, 44.dp, 5.dp, color) { Text(centre, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }
        Text(label, fontSize = 9.sp, color = Slate400, maxLines = 1)
    }
}

@Composable
fun StreakChip(streak: StreakResult, done: Int, total: Int, vacation: Boolean, level: LevelResult, weekMarks: List<WeekMark>) {
    val message = when {
        vacation -> "On vacation. Your streak is paused, nothing is lost."
        total == 0 -> "Nothing scheduled today."
        DayCompletion.of(done, total).qualifies -> "Today counts. Your streak is safe."
        else -> null
    }
    Card(
        modifier = Modifier.fillMaxWidth().testTag("streak_chip"),
        colors = CardDefaults.cardColors(containerColor = Surface1),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Border)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${streak.current}", color = Streak, fontSize = 44.sp, fontWeight = FontWeight.Black, modifier = Modifier.testTag("streak_number"))
                    Text("day streak", color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                }
                Text("Best ${streak.best}", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp))
            }
            WeekBarRow(weekMarks)
            message?.let { Text(it, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
            Text("Level ${level.level} · ${level.name} · ${level.daysToNext} days to next", color = TextSecondary, fontSize = 12.sp,
                modifier = Modifier.testTag("level_line"))
        }
    }
}

/** Seven Monday-to-Sunday segments: gold = counted, faded gold = today in progress, grey = anything else. */
@Composable
private fun WeekBarRow(marks: List<WeekMark>) {
    val counted = marks.count { it == WeekMark.COUNTED }
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.semantics { contentDescription = "This week: $counted of 7 days counted" }.testTag("week_bar")
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            marks.forEach { m ->
                Box(
                    Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(
                        when (m) {
                            WeekMark.COUNTED -> Streak
                            WeekMark.TODAY_IN_PROGRESS -> Streak.copy(alpha = 0.4f)
                            WeekMark.EMPTY -> Surface3
                        }
                    )
                )
            }
        }
        Row(Modifier.fillMaxWidth()) {
            "MTWTFSS".forEach { Text(it.toString(), color = TextMuted, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
        }
    }
}

@Composable
fun UpNextCard(item: DayItem?, onDone: (DayItem) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("up_next_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, OledBorder)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("Up next", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                if (item == null) {
                    Text("Everything on today's list is done", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                } else {
                    Text(item.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    item.time?.let { Text(it, color = Slate400, fontSize = 12.sp) }
                }
            }
            if (item != null) {
                Button(
                    onClick = { onDone(item) },
                    colors = ButtonDefaults.buttonColors(containerColor = Progress),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("up_next_done")
                ) { Text("Done", fontWeight = FontWeight.Bold, color = OnProgress) }
            }
        }
    }
}

@Composable
fun GoalsStrip(goals: List<Todo>, weekCounts: Map<String, Int>, onOpen: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onOpen() }.testTag("goals_strip"),
        colors = CardDefaults.cardColors(containerColor = SurfaceTier2),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, OledBorder)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val done = goals.count { it.isCompleted }
            Text(
                if (goals.isEmpty()) "Goals" else "Goals · $done / ${goals.size} done",
                color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium
            )
            if (goals.isEmpty()) {
                Text("No goals yet. Add one in the Goals tab.", color = Slate400, fontSize = 13.sp)
            } else {
                goals.take(3).forEach { g ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CheckCircle, null, tint = if (g.isCompleted) Progress else Surface3, modifier = Modifier.size(18.dp))
                        val progress = g.weeklyTarget?.let { " · ${weekCounts[g.id] ?: 0}/$it this week" } ?: ""
                        Text(g.title + progress, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (goals.size > 3) Text("+${goals.size - 3} more", color = Slate400, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun EmptyDayCard(onManage: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("empty_day_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, OledBorder)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Nothing scheduled today", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text("No routine items are set for today. Add or schedule some in Manage routines.", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
            Button(onClick = onManage, colors = ButtonDefaults.buttonColors(containerColor = Progress), shape = RoundedCornerShape(12.dp)) {
                Text("Manage routines", fontWeight = FontWeight.Bold, color = OnProgress)
            }
        }
    }
}
