package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.*
import com.example.ui.theme.*

/** Presentation only: all numbers come in as parameters. */
@Composable
private fun StatsCard(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceTier1),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, OledBorder)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            content()
        }
    }
}

@Composable
fun WeekComparisonCard(c: WeekComparison) {
    StatsCard("THIS WEEK VS LAST WEEK", Modifier.testTag("week_comparison_card")) {
        val note = when {
            c.deltaPoints > 0 -> "Up ${c.deltaPoints} points on the same days last week"
            c.deltaPoints < 0 -> "Down ${-c.deltaPoints} points on the same days last week"
            else -> "Level with the same days last week"
        }
        Text("${Math.round(c.thisWeek.avgPercent * 100)}% average routine", fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
        Text(note, color = Slate400, fontSize = 12.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            WeekColumn("This week", c.thisWeek)
            WeekColumn("Last week", c.lastWeek)
        }
        Text("Compared over the first ${c.daysCompared} day${if (c.daysCompared == 1) "" else "s"} of each week.", color = Slate400, fontSize = 10.sp)
    }
}

@Composable
private fun WeekColumn(label: String, s: WeekStats) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = Slate400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text("${Math.round(s.avgPercent * 100)}% avg", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
        Text("${s.qualifyingDays} good days", fontSize = 12.sp, color = Slate400)
        Text("${s.workouts} workouts", fontSize = 12.sp, color = Slate400)
        Text("${s.focusMinutes / 60}h ${s.focusMinutes % 60}m focus", fontSize = 12.sp, color = Slate400)
    }
}

@Composable
fun HeatmapCard(days: List<HeatmapDay>, today: String) {
    val byDate = remember(days) { days.associateBy { it.dateKey } }
    val weeks = remember(today) { HeatmapGrid.weeks(today) }
    var selected by remember { mutableStateOf<HeatmapDay?>(null) }
    StatsCard("LAST 12 WEEKS", Modifier.testTag("heatmap_card")) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            weeks.forEach { week ->
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    week.forEach { date ->
                        val day = byDate[date]
                        val text = day?.description() ?: "No data for $date"
                        Box(
                            Modifier
                                .fillMaxWidth().aspectRatio(1f)
                                .clip(RoundedCornerShape(3.dp))
                                .background(tileColor(day, date > today))
                                .semantics { contentDescription = text } // read out by screen readers, same text as the tap
                                .clickable(enabled = date <= today) { selected = day }
                        )
                    }
                }
            }
        }
        Text(
            selected?.description() ?: "Tap a day to see its exact percent.",
            color = if (selected == null) Slate400 else MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp, modifier = Modifier.testTag("heatmap_detail")
        )
    }
}

private fun tileColor(day: HeatmapDay?, future: Boolean) = when {
    future -> SurfaceTier1
    day == null -> SurfaceTier3
    day.vacation -> CyanDim
    day.percent >= 0.999 -> EmeraldGlow
    day.countedTowardStreak -> EmeraldDim
    day.percent > 0 -> AmberDim
    else -> SurfaceTier3
}

@Composable
fun AchievementsCard(earned: Set<String>) {
    StatsCard("ACHIEVEMENTS", Modifier.testTag("achievements_card")) {
        BadgeRules.ALL.forEach { b ->
            val has = b.id in earned
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(if (has) EmeraldGlow else SurfaceTier3))
                Column(Modifier.weight(1f)) {
                    Text(b.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (has) MaterialTheme.colorScheme.onSurface else Slate400)
                    Text(b.how, fontSize = 11.sp, color = Slate400)
                }
                Text(if (has) "Earned" else "Locked", fontSize = 11.sp, color = if (has) EmeraldGlow else Slate400)
            }
        }
    }
}

@Composable
fun WeeklyReviewCard(c: WeekComparison, goalsDone: Int, onDismiss: () -> Unit) {
    StatsCard("YOUR WEEK", Modifier.testTag("weekly_review_card")) {
        Text(
            "${c.thisWeek.qualifyingDays} good days, ${c.thisWeek.workouts} workouts, ${Math.round(c.thisWeek.avgPercent * 100)}% average routine.",
            fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            when {
                c.deltaPoints > 0 -> "That is ${c.deltaPoints} points up on last week."
                c.deltaPoints < 0 -> "A quieter week than last, and that is fine. A new week starts tomorrow."
                else -> "Steady, the same as last week."
            },
            fontSize = 12.sp, color = Slate400
        )
        if (goalsDone > 0) Text("$goalsDone goal check-offs this week.", fontSize = 12.sp, color = Slate400)
        TextButton(onClick = onDismiss, modifier = Modifier.testTag("dismiss_weekly_review")) { Text("DISMISS", color = Slate400, fontWeight = FontWeight.Black) }
    }
}
