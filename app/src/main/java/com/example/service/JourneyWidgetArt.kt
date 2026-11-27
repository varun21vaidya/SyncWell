package com.example.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import com.example.domain.JourneyCharacter
import com.example.domain.JourneyMap
import com.example.domain.RegionDef

/** The widget's map: a crop of the region image around the character, with the character drawn on it. */
object JourneyWidgetArt {
    const val WIDTH = 360
    const val HEIGHT = 220

    fun render(context: Context, region: RegionDef, step: Int, character: JourneyCharacter): Bitmap {
        val out = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val (x, y) = JourneyMap.positionOf(region, step)
        val source = decode(context, region.image)
        // A window as wide as the image whose height matches the widget's proportions
        val ch = source?.let { (it.width * HEIGHT.toFloat() / WIDTH) / it.height } ?: 0.5f
        val crop = JourneyMap.cropAround(x, y, 1f, ch)
        if (source != null) {
            val from = Rect((crop[0] * source.width).toInt(), (crop[1] * source.height).toInt(), (crop[2] * source.width).toInt(), (crop[3] * source.height).toInt())
            canvas.drawBitmap(source, from, Rect(0, 0, WIDTH, HEIGHT), Paint(Paint.FILTER_BITMAP_FLAG))
            source.recycle()
        } else {
            // A region with no painted image yet: a dark gradient, like the placeholder on the Journey screen
            canvas.drawPaint(Paint().apply { shader = LinearGradient(0f, 0f, 0f, HEIGHT.toFloat(), Color.parseColor("#1A2236"), Color.parseColor("#0D1322"), Shader.TileMode.CLAMP) })
        }
        val cx = (x - crop[0]) / (crop[2] - crop[0]) * WIDTH
        val cy = (y - crop[1]) / (crop[3] - crop[1]) * HEIGHT
        canvas.drawCircle(cx, cy - 22f, 22f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = character.color.toInt() })
        canvas.drawText(character.name.take(1), cx, cy - 12f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#05070D"); textSize = 26f; textAlign = Paint.Align.CENTER; isFakeBoldText = true
        })
        return out
    }

    /** The painted image at half size: a widget bitmap has to stay small. Null when the region has no image or it cannot be read. */
    private fun decode(context: Context, name: String?): Bitmap? {
        if (name == null) return null
        val id = context.resources.getIdentifier(name, "drawable", context.packageName).takeIf { it != 0 } ?: return null
        return runCatching { BitmapFactory.decodeResource(context.resources, id, BitmapFactory.Options().apply { inSampleSize = 2 }) }.getOrNull()
    }
}
