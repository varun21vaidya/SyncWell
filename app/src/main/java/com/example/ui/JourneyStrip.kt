package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.domain.JourneyCharacter
import com.example.domain.JourneyCharacters
import com.example.domain.JourneyMap
import com.example.domain.JourneyRail
import com.example.domain.RegionDef
import com.example.ui.theme.*
import com.example.viewmodel.SyncWellViewModel

/** The Journey card on Home: your avatar, where you are, the day rail and one motivating line. Off or paused, a one-row invitation. Tapping opens the Journey. */
@Composable
fun JourneyStrip(viewModel: SyncWellViewModel, onClick: () -> Unit) {
    val state by viewModel.journeyState.collectAsState()
    val goals by viewModel.journeyGoals.collectAsState()
    val context = LocalContext.current
    val regions = remember { JourneyMap.parse(context.resources.openRawResource(R.raw.adventure_route).bufferedReader().use { it.readText() }) }
    val s = state?.takeIf { it.enabled }
    val region = s?.let { regions.getOrNull(it.regionIndex) }
    if (s == null || region == null) {
        JourneyStripRow(JourneyMap.stripText(state, regions), null, onClick)
        return
    }
    JourneyHomeCard(
        region, s.stepInRegion, JourneyCharacters.byId(s.characterId),
        JourneyRail.motivator(region, s.stepInRegion, goals.filter { it.kind == "COMMITMENT" && it.regionIndex == s.regionIndex }), onClick
    )
}

/** Stateless Home card, so it can be drawn and tested without a ViewModel. */
@Composable
internal fun JourneyHomeCard(region: RegionDef, step: Int, character: JourneyCharacter, motivator: String, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface1).border(1.dp, Border, RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(14.dp).testTag("journey_strip"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CharacterAvatar(character, 44.dp)
            Column(Modifier.weight(1f)) {
                Text(region.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, modifier = Modifier.testTag("journey_card_region"))
                Text(JourneyMap.dayLabel(region, step), color = TextSecondary, fontSize = 12.sp, modifier = Modifier.testTag("journey_card_day"))
            }
            Text("›", color = TextMuted, fontSize = 22.sp)
        }
        JourneyDayRail(JourneyRail.pips(region, step), JourneyRail.labels(region))
        Text(motivator, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.testTag("journey_card_motivator"))
    }
}

/** The one-row invitation or pause note. */
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
