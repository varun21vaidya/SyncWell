package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DailyRoutineItem
import com.example.data.RoutineSchedule
import com.example.data.RoutineTemplate
import com.example.data.RoutineTemplateItem
import com.example.service.DayBoundaryManager
import com.example.ui.theme.*
import com.example.viewmodel.SyncWellViewModel
import java.util.UUID

val ROUTINE_CATEGORIES = listOf("DIET", "WORKOUT", "STUDY", "SUPPLEMENT", "HYGIENE", "HEALTH", "PREP", "OTHER")
private val DAY_LABELS = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat") // index = day, 0 = Sunday
private val TIME_REGEX = Regex("([01]\\d|2[0-3]):[0-5]\\d")
private val DATE_REGEX = Regex("\\d{4}-\\d{2}-\\d{2}")

fun scheduleSummary(t: RoutineTemplate): String = when (t.repeatType) {
    "DAILY" -> "Every day"
    "ONCE" -> "Once on ${t.onceDate}"
    else -> RoutineSchedule.parseDays(t.daysOfWeek).sortedBy { (it + 6) % 7 }.joinToString(", ") { DAY_LABELS[it] }
}

@Composable
private fun ItemDialog(title0: String, cat0: String, time0: String, onSave: (String, String, String?) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(title0) }
    var cat by remember { mutableStateOf(cat0) }
    var time by remember { mutableStateOf(time0) }
    val timeOk = time.isBlank() || TIME_REGEX.matches(time)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Routine item", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
                OutlinedTextField(time, { time = it }, label = { Text("Time HH:mm (optional)") }, singleLine = true, isError = !timeOk)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ROUTINE_CATEGORIES.forEach { c -> FilterChip(selected = cat == c, onClick = { cat = c }, label = { Text(c, fontSize = 11.sp) }) }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank() && timeOk, onClick = { onSave(title.trim(), cat, time.ifBlank { null }) }) { Text("SAVE", color = Emerald600, fontWeight = FontWeight.Black) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = Slate400) } }
    )
}

@Composable
private fun TemplateDialog(initial: RoutineTemplate?, onSave: (RoutineTemplate) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var repeat by remember { mutableStateOf(initial?.repeatType ?: "WEEKLY") }
    var days by remember { mutableStateOf(RoutineSchedule.parseDays(initial?.daysOfWeek ?: "[1,2,3,4,5]").toSet()) }
    var date by remember { mutableStateOf(initial?.onceDate ?: DayBoundaryManager.currentTargetDate.value) }
    val valid = name.isNotBlank() && (repeat != "WEEKLY" || days.isNotEmpty()) && (repeat != "ONCE" || DATE_REGEX.matches(date))
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New routine" else "Edit routine", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("ONCE" to "Once", "DAILY" to "Daily", "WEEKLY" to "Custom days").forEach { (k, label) ->
                        FilterChip(selected = repeat == k, onClick = { repeat = k }, label = { Text(label, fontSize = 12.sp) })
                    }
                }
                if (repeat == "WEEKLY") {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(1, 2, 3, 4, 5, 6, 0).forEach { d ->
                            FilterChip(selected = d in days, onClick = { days = if (d in days) days - d else days + d }, label = { Text(DAY_LABELS[d], fontSize = 11.sp) })
                        }
                    }
                }
                if (repeat == "ONCE") {
                    OutlinedTextField(date, { date = it }, label = { Text("Date yyyy-MM-dd") }, singleLine = true, isError = !DATE_REGEX.matches(date))
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                onSave(RoutineTemplate(
                    id = initial?.id ?: UUID.randomUUID().toString(), name = name.trim(), repeatType = repeat,
                    daysOfWeek = days.sorted().joinToString(",", "[", "]"),
                    onceDate = if (repeat == "ONCE") date else null, isStarter = initial?.isStarter ?: false
                ))
            }) { Text("SAVE", color = Emerald600, fontWeight = FontWeight.Black) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = Slate400) } }
    )
}

@Composable
private fun EditRow(title: String, subtitle: String, onUp: (() -> Unit)?, onDown: (() -> Unit)?, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceTier2).padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(subtitle, color = Slate400, fontSize = 10.sp)
        }
        if (onUp != null && onDown != null) {
            TextButton(onClick = onUp, contentPadding = PaddingValues(0.dp), modifier = Modifier.width(32.dp)) { Text("↑") }
            TextButton(onClick = onDown, contentPadding = PaddingValues(0.dp), modifier = Modifier.width(32.dp)) { Text("↓") }
        }
        TextButton(onClick = onEdit, contentPadding = PaddingValues(horizontal = 6.dp)) { Text("Edit", fontSize = 12.sp) }
        TextButton(onClick = onDelete, contentPadding = PaddingValues(horizontal = 6.dp)) { Text("✕", color = RoseCrimson) }
    }
}

