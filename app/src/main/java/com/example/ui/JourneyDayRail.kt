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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.Pip
import com.example.ui.theme.*

/**
 * One pip per day of the region, with the camp and the gate taller and gold, and the first day, camp and gate named underneath.
 * Shared by the Journey page and the Home card. Presentation only: [JourneyRail][com.example.domain.JourneyRail] decides what is shown.
 */
@Composable
fun JourneyDayRail(pips: List<Pip>, labels: List<String>, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().testTag("journey_rail"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth().height(16.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
            pips.forEach { p ->
                val marked = p.camp || p.gate
                val color = when {
                    marked && p.done -> Streak
                    marked -> Streak.copy(alpha = 0.45f)
                    p.done -> Progress
                    else -> Surface3
                }
                Box(
                    Modifier.weight(1f).height(if (marked) 16.dp else 8.dp).clip(RoundedCornerShape(4.dp)).background(color)
                        .semantics { contentDescription = "Day ${p.day}" + (if (p.done) ", walked" else "") + (if (p.camp) ", camp" else "") + (if (p.gate) ", gate" else "") }
                        .testTag("journey_pip_${p.day}")
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEach { Text(it, color = TextMuted, fontSize = 10.sp, maxLines = 1) }
        }
    }
}
