package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.ActivitySession
import com.example.data.model.BookEntity
import com.example.data.model.BookStatus
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
        repository = TrackerRepository(db.activityDao(), db.bookDao())
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
        assertEquals(1, r.customToggles.size)
        assertEquals("Chanting finished at morning", r.customToggles[0].title)
        assertTrue(r.customToggles[0].isCompleted)

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

    @Test
    fun `dynamic custom toggle title editing and daily isolation`() = runBlocking {
        repository.seedDefaultChecklistItemsIfEmpty()
        val items = repository.observeChecklistItems().first()
        val chantItem = items.first { it.title == "Chanting finished at morning" }

        // Edit heading
        repository.updateChecklistItemTitle(chantItem.id, "Morning 16 Rounds Japa")
        val updatedItems = repository.observeChecklistItems().first()
        assertTrue(updatedItems.any { it.title == "Morning 16 Rounds Japa" })
        assertFalse(updatedItems.any { it.title == "Chanting finished at morning" })
    }

    @Test
    fun `section reordering and layout persistence`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = com.example.data.prefs.LayoutPreferences(context)
        prefs.resetToDefault()

        val vm = com.example.ui.viewmodel.DailyTrackerViewModel(repository, prefs)
        val initialOrder = vm.sectionOrder.value
        assertEquals(com.example.data.model.SectionType.TOGGLES, initialOrder[0])
        assertEquals(com.example.data.model.SectionType.SLEEP, initialOrder[1])

        // Move Sleep (index 1) to top (index 0)
        vm.moveSection(1, 0)
        val reordered = vm.sectionOrder.value
        assertEquals(com.example.data.model.SectionType.SLEEP, reordered[0])
        assertEquals(com.example.data.model.SectionType.TOGGLES, reordered[1])

        // Verify loaded from preferences in a new ViewModel instance
        val vm2 = com.example.ui.viewmodel.DailyTrackerViewModel(repository, prefs)
        assertEquals(com.example.data.model.SectionType.SLEEP, vm2.sectionOrder.value[0])

        // Reset restores default
        vm2.resetSectionOrder()
        assertEquals(com.example.data.model.SectionType.TOGGLES, vm2.sectionOrder.value[0])
    }

    @Test
    fun `notes carry forward from previous day and google sheets export`() = runBlocking {
        // Save note on day 1
        repository.updateDailyNotes("2026-11-01", "Chanted with full attention. Read Gita chapter 3.")

        // Check carry forward lookup for day 2
        val candidate = repository.getLatestPreviousNotes("2026-11-02")
        assertNotNull(candidate)
        assertEquals("2026-11-01", candidate!!.first)
        assertEquals("Chanted with full attention. Read Gita chapter 3.", candidate.second)

        // Verify carry-forward applied to day 2
        repository.updateDailyNotes("2026-11-02", candidate.second)

        val day2Summary = repository.observeDailySummary("2026-11-02").first()
        assertNotNull(day2Summary)
        assertEquals("Chanted with full attention. Read Gita chapter 3.", day2Summary!!.notes)

        // Test building Google Sheets notes export records
        val context = ApplicationProvider.getApplicationContext<Context>()
        val allSummaries = repository.observeAllSummaries().first()
        val syncService = com.example.data.sheets.GoogleSheetsSyncService(context)
        val exportRecords = syncService.buildNotesExportRecords(allSummaries)
        assertTrue(exportRecords.isNotEmpty())
        assertEquals("2026-11-01", exportRecords[0].dateKey)
        assertEquals("Chanted with full attention. Read Gita chapter 3.", exportRecords[0].notes)
    }

    @Test
    fun `book library lifecycle - add, mark reading, finish increment count and re-read`() = runBlocking {
        // Seed default books
        repository.seedDefaultBooksIfEmpty()
        val initialBooks = repository.observeAllBooks().first()
        assertTrue("Expected seeded books", initialBooks.isNotEmpty())

        // Add a custom book
        val bookId = repository.addBook("Isopanisad", "A.C. Bhaktivedanta Swami")
        assertTrue(bookId > 0)

        val allBooks = repository.observeAllBooks().first()
        val addedBook = allBooks.find { it.id == bookId }
        assertNotNull(addedBook)
        assertEquals("Isopanisad", addedBook!!.title)
        assertEquals(BookStatus.NOT_READ, addedBook.status)
        assertEquals(0, addedBook.completionCount)

        // Mark as currently reading
        repository.markBookAsReading(bookId)
        val readingBooks = repository.observeBooksByStatus(BookStatus.READING).first()
        assertTrue(readingBooks.any { it.id == bookId })

        // Start a reading session tagged with this book
        val sessionId = repository.startSession(
            activityType = "READING",
            subType = "READING",
            dateKey = "2026-11-04",
            bookId = bookId,
            bookTitle = addedBook.title
        )
        assertTrue(sessionId > 0)

        val activeSessions = repository.observeActiveSessions().first()
        val activeReading = activeSessions.find { it.id == sessionId }
        assertNotNull(activeReading)
        assertEquals("Isopanisad", activeReading!!.bookTitle)
        assertEquals(bookId, activeReading.bookId)

        // Stop the session
        repository.stopSession(sessionId, activeReading.startTimeMillis + 30 * 60 * 1000)

        // Mark book as completed (1st finish)
        repository.markBookAsCompleted(bookId)
        val completedBooks = repository.observeBooksByStatus(BookStatus.COMPLETED).first()
        val completedBook = completedBooks.find { it.id == bookId }
        assertNotNull(completedBook)
        assertEquals(BookStatus.COMPLETED, completedBook!!.status)
        assertEquals(1, completedBook.completionCount)

        // User decides to re-read the book
        repository.rereadBook(bookId)
        val rereadBooks = repository.observeBooksByStatus(BookStatus.READING).first()
        val readingAgain = rereadBooks.find { it.id == bookId }
        assertNotNull(readingAgain)
        assertEquals(BookStatus.READING, readingAgain!!.status)
        assertEquals(1, readingAgain.completionCount) // Count preserved!

        // Finish re-reading (2nd finish)
        repository.markBookAsCompleted(bookId)
        val secondFinish = repository.observeAllBooks().first().find { it.id == bookId }
        assertNotNull(secondFinish)
        assertEquals(BookStatus.COMPLETED, secondFinish!!.status)
        assertEquals(2, secondFinish.completionCount)

        // Edit book title
        repository.updateBookTitle(bookId, "Sri Isopanisad (Full)")
        val editedBook = repository.observeAllBooks().first().find { it.id == bookId }
        assertNotNull(editedBook)
        assertEquals("Sri Isopanisad (Full)", editedBook!!.title)

        // Delete book
        repository.deleteBook(bookId)
        val booksAfterDelete = repository.observeAllBooks().first()
        assertTrue(booksAfterDelete.none { it.id == bookId })
    }
}
