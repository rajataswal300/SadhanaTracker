package com.example.data.repository

import com.example.data.db.ActivityDao
import com.example.data.model.ActivitySession
import com.example.data.model.DailySummary
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TrackerRepository(private val dao: ActivityDao) {

    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    fun observeSessionsForDate(dateKey: String): Flow<List<ActivitySession>> =
        dao.getSessionsForDate(dateKey)

    fun observeActiveSessions(): Flow<List<ActivitySession>> =
        dao.getActiveSessions()

    fun observeDailySummary(dateKey: String): Flow<DailySummary?> =
        dao.getDailySummary(dateKey)

    fun observeSummariesInRange(startDate: String, endDate: String): Flow<List<DailySummary>> =
        dao.getDailySummariesInRange(startDate, endDate)

    fun observeSessionsInRange(startDate: String, endDate: String): Flow<List<ActivitySession>> =
        dao.getSessionsInRange(startDate, endDate)

    fun observeAllSummaries(): Flow<List<DailySummary>> =
        dao.getAllDailySummaries()

    fun observeAllSessions(): Flow<List<ActivitySession>> =
        dao.getAllSessions()

    suspend fun startSession(
        activityType: String,
        subType: String,
        dateKey: String,
        note: String = ""
    ): Long {
        val now = System.currentTimeMillis()
        val session = ActivitySession(
            dateKey = dateKey,
            activityType = activityType,
            subType = subType,
            startTimeMillis = now,
            endTimeMillis = null,
            durationMinutes = 0,
            note = note
        )
        val id = dao.insertSession(session)

        // If this is night sleep starting, also record bedTimePreviousNight
        if (activityType == "SLEEP" && subType == "NIGHT_SLEEP") {
            val existing = dao.getDailySummaryOnce(dateKey) ?: DailySummary(dateKey = dateKey)
            dao.upsertDailySummary(existing.copy(bedTimePreviousNight = timeFormat.format(Date(now))))
        }
        return id
    }

    suspend fun stopSession(sessionId: Long, endTimeMillis: Long = System.currentTimeMillis()) {
        val session = dao.getSessionById(sessionId) ?: return
        val durationMs = (endTimeMillis - session.startTimeMillis).coerceAtLeast(0L)
        val durationMins = durationMs / (1000 * 60)

        val updated = session.copy(
            endTimeMillis = endTimeMillis,
            durationMinutes = durationMins
        )
        dao.updateSession(updated)

        // If this was sleep waking up in morning, set wakeUpTime in DailySummary
        if (session.activityType == "SLEEP") {
            val existing = dao.getDailySummaryOnce(session.dateKey) ?: DailySummary(dateKey = session.dateKey)
            if (existing.wakeUpTime == null || session.subType == "NIGHT_SLEEP") {
                dao.upsertDailySummary(
                    existing.copy(wakeUpTime = timeFormat.format(Date(endTimeMillis)))
                )
            }
        }
    }

    suspend fun setLectureWatched(dateKey: String, watched: Boolean) {
        val existing = dao.getDailySummaryOnce(dateKey) ?: DailySummary(dateKey = dateKey)
        dao.upsertDailySummary(
            existing.copy(
                prabhupadaLectureWatched = watched,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateTimes(dateKey: String, bedTime: String?, wakeTime: String?) {
        val existing = dao.getDailySummaryOnce(dateKey) ?: DailySummary(dateKey = dateKey)
        dao.upsertDailySummary(
            existing.copy(
                bedTimePreviousNight = bedTime,
                wakeUpTime = wakeTime,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteSession(sessionId: Long) {
        dao.deleteSessionById(sessionId)
    }
}
