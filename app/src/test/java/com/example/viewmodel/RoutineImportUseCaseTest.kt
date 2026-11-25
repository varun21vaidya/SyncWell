package com.example.viewmodel

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.RoutineTemplate
import com.example.data.RoutineTemplateItem
import com.example.data.SyncWellDao
import com.example.domain.RoutineImport
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoutineImportUseCaseTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: SyncWellDao

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).allowMainThreadQueries().build()
        dao = db.syncWellDao()
    }

    @After fun tearDown() = db.close()

    private val groups = RoutineImport.parse("SYNCWELL ROUTINE v1\n## New | DAYS: MON WED\n07:00 | DIET | Breakfast\n08:00 | STUDY | Focus\nEND").groups

    private fun seedOld() = runBlocking {
        dao.insertRoutineTemplate(RoutineTemplate("old", "Old", isStarter = true))
        dao.insertRoutineTemplateItems(listOf(RoutineTemplateItem("oi", "old", "Old item", "OTHER", 1, null)))
    }

    @Test fun addKeepsTheOldRoutineAndInsertsTheNewOne() = runBlocking {
        seedOld()
        assertEquals(1 to 2, RoutineImportUseCase(db, dao)(groups, replace = false))
        assertEquals(setOf("Old", "New"), dao.getRoutineTemplates().map { it.name }.toSet())
        assertEquals(3, dao.getAllRoutineTemplateItems().size)
    }

    @Test fun replaceSwapsEverythingAndImportedGroupsAreNotStarters() = runBlocking {
        seedOld()
        RoutineImportUseCase(db, dao)(groups, replace = true)
        val t = dao.getRoutineTemplates().single()
        assertEquals("New", t.name)
        assertEquals("[1,3]", t.daysOfWeek)
        assertTrue(!t.isStarter)
        assertEquals(listOf("Breakfast", "Focus"), dao.getAllRoutineTemplateItems().sortedBy { it.orderIndex }.map { it.title })
    }
}
