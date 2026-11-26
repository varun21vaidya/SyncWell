package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RegionChecklistTest {
    private val shipped = JourneyMap.parse(File("src/main/res/raw/adventure_route.json").readText())
    private val warden = Warden("W", "p", "c", "o", "x")

    private fun region(index: Int = 1, steps: Int = 7, checkpoint: Int? = 3, image: String? = "img", release: String = "2026-11-01", intro: String = "hi", w: Warden = warden) =
        RegionDef(index, "R", image, (1..steps).map { MapPoint(it, 0.1f * it % 1f, 0.5f, it == checkpoint) }, null, intro, release, w)

    @Test fun theBundledRouteHasNoBlockingIssues() {
        val blocking = RegionChecklist.check(shipped).filter { it.blocking }
        assertTrue(blocking.joinToString("\n"), blocking.isEmpty())
    }

    @Test fun theSummitIsTheOnlyRegionStillWithoutPaintedArt() {
        // when the Summit art lands, this test is the reminder to delete the placeholder flag from the data
        assertEquals(listOf("Summit"), RegionChecklist.check(shipped).filter { !it.blocking }.map { it.region })
    }

    @Test fun releaseDatesNeverGoBackwards() {
        assertEquals(shipped.map { it.releaseDate }, shipped.map { it.releaseDate }.sorted())
    }

    @Test fun aCompleteRegionPasses() = assertTrue(RegionChecklist.check(listOf(region())).isEmpty())

    @Test fun eachMissingPartIsReported() {
        fun issues(r: RegionDef) = RegionChecklist.check(listOf(r)).map { it.issue }
        assertTrue(issues(region(steps = 6)).any { it.contains("needs 7") })
        assertTrue(issues(region(checkpoint = null)).any { it.contains("checkpoint") })
        assertTrue(issues(region(checkpoint = 4)).any { it.contains("checkpoint") })
        assertTrue(issues(region(intro = " ")).contains("has no intro line"))
        assertTrue(issues(region(w = warden.copy(open = ""))).contains("has an incomplete Warden"))
        assertTrue(issues(region(release = "soon")).contains("has no valid release date"))
        assertTrue(issues(region(image = null)).single { it.contains("painted") }.isNotEmpty())
    }

    @Test fun missingArtDoesNotBlockButAnythingElseDoes() {
        val issues = RegionChecklist.check(listOf(region(image = null)))
        assertTrue(issues.none { it.blocking })
        assertTrue(RegionChecklist.check(listOf(region(intro = ""))).all { it.blocking })
    }

    @Test fun aRegionCannotShipBeforeTheOneAheadOfIt() {
        val a = region(index = 1, release = "2026-12-06")
        val b = region(index = 2, steps = 14, checkpoint = 7, release = "2026-11-29")
        assertTrue(RegionChecklist.check(listOf(a, b)).any { it.issue.contains("before the region ahead") })
    }
}
