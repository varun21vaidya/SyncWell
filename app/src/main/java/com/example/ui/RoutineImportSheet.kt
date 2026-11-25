package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RoutineTemplate
import com.example.domain.ImportResult
import com.example.domain.ImportedGroup
import com.example.domain.RoutineImport
import com.example.ui.theme.*
import com.example.viewmodel.SyncWellViewModel
import kotlinx.coroutines.launch

/** "Every day", "Mon, Wed" or "Once on ..." for a previewed group, using the same wording as the routine editor. */
fun ImportedGroup.summary(): String = scheduleSummary(
    RoutineTemplate(id = "", name = name, repeatType = repeatType, daysOfWeek = days.joinToString(",", "[", "]"), onceDate = onceDate)
)

/** Warnings that depend on what the user already has: a group name that is already in use. */
fun duplicateGroupWarnings(result: ImportResult, existingNames: Collection<String>): List<String> =
    result.groups.filter { g -> existingNames.any { it.equals(g.name, true) } }.map { "You already have a group called ${it.name}." }

/**
 * Full-screen import in three steps: copy the prompt, paste the answer, preview and save.
 * [startWithReplace] preselects Replace (onboarding, or a pasted v1 EDIT routine).
 */
@Composable
fun RoutineImportSheet(
    viewModel: SyncWellViewModel,
    startWithReplace: Boolean = false,
    onDone: (String) -> Unit,
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val templates by viewModel.routineTemplates.collectAsState()
    var step by rememberSaveable { mutableIntStateOf(0) }
    var pasted by rememberSaveable { mutableStateOf("") }
    var prompt by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<ImportResult?>(null) }
    var replace by rememberSaveable { mutableStateOf(startWithReplace) }
    var confirmReplace by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { prompt = runCatching { viewModel.routineImportPrompt() }.getOrElse { RoutineImport.prompt() } }
    // After a rotation the text survives (saveable) but the parsed result does not, so read it again
    LaunchedEffect(step) { if (step == 2 && result == null) result = RoutineImport.parse(pasted) }

    BackHandler { if (step > 0 && !saving) step-- else if (!saving) onClose() }

    fun save() {
        val r = result ?: return
        saving = true; error = null
        scope.launch {
            try {
                val (g, i) = viewModel.importRoutines(r.groups, replace)
                onDone((if (replace) "Replaced your routines with " else "Added ") + "$g ${if (g == 1) "group" else "groups"}, $i ${if (i == 1) "item" else "items"}")
            } catch (e: Exception) {
                error = "Could not save: ${e.message ?: "unknown error"}. Your routines are unchanged."
            } finally { saving = false }
        }
    }

    Column(Modifier.fillMaxSize().background(Background).padding(16.dp).testTag("import_sheet")) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            TextButton(onClick = { if (step > 0) step-- else onClose() }, enabled = !saving) { Text(if (step > 0) "← Back" else "← Close") }
            Text("Import a routine", fontSize = 20.sp, fontWeight = FontWeight.Black, color = TextPrimary)
        }
        Text("Step ${step + 1} of 3", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))

        when (step) {
            0 -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("1. Copy the prompt", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "Paste it into your AI chat, write your day underneath in your own words, answer the questions it asks, then copy its final answer and paste it here.",
                    color = TextSecondary, fontSize = 13.sp, lineHeight = 19.sp
                )
                Text("Example: \"Wake up at 6, gym for 2 hours on Mon/Wed/Fri, breakfast, vitamin D after lunch, study 8–10 pm on weekdays, skincare before bed.\"", color = TextMuted, fontSize = 12.sp, lineHeight = 18.sp)
                Surface(color = Surface1, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Border), modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp)) {
                    Text(prompt ?: "Loading…", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.verticalScroll(rememberScrollState()).padding(12.dp))
                }
                Button(
                    onClick = { prompt?.let { clipboard.setText(AnnotatedString(it)); copied = true } }, enabled = prompt != null,
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("import_copy"), shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Progress, contentColor = OnProgress)
                ) { Text(if (copied) "Copied" else "Copy prompt", fontWeight = FontWeight.Black) }
                OutlinedButton(onClick = { step = 1 }, modifier = Modifier.fillMaxWidth().height(48.dp).testTag("import_next")) { Text("I have the answer, paste it") }
            }

            1 -> Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("2. Paste the answer", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                OutlinedTextField(
                    value = pasted, onValueChange = { pasted = it.take(RoutineImport.MAX_CHARS + 1) },
                    modifier = Modifier.fillMaxWidth().weight(1f).testTag("import_text"),
                    placeholder = { Text("SYNCWELL ROUTINE v1 ...") },
                    isError = pasted.length > RoutineImport.MAX_CHARS,
                    supportingText = { if (pasted.length > RoutineImport.MAX_CHARS) Text("Too long: the limit is ${RoutineImport.MAX_CHARS} characters.") }
                )
                Button(
                    onClick = { result = RoutineImport.parse(pasted).also { if (it.isEdit) replace = true }; step = 2 },
                    enabled = pasted.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("import_read"), shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Progress, contentColor = OnProgress)
                ) { Text("Read routine", fontWeight = FontWeight.Black) }
            }

            else -> {
                val r = result
                if (r == null) Text("Reading…", color = TextSecondary) else {
                val warnings = r.warnings + if (replace) emptyList() else duplicateGroupWarnings(r, templates.map { it.name })
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("3. Check it", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        if (r.canSave) "Found ${r.groups.size} ${if (r.groups.size == 1) "group" else "groups"} and ${r.itemCount} ${if (r.itemCount == 1) "item" else "items"}. Nothing is saved yet."
                        else "No tasks could be read. Go back and check the pasted text.",
                        color = TextSecondary, fontSize = 13.sp
                    )
                    warnings.forEach { Text("⚠ $it", color = Streak, fontSize = 12.sp) }
                    r.problems.forEach { Text("Line ${it.line}: ${it.reason}  ${it.text}".trim(), color = Danger, fontSize = 12.sp) }
                    r.groups.forEach { g ->
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Surface1).padding(12.dp)) {
                            Text(g.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(g.summary(), color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 6.dp))
                            g.items.forEach { i ->
                                Text(
                                    "${i.time ?: "any time"}  ${i.category}  ${i.title}" + if (i.isRequired) "" else "  (optional)",
                                    color = if (i.unknownCategory) Streak else TextSecondary, fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
                error?.let { Text(it, color = Danger, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { replace = false; save() }, enabled = r.canSave && !saving, colors = choiceColors(selected = !replace),
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("import_add"), shape = RoundedCornerShape(14.dp)) { Text("Add to my routines", fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { replace = true; confirmReplace = true }, enabled = r.canSave && !saving,
                    colors = choiceColors(selected = replace),
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("import_replace"), shape = RoundedCornerShape(14.dp)) { Text("Replace all my routines", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }

    if (confirmReplace) AlertDialog(
        onDismissRequest = { confirmReplace = false },
        title = { Text("Replace all your routines?") },
        text = { Text("Your current routines and their items are deleted and replaced with the ones in the preview. Past days are not changed.") },
        confirmButton = { TextButton(onClick = { confirmReplace = false; save() }) { Text("Replace", color = Danger) } },
        dismissButton = { TextButton(onClick = { confirmReplace = false }) { Text("Cancel") } }
    )
}

/** The preselected save choice is green, the other one is a quiet surface button. */
@Composable
private fun choiceColors(selected: Boolean) =
    if (selected) ButtonDefaults.buttonColors(containerColor = Progress, contentColor = OnProgress)
    else ButtonDefaults.buttonColors(containerColor = Surface2, contentColor = TextPrimary)
