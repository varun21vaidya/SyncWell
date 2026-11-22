package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.CardioLine
import com.example.viewmodel.formatClock
import kotlinx.coroutines.delay

val CARDIO_TYPES = listOf("TREADMILL", "CYCLING", "ROWING", "STAIRS", "RUN", "WALK")
val CARDIO_EFFORTS = listOf("EASY", "STEADY", "HARD")
private val INCLINE_TYPES = setOf("TREADMILL", "WALK") // where incline is most useful

private fun pretty(s: String) = s.lowercase().replaceFirstChar { it.uppercase() }

/** Optional cardio finisher at the end of the exercise list. The draft is saved only when minutes are above zero. */
@Composable
fun CardioCard(draft: CardioLine?, onChange: (CardioLine?) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(draft != null) }
    var type by remember { mutableStateOf(draft?.type ?: "TREADMILL") }
    var minutes by remember { mutableIntStateOf(draft?.minutes ?: 0) }
    var distance by remember { mutableStateOf(draft?.distanceKm?.toString() ?: "") }
    var incline by remember { mutableStateOf(draft?.inclinePct?.toString() ?: "") }
    var effort by remember { mutableStateOf(draft?.effort ?: "STEADY") }
    var timerRunning by remember { mutableStateOf(false) }
    var timerSeconds by remember { mutableIntStateOf(0) }

    fun publish() = onChange(
        if (minutes > 0) CardioLine(type, minutes, distance.toDoubleOrNull(), incline.toDoubleOrNull()?.takeIf { type in INCLINE_TYPES }, effort) else null
    )
    LaunchedEffect(timerRunning) { while (timerRunning) { delay(1000); timerSeconds++ } }

    Card(
        modifier = modifier.fillMaxWidth().testTag("cardio_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, OledBorder)
    ) {
        if (!open) {
            TextButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth().height(52.dp).testTag("add_cardio_button")) {
                Text("Add cardio (optional)", color = EmeraldGlow, fontWeight = FontWeight.Bold)
            }
            return@Card
        }
        val chipColors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = EmeraldDim, selectedLabelColor = EmeraldGlow, containerColor = SurfaceTier2, labelColor = TextSecondary
        )
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Cardio (optional)")
                TextButton(onClick = { open = false; minutes = 0; timerRunning = false; timerSeconds = 0; publish() }, modifier = Modifier.height(48.dp)) {
                    Text("Remove", color = Slate400)
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CARDIO_TYPES.forEach { t -> FilterChip(selected = type == t, onClick = { type = t; publish() }, label = { Text(pretty(t)) }, colors = chipColors) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { minutes = (minutes - 5).coerceAtLeast(0); publish() }, modifier = Modifier.size(48.dp)) { Text("−", fontSize = 22.sp) }
                    Text("$minutes min", fontWeight = FontWeight.Black, fontSize = 18.sp, modifier = Modifier.widthIn(min = 72.dp).testTag("cardio_minutes"))
                    IconButton(onClick = { minutes += 5; publish() }, modifier = Modifier.size(48.dp)) { Text("+", fontSize = 22.sp) }
                }
                OutlinedButton(
                    onClick = {
                        if (timerRunning) { timerRunning = false; minutes = maxOf(1, (timerSeconds + 30) / 60); timerSeconds = 0; publish() } else timerRunning = true
                    },
                    modifier = Modifier.height(48.dp)
                ) { Text(if (timerRunning) "Stop ${formatClock(timerSeconds)}" else "Time it", fontWeight = FontWeight.Bold) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = distance, onValueChange = { distance = it.filter { c -> c.isDigit() || c == '.' }.take(6); publish() },
                    label = { Text("Distance km") }, singleLine = true, modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                if (type in INCLINE_TYPES) OutlinedTextField(
                    value = incline, onValueChange = { incline = it.filter { c -> c.isDigit() || c == '.' }.take(4); publish() },
                    label = { Text("Incline %") }, singleLine = true, modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CARDIO_EFFORTS.forEach { e -> FilterChip(selected = effort == e, onClick = { effort = e; publish() }, label = { Text(pretty(e)) }, colors = chipColors) }
            }
        }
    }
}
