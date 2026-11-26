package com.example.domain

/** The optional "why" behind the Journey: an ambition ("Land a backend role") and an identity line ("I'm becoming someone who practises daily"). */
object Ambition {
    const val MAX_AMBITION = 80
    const val MAX_IDENTITY = 100

    /** Trimmed and cut to [max]; null when nothing is left, so an empty field means "not set". */
    fun clean(text: String?, max: Int): String? = text?.trim()?.take(max)?.trimEnd()?.ifEmpty { null }
}
