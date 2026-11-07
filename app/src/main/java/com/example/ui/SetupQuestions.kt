package com.example.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SetupAnswers
import com.example.data.VitaminChoice
import com.example.ui.theme.*

data class SetupResult(
    val answers: SetupAnswers,
    val waterMl: Int,
    val location: String,
    val currentWeightKg: Double?,
    val goalWeightKg: Double?
)

private val TIME = Regex("([01]\\d|2[0-3]):[0-5]\\d")
private val DAYS = listOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 0 to "Sun")
private val PRESET_VITAMINS = listOf("Multivitamin", "Vitamin D3 + K2", "Omega-3", "Magnesium")

/** Personalization questions, shared by onboarding and Settings > Re-run setup. All fields have defaults. */
@Composable
fun SetupQuestionsForm(
    initial: SetupResult?,
    askCurrentWeight: Boolean,
    submitLabel: String,
    onSubmit: (SetupResult) -> Unit,
    onSkip: (() -> Unit)? = null
) {
    val a = initial?.answers ?: SetupAnswers()
    var wake by remember { mutableStateOf(a.wakeTime) }
    var sleep by remember { mutableStateOf(a.sleepTime) }
    var days by remember { mutableStateOf(a.workoutDays.toSet()) }
    var location by remember { mutableStateOf(initial?.location ?: "GYM") }
    var water by remember { mutableStateOf("%.1f".format((initial?.waterMl ?: 2500) / 1000.0)) }
    var current by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf(initial?.goalWeightKg?.takeIf { it > 0 }?.let { "%.1f".format(it) } ?: "") }
    var vitamins by remember { mutableStateOf(a.vitamins) }
    var customName by remember { mutableStateOf("") }
    var customTime by remember { mutableStateOf("") }

    val customTimeOk = customTime.isBlank() || TIME.matches(customTime)
    val valid = TIME.matches(wake) && TIME.matches(sleep) &&
        (water.toDoubleOrNull() ?: 0.0) > 0 &&
        (current.isBlank() || current.toDoubleOrNull() != null) && (goal.isBlank() || goal.toDoubleOrNull() != null)

    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(wake, { wake = it }, Modifier.weight(1f), label = { Text("Wake up HH:mm") }, singleLine = true, isError = !TIME.matches(wake))
            OutlinedTextField(sleep, { sleep = it }, Modifier.weight(1f), label = { Text("Sleep HH:mm") }, singleLine = true, isError = !TIME.matches(sleep))
        }

        Text("Workout days", color = Slate400, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            DAYS.forEach { (d, label) ->
                FilterChip(selected = d in days, onClick = { days = if (d in days) days - d else days + d }, label = { Text(label, fontSize = 11.sp) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("GYM" to "Gym", "HOME" to "Home").forEach { (k, label) ->
                FilterChip(selected = location == k, onClick = { location = k }, label = { Text(label) })
            }
        }

        OutlinedTextField(water, { water = it }, Modifier.fillMaxWidth(), label = { Text("Daily water goal (litres)") }, singleLine = true)
        if (askCurrentWeight) {
            OutlinedTextField(current, { current = it }, Modifier.fillMaxWidth(), label = { Text("Current weight kg (optional)") }, singleLine = true)
        }
        OutlinedTextField(goal, { goal = it }, Modifier.fillMaxWidth(), label = { Text("Goal weight kg (optional)") }, singleLine = true)

        Text("Vitamins you take", color = Slate400, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (PRESET_VITAMINS + vitamins.map { it.name }.filter { it !in PRESET_VITAMINS }).forEach { name ->
                val picked = vitamins.any { it.name == name }
                FilterChip(
                    selected = picked,
                    onClick = { vitamins = if (picked) vitamins.filter { it.name != name } else vitamins + VitaminChoice(name) },
                    label = { Text(name, fontSize = 12.sp) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            OutlinedTextField(customName, { customName = it }, Modifier.weight(2f), label = { Text("Other vitamin") }, singleLine = true)
            OutlinedTextField(customTime, { customTime = it }, Modifier.weight(1f), label = { Text("HH:mm") }, singleLine = true, isError = !customTimeOk)
            TextButton(enabled = customName.isNotBlank() && customTimeOk, onClick = {
                vitamins = vitamins.filter { it.name != customName.trim() } + VitaminChoice(customName.trim(), customTime.ifBlank { null })
                customName = ""; customTime = ""
            }) { Text("ADD") }
        }

        Button(
            enabled = valid,
            onClick = {
                onSubmit(SetupResult(
                    SetupAnswers(wake, sleep, days.toList().sorted(), vitamins),
                    ((water.toDoubleOrNull() ?: 2.5) * 1000).toInt(), location,
                    current.toDoubleOrNull(), goal.toDoubleOrNull()
                ))
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow, contentColor = OledBlack),
            shape = RoundedCornerShape(14.dp)
        ) { Text(submitLabel, fontWeight = FontWeight.Black) }
        onSkip?.let { TextButton(onClick = it, modifier = Modifier.fillMaxWidth()) { Text("Skip, use defaults", color = Slate400) } }
    }
}
