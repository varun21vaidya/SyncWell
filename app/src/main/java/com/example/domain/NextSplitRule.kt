package com.example.domain

enum class Split(val key: String, val title: String, val muscles: String) {
    PUSH("PUSH", "Push Day", "Chest + Shoulders + Triceps"),
    PULL("PULL", "Pull Day", "Back + Biceps + Rear Delts"),
    LEGS("LEGS", "Legs + Abs Day", "Thighs + Calves + Core");

    /** The label stored as the session's dayType, as the start screen has always written it. */
    val label: String get() = "$title — $muscles"
}

object NextSplitRule {
    /** Maps a stored dayType (any of the labels written so far) to its split, or null when unknown. */
    fun splitOf(dayType: String): Split? =
        Split.values().firstOrNull { dayType.contains(it.key, ignoreCase = true) || dayType.contains(it.title.substringBefore(' '), ignoreCase = true) }

    /**
     * The split after the one done most recently. [finishedDayTypes] is newest first.
     * A fresh install (or an unreadable history) starts at Push.
     */
    fun next(finishedDayTypes: List<String>): Split {
        val last = finishedDayTypes.firstNotNullOfOrNull { splitOf(it) } ?: return Split.PUSH
        return Split.values()[(last.ordinal + 1) % Split.values().size]
    }

    /** Date key of the most recent finished session of each split. [sessions] is (dayType, dateKey) in any order. */
    fun lastDone(sessions: List<Pair<String, String>>): Map<Split, String> =
        sessions.mapNotNull { (type, date) -> splitOf(type)?.let { it to date } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, dates) -> dates.max() }
}
