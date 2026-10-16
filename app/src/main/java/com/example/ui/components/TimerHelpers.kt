package com.example.ui.components

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimerHelpers {
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    fun formatMinutes(minutes: Long): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "${h}h"
            else -> "${m}m"
        }
    }

    fun formatElapsed(elapsedMs: Long): String {
        val totalSecs = (elapsedMs / 1000).coerceAtLeast(0)
        val hours = totalSecs / 3600
        val minutes = (totalSecs % 3600) / 60
        val seconds = totalSecs % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun formatTime(millis: Long): String {
        return timeFormat.format(Date(millis))
    }
}
