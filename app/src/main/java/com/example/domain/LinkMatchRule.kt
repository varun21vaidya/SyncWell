package com.example.domain

enum class LinkEvent { WORKOUT, FOCUS, WATER, SUPPLEMENT }

data class LinkItem(val id: String, val title: String, val category: String, val isDone: Boolean)

/**
 * Which routine item a tracker event belongs to, so the app can ask "tick it?". Never ticks anything itself.
 * [items] come in routine order. Returns null when nothing matches, the item is already ticked,
 * or it was already prompted today.
 */
object LinkMatchRule {
    private val WARM_OR_COOL = Regex("warm|cool|stretch", RegexOption.IGNORE_CASE)

    fun match(event: LinkEvent, items: List<LinkItem>, promptedToday: Set<String>, supplementName: String? = null): LinkItem? =
        items.firstOrNull { !it.isDone && it.id !in promptedToday && belongs(event, it, supplementName) }

    private fun belongs(event: LinkEvent, item: LinkItem, supplementName: String?) = when (event) {
        LinkEvent.WORKOUT -> item.category == "WORKOUT" && !WARM_OR_COOL.containsMatchIn(item.title)
        LinkEvent.FOCUS -> item.category == "STUDY"
        LinkEvent.WATER -> item.category == "HYGIENE" && item.title.contains(Regex("water|hydrat", RegexOption.IGNORE_CASE))
        LinkEvent.SUPPLEMENT -> item.category == "SUPPLEMENT" && supplementName != null &&
            item.title.trim().equals(supplementName.trim(), ignoreCase = true)
    }
}
