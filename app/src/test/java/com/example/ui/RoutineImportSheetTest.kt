package com.example.ui

import com.example.domain.RoutineImport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineImportSheetTest {
    private val result = RoutineImport.parse("SYNCWELL ROUTINE v1\n## Weekday morning | DAYS: MON TUE WED THU FRI\n07:00 | DIET | Breakfast\n## Every day | DAILY\n22:00 | SKINCARE | Night")

    @Test fun groupSummaryUsesTheEditorWording() {
        assertEquals("Mon, Tue, Wed, Thu, Fri", result.groups[0].summary())
        assertEquals("Every day", result.groups[1].summary())
    }

    @Test fun existingGroupNamesAreWarnedAboutIgnoringCase() {
        assertEquals(listOf("You already have a group called Every day."), duplicateGroupWarnings(result, listOf("every day", "Other")))
        assertTrue(duplicateGroupWarnings(result, emptyList()).isEmpty())
    }
}
