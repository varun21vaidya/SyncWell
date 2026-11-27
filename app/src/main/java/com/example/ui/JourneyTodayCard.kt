package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.DayCompletion
import com.example.domain.JourneyRail
import com.example.domain.RegionDef
import com.example.ui.theme.*

/**
 * "Today counts as day N": today's routine percentage on a bar with the 80% mark, and how many items are left to reach it.
 * Presentation only: the wording comes from [JourneyRail] and the maths from [DayCompletion].
 */
@Composable
fun JourneyTodayCard(region: RegionDef, step: Int, done: Int, total: Int) {
    val result = DayCompletion.of(done, total)
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface1).padding(16.dp).testTag("journey_today"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Today counts as day ${JourneyRail.todayDay(region, step)}", color = TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).testTag("journey_today_title"))
            Text("${Math.round(result.percent * 100)}%", color = if (result.qualifies) Progress else TextSecondary, fontWeight = FontWeight.Black, fontSize = 18.sp, modifier = Modifier.testTag("journey_today_percent"))
        }
        EightyBar(result.percent.toFloat(), result.qualifies)
        Text(JourneyRail.todayLine(done, total), color = TextSecondary, fontSize = 12.sp, modifier = Modifier.testTag("journey_today_line"))
    }
}

/** A bar for today with a tick where 80% sits, so the target is visible while the bar fills. */
@Composable
private fun EightyBar(fraction: Float, reached: Boolean) {
    Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Surface3)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f)).background(if (reached) Progress else Water))
        Box(Modifier.fillMaxHeight().fillMaxWidth(DayCompletion.THRESHOLD.toFloat()), contentAlignment = Alignment.CenterEnd) {
            Box(Modifier.width(2.dp).fillMaxHeight().background(TextPrimary).testTag("journey_today_mark"))
        }
    }
}
