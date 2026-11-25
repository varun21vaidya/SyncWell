package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

class RoutineImportTest {
    private val clean = """
        SYNCWELL ROUTINE v1
        ## Weekday morning | DAYS: MON TUE WED THU FRI
        06:00 | HYGIENE | Wake up, brush teeth
        06:30 | WORKOUT | Gym, 2 hours
        ## Every day | DAILY
        13:30 | SUPPLEMENT | Vit D3
        - | HEALTH | Drink 3 L water
        22:30 | SKINCARE | Night skincare | OPTIONAL
        ## Launch | ONCE: 2026-12-01
        09:00 | PREP | Pack bag
        END
    """.trimIndent()

    @Test fun cleanExample() {
        val r = RoutineImport.parse(clean)
        assertTrue(r.problems.isEmpty() && r.warnings.isEmpty())
        assertEquals(listOf("Weekday morning", "Every day", "Launch"), r.groups.map { it.name })
        assertEquals(listOf(1, 2, 3, 4, 5), r.groups[0].days)
        assertEquals("WEEKLY", r.groups[0].repeatType)
        assertEquals("ONCE", r.groups[2].repeatType)
        assertEquals("2026-12-01", r.groups[2].onceDate)
        assertNull(r.groups[1].items[1].time)
        assertFalse(r.groups[1].items[2].isRequired)
        assertEquals(6, r.itemCount)
        assertFalse(r.isEdit)
    }

    @Test fun sundayIsZero() {
        assertEquals(listOf(0, 6), RoutineImport.parse("## W | DAYS: SUN SAT\n08:00 | OTHER | x").groups[0].days)
    }

    @Test fun chattyOutputWithFenceAndTextAroundIt() {
        val r = RoutineImport.parse("Here you go! I fixed the gym time.\n```\n$clean\n```\nLet me know.")
        assertEquals(6, r.itemCount)
        assertTrue(r.problems.isEmpty())
    }

    @Test fun onlyTheLastRoutineIsReadWhenTheWholeChatIsPasted() {
        val r = RoutineImport.parse("SYNCWELL ROUTINE v1\n## Old | DAILY\n07:00 | OTHER | Draft\nEND\nmore chat\n$clean")
        assertEquals(3, r.groups.size)
        assertTrue(r.warnings.contains("Found 2 routines, used the last one."))
    }

    @Test fun missingHeaderStillParsesWithAWarning() {
        val r = RoutineImport.parse("## A | DAILY\n07:00 | DIET | Breakfast")
        assertEquals(1, r.itemCount)
        assertEquals(1, r.warnings.size)
    }

    @Test fun editMarkerPreselectsReplace() {
        assertTrue(RoutineImport.parse("SYNCWELL ROUTINE v1 EDIT\n## A | DAILY\n07:00 | DIET | Breakfast\nEND").isEdit)
    }

    @Test fun timesInTwelveHourForm() {
        assertEquals("07:00", RoutineImport.parseTime("7:00"))
        assertEquals("07:00", RoutineImport.parseTime("7 am"))
        assertEquals("19:30", RoutineImport.parseTime("7:30 PM"))
        assertEquals("00:15", RoutineImport.parseTime("12:15 am"))
        assertEquals("12:00", RoutineImport.parseTime("12pm"))
        assertNull(RoutineImport.parseTime("25:00"))
        assertNull(RoutineImport.parseTime("13 pm"))
        assertNull(RoutineImport.parseTime("7")) // a bare number is too vague
        assertNull(RoutineImport.parseTime("morning"))
    }

    @Test fun caseIsIgnoredAndUnknownCategoryBecomesOther() {
        val r = RoutineImport.parse("SYNCWELL ROUTINE v1\n## a | days: mon\n7:00 | diet | Eat\n8:00 | MEDITATION | Sit")
        assertEquals(listOf(1), r.groups[0].days)
        assertEquals("DIET", r.groups[0].items[0].category)
        assertEquals("OTHER", r.groups[0].items[1].category)
        assertTrue(r.groups[0].items[1].unknownCategory)
    }

    @Test fun itemBeforeAnyGroupGoesIntoMyRoutine() {
        val g = RoutineImport.parse("07:00 | DIET | Breakfast").groups.single()
        assertEquals("My routine", g.name)
        assertEquals("DAILY", g.repeatType)
    }

    @Test fun brokenLinesAreSkippedAndListedWithTheirNumber() {
        val r = RoutineImport.parse("SYNCWELL ROUTINE v1\n## A | DAILY\nnonsense\n25:00 | DIET | Late\n07:00 | DIET |\n08:00 | DIET | Fine")
        assertEquals(1, r.itemCount)
        assertEquals(listOf(3, 4, 5), r.problems.map { it.line })
    }

