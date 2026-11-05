package com.example.data.repository

import com.example.data.db.ActivityDao
import com.example.data.db.BookDao
import com.example.data.firestore.FirestoreSyncRepository
import com.example.data.model.ActivitySession
import com.example.data.model.BookEntity
import com.example.data.model.BookStatus
import com.example.data.model.ChecklistItem
import com.example.data.model.DailyChecklistCompletion
import com.example.data.model.DailySummary
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TrackerRepository(
    private val dao: ActivityDao,
    private val bookDao: BookDao,
    val firestoreSync: FirestoreSyncRepository? = null
) {

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

    // Checklist Observation
    fun observeChecklistItems(): Flow<List<ChecklistItem>> =
        dao.getAllChecklistItems()

    fun observeChecklistCompletionsForDate(dateKey: String): Flow<List<DailyChecklistCompletion>> =
        dao.getChecklistCompletionsForDate(dateKey)

    fun observeAllChecklistCompletions(): Flow<List<DailyChecklistCompletion>> =
        dao.getAllChecklistCompletions()

    suspend fun seedDefaultChecklistItemsIfEmpty() {
        // Clean up "Yoga" if present from earlier runs
        dao.deleteChecklistItemByTitle("Yoga")
        val count = dao.getChecklistItemsCount()
        if (count == 0) {
            val defaultItem = ChecklistItem(title = "Chanting finished at morning")
            val id = dao.insertChecklistItem(defaultItem)
            firestoreSync?.syncChecklistItemToCloud(defaultItem.copy(id = id))
        }
    }

    suspend fun addChecklistItem(title: String): Long {
        if (title.isBlank()) return -1L
        val item = ChecklistItem(title = title.trim())
        val id = dao.insertChecklistItem(item)
        firestoreSync?.syncChecklistItemToCloud(item.copy(id = id))
        return id
    }

    suspend fun updateChecklistItemTitle(itemId: Long, newTitle: String) {
        if (newTitle.isNotBlank()) {
            dao.updateChecklistItemTitle(itemId, newTitle.trim())
            firestoreSync?.syncChecklistItemToCloud(ChecklistItem(id = itemId, title = newTitle.trim()))
        }
    }

    suspend fun deleteChecklistItem(itemId: Long) {
        dao.deleteChecklistItem(itemId)
        dao.deleteCompletionsForItem(itemId)
        firestoreSync?.deleteChecklistItemFromCloud(itemId)
    }

    suspend fun setChecklistCompletion(dateKey: String, itemId: Long, isCompleted: Boolean) {
        val completion = DailyChecklistCompletion(
            dateKey = dateKey,
            itemId = itemId,
            isCompleted = isCompleted,
            completedAtMillis = System.currentTimeMillis()
        )
        dao.upsertChecklistCompletion(completion)
        firestoreSync?.syncChecklistCompletionToCloud(completion)
    }

    suspend fun startSession(
        activityType: String,
        subType: String,
        dateKey: String,
        note: String = "",
        bookId: Long? = null,
        bookTitle: String? = null
    ): Long {
        val now = System.currentTimeMillis()
        val session = ActivitySession(
            dateKey = dateKey,
            activityType = activityType,
            subType = subType,
            startTimeMillis = now,
            endTimeMillis = null,
            durationMinutes = 0,
            note = note,
            bookId = bookId,
            bookTitle = bookTitle
        )
        val id = dao.insertSession(session)
        val insertedSession = session.copy(id = id)
        firestoreSync?.syncSessionToCloud(insertedSession)

        // If this reading session is tagged with a book, ensure lastReadTimestamp is updated
        if (activityType == "READING" && bookId != null) {
            val book = bookDao.getBookById(bookId)
            if (book != null && book.status == BookStatus.NOT_READ) {
                bookDao.updateBookStatus(bookId, BookStatus.READING, now)
                bookDao.getBookById(bookId)?.let { firestoreSync?.syncBookToCloud(it) }
            }
        }

        // If this is night sleep starting, also record bedTimePreviousNight
        if (activityType == "SLEEP" && subType == "NIGHT_SLEEP") {
            val existing = dao.getDailySummaryOnce(dateKey) ?: DailySummary(dateKey = dateKey)
            val updatedSummary = existing.copy(bedTimePreviousNight = timeFormat.format(Date(now)))
            dao.upsertDailySummary(updatedSummary)
            firestoreSync?.syncSummaryToCloud(updatedSummary)
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
        firestoreSync?.syncSessionToCloud(updated)

        // If this was sleep waking up in morning, set wakeUpTime in DailySummary
        if (session.activityType == "SLEEP") {
            val existing = dao.getDailySummaryOnce(session.dateKey) ?: DailySummary(dateKey = session.dateKey)
            if (existing.wakeUpTime == null || session.subType == "NIGHT_SLEEP") {
                val updatedSummary = existing.copy(wakeUpTime = timeFormat.format(Date(endTimeMillis)))
                dao.upsertDailySummary(updatedSummary)
                firestoreSync?.syncSummaryToCloud(updatedSummary)
            }
        }
    }

    suspend fun setLectureWatched(dateKey: String, watched: Boolean) {
        val existing = dao.getDailySummaryOnce(dateKey) ?: DailySummary(dateKey = dateKey)
        val updatedSummary = existing.copy(
            prabhupadaLectureWatched = watched,
            updatedAtMillis = System.currentTimeMillis()
        )
        dao.upsertDailySummary(updatedSummary)
        firestoreSync?.syncSummaryToCloud(updatedSummary)
    }

    suspend fun setMayaAttack(dateKey: String, attacked: Boolean) {
        val existing = dao.getDailySummaryOnce(dateKey) ?: DailySummary(dateKey = dateKey)
        val updatedSummary = existing.copy(
            strongMayaAttack = attacked,
            updatedAtMillis = System.currentTimeMillis()
        )
        dao.upsertDailySummary(updatedSummary)
        firestoreSync?.syncSummaryToCloud(updatedSummary)
    }

    suspend fun updateTimes(dateKey: String, bedTime: String?, wakeTime: String?) {
        val existing = dao.getDailySummaryOnce(dateKey) ?: DailySummary(dateKey = dateKey)
        val updatedSummary = existing.copy(
            bedTimePreviousNight = bedTime,
            wakeUpTime = wakeTime,
            updatedAtMillis = System.currentTimeMillis()
        )
        dao.upsertDailySummary(updatedSummary)
        firestoreSync?.syncSummaryToCloud(updatedSummary)
    }

    suspend fun deleteSession(sessionId: Long) {
        dao.deleteSessionById(sessionId)
        firestoreSync?.deleteSessionFromCloud(sessionId)
    }

    suspend fun getLatestPreviousNotes(beforeDateKey: String): Pair<String, String>? {
        val prevSummary = dao.getLatestPreviousSummaryWithNotes(beforeDateKey)
        return if (prevSummary != null && prevSummary.notes.isNotBlank()) {
            Pair(prevSummary.dateKey, prevSummary.notes)
        } else {
            null
        }
    }

    suspend fun updateDailyNotes(dateKey: String, notes: String) {
        val existing = dao.getDailySummaryOnce(dateKey) ?: DailySummary(dateKey = dateKey)
        val updatedSummary = existing.copy(
            notes = notes,
            updatedAtMillis = System.currentTimeMillis()
        )
        dao.upsertDailySummary(updatedSummary)
        firestoreSync?.syncSummaryToCloud(updatedSummary)
    }

    fun observeAllSummariesWithNotes(): Flow<List<DailySummary>> =
        dao.getAllSummariesWithNotes()

    // Book Library Operations
    fun observeAllBooks(): Flow<List<BookEntity>> =
        bookDao.getAllBooks()

    fun observeBooksByStatus(status: BookStatus): Flow<List<BookEntity>> =
        bookDao.getBooksByStatus(status)

    suspend fun seedDefaultBooksIfEmpty() {
        val count = bookDao.getBooksCount()
        if (count == 0) {
            val defaultBooks = listOf(
                BookEntity(title = "Bhagavad Gita As It Is", status = BookStatus.READING),
                BookEntity(title = "Srimad Bhagavatam - Canto 1", status = BookStatus.NOT_READ),
                BookEntity(title = "The Science of Self-Realization", status = BookStatus.NOT_READ),
                BookEntity(title = "Nectar of Devotion", status = BookStatus.NOT_READ)
            )
            for (b in defaultBooks) {
                val id = bookDao.insertBook(b)
                firestoreSync?.syncBookToCloud(b.copy(id = id))
            }
        }
    }

    suspend fun addBook(title: String): Long {
        if (title.isBlank()) return -1L
        val book = BookEntity(
            title = title.trim(),
            status = BookStatus.NOT_READ,
            completionCount = 0
        )
        val id = bookDao.insertBook(book)
        val inserted = book.copy(id = id)
        firestoreSync?.syncBookToCloud(inserted)
        return id
    }

    suspend fun updateBookTitle(id: Long, newTitle: String) {
        if (newTitle.isNotBlank()) {
            bookDao.updateBookTitle(id, newTitle.trim())
            bookDao.getBookById(id)?.let { firestoreSync?.syncBookToCloud(it) }
        }
    }

    suspend fun updateBookStatus(id: Long, status: BookStatus) {
        bookDao.updateBookStatus(id, status, System.currentTimeMillis())
        bookDao.getBookById(id)?.let { firestoreSync?.syncBookToCloud(it) }
    }

    suspend fun markBookAsReading(id: Long) {
        bookDao.updateBookStatus(id, BookStatus.READING, System.currentTimeMillis())
        bookDao.getBookById(id)?.let { firestoreSync?.syncBookToCloud(it) }
    }

    suspend fun markBookAsCompleted(id: Long) {
        // Increments completionCount by 1 and marks status as COMPLETED
        bookDao.markBookCompleted(id, BookStatus.COMPLETED, System.currentTimeMillis())
        bookDao.getBookById(id)?.let { firestoreSync?.syncBookToCloud(it) }
    }

    suspend fun rereadBook(id: Long) {
        // Moves back to READING while preserving completionCount
        bookDao.updateBookStatus(id, BookStatus.READING, System.currentTimeMillis())
        bookDao.getBookById(id)?.let { firestoreSync?.syncBookToCloud(it) }
    }

    suspend fun incrementBookCompletion(id: Long) {
        bookDao.incrementCompletionCount(id, System.currentTimeMillis())
        bookDao.getBookById(id)?.let { firestoreSync?.syncBookToCloud(it) }
    }

    suspend fun deleteBook(id: Long) {
        bookDao.deleteBookById(id)
        firestoreSync?.deleteBookFromCloud(id)
    }
}
