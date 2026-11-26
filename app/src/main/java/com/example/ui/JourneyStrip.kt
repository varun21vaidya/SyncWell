package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.JourneyState
import com.example.domain.JourneyCharacters
import com.example.domain.JourneyMap
import com.example.domain.RegionDef
import com.example.ui.theme.*
import com.example.viewmodel.SyncWellViewModel

/** The one-row Journey strip on Home: where the character is, or an invitation. Tapping opens the Journey. */
@Composable
fun JourneyStrip(viewModel: SyncWellViewModel, onClick: () -> Unit) {
    val state by viewModel.journeyState.collectAsState()
    val context = LocalContext.current
    val regions = remember { JourneyMap.parse(context.resources.openRawResource(R.raw.adventure_route).bufferedReader().use { it.readText() }) }
    JourneyStripRow(stripText(state, regions), state?.takeIf { it.enabled }?.characterId, onClick)
}

/** The strip's words: "Ironwood Outpost · Day 9 of 14 · Camp on day 7", a pause note, or the invitation. Never uses the word step. */
internal fun stripText(state: JourneyState?, regions: List<RegionDef>): String = when {
    state == null -> "Try the Journey: walk a painted map, one day at a time"
    !state.enabled -> "Journey paused. Tap to resume"
    else -> regions.getOrElse(state.regionIndex) { regions.last() }.let { r ->
        listOfNotNull(r.name, JourneyMap.dayLabel(r, state.stepInRegion), JourneyMap.nextMilestone(r, state.stepInRegion)).joinToString(" · ")
    }
}

@Composable
internal fun JourneyStripRow(text: String, characterId: String?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface1).clickable(onClick = onClick).padding(12.dp).testTag("journey_strip"),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        characterId?.let { CharacterSprite(JourneyCharacters.byId(it), 28.dp) }
        Text(text, color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
    }
}
