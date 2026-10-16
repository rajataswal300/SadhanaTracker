package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_sessions")
data class ActivitySession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateKey: String, // YYYY-MM-DD
    val activityType: String, // "SLEEP" or "READING"
    val subType: String = "", // "NIGHT_SLEEP", "NAP", or "READING"
    val startTimeMillis: Long = System.currentTimeMillis(),
    val endTimeMillis: Long? = null,
    val durationMinutes: Long = 0,
    val note: String = ""
) {
    val isRunning: Boolean get() = endTimeMillis == null
}

@Entity(tableName = "daily_summaries")
data class DailySummary(
    @PrimaryKey
    val dateKey: String, // YYYY-MM-DD
    val prabhupadaLectureWatched: Boolean = false,
    val wakeUpTime: String? = null,
    val bedTimePreviousNight: String? = null,
    val notes: String = "",
    val updatedAtMillis: Long = System.currentTimeMillis()
)
