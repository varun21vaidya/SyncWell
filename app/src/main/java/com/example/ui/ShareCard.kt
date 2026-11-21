package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.example.domain.WeekComparison
import java.io.File

/** Draws the week summary as an image and offers it to the system share sheet. Runs only on an explicit tap. */
object ShareCard {
    private const val W = 1080
    private const val H = 1350

    fun render(c: WeekComparison, streak: Int): Bitmap {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.parseColor("#0B0D12"))
        val card = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#161A23") }
        canvas.drawRoundRect(RectF(60f, 60f, W - 60f, H - 60f), 48f, 48f, card)

        fun text(s: String, x: Float, y: Float, size: Float, color: String, bold: Boolean = false) {
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = Color.parseColor(color); textSize = size
                typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            }
            canvas.drawText(s, x, y, p)
        }
        text("SyncWell", 120f, 180f, 56f, "#34D399", bold = true)
        text("My week", 120f, 270f, 96f, "#FFFFFF", bold = true)
        text("${Math.round(c.thisWeek.avgPercent * 100)}%", 120f, 520f, 240f, "#34D399", bold = true)
        text("average routine", 120f, 600f, 52f, "#9AA3B2")
        text("${c.thisWeek.qualifyingDays} good days", 120f, 780f, 72f, "#FFFFFF", bold = true)
        text("${c.thisWeek.workouts} workouts", 120f, 880f, 72f, "#FFFFFF", bold = true)
        text("${c.thisWeek.focusMinutes / 60}h ${c.thisWeek.focusMinutes % 60}m focus", 120f, 980f, 72f, "#FFFFFF", bold = true)
        if (streak > 0) text("$streak-day streak", 120f, 1150f, 80f, "#F59E0B", bold = true)
        return bmp
    }

    fun share(context: Context, c: WeekComparison, streak: Int) {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, "my-week.png")
        file.outputStream().use { render(c, streak).compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Share my week").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
