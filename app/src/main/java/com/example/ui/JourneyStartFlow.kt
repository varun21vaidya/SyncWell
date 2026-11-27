package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.JourneyCharacters
import com.example.domain.WhyDraft
import com.example.ui.theme.*

/**
 * The first start: "Before you set off" (the optional why), then "Choose your traveller". Skipping the first still goes on to the second.
 * [onFinish] gets the chosen traveller and whatever was written. Presentation only.
 */
@Composable
fun JourneyStartFlow(today: String, onFinish: (character: String, why: WhyDraft?) -> Unit, onClose: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var why by remember { mutableStateOf<WhyDraft?>(null) }
    var picked by rememberSaveable { mutableStateOf(JourneyCharacters.all.first().id) }
    if (step == 0) {
        BeforeYouSetOff(today, onSave = { a, i, d, r -> why = WhyDraft(a, i, d, r) }, onNext = { step = 1 }, onClose = onClose)
    } else {
        ChooseTravellerScreen(picked, { picked = it }, confirm = { "Walk as ${it.name}" }, onConfirm = { onFinish(picked, why) }, onBack = { step = 0 })
    }
}

@Composable
private fun BeforeYouSetOff(today: String, onSave: (String?, String?, String?, String?) -> Unit, onNext: () -> Unit, onClose: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Background).verticalScroll(rememberScrollState()).testTag("journey_before")) {
        TextButton(onClick = onClose, modifier = Modifier.padding(horizontal = 8.dp)) { Text("← Close") }
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Before you set off", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text("Why are you walking? Everything here is optional, and the Wardens quote it at the gates.", color = TextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
        }
        AmbitionForm(null, null, null, null, today, onSave, onNext, saveLabel = "Set off", cancelLabel = "Skip for now", showTitle = false)
    }
}

/**
 * "Choose your traveller": the chosen character large on a pedestal, the others in a row below, a line about them and a confirm button.
 * Used for the first start and for changing the traveller later in Settings (looks only, nothing about the rules changes).
 */
@Composable
fun ChooseTravellerScreen(selectedId: String, onPick: (String) -> Unit, confirm: (com.example.domain.JourneyCharacter) -> String, onConfirm: () -> Unit, onBack: () -> Unit) {
    val c = JourneyCharacters.byId(selectedId)
    Column(Modifier.fillMaxSize().background(Background).verticalScroll(rememberScrollState()).testTag("journey_choose")) {
        TextButton(onClick = onBack, modifier = Modifier.padding(horizontal = 8.dp)) { Text("← Back") }
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Choose your traveller", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Pedestal(c)
            Text(c.name, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.testTag("journey_choose_name"))
            Text(c.blurb, color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.testTag("journey_choose_blurb"))
            CharacterPicker(selectedId, onPick)
            Button(
                onClick = onConfirm, modifier = Modifier.fillMaxWidth().height(52.dp).testTag("journey_choose_confirm"), shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Progress, contentColor = OnProgress)
            ) { Text(confirm(c), fontWeight = FontWeight.Black) }
            Text("Only how your traveller looks. Nothing about the rules changes.", color = TextMuted, fontSize = 12.sp)
        }
    }
}

/** The character standing on a soft disc. */
@Composable
private fun Pedestal(c: com.example.domain.JourneyCharacter) {
    Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.BottomCenter) {
        Box(Modifier.fillMaxWidth(0.7f).height(36.dp).clip(RoundedCornerShape(50)).background(Surface3))
        CharacterFull(c, 270.dp, Modifier.padding(bottom = 14.dp).testTag("journey_pedestal"))
    }
}

/** The six travellers in a row. */
@Composable
internal fun CharacterPicker(selectedId: String, onPick: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        JourneyCharacters.all.forEach { c ->
            val selected = c.id == selectedId
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (selected) Surface3 else Color.Transparent).clickable { onPick(c.id) }.padding(vertical = 6.dp).testTag("journey_char_${c.id}")) {
                CharacterAvatar(c, 36.dp)
                Text(c.name, color = if (selected) Progress else TextMuted, fontSize = 10.sp, maxLines = 1)
            }
        }
    }
}
