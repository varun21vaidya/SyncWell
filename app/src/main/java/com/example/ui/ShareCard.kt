package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.FileProvider
import com.example.domain.Badge
import com.example.domain.BadgeRules
import com.example.domain.WeekComparison
import java.io.File
import androidx.compose.ui.graphics.Canvas as ComposeCanvas

/** Draws a summary as an image and offers it to the system share sheet. Runs only on an explicit tap. */
object ShareCard {
    internal const val W = 1080
    internal const val H = 1350

    internal fun base(): Pair<Bitmap, Canvas> {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.parseColor("#05070D"))
        val card = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0D1322") }
        canvas.drawRoundRect(RectF(60f, 60f, W - 60f, H - 60f), 48f, 48f, card)
        return bmp to canvas
    }

    internal fun Canvas.text(s: String, x: Float, y: Float, size: Float, color: String, bold: Boolean = false, center: Boolean = false) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.parseColor(color); textSize = size
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            if (center) textAlign = Paint.Align.CENTER
        }
        drawText(s, x, y, p)
    }

    fun render(c: WeekComparison, streak: Int): Bitmap {
        val (bmp, canvas) = base()
        canvas.text("SyncWell", 120f, 180f, 56f, "#3DDC97", bold = true)
        canvas.text("My week", 120f, 270f, 96f, "#E8EEF7", bold = true)
        canvas.text("${Math.round(c.thisWeek.avgPercent * 100)}%", 120f, 520f, 240f, "#3DDC97", bold = true)
        canvas.text("average routine", 120f, 600f, 52f, "#9AA8BF")
        canvas.text("${c.thisWeek.qualifyingDays} good days", 120f, 780f, 72f, "#E8EEF7", bold = true)
        canvas.text("${c.thisWeek.workouts} workouts", 120f, 880f, 72f, "#E8EEF7", bold = true)
        canvas.text("${c.thisWeek.focusMinutes / 60}h ${c.thisWeek.focusMinutes % 60}m focus", 120f, 980f, 72f, "#E8EEF7", bold = true)
        if (streak > 0) canvas.text("$streak-day streak", 120f, 1150f, 80f, "#FFC857", bold = true)
        return bmp
    }

    /** The medal itself, drawn by the same code as the app, with its name. */
    fun renderBadge(badge: Badge): Bitmap {
        val (bmp, canvas) = base()
        canvas.text("SyncWell", W / 2f, 200f, 56f, "#3DDC97", bold = true, center = true)
        canvas.text(BadgeRules.stampTitle(badge), W / 2f, 300f, 76f, "#E8EEF7", bold = true, center = true)
        val medal = 600f
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, ComposeCanvas(canvas), Size(medal, medal)) {
            translate((W - medal) / 2f, 380f) { drawMedal(badge, MedalPaths(), 0.3f) }
        }
        canvas.text(BadgeRules.stampBody(badge), W / 2f, 1100f, 44f, "#9AA8BF", center = true)
        return bmp
    }

    fun share(context: Context, c: WeekComparison, streak: Int) = send(context, render(c, streak), "my-week.png", "Share my week")

    fun shareBadge(context: Context, badge: Badge) = send(context, renderBadge(badge), "my-medal.png", "Share my medal")

    internal fun send(context: Context, bmp: Bitmap, fileName: String, chooserTitle: String) {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, fileName)
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
