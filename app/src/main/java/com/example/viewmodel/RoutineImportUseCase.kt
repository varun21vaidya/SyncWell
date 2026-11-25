package com.example.viewmodel

import androidx.room.withTransaction
import com.example.data.AppDatabase
import com.example.data.SyncWellDao
import com.example.domain.ImportedGroup
import com.example.domain.RoutineImport

/**
 * Saves an imported routine in one transaction, so a kill halfway cannot leave the user with no routine.
 * Replace clears every template first (items go with them through the foreign-key cascade); Add only inserts.
 * Past days (`daily_routines`) are never touched.
 */
class RoutineImportUseCase(private val db: AppDatabase, private val dao: SyncWellDao) {

    /** Returns how many groups and items were saved. */
    suspend operator fun invoke(groups: List<ImportedGroup>, replace: Boolean): Pair<Int, Int> {
        val (templates, items) = RoutineImport.toEntities(groups)
        db.withTransaction {
            if (replace) dao.clearRoutineTemplates()
            templates.forEach { dao.insertRoutineTemplate(it) }
            dao.insertRoutineTemplateItems(items)
        }
        return templates.size to items.size
    }
}
