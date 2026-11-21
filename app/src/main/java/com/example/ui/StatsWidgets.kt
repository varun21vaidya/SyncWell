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
import androidx.compose.ui.text.style.TextAlign
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
            Text(title, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            content()
        }
    }
}

@Composable
fun WeekComparisonCard(c: WeekComparison, streak: Int = 0) {
    StatsCard("This week vs last week", Modifier.testTag("week_comparison_card")) {
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
        val context = androidx.compose.ui.platform.LocalContext.current
        TextButton(onClick = { ShareCard.share(context, c, streak) }, modifier = Modifier.testTag("share_week")) {
            Text("Share my week", color = Progress, fontWeight = FontWeight.Bold)
        }
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
    StatsCard("Last 12 weeks", Modifier.testTag("heatmap_card")) {
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
    future -> Surface1
    day == null -> Surface3
    day.vacation -> WaterDim
    day.countedTowardStreak -> Progress // 80% or more
    day.percent >= 0.5 -> Progress.copy(alpha = 0.55f)
    day.percent > 0 -> Progress.copy(alpha = 0.28f)
    else -> Surface3
}

@Composable
fun MedalGrid(states: List<BadgeState>, dates: Map<String, String>, onSelect: (BadgeState) -> Unit) {
    StatsCard("Medals", Modifier.testTag("medal_grid")) {
        Text("${states.count { it.earned }} of ${states.size} earned", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        var index = 0
        BadgeGroup.values().forEach { g ->
            val inGroup = states.filter { it.badge.group == g }.sortedBy { !it.earned } // earned first, catalogue order otherwise
            if (inGroup.isEmpty()) return@forEach
            Text(g.title, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 6.dp))
            inGroup.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { s -> MedalCell(s, dates[s.badge.id], index++, Modifier.weight(1f), onSelect) }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun MedalCell(s: BadgeState, date: String?, index: Int, modifier: Modifier, onSelect: (BadgeState) -> Unit) {
    Column(
        modifier.clickable { onSelect(s) }.padding(vertical = 6.dp).testTag("medal_${s.badge.id}"),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        BadgeMedal(s.badge, s.earned, 84.dp, staggerMs = (index % 6) * 280)
        Text(s.badge.title, color = if (s.earned) TextPrimary else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 2, textAlign = TextAlign.Center)
        if (s.earned) {
            val times = if (s.badge.id == "FRESH_START" && s.count > 1) " x${s.count}" else ""
            Text(BadgeRules.earnedLabel(date) + times, color = TextMuted, fontSize = 11.sp)
        } else {
            LinearProgressIndicator(
                progress = { s.fraction }, modifier = Modifier.width(64.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = Streak, trackColor = Surface3
            )
            Text(s.progressText, color = TextMuted, fontSize = 11.sp)
        }
    }
}

/** Opens when a medal is tapped: its name, how to earn it, and the day it was earned or its progress. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedalSheet(s: BadgeState, date: String?, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Surface2) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp).navigationBarsPadding().testTag("medal_sheet"),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BadgeMedal(s.badge, s.earned, 140.dp)
            Text(s.badge.title, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text(s.badge.how, color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
            if (s.earned) Text(BadgeRules.earnedLabel(date), color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            else {
                LinearProgressIndicator(progress = { s.fraction }, modifier = Modifier.fillMaxWidth(0.6f).height(6.dp).clip(RoundedCornerShape(3.dp)), color = Streak, trackColor = Surface3)
                Text(BadgeRules.progressLine(s), color = TextSecondary, fontSize = 13.sp)
            }
            TextButton(onClick = onDismiss) { Text("Keep going", color = Progress, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun WeeklyReviewCard(c: WeekComparison, goalsDone: Int, onDismiss: () -> Unit) {
    StatsCard("Your week", Modifier.testTag("weekly_review_card")) {
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
        TextButton(onClick = onDismiss, modifier = Modifier.testTag("dismiss_weekly_review")) { Text("Hide", color = TextSecondary, fontWeight = FontWeight.Bold) }
    }
}
