package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class) // org.json is only real under Robolectric
@Config(sdk = [34])
class JourneyMapTest {
    // the shipped data file, so the test fails when the data and the rules drift apart
    private val regions = JourneyMap.parse(File("src/main/res/raw/adventure_route.json").readText())

    @Test fun theShippedRouteMatchesTheRulesStepForStep() {
        assertEquals((0..JourneyRoute.SUMMIT_REGION).map { JourneyRoute.stepsIn(it) }, regions.take(6).map { it.steps })
        assertEquals(JourneyRoute.SUMMIT_STEPS, regions.take(6).sumOf { it.steps })
    }

    @Test fun checkpointsMatchTheRoute() {
        regions.take(6).forEach { r ->
            assertEquals("region ${r.name}", JourneyRoute.checkpointOf(r.index), r.points.firstOrNull { it.checkpoint }?.number)
        }
    }

    @Test fun everyPointIsOnTheImageAndNumberedInOrder() {
        regions.forEach { r ->
            assertEquals((1..r.steps).toList(), r.points.map { it.number })
            assertTrue(r.points.all { it.x in 0f..1f && it.y in 0f..1f })
        }
    }

    @Test fun everyPaintedRegionHasItsImageFile() {
        regions.filter { it.image != null }.forEach { assertTrue(it.image, File("src/main/res/drawable-nodpi/${it.image}.webp").exists()) }
        assertNull(regions.last().image) // the Summit art is not painted yet
        assertEquals("Summit", regions.last().name)
    }

    @Test fun theSpriteStandsOnTheStepPointAndStartsBelowTheFirstOne() {
        val r = regions[1]
        assertEquals(r.points[2].x to r.points[2].y, JourneyMap.positionOf(r, 3))
        val start = JourneyMap.positionOf(r, 0)
        assertEquals(r.points[0].x, start.first, 0f)
        assertTrue(start.second > r.points[0].y && start.second <= 1f)
        assertEquals(JourneyMap.positionOf(r, r.steps), JourneyMap.positionOf(r, r.steps + 5)) // never past the gate
    }

    @Test fun regionsShowClearOnlyWhenReachedAndReleased() {
        val today = "2026-11-26"
        assertEquals(RegionView.CURRENT, JourneyMap.view(regions[2], 2, 2, today))
        assertEquals(RegionView.DONE, JourneyMap.view(regions[1], 2, 2, today))
        assertEquals(RegionView.LOCKED, JourneyMap.view(regions[3], 2, 2, today)) // not reached
        assertEquals(RegionView.LOCKED, JourneyMap.view(regions[5], 5, 5, today)) // reached but not released yet
        assertEquals(RegionView.CURRENT, JourneyMap.view(regions[5], 5, 5, "2026-12-06"))
    }

    @Test fun dayWordsNeverSayStep() {
        val r = regions[2]
        assertEquals("Day 9 of 14", JourneyMap.dayLabel(r, 9))
        assertEquals("Camp on day 7", JourneyMap.nextMilestone(r, 3))
        assertEquals("Gate on day 14", JourneyMap.nextMilestone(r, 9))
        assertNull(JourneyMap.nextMilestone(r, 14))
        assertEquals("Gate on day 3", JourneyMap.nextMilestone(regions[0], 1)) // Home Start has no camp
    }

    @Test fun charactersAreSixAndAnUnknownIdFallsBackToMax() {
        assertEquals(listOf("Max", "Ivy", "Lee", "Emma", "Charles", "Georgia"), JourneyCharacters.all.map { it.name })
        assertEquals("max", JourneyCharacters.byId("nobody").id)
        assertEquals("ivy", JourneyCharacters.byId("ivy").id)
    }

    @Test fun weatherComesFromTheRegionTag() {
        assertEquals(Weather.SUN, JourneyMap.weatherOf(regions[0]))
        assertEquals(Weather.RAIN, JourneyMap.weatherOf(regions[3]))
        assertEquals(Weather.SNOW, JourneyMap.weatherOf(regions[4]))
        assertEquals(Weather.SUN, JourneyMap.weatherOf(null))
    }
}