    @Test fun badGroupLineSkipsItsItemsButNotTheNextGroup() {
        val r = RoutineImport.parse("SYNCWELL ROUTINE v1\n## A | DAYS: FUNDAY\n07:00 | DIET | Lost\n## B | DAILY\n08:00 | DIET | Kept")
        assertEquals(listOf("B"), r.groups.map { it.name })
        assertEquals(2, r.problems.size)
    }

    @Test fun badOnceDateIsRejected() {
        assertTrue(RoutineImport.parse("## A | ONCE: 2026-02-30\n07:00 | DIET | x").groups.isEmpty())
    }

    @Test fun emptyPasteGivesNothingToSave() {
        assertFalse(RoutineImport.parse("").canSave)
        assertFalse(RoutineImport.parse("just some chat").canSave)
    }

    @Test fun limitsStopABadPaste() {
        assertTrue(RoutineImport.parse("x".repeat(RoutineImport.MAX_CHARS + 1)).problems.isNotEmpty())
        val manyItems = "## A | DAILY\n" + (1..200).joinToString("\n") { "07:00 | DIET | Item $it" }
        assertEquals(RoutineImport.MAX_ITEMS, RoutineImport.parse(manyItems).itemCount)
        val manyGroups = (1..30).joinToString("\n") { "## G$it | DAILY\n07:00 | DIET | x" }
        assertEquals(RoutineImport.MAX_GROUPS, RoutineImport.parse(manyGroups).groups.size)
        assertTrue(RoutineImport.parse("## A | DAILY\n07:00 | DIET | ${"t".repeat(61)}").problems.isNotEmpty())
    }

    @Test fun roundTripKeepsEverything() {
        val first = RoutineImport.parse(clean)
        val (templates, items) = RoutineImport.toEntities(first.groups)
        assertTrue(templates.none { it.isStarter })
        assertEquals("[1,2,3,4,5]", templates[0].daysOfWeek)
        val text = RoutineImport.format(templates, items)
        val again = RoutineImport.parse(text)
        assertTrue(again.isEdit)
        assertEquals(first.groups, again.groups)
        val (t2, i2) = RoutineImport.toEntities(again.groups)
        assertEquals(text, RoutineImport.format(t2, i2))
    }

    @Test fun pipesInTitlesCannotBreakTheFormat() {
        val (t, i) = RoutineImport.toEntities(listOf(ImportedGroup("A|B", "DAILY", items = listOf(ImportedItem("07:00", "DIET", "x | y")))))
        val g = RoutineImport.parse(RoutineImport.format(t, i)).groups.single()
        assertEquals("A/B", g.name)
        assertEquals("x / y", g.items.single().title)
    }

    @Test fun supplementLineIsInsertedAfterTheDietRuleOnly() {
        val plain = RoutineImport.prompt()
        assertEquals(plain, RoutineImport.prompt(listOf(" ", "")))
        val with = RoutineImport.prompt(listOf("Vit D3", "Omega 3", "Vit D3"))
        val lines = with.lines()
        val at = lines.indexOfFirst { it.startsWith("- Meals and snacks are DIET.") }
        assertEquals("- My supplements are named exactly: Vit D3, Omega 3. Use these exact names for SUPPLEMENT tasks.", lines[at + 1])
        assertEquals(plain.lines().size + 1, lines.size)
    }

    @Test fun exportEndsWithTheEditRoutineAndTheChangeLine() {
        val (t, i) = RoutineImport.toEntities(RoutineImport.parse(clean).groups)
        val out = RoutineImport.exportPrompt(emptyList(), t, i)
        assertTrue(out.startsWith(RoutineImport.prompt() + "\nSYNCWELL ROUTINE v1 EDIT\n"))
        assertTrue(out.endsWith("\nEND\n\nThe routine above is my current routine. I want to change it as described below.\nRun phase 1 on the whole routine again, including my change, and ask me about anything unclear.\nIn phase 2, return the WHOLE updated routine, not only the changed lines, and make the first line inside the code block SYNCWELL ROUTINE v1 EDIT instead of SYNCWELL ROUTINE v1.\n\nMy change:\n"))
    }

    @Test fun promptMatchesThePlanDocument() {
        val doc = File("../docs/FEATURE_PLAN_ROUTINE_IMPORT.md")
        assumeTrue(doc.exists()) // docs/ is not tracked in git
        val text = doc.readText().replace("\r\n", "\n")
        val block = text.substringAfter("### Exact import prompt").substringAfter("```text\n").substringBefore("\n```\n")
        assertEquals(block.trimEnd(), RoutineImport.prompt().trimEnd())
    }
}