@Composable
fun ManageRoutinesScreen(viewModel: SyncWellViewModel, onBack: () -> Unit) {
    val templates by viewModel.routineTemplates.collectAsState()
    val todayItems by viewModel.activeRoutineItems.collectAsState()
    var openId by remember { mutableStateOf<String?>(null) }
    val open = templates.find { it.id == openId }

    BackHandler { if (openId != null) openId = null else onBack() }

    if (open != null) {
        TemplateItemsEditor(viewModel, open, onBack = { openId = null })
        return
    }

    var templateDialog by remember { mutableStateOf<RoutineTemplate?>(null) }
    var showNew by remember { mutableStateOf(false) }
    var editToday by remember { mutableStateOf<DailyRoutineItem?>(null) }
    var showAddToday by remember { mutableStateOf(false) }
    var deleteTemplate by remember { mutableStateOf<RoutineTemplate?>(null) }
    var confirmReset by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("← Back") }
                Text("Manage routines", fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
            }
        }
        item { Text("TODAY'S LIST (today only)", color = Slate400, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        itemsIndexed(todayItems) { i, it ->
            EditRow(it.title, "${it.scheduledTime ?: "any time"} · ${it.category}",
                { viewModel.moveTodayItem(i, -1) }, { viewModel.moveTodayItem(i, 1) },
                { editToday = it }, { viewModel.deleteTodayItem(it.id) })
        }
        item { OutlinedButton(onClick = { showAddToday = true }) { Text("+ Add item to today") } }

        item { Spacer(Modifier.height(8.dp)); Text("MY ROUTINES (repeat on a schedule)", color = Slate400, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        items(templates, key = { it.id }) { t ->
            val expired = t.repeatType == "ONCE" && (t.onceDate ?: "") < DayBoundaryManager.currentTargetDate.value
            Row(
                Modifier.fillMaxWidth().alpha(if (expired) 0.45f else 1f).clip(RoundedCornerShape(12.dp)).background(SurfaceTier2).clickable { openId = t.id }.padding(start = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
                    Text(t.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(scheduleSummary(t), color = Slate400, fontSize = 11.sp)
                }
                TextButton(onClick = { templateDialog = t }) { Text("Schedule", fontSize = 12.sp) }
                TextButton(onClick = { deleteTemplate = t }) { Text("✕", color = RoseCrimson) }
            }
        }
        item { OutlinedButton(onClick = { showNew = true }) { Text("+ New routine") } }
        item {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { confirmReset = true }) { Text("Reset starter routines to defaults", color = Slate400) }
        }
    }

    if (showNew || templateDialog != null) {
        TemplateDialog(templateDialog, onDismiss = { showNew = false; templateDialog = null }, onSave = {
            viewModel.saveRoutineTemplate(it)
            if (showNew) openId = it.id
            showNew = false; templateDialog = null
        })
    }
    if (showAddToday) ItemDialog("", "OTHER", "", { t, c, time -> viewModel.addTodayItem(t, c, time); showAddToday = false }, { showAddToday = false })
    editToday?.let { e ->
        ItemDialog(e.title, e.category, e.scheduledTime ?: "", { t, c, time ->
            viewModel.saveTodayItem(e.copy(title = t, category = c, scheduledTime = time)); editToday = null
        }, { editToday = null })
    }
    deleteTemplate?.let { t ->
        AlertDialog(
            onDismissRequest = { deleteTemplate = null },
            title = { Text("Delete \"${t.name}\"?") },
            text = { Text("Future days will no longer include it. Past days are not changed.") },
            confirmButton = { TextButton(onClick = { viewModel.deleteRoutineTemplate(t.id); deleteTemplate = null }) { Text("DELETE", color = RoseCrimson) } },
            dismissButton = { TextButton(onClick = { deleteTemplate = null }) { Text("CANCEL") } }
        )
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset starter routines?") },
            text = { Text("Rebuilds the starter routines and discards your edits to them. Routines you created stay. Today's list is rebuilt.") },
            confirmButton = { TextButton(onClick = { viewModel.resetRoutinesToDefaults(); confirmReset = false }) { Text("RESET", color = RoseCrimson) } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("CANCEL") } }
        )
    }
}

@Composable
private fun TemplateItemsEditor(viewModel: SyncWellViewModel, template: RoutineTemplate, onBack: () -> Unit) {
    val items by viewModel.templateItems(template.id).collectAsState(initial = emptyList())
    var changed by remember { mutableStateOf(false) }
    var askApply by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<RoutineTemplateItem?>(null) }
    var adding by remember { mutableStateOf(false) }

    fun leave() { if (changed) askApply = true else onBack() }
    BackHandler { leave() }

    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { leave() }) { Text("← Back") }
                Column {
                    Text(template.name, fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
                    Text(scheduleSummary(template), color = Slate400, fontSize = 11.sp)
                }
            }
        }
        items(items, key = { it.id }) { it ->
            EditRow(it.title, "${it.scheduledTime ?: "any time"} · ${it.category}",
                null, null, // the day list is ordered by time, so moving items inside a routine would do nothing
                { editing = it }, { viewModel.deleteTemplateItem(it.id); changed = true })
        }
        item { OutlinedButton(onClick = { adding = true }) { Text("+ Add item") } }
    }

    if (adding) ItemDialog("", "OTHER", "", { t, c, time ->
        val next = (items.maxOfOrNull { it.orderIndex } ?: 0) + 1
        viewModel.saveTemplateItem(RoutineTemplateItem(UUID.randomUUID().toString(), template.id, t, c, next, time))
        changed = true; adding = false
    }, { adding = false })
    editing?.let { e ->
        ItemDialog(e.title, e.category, e.scheduledTime ?: "", { t, c, time ->
            viewModel.saveTemplateItem(e.copy(title = t, category = c, scheduledTime = time)); changed = true; editing = null
        }, { editing = null })
    }
    if (askApply) {
        AlertDialog(
            onDismissRequest = { askApply = false },
            title = { Text("Apply to today?") },
            text = { Text("Rebuild today's list with these changes? Items you already ticked stay ticked. Future days use the changes either way.") },
            confirmButton = { TextButton(onClick = { viewModel.rebuildTodayRoutine(); askApply = false; onBack() }) { Text("APPLY TO TODAY", color = Emerald600, fontWeight = FontWeight.Black) } },
            dismissButton = { TextButton(onClick = { askApply = false; onBack() }) { Text("FUTURE DAYS ONLY", color = Slate400) } }
        )
    }
}
