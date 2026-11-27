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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.domain.CommitmentRules
import com.example.domain.JourneyMap
import com.example.domain.JourneyRail
import com.example.domain.JourneyPosition
import com.example.domain.JourneyRoute
import com.example.domain.JourneyShare
import com.example.domain.MomentQueue
import com.example.domain.RegionDef
import com.example.domain.SetbackNote
import com.example.domain.TravelLog
import kotlinx.coroutines.delay
import com.example.service.DayBoundaryManager
import com.example.ui.theme.*
import com.example.viewmodel.SyncWellViewModel

/** The places the Journey can show: the page itself, the screens behind the map icons, and the two editors. */
private enum class JourneyView { PAGE, PASSPORT, LOG, REGIONS, COMMITMENT, WEEKLY }

/** The Journey: opt-in start, then the page (map, day rail, today, gate challenge, weekly goals) with its icons and editors. */
@Composable
fun JourneyScreen(viewModel: SyncWellViewModel, onBack: () -> Unit = {}) {
    val state by viewModel.journeyState.collectAsState()
    val goals by viewModel.journeyGoals.collectAsState()
    val context = LocalContext.current
    val regions = remember { JourneyMap.parse(context.resources.openRawResource(R.raw.adventure_route).bufferedReader().use { it.readText() }) }
    var view by rememberSaveable { mutableStateOf(JourneyView.PAGE) }
    BackHandler(enabled = view != JourneyView.PAGE) { view = JourneyView.PAGE }
    val s = state
    if (s == null) { // first start: Before you set off, then Choose your traveller
        val today by DayBoundaryManager.currentTargetDate.collectAsState()
        JourneyStartFlow(today, onFinish = { id, why -> viewModel.startJourney(id, why) }, onClose = onBack)
        return
    }
    if (!s.enabled) { // paused: back where the region started, no questions
        Column(Modifier.fillMaxSize().background(Background).padding(16.dp).testTag("journey_screen"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TextButton(onClick = onBack, modifier = Modifier.testTag("journey_back")) { Text("← Back") }
            JourneyResume { viewModel.setJourneyEnabled(true, s.characterId) }
        }
        return
    }
    if (JourneySubView(view, viewModel, s, regions, goals) { view = JourneyView.PAGE }) return
    JourneyPage(viewModel, s, regions, goals, onBack, open = { view = it })
}

/** Draws the screen behind a map icon or an editor when one is open. Returns false for the page itself. */
@Composable
private fun JourneySubView(view: JourneyView, viewModel: SyncWellViewModel, s: JourneyState, regions: List<RegionDef>, goals: List<Goal>, close: () -> Unit): Boolean {
    val today by DayBoundaryManager.currentTargetDate.collectAsState()
    when (view) {
        JourneyView.PAGE -> return false
        JourneyView.PASSPORT -> PassportScreen(viewModel.journeyPassport.collectAsState().value, regions, close)
        JourneyView.LOG -> TravelLogScreen(viewModel.journeyNotes.collectAsState().value, close)
        JourneyView.REGIONS -> RegionsScreen(regions, s.regionIndex, s.furthestRegion, today, close)
        JourneyView.COMMITMENT -> CommitmentEditor(s.regionIndex, goals.filter { it.kind == "COMMITMENT" }, onSave = { viewModel.saveCommitment(it) }, onClose = close)
        JourneyView.WEEKLY -> CommitmentEditor(s.regionIndex, goals.filter { it.kind == "WEEKLY" }, onSave = { viewModel.saveWeeklyGoal(it) }, onClose = close, weekly = true)
    }
    return true
}

@Composable
internal fun JourneyResume(onResume: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Surface1), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Welcome back", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text("You paused the Journey, so you start again at the beginning of your current region. Earlier regions, stamps and badges stay.", color = TextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
            Button(
                onClick = onResume, modifier = Modifier.fillMaxWidth().height(52.dp).testTag("journey_start"), shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Progress, contentColor = OnProgress)
            ) { Text("Resume the Journey", fontWeight = FontWeight.Black) }
        }
    }
}

