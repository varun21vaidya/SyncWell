package com.example.service

import com.example.data.RoutineSchedule
import com.example.data.SyncWellDao

/** How many routine items are ticked and how many there are today, shared by every reminder. */
object RoutineProgress {
    /** (done, total). Before the app is opened on a day there is no list yet, so the total comes from the schedule. */
    suspend fun counts(dao: SyncWellDao, today: String): Pair<Int, Int> {
        val routine = dao.getDailyRoutineDirectly(today)
        val items = routine?.let { dao.getDailyRoutineItems(it.id) } ?: emptyList()
        val total = if (items.isNotEmpty()) items.size
        else RoutineSchedule.templatesForDate(dao.getRoutineTemplates(), today).sumOf { dao.getRoutineTemplateItems(it.id).size }
        return items.count { it.isDone } to total
    }
}
