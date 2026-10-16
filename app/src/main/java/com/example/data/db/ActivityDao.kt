package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ActivitySession
import com.example.data.model.DailySummary
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {

    // Sessions
    @Query("SELECT * FROM activity_sessions WHERE dateKey = :dateKey ORDER BY startTimeMillis ASC")
    fun getSessionsForDate(dateKey: String): Flow<List<ActivitySession>>

    @Query("SELECT * FROM activity_sessions WHERE endTimeMillis IS NULL ORDER BY startTimeMillis DESC")
    fun getActiveSessions(): Flow<List<ActivitySession>>

    @Query("SELECT * FROM activity_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Long): ActivitySession?

    @Query("SELECT * FROM activity_sessions WHERE dateKey BETWEEN :startDate AND :endDate ORDER BY startTimeMillis ASC")
    fun getSessionsInRange(startDate: String, endDate: String): Flow<List<ActivitySession>>

    @Query("SELECT * FROM activity_sessions ORDER BY startTimeMillis ASC")
    fun getAllSessions(): Flow<List<ActivitySession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ActivitySession): Long

    @Update
    suspend fun updateSession(session: ActivitySession)

    @Query("DELETE FROM activity_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)

    // Daily Summary
    @Query("SELECT * FROM daily_summaries WHERE dateKey = :dateKey LIMIT 1")
    fun getDailySummary(dateKey: String): Flow<DailySummary?>

    @Query("SELECT * FROM daily_summaries WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getDailySummaryOnce(dateKey: String): DailySummary?

    @Query("SELECT * FROM daily_summaries WHERE dateKey BETWEEN :startDate AND :endDate ORDER BY dateKey ASC")
    fun getDailySummariesInRange(startDate: String, endDate: String): Flow<List<DailySummary>>

    @Query("SELECT * FROM daily_summaries ORDER BY dateKey ASC")
    fun getAllDailySummaries(): Flow<List<DailySummary>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailySummary(summary: DailySummary)
}
