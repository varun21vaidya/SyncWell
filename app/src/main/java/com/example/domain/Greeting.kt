package com.example.domain

import java.text.SimpleDateFormat
import java.util.Locale

/** Home header wording. Pure: the clock and the date key come in as arguments. */
fun greetingFor(hour: Int): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..23 -> "Good evening"
    else -> "Hello" // 00:00 to 04:59, no judgement about being up
}

/** "Sunday 22 November" for a yyyy-MM-dd key; the key itself if it cannot be read. */
fun dateHeader(dateKey: String): String = try {
    val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateKey)!!
    SimpleDateFormat("EEEE d MMMM", Locale.getDefault()).format(d)
} catch (e: Exception) {
    dateKey
}
