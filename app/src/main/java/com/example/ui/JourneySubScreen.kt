package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PassportStamp
import com.example.domain.JourneyMap
import com.example.domain.RegionDef
import com.example.domain.RegionView
import com.example.ui.theme.*

/** A full screen with a back button and a title, for the pages that open from the map icons. */
@Composable
fun JourneySubScreen(title: String, tag: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(Background).testTag(tag)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Back") }
            Text(title, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
fun PassportScreen(stamps: List<PassportStamp>, regions: List<RegionDef>, onBack: () -> Unit) =
    JourneySubScreen("Passport", "journey_passport_screen", onBack) { PassportCard(stamps, regions) }

/** Every region: the ones reached and released clear, the rest ahead with only their name. */
@Composable
fun RegionsScreen(regions: List<RegionDef>, current: Int, furthest: Int, today: String, onBack: () -> Unit) =
    JourneySubScreen("Regions", "journey_regions_screen", onBack) {
        regions.forEach { r ->
            val view = JourneyMap.view(r, current, furthest, today)
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Surface1).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(r.name, color = if (view == RegionView.LOCKED) TextMuted else TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(when (view) { RegionView.CURRENT -> "You are here"; RegionView.DONE -> "Crossed"; RegionView.LOCKED -> "Ahead" }, color = if (view == RegionView.CURRENT) Progress else TextMuted, fontSize = 12.sp)
            }
        }
    }
