package com.example.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

object RoutineSchedule {

    /** "[1,2,3]" -> [1,2,3]; 0 = Sunday. */
    fun parseDays(s: String): List<Int> = Regex("\\d").findAll(s).map { it.value.toInt() }.toList()

    fun dayOfWeek(dateKey: String): Int {
        val cal = Calendar.getInstance()
        cal.time = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateKey)!!
        return cal.get(Calendar.DAY_OF_WEEK) - 1
    }

    fun templatesForDate(templates: List<RoutineTemplate>, dateKey: String): List<RoutineTemplate> =
        templates.filter {
            it.isActive && when (it.repeatType) {
                "DAILY" -> true
                "ONCE" -> it.onceDate == dateKey
                else -> dayOfWeek(dateKey) in parseDays(it.daysOfWeek)
            }
        }

    private fun minutes(t: String) = t.take(2).toInt() * 60 + t.takeLast(2).toInt()

    /** Merges the items of every matching template into one list ordered from [wakeTime], so 00:30 comes after 23:00. */
    fun buildDayItems(routineId: String, items: List<RoutineTemplateItem>, doneTitles: Set<String> = emptySet(), wakeTime: String = "00:00"): List<DailyRoutineItem> =
        items.sortedWith(compareBy({ t -> t.scheduledTime?.let { (minutes(it) - minutes(wakeTime) + 1440) % 1440 } ?: Int.MAX_VALUE }, { it.orderIndex }))
            .mapIndexed { i, t ->
                DailyRoutineItem(UUID.randomUUID().toString(), routineId, t.title, t.category, i + 1, t.scheduledTime, t.title in doneTitles)
            }
}