/** The page: map with icons, then (padded) the day rail, today, the stacked moments, the gate challenge and the weekly goals. */
@Composable
private fun JourneyPage(viewModel: SyncWellViewModel, s: JourneyState, regions: List<RegionDef>, goals: List<Goal>, onBack: () -> Unit, open: (JourneyView) -> Unit) {
    val today by DayBoundaryManager.currentTargetDate.collectAsState()
    val region = regions.getOrNull(s.regionIndex) // null once the Summit gate is crossed and no later region is bundled yet
    val walk = region?.let { rememberWalk(viewModel, s, it) }
    val seenReviews by viewModel.journeySeenReviews.collectAsState()
    val seenMoments by viewModel.journeySeenMoments.collectAsState()
    var setback by remember(walk?.note) { mutableStateOf(walk?.note) } // the walk-back note shows once, then the page
    val moment = MomentQueue.next(s, region, goals, seenReviews, seenMoments, setback)
    if (moment != null) { // a letter, the walk back, a ticket, the camp, gate day or the Summit takes over the screen once
        JourneyMomentHost(moment, viewModel, s, region, regions, goals) { setback = null }
        return
    }
    Column(Modifier.fillMaxSize().background(Background).verticalScroll(rememberScrollState()).testTag("journey_screen")) {
        if (region == null || walk == null) {
            TextButton(onClick = onBack, modifier = Modifier.testTag("journey_back")) { Text("← Back") }
        } else {
            val actions = rememberMapActions(viewModel, s, region, regions, onBack, open)
            JourneyMapHeader(region, walk.shownStep, JourneyCharacters.byId(s.characterId), actions)
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                DayRailBlock(region, s.stepInRegion)
                if (s.stepInRegion >= region.steps) { // after gate day's screen, the way through stays on the page until it is crossed
                    WardenCard(region, goals.filter { it.kind == "COMMITMENT" && it.regionIndex == s.regionIndex }, s.ambitionText, s.identityText) { viewModel.crossJourneyGate(region.name) }
                }
                val items by viewModel.activeRoutineItems.collectAsState()
                JourneyTodayCard(region, s.stepInRegion, items.count { it.isDone }, items.size)
                val commitments = goals.filter { it.kind == "COMMITMENT" && it.regionIndex == s.regionIndex }
                CommitmentsCard(viewModel, commitments, region, canAdd = s.regionIndex > 0, onAdd = { open(JourneyView.COMMITMENT) })
                WeeklyGoalsCard(viewModel, CommitmentRules.thisWeek(goals, today), onAdd = { open(JourneyView.WEEKLY) })
            }
        }
        if (region == null) Column(Modifier.padding(16.dp)) { SummitCard() }
    }
}

/** "Day 9 of 14" and "Gate in 5 days" over the pip rail. */
@Composable
private fun DayRailBlock(region: RegionDef, step: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row {
            Text(JourneyMap.dayLabel(region, step), color = TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).testTag("journey_day"))
            Text(JourneyRail.gateText(region, step), color = TextSecondary, fontSize = 13.sp, modifier = Modifier.testTag("journey_gate_text"))
        }
        JourneyDayRail(JourneyRail.pips(region, step), JourneyRail.labels(region))
    }
}

/** The four map icons: Passport, Travel log and Regions open their screens, Share opens the share sheet with the Journey card. */
@Composable
private fun rememberMapActions(viewModel: SyncWellViewModel, s: JourneyState, region: RegionDef, regions: List<RegionDef>, onBack: () -> Unit, open: (JourneyView) -> Unit): MapActions {
    val context = LocalContext.current
    val passport by viewModel.journeyPassport.collectAsState()
    return MapActions(
        onBack = onBack, onPassport = { open(JourneyView.PASSPORT) }, onLog = { open(JourneyView.LOG) }, onRegions = { open(JourneyView.REGIONS) },
        onShare = {
            val position = JourneyPosition(s.regionIndex, s.stepInRegion)
            JourneyShareCards.share(context, JourneyShare.journey(region, s.stepInRegion, position, passport.size, JourneyCharacters.byId(s.characterId).name), "my-journey.png", "Share my Journey")
        }
    )
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
    BoxWithConstraints(Modifier.fillMaxWidth().height(360.dp).background(Surface1).testTag("journey_map")) {
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
    CharacterAvatar(character, size, modifier)
}
