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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
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
    fun `toggle strong maya attack and verify daily isolation`(): Unit = runBlocking {
        val day1 = "2026-10-29"
        val day2 = "2026-10-30"

        // Day 1: Turn on maya attack and lecture
        repository.setMayaAttack(day1, true)
        repository.setLectureWatched(day1, true)

        val day1Summary = repository.observeDailySummary(day1).first()
        assertNotNull(day1Summary)
        assertTrue(day1Summary!!.strongMayaAttack)
        assertTrue(day1Summary.prabhupadaLectureWatched)

        // Day 2: Next day, switches start OFF (false)
        val day2Summary = repository.observeDailySummary(day2).first()
        assertFalse(day2Summary?.strongMayaAttack ?: false)
        assertFalse(day2Summary?.prabhupadaLectureWatched ?: false)
    }

    @Test
    fun `checklist seeds defaults without Yoga and clears each day`(): Unit = runBlocking {
        // Seed initial items
        repository.seedDefaultChecklistItemsIfEmpty()
        val items = repository.observeChecklistItems().first()
        assertEquals(1, items.size)
        assertEquals("Chanting finished at morning", items[0].title)

        // Day 1: User checks "Chanting finished at morning"
        val day1 = "2026-10-27"
        repository.setChecklistCompletion(day1, items[0].id, true)

        val day1Completions = repository.observeChecklistCompletionsForDate(day1).first()
        assertEquals(1, day1Completions.size)
        assertTrue(day1Completions[0].isCompleted)

        // Day 2: Next day checklist is cleared / fresh
        val day2 = "2026-10-28"
        val day2Completions = repository.observeChecklistCompletionsForDate(day2).first()
        assertEquals(0, day2Completions.size)

        // User adds a new item to checklist
        val newItemId = repository.addChecklistItem("Gita Reading")
        assertTrue(newItemId > 0)
        val itemsAfterAdd = repository.observeChecklistItems().first()
        assertEquals(2, itemsAfterAdd.size)

        // Delete an item
        repository.deleteChecklistItem(newItemId)
        val itemsAfterDelete = repository.observeChecklistItems().first()
        assertEquals(1, itemsAfterDelete.size)

        // Test Yoga cleanup: if Yoga is added, re-running seed removes it
        repository.addChecklistItem("Yoga")
        assertEquals(2, repository.observeChecklistItems().first().size)
        repository.seedDefaultChecklistItemsIfEmpty()
        val cleanedItems = repository.observeChecklistItems().first()
        assertFalse(cleanedItems.any { it.title == "Yoga" })
    }

    @Test
    fun `timer formatting helpers`() {
        assertEquals("2h 15m", TimerHelpers.formatMinutes(135))
        assertEquals("45m", TimerHelpers.formatMinutes(45))
        assertEquals("01:00:00", TimerHelpers.formatElapsed(3600 * 1000))
    }

    @Test
    fun `google sheets export records formatting and csv generation`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val syncService = com.example.data.sheets.GoogleSheetsSyncService(context)

        val summaries = listOf(
            DailySummary(
                dateKey = "2026-10-30",
                prabhupadaLectureWatched = true,
                strongMayaAttack = false,
                wakeUpTime = "04:30 AM",
                bedTimePreviousNight = "10:00 PM"
            )
        )
        val sessions = listOf(
            ActivitySession(
                dateKey = "2026-10-30",
                activityType = "SLEEP",
                durationMinutes = 390
            ),
            ActivitySession(
                dateKey = "2026-10-30",
                activityType = "READING",
                durationMinutes = 60
            )
        )
        val checklistItems = listOf(
            com.example.data.model.ChecklistItem(id = 1, title = "Chanting finished at morning")
        )
        val completions = listOf(
            com.example.data.model.DailyChecklistCompletion(dateKey = "2026-10-30", itemId = 1, isCompleted = true)
        )

        val records = syncService.buildDailyExportRecords(summaries, sessions, checklistItems, completions)
        assertEquals(1, records.size)
        val r = records[0]
        assertEquals("2026-10-30", r.dateKey)
        assertEquals(6.5, r.sleepHoursDecimal, 0.01)
        assertEquals(60L, r.readingMinutes)
        assertEquals(1, r.lectureNumeric)
        assertEquals(0, r.mayaAttackNumeric)
        assertEquals("1/1", r.checklistRatio)
        assertEquals(100, r.checklistPercent)

        val uri = syncService.exportToCsvFile(records)
        assertNotNull(uri)
    }

    @Test
    fun `past day summary update and retroactive editing`() = runBlocking {
        val pastDate = "2026-10-25"
        repository.setLectureWatched(pastDate, true)
        repository.setMayaAttack(pastDate, true)
        repository.updateTimes(pastDate, "10:30 PM", "04:30 AM")

        val summary = repository.observeDailySummary(pastDate).first()
        assertNotNull(summary)
        assertTrue(summary!!.prabhupadaLectureWatched)
        assertTrue(summary.strongMayaAttack)
        assertEquals("10:30 PM", summary.bedTimePreviousNight)
        assertEquals("04:30 AM", summary.wakeUpTime)
    }

    @Test
    fun `navigation into future dates is blocked`() {
        val vm = com.example.ui.viewmodel.DailyTrackerViewModel(repository)
        val today = vm.selectedDateKey.value

        // Attempting to advance beyond today should stay on today
        vm.navigateDate(1)
        assertEquals(today, vm.selectedDateKey.value)

        // Stepping back into yesterday
        vm.navigateDate(-1)
        assertFalse(vm.isToday())

        // Stepping forward back to today works
        vm.navigateDate(1)
        assertEquals(today, vm.selectedDateKey.value)
        assertTrue(vm.isToday())
    }
}
