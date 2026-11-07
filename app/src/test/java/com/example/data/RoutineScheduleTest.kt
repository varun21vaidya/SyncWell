package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineScheduleTest {
    // 2026-11-02 is a Monday, 2026-11-07 a Saturday, 2026-11-08 a Sunday
    private val daily = RoutineTemplate("d", "Daily", repeatType = "DAILY")
    private val weekdays = RoutineTemplate("w", "Weekdays", repeatType = "WEEKLY", daysOfWeek = "[1,2,3,4,5]")
    private val once = RoutineTemplate("o", "Once", repeatType = "ONCE", onceDate = "2026-11-07")
    private val all = listOf(daily, weekdays, once)

    private fun ids(date: String) = RoutineSchedule.templatesForDate(all, date).map { it.id }

    @Test fun dayOfWeekIsSundayZero() {
        assertEquals(1, RoutineSchedule.dayOfWeek("2026-11-02"))
        assertEquals(0, RoutineSchedule.dayOfWeek("2026-11-08"))
    }

    @Test fun mondayGetsDailyAndWeekdays() = assertEquals(listOf("d", "w"), ids("2026-11-02"))

    @Test fun saturdayGetsDailyAndOnceOnly() = assertEquals(listOf("d", "o"), ids("2026-11-07"))

    @Test fun sundayGetsOnlyDaily() = assertEquals(listOf("d"), ids("2026-11-08"))

    @Test fun inactiveTemplateIsSkipped() {
        assertTrue(RoutineSchedule.templatesForDate(listOf(daily.copy(isActive = false)), "2026-11-02").isEmpty())
    }

    @Test fun dayItemsAreMergedInTimeOrder() {
        val items = listOf(
            RoutineTemplateItem("1", "d", "Lunch", "DIET", 1, "12:00"),
            RoutineTemplateItem("2", "w", "Breakfast", "DIET", 1, "08:00"),
            RoutineTemplateItem("3", "w", "Anytime", "OTHER", 2, null)
        )
        val out = RoutineSchedule.buildDayItems("r", items, setOf("Breakfast"))
        assertEquals(listOf("Breakfast", "Lunch", "Anytime"), out.map { it.title })
        assertEquals(listOf(true, false, false), out.map { it.isDone })
    }

    @Test fun starterWorkoutOnlyOnChosenDaysAndVitaminsIncluded() {
        val (templates, items) = DatabaseSeeder.buildStarterTemplates(
            SetupAnswers("06:30", "22:30", listOf(2, 4), listOf(VitaminChoice("Magnesium"), VitaminChoice("Zinc", "10:00")))
        )
        val workout = templates.single { it.name == "Workout Days" }
        assertEquals(listOf(2, 4), RoutineSchedule.parseDays(workout.daysOfWeek))
        val supp = items.filter { it.category == "SUPPLEMENT" }
        assertEquals("21:45", supp.single { it.title == "Magnesium" }.scheduledTime)
        assertEquals("10:00", supp.single { it.title == "Zinc" }.scheduledTime)
        assertEquals("06:30", items.first { it.title == "Wake up & drink water" }.scheduledTime)
    }

    @Test fun noWorkoutDaysMeansNoWorkoutTemplate() {
        val (templates, _) = DatabaseSeeder.buildStarterTemplates(SetupAnswers(workoutDays = emptyList()))
        assertTrue(templates.none { it.name == "Workout Days" })
    }
}
