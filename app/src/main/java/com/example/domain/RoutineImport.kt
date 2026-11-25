package com.example.domain

import com.example.data.RoutineTemplate
import com.example.data.RoutineTemplateItem
import com.example.ui.ROUTINE_CATEGORIES
import java.util.UUID

data class ImportedItem(
    val time: String?, // "HH:MM" or null
    val category: String,
    val title: String,
    val isRequired: Boolean = true,
    val unknownCategory: Boolean = false // the pasted category was not one of ours and became OTHER
)

data class ImportedGroup(
    val name: String,
    val repeatType: String, // DAILY, WEEKLY, ONCE
    val days: List<Int> = emptyList(), // 0 = Sunday, only for WEEKLY
    val onceDate: String? = null,
    val items: List<ImportedItem>
)

data class LineProblem(val line: Int, val text: String, val reason: String)

data class ImportResult(
    val groups: List<ImportedGroup> = emptyList(),
    val problems: List<LineProblem> = emptyList(),
    val warnings: List<String> = emptyList(),
    val isEdit: Boolean = false // pasted text began with the v1 EDIT marker, so Replace is preselected
) {
    val itemCount get() = groups.sumOf { it.items.size }
    val canSave get() = itemCount > 0
}

/** Builds the AI prompt and reads the AI's answer. Pure Kotlin so it is fully unit tested. */
object RoutineImport {
    const val MAX_CHARS = 20_000
    const val MAX_GROUPS = 20
    const val MAX_ITEMS = 150
    const val MAX_TITLE = 60

    private const val DEFAULT_GROUP = "My routine"
    private const val DIET_RULE_PREFIX = "- Meals and snacks are DIET."
    private val DAY_CODES = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT") // index = day, 0 = Sunday
    private val EVERY_DAY = "[1,2,3,4,5,6,0]"
    private val HEADER = Regex("^SYNCWELL ROUTINE v1( EDIT)?\\s*$", RegexOption.IGNORE_CASE)
    private val TIME = Regex("^(\\d{1,2})(?::(\\d{2}))?\\s*([ap]m)?$", RegexOption.IGNORE_CASE)
    private val DATE = Regex("^\\d{4}-\\d{2}-\\d{2}$")

    private val PROMPT = """You are helping me set up my daily routine in the SyncWell app.
Work in two phases. Never skip phase 1.

PHASE 1 - CHECK AND ASK
Read my routine at the bottom. Before writing anything in the final format, find every problem:
- Missing or vague times, like "morning", "after work" or "before bed".
- Which days each task happens: every day, weekdays, weekends, or specific days.
- Impossible or clashing timings: two tasks at the same time, a long task that runs into the next one, tasks before I wake up or after I go to sleep, less than 5 hours of sleep, times past midnight.
- Inconsistencies: for example "daily" in one place and "Monday to Friday" in another, or waking up at 6:00 but going to the gym at 5:30.
- Dependencies and order: for example "vitamins after lunch" when I gave no lunch time, or "protein after workout" on days without a workout.
- Tasks that are really several tasks, or too vague to tick off.
- Whether a task is optional.

Then ask me about all of them:
- If this chat has a tool for asking questions with clickable options (an "ask user question", multiple-choice or form tool), use it.
- Otherwise ask in plain text as a numbered list. Give each question 2 to 4 short options labelled a, b, c, d and mark the one you suggest, so I can reply like "1b, 2a, 3: 7:30".
- Ask everything in one round if you can, at most 8 questions. Ask another round only if my answers create new problems.
- Never guess silently. Do not ask about things that are already clear.
- If there are no problems, say so and go straight to phase 2.

PHASE 2 - FINAL ROUTINE
When everything is clear, write one short line saying what you fixed. Then write the routine in exactly this format inside one code block, with nothing else inside the code block:

SYNCWELL ROUTINE v1
## Group name | DAILY
HH:MM | CATEGORY | Task title
HH:MM | CATEGORY | Task title | OPTIONAL
END

Format rules:
- Start a new group with a line beginning "## ", then the group name, then " | ", then when it repeats. Write the repeat in one of these 3 ways:
  DAILY
  DAYS: MON TUE WED THU FRI SAT SUN (list only the days it happens)
  ONCE: YYYY-MM-DD
- Every task line is: time | CATEGORY | title. Add " | OPTIONAL" at the end only if I said the task is optional.
- Write the time as 24-hour HH:MM, for example 06:30 or 21:15. Write - instead of a time only for tasks that truly have no fixed time, and only if I confirmed this in phase 1.
- CATEGORY must be exactly one of: DIET, WORKOUT, STUDY, SUPPLEMENT, HYGIENE, SKINCARE, HAIRCARE, HEALTH, PREP, OTHER.
- Meals and snacks are DIET. Drinking water is HYGIENE, and its title must contain the word "water". Gym or exercise sessions are WORKOUT, with warm-up and cool-down as separate tasks. Study or deep work is STUDY.
- Put tasks that happen on the same days in the same group. Inside a group, order tasks by time.
- Keep each title under 40 characters. Split long activities into separate tasks.
- Write the titles in the same language I use. Keep the format words and the categories in English.
- Do not add tasks I did not mention or agree to in phase 1.
- If I ask for a change later, reply with the WHOLE updated routine again in the same format.

My routine:
"""

