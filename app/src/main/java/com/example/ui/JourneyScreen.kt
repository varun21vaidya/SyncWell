package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.Goal
import com.example.data.JourneyState
import com.example.domain.JourneyCharacter
import com.example.domain.JourneyCharacters
import com.example.domain.JourneyMap
import com.example.domain.JourneyPosition
import com.example.domain.JourneyRoute
import com.example.domain.JourneyShare
import com.example.domain.RegionDef
import com.example.domain.RegionView
import com.example.domain.SetbackNote
import com.example.domain.TravelLog
import kotlinx.coroutines.delay
import com.example.service.DayBoundaryManager
import com.example.ui.theme.*
import com.example.viewmodel.SyncWellViewModel

/** The Journey: opt-in start, then the map with the character, this region's commitments and the regions list. */
@Composable
fun JourneyScreen(viewModel: SyncWellViewModel) {
    val state by viewModel.journeyState.collectAsState()
    val goals by viewModel.journeyGoals.collectAsState()
    val context = LocalContext.current
    val regions = remember { JourneyMap.parse(context.resources.openRawResource(R.raw.adventure_route).bufferedReader().use { it.readText() }) }
    var editing by remember { mutableStateOf(false) }
    var showLog by remember { mutableStateOf(false) }
    val s = state
    if (showLog) {
        val notes by viewModel.journeyNotes.collectAsState()
        BackHandler { showLog = false }
        TravelLogScreen(notes) { showLog = false }
        return
    }

    if (editing && s != null) {
        CommitmentEditor(s.regionIndex, goals.filter { it.kind == "COMMITMENT" }, onSave = { viewModel.saveCommitment(it) }, onClose = { editing = false })
        return
    }
    Column(Modifier.fillMaxSize().background(Background).verticalScroll(rememberScrollState()).padding(16.dp).testTag("journey_screen"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (s == null || !s.enabled) JourneyStart(s?.characterId, resuming = s != null) { id -> viewModel.setJourneyEnabled(true, id) }
        else JourneyTrail(viewModel, s, regions, goals, onAddCommitment = { editing = true }, onOpenLog = { showLog = true })
    }
}

@Composable
internal fun JourneyStart(characterId: String?, resuming: Boolean, onStart: (String) -> Unit) {
    var picked by remember { mutableStateOf(characterId ?: JourneyCharacters.all.first().id) }
    Card(colors = CardDefaults.cardColors(containerColor = Surface1), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (resuming) "Welcome back" else "Walk your routine", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text(
                if (resuming) "You paused the Journey, so you start again at the beginning of your current region. Earlier regions, stamps and badges stay."
                else "A character walks a painted map, one day for every day you reach 80%. Missed days can send you back to the last camp, never further than your current region. It is optional and you can pause it any time.",
                color = TextSecondary, fontSize = 14.sp, lineHeight = 20.sp
            )
            Text("Choose your character", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            CharacterPicker(picked) { picked = it }
            Button(
                onClick = { onStart(picked) }, modifier = Modifier.fillMaxWidth().height(52.dp).testTag("journey_start"), shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Progress, contentColor = OnProgress)
            ) { Text(if (resuming) "Resume the Journey" else "Start the Journey", fontWeight = FontWeight.Black) }
        }
    }
}

