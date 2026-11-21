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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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

private fun ringColor(category: String) = when (category) {
    "DIET" -> AmberFlame
    "SUPPLEMENT" -> VioletPulse
    "SKINCARE" -> RoseAccent
    "WORKOUT" -> EmeraldGlow
    else -> CyanLaser
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
                ProgressRing(percent.toFloat(), 96.dp, 10.dp, EmeraldGlow) {
                    Text("${(percent * 100).toInt()}%", fontSize = 22.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                }
                Column {
                    Text("TODAY'S ROUTINE", color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
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
fun StreakChip(streak: StreakResult, done: Int, total: Int, vacation: Boolean, level: LevelResult) {
    val message = when {
        vacation -> "On vacation. Your streak is paused, nothing is lost."
        total == 0 -> "Nothing scheduled today."
        DayCompletion.of(done, total).qualifies -> "Today counts. Your streak is safe."
        else -> "${DayCompletion.itemsToQualify(done, total)} more to keep your streak"
    }
    val bestLine = when {
        streak.current >= streak.best && streak.best > 0 -> "Your best streak so far"
        streak.best > 0 -> "${streak.best - streak.current} to reach your best (${streak.best})"
        else -> "Tick 80% of your routine to start a streak"
    }
    Card(
        modifier = Modifier.fillMaxWidth().testTag("streak_chip"),
        colors = CardDefaults.cardColors(containerColor = AmberDim),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Star, "Streak", tint = AmberFlame, modifier = Modifier.size(20.dp))
                Text("${streak.current} day streak · Best ${streak.best}", color = AmberFlame, fontSize = 16.sp, fontWeight = FontWeight.Black)
            }
            Text(message, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            if (!vacation) Text(bestLine, color = Slate400, fontSize = 12.sp)
            Text("Level ${level.level} · ${level.name} · ${level.daysToNext} days to next", color = Slate400, fontSize = 12.sp,
                modifier = Modifier.testTag("level_line"))
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
                Text("UP NEXT", color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
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
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("up_next_done")
                ) { Text("DONE", fontWeight = FontWeight.Black, color = Color.White) }
            }
        }
    }
}

@Composable
fun GoalsStrip(goals: List<Todo>, onOpen: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onOpen() }.testTag("goals_strip"),
        colors = CardDefaults.cardColors(containerColor = SurfaceTier2),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, OledBorder)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val done = goals.count { it.isCompleted }
            Text(
                if (goals.isEmpty()) "GOALS" else "GOALS · $done / ${goals.size} done",
                color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp
            )
            if (goals.isEmpty()) {
                Text("No goals yet. Add one in the Goals tab.", color = Slate400, fontSize = 13.sp)
            } else {
                goals.take(3).forEach { g ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CheckCircle, null, tint = if (g.isCompleted) EmeraldGlow else SurfaceTier3, modifier = Modifier.size(18.dp))
                        Text(g.title, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            Text("NOTHING SCHEDULED TODAY", color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text("No routine items are set for today. Add or schedule some in Manage routines.", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
            Button(onClick = onManage, colors = ButtonDefaults.buttonColors(containerColor = Emerald600), shape = RoundedCornerShape(12.dp)) {
                Text("MANAGE ROUTINES", fontWeight = FontWeight.Black, color = Color.White)
            }
        }
    }
}
