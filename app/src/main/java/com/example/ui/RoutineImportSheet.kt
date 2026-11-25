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
import androidx.compose.ui.Alignment
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

private fun plural(n: Int, one: String, many: String) = "$n ${if (n == 1) one else many}"

/** The message shown on the Routine tab after a save. */
fun importDoneMessage(groups: Int, items: Int, replaced: Boolean) =
    (if (replaced) "Replaced your routines with " else "Added ") + "${plural(groups, "group", "groups")}, ${plural(items, "item", "items")}"

private const val STEP_COPY = 0
private const val STEP_PASTE = 1
private const val STEP_PREVIEW = 2

/**
 * Full-screen import in three steps: copy the prompt, paste the answer, preview and save.
 * [startWithReplace] preselects Replace (onboarding, or a pasted v1 EDIT routine).
 * This function only holds the state; each step is its own composable below.
 */
@Composable
fun RoutineImportSheet(
    viewModel: SyncWellViewModel,
    startWithReplace: Boolean = false,
    onDone: (String) -> Unit,
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val templates by viewModel.routineTemplates.collectAsState()
    var step by rememberSaveable { mutableIntStateOf(STEP_COPY) }
    var pasted by rememberSaveable { mutableStateOf("") }
    var replace by rememberSaveable { mutableStateOf(startWithReplace) }
    var prompt by remember { mutableStateOf<String?>(null) }
    var confirmReplace by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    // Parsed from the pasted text only while the preview is open, so a rotation needs no saved copy of it
    val result = remember(pasted, step) { if (step == STEP_PREVIEW) RoutineImport.parse(pasted) else null }

    LaunchedEffect(Unit) { prompt = runCatching { viewModel.routineImportPrompt() }.getOrElse { RoutineImport.prompt() } }
    BackHandler(enabled = !saving) { if (step > STEP_COPY) step-- else onClose() }

    fun save() {
        val r = result ?: return
        saving = true; error = null
        scope.launch {
            try {
                val (g, i) = viewModel.importRoutines(r.groups, replace)
                onDone(importDoneMessage(g, i, replace))
            } catch (e: Exception) {
                error = "Could not save: ${e.message ?: "unknown error"}. Your routines are unchanged."
            } finally { saving = false }
        }
    }

    Column(Modifier.fillMaxSize().background(Background).padding(16.dp).testTag("import_sheet")) {
        SheetHeader(step, enabled = !saving) { if (step > STEP_COPY) step-- else onClose() }
        Column(Modifier.weight(1f).fillMaxWidth()) {
            when (step) {
                STEP_COPY -> CopyStep(prompt) { step = STEP_PASTE }
                STEP_PASTE -> PasteStep(pasted, { pasted = it.take(RoutineImport.MAX_CHARS + 1) }) {
                    if (RoutineImport.parse(pasted).isEdit) replace = true
                    step = STEP_PREVIEW
                }
                else -> PreviewStep(
                    result, replace, saving, error,
                    warnings = result?.let { it.warnings + if (replace) emptyList() else duplicateGroupWarnings(it, templates.map { t -> t.name }) }.orEmpty(),
                    onAdd = { replace = false; save() }, onReplace = { replace = true; confirmReplace = true }
                )
            }
        }
    }

    if (confirmReplace) ConfirmReplaceDialog(onConfirm = { confirmReplace = false; save() }, onCancel = { confirmReplace = false })
}

@Composable
private fun SheetHeader(step: Int, enabled: Boolean, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onBack, enabled = enabled) { Text(if (step > STEP_COPY) "← Back" else "← Close") }
        Text("Import a routine", fontSize = 20.sp, fontWeight = FontWeight.Black, color = TextPrimary)
    }
    Text("Step ${step + 1} of 3", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
}

@Composable
private fun StepTitle(text: String) = Text(text, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)

@Composable
private fun PrimaryAction(label: String, tag: String, enabled: Boolean = true, onClick: () -> Unit) =
    Button(
        onClick = onClick, enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(52.dp).testTag(tag), shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Progress, contentColor = OnProgress)
    ) { Text(label, fontWeight = FontWeight.Black) }

