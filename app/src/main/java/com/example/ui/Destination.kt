package com.example.ui

/** The four bottom tabs. Stats, Goals and Settings open from icons on Home as full screens. */
enum class Tab(val title: String) { HOME("Home"), ROUTINE("Routine"), WORKOUT("Workout"), FOCUS("Focus") }

/** Full screens opened from the Home header, with a back arrow. */
enum class Overlay(val title: String) { STATS("Stats"), GOALS("Goals"), SETTINGS("Settings") }

/** Where a tap or a deep link should land: a tab, and optionally a full screen on top of it. */
data class Destination(val tab: Tab, val overlay: Overlay? = null) {
    companion object {
        val HOME = Destination(Tab.HOME)
        val ROUTINE = Destination(Tab.ROUTINE)
        val FOCUS = Destination(Tab.FOCUS)
        val GOALS = Destination(Tab.HOME, Overlay.GOALS)

        /**
         * Reads a destination name from a notification or deep link. Goals, Stats and Settings (and the older Tasks / Config / You
         * names) open that screen on top of Home. Unknown names give null, so the app just opens where it was.
         */
        fun parse(name: String?): Destination? = when (name?.trim()?.uppercase()) {
            "HOME" -> HOME
            "ROUTINE" -> ROUTINE
            "WORKOUT" -> Destination(Tab.WORKOUT)
            "FOCUS" -> FOCUS
            "YOU" -> Destination(Tab.HOME, Overlay.SETTINGS)
            "STATS" -> Destination(Tab.HOME, Overlay.STATS)
            "GOALS", "TASKS" -> GOALS
            "CONFIG", "SETTINGS" -> Destination(Tab.HOME, Overlay.SETTINGS)
            else -> null
        }
    }
}

/** Display form of a stored category code: "SKINCARE" becomes "Skincare". */
fun categoryLabel(code: String): String = code.lowercase().replaceFirstChar { it.uppercase() }
