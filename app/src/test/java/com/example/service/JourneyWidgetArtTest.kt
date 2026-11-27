package com.example.service

import androidx.test.core.app.ApplicationProvider
import com.example.domain.JourneyCharacters
import com.example.domain.JourneyMap
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JourneyWidgetArtTest {
    private val regions = JourneyMap.parse(File("src/main/res/raw/adventure_route.json").readText())
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test fun thePaintedRegionsAndThePlaceholderAllGiveAWidgetSizedBitmap() {
        regions.forEach { r ->
            val bmp = JourneyWidgetArt.render(context, r, 2, JourneyCharacters.byId("ivy"))
            assertEquals(r.name, JourneyWidgetArt.WIDTH, bmp.width)
            assertEquals(r.name, JourneyWidgetArt.HEIGHT, bmp.height)
        }
    }

    @Test fun theStartAndTheGateDrawWithoutLeavingTheImage() {
        val r = regions[2]
        listOf(0, 1, r.steps, r.steps + 3).forEach { step -> JourneyWidgetArt.render(context, r, step, JourneyCharacters.byId("max")).recycle() }
    }

    @Test fun theSmallBitmapStaysWellUnderTheWidgetLimit() {
        val bytes = JourneyWidgetArt.WIDTH * JourneyWidgetArt.HEIGHT * 4
        assertEquals(true, bytes < 1_000_000) // RemoteViews bitmaps are capped by the screen size, so keep it small
    }
}
