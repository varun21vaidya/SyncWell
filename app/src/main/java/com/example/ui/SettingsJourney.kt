package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.JourneyState
import com.example.domain.JourneyCharacters
import com.example.domain.JourneyReminderSettings
import com.example.service.DayBoundaryManager
import com.example.ui.theme.*
import com.example.viewmodel.SyncWellViewModel

/** The full screens the Journey group can open in place of Settings. */
enum class JourneySettingsView { NONE, START, TRAVELLER }

/** The Journey group in Settings: the mode switch, your traveller, why you walk and the five reminders. */
@Composable
fun SettingsJourneyGroup(viewModel: SyncWellViewModel, open: (JourneySettingsView) -> Unit) {
    val state by viewModel.journeyState.collectAsState()
    val reminders by viewModel.journeyReminders.collectAsState()
    val today by DayBoundaryManager.currentTargetDate.collectAsState()
    SettingsJourneyContent(
        state, reminders, today,
        onToggle = { on -> if (on && state == null) open(JourneySettingsView.START) else viewModel.setJourneyEnabled(on, state?.characterId ?: "max") },
        onChangeTraveller = { open(JourneySettingsView.TRAVELLER) },
        onSaveWhy = { a, i, d, r -> viewModel.saveAmbition(a, i, d, r) },
        onReminders = { viewModel.saveJourneyReminders(it) }
    )
}

/** The screens the group opens: the start flow for a first start, and the traveller picker for a change. */
@Composable
fun SettingsJourneyScreens(viewModel: SyncWellViewModel, view: JourneySettingsView, close: () -> Unit) {
    val state by viewModel.journeyState.collectAsState()
    val today by DayBoundaryManager.currentTargetDate.collectAsState()
    when (view) {
        JourneySettingsView.START -> JourneyStartFlow(today, onFinish = { id, why ->
            viewModel.startJourney(id, why); close()
        }, onClose = close)
        JourneySettingsView.TRAVELLER -> {
            var picked by remember { mutableStateOf(state?.characterId ?: JourneyCharacters.all.first().id) }
            ChooseTravellerScreen(picked, { picked = it }, confirm = { "Use ${it.name}" }, onConfirm = { viewModel.setJourneyCharacter(picked); close() }, onBack = close)
        }
        JourneySettingsView.NONE -> Unit
    }
}

/** Stateless, so it can be drawn and tested without a ViewModel. */
@Composable
internal fun SettingsJourneyContent(
    state: JourneyState?, reminders: JourneyReminderSettings, today: String,
    onToggle: (Boolean) -> Unit, onChangeTraveller: () -> Unit,
    onSaveWhy: (String?, String?, String?, String?) -> Unit, onReminders: (JourneyReminderSettings) -> Unit
) {
    val on = state?.enabled == true
    Card(Modifier.fillMaxWidth().testTag("settings_journey"), colors = CardDefaults.cardColors(containerColor = SurfaceTier1), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, OledBorder)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Journey", color = Slate400, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            ModeRow(on, state != null, onToggle)
            if (state != null) {
                TravellerRow(state, onChangeTraveller)
                WhyRow(state, today, onSaveWhy)
            }
        }
    }
    if (state != null) JourneyRemindersCard(reminders, onReminders)
}

@Composable
private fun ModeRow(on: Boolean, started: Boolean, onToggle: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Journey mode", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
            Text(
                when {
                    on -> "Pausing sends you back to the start of your current region when you return. Earlier regions and stamps stay."
                    started -> "Paused. Switching it on again restarts your current region."
                    else -> "Walk a painted map, one day for every day you reach 80%. Your routine, streaks and badges work the same either way."
                },
                color = Slate400, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.testTag("settings_journey_note")
            )
        }
        Switch(checked = on, onCheckedChange = onToggle, colors = SwitchDefaults.colors(checkedThumbColor = EmeraldGlow, checkedTrackColor = EmeraldDim), modifier = Modifier.testTag("journey_switch"))
    }
}

@Composable
private fun TravellerRow(state: JourneyState, onChange: () -> Unit) {
    val c = JourneyCharacters.byId(state.characterId)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CharacterAvatar(c, 40.dp)
        Column(Modifier.weight(1f)) {
            Text("Your traveller", color = Slate400, fontSize = 11.sp)
            Text(c.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("settings_traveller_name"))
        }
        TextButton(onClick = onChange, modifier = Modifier.testTag("journey_change_traveller")) { Text("Change") }
    }
}

@Composable
private fun WhyRow(state: JourneyState, today: String, onSave: (String?, String?, String?, String?) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Why you walk", color = Slate400, fontSize = 11.sp)
            Text(state.ambitionText ?: state.identityText ?: "Not written yet. Optional.", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, modifier = Modifier.testTag("settings_why_text"))
        }
        TextButton(onClick = { editing = true }, modifier = Modifier.testTag("settings_why_edit")) { Text("Edit") }
    }
    if (editing) AmbitionDialog(state, today, onSave = onSave, onClose = { editing = false })
}
