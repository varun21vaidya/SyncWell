package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import com.example.domain.ShareLines

/** Draws the Journey, Promise and Ambition cards as PNGs and offers them to the share sheet, on the same card as the week and medal cards. Runs only on a tap. */
object JourneyShareCards {
    private const val LEFT = 120f
    private val TEXT_WIDTH = ShareCard.W - 2 * LEFT

    fun render(card: ShareLines): Bitmap = with(ShareCard) {
        val (bmp, canvas) = base()
        canvas.text("SyncWell", LEFT, 180f, 56f, "#3DDC97", bold = true)
        canvas.text(card.title, LEFT, 290f, 96f, "#E8EEF7", bold = true)
        var y = 460f
        card.lines.forEachIndexed { i, line ->
            val big = i == 0
            val size = if (big) 76f else 56f
            for (part in wrapOnCard(line, size, big)) {
                canvas.text(part, LEFT, y, size, if (big) "#FFC857" else "#E8EEF7", bold = big)
                y += size * 1.3f
            }
            y += 28f
        }
        canvas.text(card.footer, LEFT, H - 150f, 38f, "#7A89A2")
        bmp
    }

    fun share(context: Context, card: ShareLines, fileName: String, chooserTitle: String) = ShareCard.send(context, render(card), fileName, chooserTitle)

    /**
     * Breaks [text] into lines no wider than [width] by the given [measure]; a very long word is cut rather than run off the edge.
     * The measuring function is a parameter so the logic can be tested without a real font.
     */
    internal fun wrap(text: String, width: Float, measure: (String) -> Float): List<String> {
        val out = mutableListOf<String>()
        var line = ""
        for (word in text.split(" ")) {
            val next = if (line.isEmpty()) word else "$line $word"
            if (measure(next) <= width) { line = next; continue }
            if (line.isNotEmpty()) out += line
            line = word
            while (measure(line) > width && line.length > 1) { // one word wider than the card
                val fit = (line.length downTo 1).firstOrNull { measure(line.take(it)) <= width } ?: 1
                out += line.take(fit); line = line.drop(fit)
            }
        }
        if (line.isNotEmpty()) out += line
        return out.ifEmpty { listOf("") }
    }

    private fun wrapOnCard(text: String, size: Float, bold: Boolean): List<String> {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = size; isFakeBoldText = bold }
        return wrap(text, TEXT_WIDTH) { paint.measureText(it) }
    }
}
