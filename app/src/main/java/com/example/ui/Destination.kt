package com.example.ui

/** The five bottom tabs. Stats, Goals and Config live inside You. */
enum class Tab(val title: String) { HOME("Home"), ROUTINE("Routine"), WORKOUT("Workout"), FOCUS("Focus"), YOU("You") }

/** The three views inside the You tab, shown as a segmented control. Stats is the default. */
enum class YouSegment(val title: String) { STATS("Stats"), GOALS("Goals"), CONFIG("Config") }

/** Where a tap or a deep link should land: a tab, and for You the segment. */
data class Destination(val tab: Tab, val segment: YouSegment? = null) {
    companion object {
        val HOME = Destination(Tab.HOME)
        val ROUTINE = Destination(Tab.ROUTINE)
        val FOCUS = Destination(Tab.FOCUS)
        val GOALS = Destination(Tab.YOU, YouSegment.GOALS)

        /**
         * Reads a destination name from a notification or deep link. Goals, Stats and Config (and the older Tasks / Settings
         * names) now open You on that segment. Unknown names give null, so the app just opens where it was.
         */
        fun parse(name: String?): Destination? = when (name?.trim()?.uppercase()) {
            "HOME" -> HOME
            "ROUTINE" -> ROUTINE
            "WORKOUT" -> Destination(Tab.WORKOUT)
            "FOCUS" -> FOCUS
            "YOU" -> Destination(Tab.YOU)
            "STATS" -> Destination(Tab.YOU, YouSegment.STATS)
            "GOALS", "TASKS" -> GOALS
            "CONFIG", "SETTINGS" -> Destination(Tab.YOU, YouSegment.CONFIG)
            else -> null
        }
    }
}

/** Display form of a stored category code: "SKINCARE" becomes "Skincare". */
fun categoryLabel(code: String): String = code.lowercase().replaceFirstChar { it.uppercase() }
