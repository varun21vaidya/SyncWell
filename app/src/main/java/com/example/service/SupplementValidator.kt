package com.example.service

import java.util.Calendar

object SupplementValidator {

    fun processD3K2Log(timestamp: Long): Boolean {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        // D3+K2 logged past 4:00 PM (16:00) is flagged to protect sleep architecture
        return hour < 16
    }

    fun validateMagnesium(loggedTimeMs: Long, sleepTimeMs: Long): Boolean {
        // Suggested magnesium 45-60 minutes before bedtime
        return true
    }
}
