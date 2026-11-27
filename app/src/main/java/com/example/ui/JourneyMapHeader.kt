package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.JourneyCharacter
import com.example.domain.RegionDef
import com.example.ui.theme.*

/** What the icons on the map do. The icons replace the cards that used to stack under the map. */
class MapActions(val onBack: () -> Unit, val onPassport: () -> Unit, val onLog: () -> Unit, val onRegions: () -> Unit, val onShare: () -> Unit)

/**
 * The full-bleed map with the back arrow, the region number and name over the art, and Passport, Travel log, Regions and Share
 * as icons on the top right.
 */
@Composable
fun JourneyMapHeader(region: RegionDef, step: Int, character: JourneyCharacter, actions: MapActions) {
    Box(Modifier.fillMaxWidth()) {
        JourneyMapCard(region, step, character)
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(0f to Color(0xCC05070D), 0.4f to Color.Transparent)))
        Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = actions.onBack, modifier = Modifier.testTag("journey_back")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(Modifier.weight(1f))
                MapIcon(Icons.Default.Star, "Passport", "journey_icon_passport", actions.onPassport)
                MapIcon(Icons.Default.DateRange, "Travel log", "journey_open_log", actions.onLog)
                MapIcon(Icons.Default.Place, "Regions", "journey_icon_regions", actions.onRegions)
                MapIcon(Icons.Default.Share, "Share my Journey", "journey_share", actions.onShare)
            }
            Column(Modifier.padding(start = 12.dp, end = 12.dp)) {
                Text("Region ${region.index + 1}", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(region.name, color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("journey_region"))
            }
        }
    }
}

@Composable
private fun MapIcon(icon: ImageVector, description: String, tag: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp).testTag(tag)) { Icon(icon, contentDescription = description, tint = TextPrimary) }
}
