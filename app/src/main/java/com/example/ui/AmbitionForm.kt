package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.JourneyState
import com.example.domain.Ambition
import com.example.ui.theme.*

/** The Journey's "why", shown under the map: the user's own words, or a gentle invitation to write them. Both lines are optional. */
@Composable
fun AmbitionCard(state: JourneyState, onEdit: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface1).padding(16.dp).testTag("journey_ambition"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Why you walk", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        if (state.ambitionText == null && state.identityText == null) {
            Text("Optional: name what this trail is preparing you for. The Wardens will quote it at the gates.", color = TextSecondary, fontSize = 13.sp)
        }
        state.ambitionText?.let { Text(it, color = TextPrimary, fontWeight = FontWeight.Bold) }
        state.identityText?.let { Text(it, color = TextSecondary, fontSize = 13.sp) }
        TextButton(onClick = onEdit, modifier = Modifier.testTag("journey_ambition_edit")) { Text(if (state.ambitionText == null && state.identityText == null) "Write it" else "Edit") }
    }
}

/** The dialog window around [AmbitionForm]. */
@Composable
fun AmbitionDialog(ambition: String?, identity: String?, onSave: (String?, String?) -> Unit, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        Surface(shape = RoundedCornerShape(24.dp), color = Surface2) { AmbitionForm(ambition, identity, onSave, onClose) }
    }
}

/** Two optional lines with the research note beside them. Presentation only: [onSave] gets the cleaned text, or null for an empty field. */
@Composable
fun AmbitionForm(ambition: String?, identity: String?, onSave: (String?, String?) -> Unit, onClose: () -> Unit) {
    var a by remember { mutableStateOf(ambition.orEmpty()) }
    var i by remember { mutableStateOf(identity.orEmpty()) }
    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Why you walk", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(a, { a = it.take(Ambition.MAX_AMBITION) }, Modifier.fillMaxWidth().testTag("ambition_text"), label = { Text("Ambition (optional)") }, placeholder = { Text("Land a backend role") }, singleLine = true)
        OutlinedTextField(i, { i = it.take(Ambition.MAX_IDENTITY) }, Modifier.fillMaxWidth().testTag("identity_text"), label = { Text("Who you are becoming (optional)") }, placeholder = { Text("I'm becoming someone who practises daily") })
        Text(WHY_IDENTITY, color = TextMuted, fontSize = 11.sp, lineHeight = 16.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onClose) { Text("Cancel") }
            TextButton(onClick = { onSave(Ambition.clean(a, Ambition.MAX_AMBITION), Ambition.clean(i, Ambition.MAX_IDENTITY)); onClose() }, modifier = Modifier.testTag("ambition_save")) { Text("Save") }
        }
    }
}
