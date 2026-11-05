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
    val note: String = "",
    val bookId: Long? = null,
    val bookTitle: String? = null
) {
    val isRunning: Boolean get() = endTimeMillis == null
}

enum class BookStatus {
    NOT_READ,    // Initial state: Pending / To Read
    READING,     // Currently Reading (one or more books simultaneously)
    COMPLETED    // Finished at least once (completionCount >= 1)
}

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val status: BookStatus = BookStatus.NOT_READ,
    val completionCount: Int = 0,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val lastReadTimestamp: Long? = null
)

@Entity(tableName = "daily_summaries")
data class DailySummary(
    @PrimaryKey
    val dateKey: String, // YYYY-MM-DD
    val prabhupadaLectureWatched: Boolean = false,
    val strongMayaAttack: Boolean = false,
    val wakeUpTime: String? = null,
    val bedTimePreviousNight: String? = null,
    val notes: String = "",
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "checklist_items")
data class ChecklistItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "daily_checklist_completions",
    primaryKeys = ["dateKey", "itemId"]
)
data class DailyChecklistCompletion(
    val dateKey: String,
    val itemId: Long,
    val isCompleted: Boolean = false,
    val completedAtMillis: Long = System.currentTimeMillis()
)

data class ChecklistEntry(
    val item: ChecklistItem,
    val isCompleted: Boolean
)
