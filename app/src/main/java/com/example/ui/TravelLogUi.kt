package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.JourneyNote
import com.example.ui.theme.*

/** The optional one-line reflection offered once the character reaches a region's camp. Presentation only: [onSave] stores it. */
@Composable
fun CampReflectionCard(regionName: String, existing: JourneyNote?, onSave: (String) -> Unit) {
    var text by remember(existing?.id) { mutableStateOf(existing?.text.orEmpty()) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface1).padding(16.dp).testTag("journey_camp"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Camp in $regionName", color = TextPrimary, fontWeight = FontWeight.Bold)
        Text("Optional: one line about how the trail is going.", color = TextMuted, fontSize = 12.sp)
        OutlinedTextField(text, { text = it.take(140) }, Modifier.fillMaxWidth().testTag("camp_text"), singleLine = true, placeholder = { Text("What helped this week?") })
        TextButton(onClick = { onSave(text) }, enabled = text.isNotBlank() && text.trim() != existing?.text, modifier = Modifier.testTag("camp_save")) {
            Text(if (existing == null) "Save to the log" else "Update")
        }
    }
}

/** The travel log as its own screen: an auto-written timeline of days and goals, with the user's camp reflections in italics. Newest first. */
@Composable
fun TravelLogScreen(notes: List<JourneyNote>, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Background).padding(16.dp).testTag("travel_log")) {
        Row { TextButton(onClick = onBack) { Text("← Back") }; Text("Travel log", fontSize = 20.sp, fontWeight = FontWeight.Black, color = TextPrimary, modifier = Modifier.padding(top = 10.dp)) }
        if (notes.isEmpty()) Text("Nothing here yet. Your days on the trail will appear as you walk them.", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(notes, key = { it.id }) { n ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Surface1).padding(12.dp)) {
                    Text(n.date, color = TextMuted, fontSize = 12.sp, modifier = Modifier.width(88.dp))
                    Text(
                        n.text, color = if (n.kind == "REFLECTION") TextPrimary else TextSecondary, fontSize = 13.sp,
                        fontStyle = if (n.kind == "REFLECTION") FontStyle.Italic else FontStyle.Normal, modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
