package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.ActivitySession
import com.example.data.model.DailySummary
import com.example.data.repository.TrackerRepository
import com.example.ui.components.TimerHelpers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: TrackerRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = TrackerRepository(db.activityDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Daily Sadhana", appName)
    }

    @Test
    fun `start and stop sleep session calculates duration and updates summary`() = runBlocking {
        val dateKey = "2026-10-15"
        val sessionId = repository.startSession("SLEEP", "NIGHT_SLEEP", dateKey)
        assertTrue(sessionId > 0)

        val active = repository.observeActiveSessions().first()
        assertEquals(1, active.size)
        assertEquals("SLEEP", active[0].activityType)

        // 8 hours later
        val endMs = active[0].startTimeMillis + (8 * 3600 * 1000)
        repository.stopSession(sessionId, endMs)

        val activeAfter = repository.observeActiveSessions().first()
        assertEquals(0, activeAfter.size)

        val daySessions = repository.observeSessionsForDate(dateKey).first()
        assertEquals(1, daySessions.size)
        assertEquals(480L, daySessions[0].durationMinutes)

        val summary = repository.observeDailySummary(dateKey).first()
        assertNotNull(summary)
        assertNotNull(summary?.wakeUpTime)
    }

    @Test
    fun `toggle lecture watched updates daily summary`() = runBlocking {
        val dateKey = "2026-10-15"
        repository.setLectureWatched(dateKey, true)

        val summary = repository.observeDailySummary(dateKey).first()
        assertNotNull(summary)
        assertEquals(true, summary?.prabhupadaLectureWatched)
    }

    @Test
    fun `timer formatting helpers`() {
        assertEquals("2h 15m", TimerHelpers.formatMinutes(135))
        assertEquals("45m", TimerHelpers.formatMinutes(45))
        assertEquals("01:00:00", TimerHelpers.formatElapsed(3600 * 1000))
    }
}