@Composable
private fun CopyStep(prompt: String?, onNext: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StepTitle("1. Copy the prompt")
        Text(
            "Paste it into your AI chat, write your day underneath in your own words, answer the questions it asks, then copy its final answer and paste it here.",
            color = TextSecondary, fontSize = 13.sp, lineHeight = 19.sp
        )
        Text("Example: \"Wake up at 6, gym for 2 hours on Mon/Wed/Fri, breakfast, vitamin D after lunch, study 8–10 pm on weekdays, skincare before bed.\"", color = TextMuted, fontSize = 12.sp, lineHeight = 18.sp)
        Surface(color = Surface1, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Border), modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp)) {
            Text(prompt ?: "Loading…", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.verticalScroll(rememberScrollState()).padding(12.dp))
        }
        PrimaryAction(if (copied) "Copied" else "Copy prompt", "import_copy", enabled = prompt != null) {
            prompt?.let { clipboard.setText(AnnotatedString(it)); copied = true }
        }
        OutlinedButton(onClick = onNext, modifier = Modifier.fillMaxWidth().height(48.dp).testTag("import_next")) { Text("I have the answer, paste it") }
    }
}

@Composable
private fun PasteStep(text: String, onChange: (String) -> Unit, onRead: () -> Unit) {
    val tooLong = text.length > RoutineImport.MAX_CHARS
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StepTitle("2. Paste the answer")
        OutlinedTextField(
            value = text, onValueChange = onChange,
            modifier = Modifier.fillMaxWidth().weight(1f).testTag("import_text"),
            placeholder = { Text("SYNCWELL ROUTINE v1 ...") },
            isError = tooLong,
            supportingText = { if (tooLong) Text("Too long: the limit is ${RoutineImport.MAX_CHARS} characters.") }
        )
        PrimaryAction("Read routine", "import_read", enabled = text.isNotBlank(), onClick = onRead)
    }
}

@Composable
private fun PreviewStep(
    r: ImportResult?, replace: Boolean, saving: Boolean, error: String?, warnings: List<String>,
    onAdd: () -> Unit, onReplace: () -> Unit
) {
    if (r == null) { Text("Reading…", color = TextSecondary); return }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StepTitle("3. Check it")
            Text(
                if (r.canSave) "Found ${plural(r.groups.size, "group", "groups")} and ${plural(r.itemCount, "item", "items")}. Nothing is saved yet."
                else "No tasks could be read. Go back and check the pasted text.",
                color = TextSecondary, fontSize = 13.sp
            )
            warnings.forEach { Text("⚠ $it", color = Streak, fontSize = 12.sp) }
            r.problems.forEach { Text("Line ${it.line}: ${it.reason}  ${it.text}".trim(), color = Danger, fontSize = 12.sp) }
            r.groups.forEach { GroupCard(it) }
        }
        error?.let { Text(it, color = Danger, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) }
        SaveChoice("Add to my routines", "import_add", selected = !replace, enabled = r.canSave && !saving, onClick = onAdd)
        SaveChoice("Replace all my routines", "import_replace", selected = replace, enabled = r.canSave && !saving, onClick = onReplace)
    }
}

/** The preselected save choice is green, the other one is a quiet surface button. */
@Composable
private fun SaveChoice(label: String, tag: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Spacer(Modifier.height(8.dp))
    Button(
        onClick = onClick, enabled = enabled,
        colors = if (selected) ButtonDefaults.buttonColors(containerColor = Progress, contentColor = OnProgress)
        else ButtonDefaults.buttonColors(containerColor = Surface2, contentColor = TextPrimary),
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag(tag), shape = RoundedCornerShape(14.dp)
    ) { Text(label, fontWeight = FontWeight.Bold) }
}

@Composable
private fun GroupCard(g: ImportedGroup) {
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

@Composable
private fun ConfirmReplaceDialog(onConfirm: () -> Unit, onCancel: () -> Unit) = AlertDialog(
    onDismissRequest = onCancel,
    title = { Text("Replace all your routines?") },
    text = { Text("Your current routines and their items are deleted and replaced with the ones in the preview. Past days are not changed.") },
    confirmButton = { TextButton(onClick = onConfirm) { Text("Replace", color = Danger) } },
    dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } }
)

/** The two buttons on the Routine tab: open the import, and copy the current routine as an edit prompt. */
@Composable
fun RoutineImportButtons(viewModel: SyncWellViewModel, onImport: () -> Unit, onNotice: (String) -> Unit) {
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onImport, modifier = Modifier.testTag("routine_import_btn")) {
            Text("Import with AI prompt", color = EmeraldGlow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        TextButton(onClick = {
            scope.launch {
                runCatching { viewModel.routineExportText() }
                    .onSuccess { clipboard.setText(AnnotatedString(it)); onNotice("Copied. Paste it into your AI chat and write your change under \"My change:\".") }
                    .onFailure { onNotice("Could not copy your routine.") }
            }
        }, modifier = Modifier.testTag("routine_export_btn")) { Text("Copy my routine as text", color = Slate400, fontSize = 12.sp) }
    }
}
