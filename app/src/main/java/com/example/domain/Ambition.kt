package com.example.domain

import com.example.data.JourneyState
import java.text.SimpleDateFormat
import java.util.Locale

/** The optional "why" behind the Journey: an ambition ("Land a backend role") and an identity line ("I'm becoming someone who practises daily"). */
object Ambition {
    const val MAX_AMBITION = 80
    const val MAX_IDENTITY = 100
    const val MAX_REWARD = 60
    private val DATE = Regex("^\\d{4}-\\d{2}-\\d{2}$")

    /** Trimmed and cut to [max]; null when nothing is left, so an empty field means "not set". */
    fun clean(text: String?, max: Int): String? = text?.trim()?.take(max)?.trimEnd()?.ifEmpty { null }

    /** A target date as "yyyy-MM-dd" that is a real calendar date and not before [today]; null otherwise (empty means no date). */
    fun cleanDate(text: String?, today: String): String? =
        text?.trim()?.takeIf { DATE.matches(it) && it >= today && isRealDate(it) }

    private fun isRealDate(d: String) =
        runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(d) != null }.getOrDefault(false)

    enum class DateStatus { EMPTY, VALID, KEPT, INVALID }

    /** What the date field holds: nothing, a good new date, the saved date left as it was (even if it has passed), or something to fix. */
    fun dateStatus(text: String, saved: String?, today: String): DateStatus = when {
        text.isBlank() -> DateStatus.EMPTY
        text.trim() == saved -> DateStatus.KEPT
        cleanDate(text, today) != null -> DateStatus.VALID
        else -> DateStatus.INVALID
    }
}

/** The favourite thing the user gives up until the ambition is reached, and when it comes back. Honor system. */
sealed class BigRewardRule {
    /** Unlocks on the target date: the date is under 66 days from the start. */
    data class OnDate(val date: String) : BigRewardRule()

    /** Unlocks at the Summit: the target date is 66 days or more away, so the trail itself is the measure. */
    object AtSummit : BigRewardRule()
}

object BigReward {
    fun rule(ambitionStart: String, targetDate: String): BigRewardRule =
        if (WeekKey.range(ambitionStart, targetDate).size - 1 < JourneyRoute.SUMMIT_STEPS) BigRewardRule.OnDate(targetDate) else BigRewardRule.AtSummit

    /** The words shown beside the field, so the user knows when the thing comes back before they commit to it. */
    fun explain(ambitionStart: String, targetDate: String): String = when (val r = rule(ambitionStart, targetDate)) {
        is BigRewardRule.OnDate -> "It comes back on ${r.date}."
        BigRewardRule.AtSummit -> "It comes back when you reach the Summit."
    }

    /** True once the thing may come back: a reward and a date are set, and the rule's moment has arrived. */
    fun due(s: JourneyState, today: String, summitReached: Boolean): Boolean {
        val start = s.ambitionStart ?: return false
        val target = s.targetDate ?: return false
        if (s.bigRewardText == null) return false
        return when (val r = rule(start, target)) {
            is BigRewardRule.OnDate -> today >= r.date
            BigRewardRule.AtSummit -> summitReached
        }
    }
}