    private val EXPORT_TAIL = """
The routine above is my current routine. I want to change it as described below.
Run phase 1 on the whole routine again, including my change, and ask me about anything unclear.
In phase 2, return the WHOLE updated routine, not only the changed lines, and make the first line inside the code block SYNCWELL ROUTINE v1 EDIT instead of SYNCWELL ROUTINE v1.

My change:
"""

    /** The text behind "Copy prompt". [supplementNames] are the names the user has logged, in first-logged order. */
    fun prompt(supplementNames: List<String> = emptyList()): String {
        val names = supplementNames.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (names.isEmpty()) return PROMPT
        val extra = "- My supplements are named exactly: ${names.joinToString(", ")}. Use these exact names for SUPPLEMENT tasks."
        return PROMPT.lines().flatMap { if (it.startsWith(DIET_RULE_PREFIX)) listOf(it, extra) else listOf(it) }.joinToString("\n")
    }

    /** The text behind "Copy my routine as text": the prompt, then the current routine in the EDIT format. */
    fun exportPrompt(supplementNames: List<String>, templates: List<RoutineTemplate>, items: List<RoutineTemplateItem>): String =
        prompt(supplementNames) + "\n" + format(templates, items) + "\n" + EXPORT_TAIL

    /** Writes the routine in the import format, with the EDIT header so a paste back preselects Replace. */
    fun format(templates: List<RoutineTemplate>, items: List<RoutineTemplateItem>): String = buildString {
        appendLine("SYNCWELL ROUTINE v1 EDIT")
        val byTemplate = items.groupBy { it.templateId }
        for (t in templates) {
            val repeat = when (t.repeatType) {
                "DAILY" -> "DAILY"
                "ONCE" -> "ONCE: ${t.onceDate}"
                else -> "DAYS: " + com.example.data.RoutineSchedule.parseDays(t.daysOfWeek).sortedBy { (it + 6) % 7 }.joinToString(" ") { DAY_CODES[it] }
            }
            appendLine("## ${clean(t.name)} | $repeat")
            for (i in byTemplate[t.id].orEmpty().sortedBy { it.orderIndex }) {
                appendLine("${i.scheduledTime ?: "-"} | ${i.category} | ${clean(i.title)}" + if (i.isRequired) "" else " | OPTIONAL")
            }
        }
        append("END")
    }

    // A pipe or line break inside a name would break the line format
    private fun clean(s: String) = s.replace('|', '/').replace(Regex("\\s+"), " ").trim()

    /** Turns the preview groups into rows to save. Imported routines are the user's own, never starters. */
    fun toEntities(groups: List<ImportedGroup>): Pair<List<RoutineTemplate>, List<RoutineTemplateItem>> {
        val templates = mutableListOf<RoutineTemplate>()
        val items = mutableListOf<RoutineTemplateItem>()
        for (g in groups) {
            val id = UUID.randomUUID().toString()
            templates += RoutineTemplate(
                id = id, name = g.name, repeatType = g.repeatType,
                daysOfWeek = if (g.repeatType == "WEEKLY") g.days.joinToString(",", "[", "]") else EVERY_DAY,
                onceDate = g.onceDate, isStarter = false
            )
            g.items.forEachIndexed { i, it ->
                items += RoutineTemplateItem(UUID.randomUUID().toString(), id, it.title, it.category, i + 1, it.time, it.isRequired)
            }
        }
        return templates to items
    }

