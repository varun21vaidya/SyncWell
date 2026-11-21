package com.example.domain

object CategoryLineRule {
    /** Every category with an item today, in order of first appearance, as e.g. "Skincare 2 of 3". */
    fun lines(items: List<DayItem>): List<String> =
        items.groupBy { it.category }.map { (c, l) ->
            "${c.lowercase().replaceFirstChar { it.uppercase() }} ${l.count { it.isDone }} of ${l.size}"
        }
}
