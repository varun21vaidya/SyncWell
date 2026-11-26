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
import com.example.domain.JourneyReminderPolicy
import com.example.domain.JourneyReminderSettings
import com.example.domain.JourneySlot
import com.example.ui.theme.*

/**
 * The reminder controls: a master switch, one switch per slot, and quiet hours. Presentation only: every change goes out
 * through [onChange] as new settings. The two gentle reminders pause while these are on.
 */
@Composable
fun JourneyRemindersCard(settings: JourneyReminderSettings, onChange: (JourneyReminderSettings) -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface1).padding(16.dp).testTag("journey_reminders"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Journey reminders", color = TextPrimary, fontWeight = FontWeight.Bold)
                Text("Up to five calm reminders a day. Each is skipped once today already counts, and all are silent in vacation mode.", color = TextMuted, fontSize = 12.sp)
            }
            Switch(settings.master, { onChange(settings.copy(master = it)) }, modifier = Modifier.testTag("jrem_master"))
        }
        if (settings.master) {
            JourneySlot.values().forEach { slot ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${slot.label}  ${slot.body}", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Switch(slot in settings.slots, { on -> onChange(settings.copy(slots = if (on) settings.slots + slot else settings.slots - slot)) }, modifier = Modifier.testTag("jrem_slot_${slot.name}"))
                }
            }
            QuietHours(settings, onChange)
        }
    }
}

@Composable
private fun QuietHours(settings: JourneyReminderSettings, onChange: (JourneyReminderSettings) -> Unit) {
    var start by remember { mutableStateOf(settings.quietStart?.let { JourneyReminderPolicy.formatTime(it) }.orEmpty()) }
    var end by remember { mutableStateOf(settings.quietEnd?.let { JourneyReminderPolicy.formatTime(it) }.orEmpty()) }
    // Empty means "use bedtime to wake time"; anything else must be a time, and only a valid pair is saved
    val startOk = start.isBlank() || JourneyReminderPolicy.parseTime(start) != null
    val endOk = end.isBlank() || JourneyReminderPolicy.parseTime(end) != null
    fun push(s: String, e: String) {
        val a = if (s.isBlank()) null else JourneyReminderPolicy.parseTime(s)
        val b = if (e.isBlank()) null else JourneyReminderPolicy.parseTime(e)
        if ((s.isBlank() || a != null) && (e.isBlank() || b != null)) onChange(settings.copy(quietStart = a, quietEnd = b))
    }
    Text("Quiet hours (empty uses your bedtime and wake time)", color = TextMuted, fontSize = 12.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(start, { start = it; push(it, end) }, Modifier.weight(1f).testTag("jrem_quiet_start"), label = { Text("From HH:MM") }, singleLine = true, isError = !startOk)
        OutlinedTextField(end, { end = it; push(start, it) }, Modifier.weight(1f).testTag("jrem_quiet_end"), label = { Text("Until HH:MM") }, singleLine = true, isError = !endOk)
    }
}
