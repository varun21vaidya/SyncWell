package com.example.domain

data class SetRef(val weightKg: Double, val reps: Int)

/** One completed set with enough context to order it in time. [sortKey] sorts sessions oldest first. */
data class HistoricSet(val sessionId: String, val sortKey: String, val exerciseId: String, val weightKg: Double, val reps: Int)

object PersonalBestRule {
    /** A set beats another by heavier weight first, then by more reps at the same weight. */
    fun beats(a: SetRef, b: SetRef): Boolean = a.weightKg > b.weightKg || (a.weightKg == b.weightKg && a.reps > b.reps)

    fun best(sets: List<SetRef>): SetRef? = sets.reduceOrNull { a, b -> if (beats(b, a)) b else a }

    /**
     * The one set of this session that is a new personal best, or null. Marks a single set per exercise
     * (the best one, the earliest if tied), and only when it beats [previousBest]. With no earlier history
     * nothing is marked: a first ever lift is a starting point, not a record.
     */
    fun newBestIndex(previousBest: SetRef?, sets: List<SetRef>): Int? {
        if (previousBest == null || sets.isEmpty()) return null
        var bestIdx = 0
        for (i in 1 until sets.size) if (beats(sets[i], sets[bestIdx])) bestIdx = i
        return bestIdx.takeIf { beats(sets[it], previousBest) }
    }

    /** Number of exercises per session that set a new best over every earlier session. Sessions with none are absent. */
    fun newBestsBySession(rows: List<HistoricSet>): Map<String, Int> {
        val running = HashMap<String, SetRef>()
        val counts = LinkedHashMap<String, Int>()
        rows.groupBy { it.sortKey to it.sessionId }.toSortedMap(compareBy({ it.first }, { it.second })).forEach { (key, sessionRows) ->
            sessionRows.groupBy { it.exerciseId }.forEach { (exerciseId, sets) ->
                val sessionBest = best(sets.map { SetRef(it.weightKg, it.reps) }) ?: return@forEach
                val before = running[exerciseId]
                if (before != null && beats(sessionBest, before)) counts[key.second] = (counts[key.second] ?: 0) + 1
                if (before == null || beats(sessionBest, before)) running[exerciseId] = sessionBest
            }
        }
        return counts
    }
}
