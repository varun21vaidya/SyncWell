package com.example.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Locale

object DayBoundaryManager {
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    
    private val _currentTargetDate = MutableStateFlow(sdf.format(Date()))
    val currentTargetDate: StateFlow<String> = _currentTargetDate

    fun refreshTargetDate() {
        _currentTargetDate.value = sdf.format(Date())
    }
}
