package com.example.ui

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Guards the copy sweep: no ALL-CAPS labels and none of the old negative or shouty words in the screens. */
class CopyStyleTest {
    private val sources: List<File> by lazy {
        (File("src/main/java/com/example/ui").walkTopDown().filter { it.extension == "kt" } + File("src/main/java/com/example/MainActivity.kt")).toList()
    }

    private val allowed = setOf("OLED", "PR", "BMI", "AM", "PM", "RPE", "HIIT", "OK", "GPS", "JSON", "PDF", "CSV", "HR", "KG", "UI", "ID", "MTWTFSS")
    private val caps = Regex("""(?:\bText|\bSectionLabel|\bStatsCard)\(\s*(?:text\s*=\s*)?"([A-Z][A-Z0-9 &'/,.\-:!?·+()%]*[A-Z0-9!?.)])"""")

    @Test fun sourcesAreFound() = assertTrue(sources.size > 10)

    @Test fun noAllCapsLabelsInTextCalls() {
        val offenders = sources.flatMap { f ->
            caps.findAll(f.readText()).map { it.groupValues[1] }
                .filter { s -> val letters = s.filter { c -> c.isLetter() }; letters.length >= 3 && s == s.uppercase() && s.split(' ').any { w -> w.filter { c -> c.isLetter() } !in allowed && w.any { c -> c.isLetter() } } }
                .map { "${f.name}: $it" }
        }
        assertTrue("ALL-CAPS labels left: $offenders", offenders.isEmpty())
    }

    @Test fun oldShoutyAndNegativeWordsAreGone() {
        val banned = listOf("\"NICE\"", "\"DISMISS\"", "\"Locked\"", "\"LOCKED\"", "Executive Statistics", "SyncWell Control", "EXECUTIVE OVERVIEW")
        val hits = sources.flatMap { f -> val t = f.readText(); banned.filter { it in t }.map { "${f.name}: $it" } }
        assertTrue("Old copy left: $hits", hits.isEmpty())
    }
}
