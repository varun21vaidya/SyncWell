package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PassportStamp
import com.example.domain.JourneyMap
import com.example.domain.RegionDef
import com.example.domain.Weather
import com.example.ui.theme.*

/** A generated vector stamp: a double ring, the weather of the region, the label and the date. A slight tilt, fixed by the stamp's id, makes it look pressed by hand. */
@Composable
fun PassportStampView(stamp: PassportStamp, weather: Weather, size: Dp = 96.dp, modifier: Modifier = Modifier) {
    val ink = if (stamp.kind == "REGION") Progress else Streak
    Box(modifier.size(size).rotate(((stamp.id.hashCode() % 9) - 4).toFloat()).testTag("stamp_${stamp.id}"), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = this.size.minDimension / 2
            drawCircle(ink, r - 2f, style = Stroke(width = 3f))
            drawCircle(ink.copy(alpha = 0.55f), r - 9f, style = Stroke(width = 1.5f))
            drawWeather(weather, ink, Offset(center.x, center.y - r * 0.52f), r * 0.2f)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = size * 0.18f, start = size * 0.14f, end = size * 0.14f)) {
            Text(stamp.label, color = ink, fontSize = (size.value * 0.12f).sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(stamp.earnedAt, color = ink.copy(alpha = 0.8f), fontSize = (size.value * 0.09f).sp, maxLines = 1)
        }
    }
}

/** The weather icon of the stamp, drawn from simple shapes so it stays crisp at any size. */
private fun DrawScope.drawWeather(weather: Weather, ink: Color, c: Offset, s: Float) {
    when (weather) {
        Weather.SUN -> {
            drawCircle(ink, s * 0.55f, c)
            for (i in 0 until 8) {
                val a = Math.toRadians(i * 45.0)
                drawLine(ink, Offset(c.x + (s * 0.85f * Math.cos(a)).toFloat(), c.y + (s * 0.85f * Math.sin(a)).toFloat()), Offset(c.x + (s * 1.1f * Math.cos(a)).toFloat(), c.y + (s * 1.1f * Math.sin(a)).toFloat()), strokeWidth = 2f)
            }
        }
        Weather.RAIN -> for (i in -1..1) drawLine(ink, Offset(c.x + i * s * 0.6f, c.y - s * 0.5f), Offset(c.x + i * s * 0.6f - s * 0.2f, c.y + s * 0.5f), strokeWidth = 3f)
        Weather.SNOW -> for (i in -1..1) for (j in 0..1) drawCircle(ink, 2.5f, Offset(c.x + i * s * 0.6f + j * s * 0.25f, c.y + (j * 2 - 1) * s * 0.4f))
    }
}

/** The passport: every stamp earned by crossing a region or meeting a commitment, oldest first. */
@Composable
fun PassportCard(stamps: List<PassportStamp>, regions: List<RegionDef>) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface1).padding(16.dp).testTag("journey_passport"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Passport", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        if (stamps.isEmpty()) Text("Cross a region or meet a commitment to earn your first stamp.", color = TextSecondary, fontSize = 13.sp)
        else androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            stamps.forEach { PassportStampView(it, JourneyMap.weatherOf(regions.getOrNull(it.regionIndex))) }
        }
    }
}