@Composable
private fun JourneyTrail(viewModel: SyncWellViewModel, s: JourneyState, regions: List<RegionDef>, goals: List<Goal>, onAddCommitment: () -> Unit, onOpenLog: () -> Unit) {
    val today by DayBoundaryManager.currentTargetDate.collectAsState()
    val seen by viewModel.journeySeenReviews.collectAsState()
    val region = regions.getOrNull(s.regionIndex) // null once the Summit gate is crossed and no later region is bundled yet
    if (region == null) SummitCard() else {
        val commitments = goals.filter { it.kind == "COMMITMENT" && it.regionIndex == s.regionIndex }
        val walk = rememberWalk(viewModel, s, region)
        JourneyHeader(region, s.stepInRegion)
        walk.note?.let { SetbackBanner(it) }
        ReviewCard(viewModel, goals, seen)
        JourneyMapCard(region, walk.shownStep, JourneyCharacters.byId(s.characterId))
        if (s.stepInRegion >= region.steps) WardenCard(region, commitments, s.ambitionText, s.identityText) { viewModel.crossJourneyGate(region.name) }
        CommitmentsCard(viewModel, commitments, canAdd = s.regionIndex > 0, onAdd = onAddCommitment)
        val camp = TravelLog.campDay(region)
        if (camp != null && s.stepInRegion >= camp) {
            val notes by viewModel.journeyNotes.collectAsState()
            CampReflectionCard(region.name, notes.firstOrNull { it.id == "camp-${s.regionIndex}" }) { viewModel.saveCampReflection(s.regionIndex, it) }
        }
    }
    val passport by viewModel.journeyPassport.collectAsState()
    PassportCard(passport, regions)
    val context = LocalContext.current
    val shareRegion = regions.getOrNull(s.regionIndex)
    if (shareRegion != null) {
        TextButton(onClick = {
            val position = JourneyPosition(s.regionIndex, s.stepInRegion)
            JourneyShareCards.share(context, JourneyShare.journey(shareRegion, s.stepInRegion, position, passport.size, JourneyCharacters.byId(s.characterId).name), "my-journey.png", "Share my Journey")
        }, modifier = Modifier.testTag("journey_share")) { Text("Share my Journey", color = Progress) }
    }
    TextButton(onClick = onOpenLog, modifier = Modifier.testTag("journey_open_log")) { Text("Open the travel log", color = Progress) }
    var editingWhy by remember { mutableStateOf(false) }
    AmbitionCard(s, onEdit = { editingWhy = true }, onShare = {
        JourneyShareCards.share(context, JourneyShare.ambition(s, today, JourneyRoute.totalSteps(JourneyPosition(s.regionIndex, s.stepInRegion)), showReward = false), "my-ambition.png", "Share my ambition")
    })
    val reminders by viewModel.journeyReminders.collectAsState()
    JourneyRemindersCard(reminders) { viewModel.saveJourneyReminders(it) }
    if (editingWhy) AmbitionDialog(s.ambitionText, s.identityText, onSave = { a, i -> viewModel.saveAmbition(a, i) }, onClose = { editingWhy = false })
    Text("Your character", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    CharacterPicker(s.characterId) { viewModel.setJourneyCharacter(it) }
    RegionsList(regions, s.regionIndex, s.furthestRegion, today)
    TextButton(onClick = { viewModel.setJourneyEnabled(false) }, modifier = Modifier.testTag("journey_pause")) { Text("Pause the Journey", color = TextMuted) }
}

private class Walk(val shownStep: Int, val note: String?)

/** Starts the sprite where the user last saw it and walks it to today's day, so a step or a setback plays out on opening. */
@Composable
private fun rememberWalk(viewModel: SyncWellViewModel, s: JourneyState, region: RegionDef): Walk {
    val last = remember { viewModel.journeyLastSeen()?.takeIf { it.first == s.regionIndex } }
    var shown by remember { mutableIntStateOf(last?.second ?: s.stepInRegion) }
    val note = remember { last?.let { SetbackNote.of(region.name, it.second, s.stepInRegion, region.points.firstOrNull { p -> p.checkpoint }?.number) } }
    LaunchedEffect(s.regionIndex, s.stepInRegion) {
        delay(500)
        shown = s.stepInRegion
        viewModel.markJourneySeen(s.regionIndex, s.stepInRegion)
    }
    return Walk(shown, note)
}

/** The region image with the character on its day. Step markers are baked into the painted images, so only the character is drawn on them. */
@Composable
internal fun JourneyMapCard(region: RegionDef, step: Int, character: JourneyCharacter) {
    val context = LocalContext.current
    val reduced = LocalReducedMotion.current
    val (tx, ty) = JourneyMap.positionOf(region, step)
    val x by animateFloatAsState(tx, if (reduced) snap() else tween(700), label = "journey_x")
    val y by animateFloatAsState(ty, if (reduced) snap() else tween(700), label = "journey_y")
    val resId = remember(region.image) { region.image?.let { context.resources.getIdentifier(it, "drawable", context.packageName) } ?: 0 }
    BoxWithConstraints(Modifier.fillMaxWidth().height(360.dp).clip(RoundedCornerShape(20.dp)).background(Surface1).testTag("journey_map")) {
        if (resId != 0) Image(painterResource(resId), contentDescription = "Map of ${region.name}", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else PlaceholderTrail(region)
        WeatherOverlay(JourneyMap.weatherOf(region))
        val size = 36.dp
        CharacterSprite(character, size, Modifier.offset(maxWidth * x - size / 2, maxHeight * y - size).testTag("journey_character"))
    }
}

/** Stand-in for a region that has no painted image yet: the trail's points on a dark gradient. */
@Composable
private fun PlaceholderTrail(region: RegionDef) {
    Canvas(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Surface3, Surface1)))) {
        region.points.forEach { p ->
            drawCircle(if (p.checkpoint) Streak else TextMuted, radius = if (p.checkpoint) 9f else 6f, center = Offset(p.x * size.width, p.y * size.height))
        }
    }
}

/** Placeholder sprite until the painted characters exist: a coloured disc with the initial. */
@Composable
fun CharacterSprite(character: JourneyCharacter, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape).background(Color(character.color)), contentAlignment = Alignment.Center) {
        Text(character.name.take(1), color = Background, fontWeight = FontWeight.Black, fontSize = (size.value * 0.5f).sp)
    }
}

@Composable
private fun CharacterPicker(selectedId: String, onPick: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        JourneyCharacters.all.forEach { c ->
            val selected = c.id == selectedId
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (selected) Surface3 else Color.Transparent).clickable { onPick(c.id) }.padding(vertical = 6.dp).testTag("journey_char_${c.id}")) {
                CharacterSprite(c, 36.dp)
                Text(c.name, color = if (selected) Progress else TextMuted, fontSize = 10.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun RegionsList(regions: List<RegionDef>, current: Int, furthest: Int, today: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Regions", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        regions.forEach { r ->
            val view = JourneyMap.view(r, current, furthest, today)
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Surface1).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(r.name, color = if (view == RegionView.LOCKED) TextMuted else TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(when (view) { RegionView.CURRENT -> "You are here"; RegionView.DONE -> "Crossed"; RegionView.LOCKED -> "Ahead" }, color = if (view == RegionView.CURRENT) Progress else TextMuted, fontSize = 12.sp)
            }
        }
    }
}