    /** Reads the AI's answer. Forgiving about chatter and formatting, and lists every line it could not use. */
    fun parse(text: String): ImportResult {
        if (text.length > MAX_CHARS) return ImportResult(problems = listOf(LineProblem(0, "", "The text is longer than $MAX_CHARS characters.")))
        val all = text.lines()
        val headers = all.indices.filter { HEADER.matches(all[it].trim().trim('`')) }
        val start = headers.lastOrNull()
        val warnings = mutableListOf<String>()
        if (headers.size > 1) warnings += "Found ${headers.size} routines, used the last one."
        val range = (start?.plus(1) ?: 0) until all.size

        val groups = mutableListOf<ImportedGroup>()
        val problems = mutableListOf<LineProblem>()
        var name: String? = null
        var repeat: Repeat? = null
        var rows = mutableListOf<ImportedItem>()
        var skipping = false // the current group line was bad, so its items are skipped too
        var itemTotal = 0
        var groupsFull = false

        fun flush() {
            val n = name
            val r = repeat
            if (n != null && r != null) {
                if (rows.isEmpty()) warnings += "The group \"$n\" has no tasks, so it was left out."
                else groups += ImportedGroup(n, r.type, r.days, r.onceDate, rows.toList())
            }
            name = null; repeat = null; rows = mutableListOf()
        }

        for (idx in range) {
            val line = all[idx].trim()
            val no = idx + 1
            if (line.isEmpty() || line.startsWith("```")) continue
            if (line.equals("END", true)) break
            if (line.startsWith("##")) {
                flush()
                skipping = false
                if (groups.size >= MAX_GROUPS) {
                    if (!groupsFull) problems += LineProblem(no, line, "More than $MAX_GROUPS groups, the rest were skipped.")
                    groupsFull = true; skipping = true; continue
                }
                val parts = line.removePrefix("##").split('|').map { it.trim() }
                val groupName = parts[0].take(MAX_TITLE)
                if (groupName.isEmpty()) { problems += LineProblem(no, line, "The group has no name."); skipping = true; continue }
                val parsed = parseRepeat(parts.getOrNull(1))
                if (parsed.error != null) { problems += LineProblem(no, line, parsed.error); skipping = true; continue }
                if (parts.size < 2) warnings += "The group \"$groupName\" has no repeat, so it repeats every day."
                name = groupName; repeat = parsed.repeat
                continue
            }
            if (skipping) { problems += LineProblem(no, line, "Skipped because its group line could not be read."); continue }
            val item = parseItem(line, no, problems) ?: continue
            if (itemTotal >= MAX_ITEMS) {
                if (itemTotal == MAX_ITEMS) problems += LineProblem(no, line, "More than $MAX_ITEMS tasks, the rest were skipped.")
                itemTotal++; continue
            }
            if (name == null) { name = DEFAULT_GROUP; repeat = Repeat("DAILY") }
            rows += item
            itemTotal++
        }
        flush()
        if (start == null && groups.isNotEmpty()) warnings += "The header line SYNCWELL ROUTINE v1 is missing, the lines were read anyway."
        val edit = start != null && all[start].contains("EDIT", true)
        return ImportResult(groups, problems, warnings, edit)
    }

    private data class Repeat(val type: String, val days: List<Int> = emptyList(), val onceDate: String? = null)
    private class RepeatParse(val repeat: Repeat? = null, val error: String? = null)

    private fun parseRepeat(raw: String?): RepeatParse {
        val s = raw?.trim().orEmpty()
        if (s.isEmpty() || s.equals("DAILY", true)) return RepeatParse(Repeat("DAILY"))
        if (s.startsWith("ONCE", true)) {
            val d = s.drop(4).trim().removePrefix(":").trim()
            return if (DATE.matches(d) && validDate(d)) RepeatParse(Repeat("ONCE", onceDate = d)) else RepeatParse(error = "\"$d\" is not a date like 2026-12-01.")
        }
        if (s.startsWith("DAYS", true)) {
            val tokens = s.drop(4).trim().removePrefix(":").split(Regex("[\\s,]+")).filter { it.isNotEmpty() }
            val days = tokens.map { t -> DAY_CODES.indexOf(t.take(3).uppercase()).also { if (it < 0) return RepeatParse(error = "\"$t\" is not a day. Use MON TUE WED THU FRI SAT SUN.") } }.distinct()
            return if (days.isEmpty()) RepeatParse(error = "DAYS needs at least one day.") else RepeatParse(Repeat("WEEKLY", days))
        }
        return RepeatParse(error = "\"$s\" is not DAILY, DAYS: ... or ONCE: ...")
    }

    private fun validDate(d: String): Boolean {
        val (y, m, day) = d.split("-").map { it.toInt() }
        if (m !in 1..12 || day < 1) return false
        val max = intArrayOf(31, if (y % 4 == 0 && (y % 100 != 0 || y % 400 == 0)) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)[m - 1]
        return day <= max
    }

    private fun parseItem(line: String, no: Int, problems: MutableList<LineProblem>): ImportedItem? {
        val parts = line.split('|').map { it.trim() }
        if (parts.size < 3) { problems += LineProblem(no, line, "Does not look like: time | CATEGORY | title"); return null }
        val time = parseTime(parts[0]) ?: if (parts[0].isEmpty() || parts[0] == "-") null else { problems += LineProblem(no, line, "\"${parts[0]}\" is not a time like 06:30."); return null }
        val title = parts[2]
        if (title.isEmpty()) { problems += LineProblem(no, line, "The task has no title."); return null }
        if (title.length > MAX_TITLE) { problems += LineProblem(no, line, "The title is longer than $MAX_TITLE characters."); return null }
        val category = parts[1].uppercase()
        val known = category in ROUTINE_CATEGORIES
        return ImportedItem(time, if (known) category else "OTHER", title, !parts.getOrNull(3).equals("OPTIONAL", true), !known)
    }

    /** "7:00", "7 am", "7:30 PM", "19:30" -> "HH:MM"; null when it is not a time. */
    fun parseTime(raw: String): String? {
        val m = TIME.matchEntire(raw.trim()) ?: return null
        val ampm = m.groupValues[3].lowercase()
        if (m.groupValues[2].isEmpty() && ampm.isEmpty()) return null // a bare number is too vague to guess
        var h = m.groupValues[1].toInt()
        val min = m.groupValues[2].ifEmpty { "0" }.toInt()
        if (min > 59) return null
        if (ampm.isNotEmpty()) {
            if (h !in 1..12) return null
            h = h % 12 + if (ampm == "pm") 12 else 0
        } else if (h > 23) return null
        return "%02d:%02d".format(h, min)
    }
}
