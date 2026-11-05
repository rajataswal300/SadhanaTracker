package com.example.data.firestore

import android.content.Context
import android.util.Log
import com.example.data.db.ActivityDao
import com.example.data.db.BookDao
import com.example.data.model.ActivitySession
import com.example.data.model.BookEntity
import com.example.data.model.BookStatus
import com.example.data.model.ChecklistItem
import com.example.data.model.DailyChecklistCompletion
import com.example.data.model.DailySummary
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SyncStatus {
    IDLE,
    SYNCING,
    SYNCED,
    ERROR
}

class FirestoreSyncRepository(
    private val context: Context,
    private val activityDao: ActivityDao,
    private val bookDao: BookDao
) {
    private val tag = "FirestoreSync"

    private val firestore: FirebaseFirestore? by lazy {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            val dbId = try {
                val resId = context.resources.getIdentifier("firestore_database_id", "string", context.packageName)
                if (resId != 0) context.getString(resId) else null
            } catch (e: Exception) {
                null
            }
            val app = FirebaseApp.getInstance()
            if (!dbId.isNullOrEmpty() && dbId != "(default)") {
                FirebaseFirestore.getInstance(app, dbId)
            } else {
                FirebaseFirestore.getInstance(app)
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize FirebaseFirestore", e)
            null
        }
    }

    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncTimeMillis = MutableStateFlow<Long?>(null)
    val lastSyncTimeMillis: StateFlow<Long?> = _lastSyncTimeMillis.asStateFlow()

    private val activeListeners = mutableListOf<ListenerRegistration>()
    private var activeUserId: String? = null

    fun startSync(userId: String, scope: CoroutineScope) {
        if (activeUserId == userId && activeListeners.isNotEmpty()) {
            return
        }
        stopSync()
        activeUserId = userId
        _syncStatus.value = SyncStatus.SYNCING

        scope.launch(Dispatchers.IO) {
            try {
                // 1. Initial bidirectional merge: push existing local records to Firestore
                uploadLocalDataToCloud(userId)

                // 2. Attach real-time snapshot listeners to keep local Room in sync with Firestore
                withContext(Dispatchers.Main) {
                    attachListeners(userId, scope)
                }
                _syncStatus.value = SyncStatus.SYNCED
                _lastSyncTimeMillis.value = System.currentTimeMillis()
            } catch (e: Exception) {
                Log.e(tag, "Error during initial sync", e)
                _syncStatus.value = SyncStatus.ERROR
            }
        }
    }

    fun stopSync() {
        for (listener in activeListeners) {
            listener.remove()
        }
        activeListeners.clear()
        activeUserId = null
        _syncStatus.value = SyncStatus.IDLE
    }

    private suspend fun uploadLocalDataToCloud(userId: String) = withContext(Dispatchers.IO) {
        try {
            val fs = firestore ?: return@withContext
            val userDoc = fs.collection("users").document(userId)

            // Upload daily summaries
            val summaries = activityDao.getAllDailySummariesOnce()
            for (summary in summaries) {
                val data = hashMapOf(
                    "dateKey" to summary.dateKey,
                    "prabhupadaLectureWatched" to summary.prabhupadaLectureWatched,
                    "strongMayaAttack" to summary.strongMayaAttack,
                    "wakeUpTime" to summary.wakeUpTime,
                    "bedTimePreviousNight" to summary.bedTimePreviousNight,
                    "notes" to summary.notes,
                    "updatedAtMillis" to summary.updatedAtMillis
                )
                userDoc.collection("daily_summaries")
                    .document(summary.dateKey)
                    .set(data, SetOptions.merge())
            }

            // Upload activity sessions
            val sessions = activityDao.getAllSessionsOnce()
            for (session in sessions) {
                val data = hashMapOf(
                    "id" to session.id,
                    "dateKey" to session.dateKey,
                    "activityType" to session.activityType,
                    "subType" to session.subType,
                    "startTimeMillis" to session.startTimeMillis,
                    "endTimeMillis" to session.endTimeMillis,
                    "durationMinutes" to session.durationMinutes,
                    "note" to session.note,
                    "bookId" to session.bookId,
                    "bookTitle" to session.bookTitle
                )
                userDoc.collection("activity_sessions")
                    .document(session.id.toString())
                    .set(data, SetOptions.merge())
            }

            // Upload books
            val books = bookDao.getAllBooksOnce()
            for (book in books) {
                val data = hashMapOf(
                    "id" to book.id,
                    "title" to book.title,
                    "status" to book.status.name,
                    "completionCount" to book.completionCount,
                    "createdAtMillis" to book.createdAtMillis,
                    "lastReadTimestamp" to book.lastReadTimestamp
                )
                userDoc.collection("books")
                    .document(book.id.toString())
                    .set(data, SetOptions.merge())
            }

            // Upload checklist items
            val checklistItems = activityDao.getAllChecklistItemsOnce()
            for (item in checklistItems) {
                val data = hashMapOf(
                    "id" to item.id,
                    "title" to item.title,
                    "createdAtMillis" to item.createdAtMillis
                )
                userDoc.collection("checklist_items")
                    .document(item.id.toString())
                    .set(data, SetOptions.merge())
            }

            // Upload checklist completions
            val completions = activityDao.getAllChecklistCompletionsOnce()
            for (comp in completions) {
                val docKey = "${comp.dateKey}_${comp.itemId}"
                val data = hashMapOf(
                    "dateKey" to comp.dateKey,
                    "itemId" to comp.itemId,
                    "isCompleted" to comp.isCompleted,
                    "completedAtMillis" to comp.completedAtMillis
                )
                userDoc.collection("checklist_completions")
                    .document(docKey)
                    .set(data, SetOptions.merge())
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to upload local data to cloud", e)
        }
    }

    private fun attachListeners(userId: String, scope: CoroutineScope) {
        val fs = firestore ?: return
        val userDoc = fs.collection("users").document(userId)

        // 1. Daily Summaries listener
        val summarySub = userDoc.collection("daily_summaries")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(tag, "Daily summaries listen error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    scope.launch(Dispatchers.IO) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                DailySummary(
                                    dateKey = doc.getString("dateKey") ?: doc.id,
                                    prabhupadaLectureWatched = doc.getBoolean("prabhupadaLectureWatched") ?: false,
                                    strongMayaAttack = doc.getBoolean("strongMayaAttack") ?: false,
                                    wakeUpTime = doc.getString("wakeUpTime"),
                                    bedTimePreviousNight = doc.getString("bedTimePreviousNight"),
                                    notes = doc.getString("notes") ?: "",
                                    updatedAtMillis = doc.getLong("updatedAtMillis") ?: System.currentTimeMillis()
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        if (list.isNotEmpty()) {
                            activityDao.upsertDailySummaries(list)
                        }
                        _syncStatus.value = SyncStatus.SYNCED
                        _lastSyncTimeMillis.value = System.currentTimeMillis()
                    }
                }
            }
        activeListeners.add(summarySub)

        // 2. Activity Sessions listener
        val sessionSub = userDoc.collection("activity_sessions")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(tag, "Sessions listen error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    scope.launch(Dispatchers.IO) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: return@mapNotNull null
                                val dateKey = doc.getString("dateKey") ?: return@mapNotNull null
                                val activityType = doc.getString("activityType") ?: "SLEEP"
                                ActivitySession(
                                    id = id,
                                    dateKey = dateKey,
                                    activityType = activityType,
                                    subType = doc.getString("subType") ?: "",
                                    startTimeMillis = doc.getLong("startTimeMillis") ?: System.currentTimeMillis(),
                                    endTimeMillis = doc.getLong("endTimeMillis"),
                                    durationMinutes = doc.getLong("durationMinutes") ?: 0L,
                                    note = doc.getString("note") ?: "",
                                    bookId = doc.getLong("bookId"),
                                    bookTitle = doc.getString("bookTitle")
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        if (list.isNotEmpty()) {
                            activityDao.upsertSessions(list)
                        }
                        _syncStatus.value = SyncStatus.SYNCED
                        _lastSyncTimeMillis.value = System.currentTimeMillis()
                    }
                }
            }
        activeListeners.add(sessionSub)

        // 3. Books listener
        val booksSub = userDoc.collection("books")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(tag, "Books listen error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    scope.launch(Dispatchers.IO) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: return@mapNotNull null
                                val title = doc.getString("title") ?: return@mapNotNull null
                                val statusStr = doc.getString("status") ?: "NOT_READ"
                                val status = try {
                                    BookStatus.valueOf(statusStr)
                                } catch (e: Exception) {
                                    BookStatus.NOT_READ
                                }
                                BookEntity(
                                    id = id,
                                    title = title,
                                    status = status,
                                    completionCount = (doc.getLong("completionCount") ?: 0L).toInt(),
                                    createdAtMillis = doc.getLong("createdAtMillis") ?: System.currentTimeMillis(),
                                    lastReadTimestamp = doc.getLong("lastReadTimestamp")
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        if (list.isNotEmpty()) {
                            bookDao.upsertBooks(list)
                        }
                        _syncStatus.value = SyncStatus.SYNCED
                        _lastSyncTimeMillis.value = System.currentTimeMillis()
                    }
                }
            }
        activeListeners.add(booksSub)

        // 4. Checklist Items listener
        val checklistSub = userDoc.collection("checklist_items")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(tag, "Checklist items listen error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    scope.launch(Dispatchers.IO) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: return@mapNotNull null
                                val title = doc.getString("title") ?: return@mapNotNull null
                                ChecklistItem(
                                    id = id,
                                    title = title,
                                    createdAtMillis = doc.getLong("createdAtMillis") ?: System.currentTimeMillis()
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        if (list.isNotEmpty()) {
                            activityDao.upsertChecklistItems(list)
                        }
                        _syncStatus.value = SyncStatus.SYNCED
                        _lastSyncTimeMillis.value = System.currentTimeMillis()
                    }
                }
            }
        activeListeners.add(checklistSub)

        // 5. Checklist Completions listener
        val completionSub = userDoc.collection("checklist_completions")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(tag, "Checklist completions listen error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    scope.launch(Dispatchers.IO) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                val dateKey = doc.getString("dateKey") ?: return@mapNotNull null
                                val itemId = doc.getLong("itemId") ?: return@mapNotNull null
                                DailyChecklistCompletion(
                                    dateKey = dateKey,
                                    itemId = itemId,
                                    isCompleted = doc.getBoolean("isCompleted") ?: false,
                                    completedAtMillis = doc.getLong("completedAtMillis") ?: System.currentTimeMillis()
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        if (list.isNotEmpty()) {
                            activityDao.upsertChecklistCompletions(list)
                        }
                        _syncStatus.value = SyncStatus.SYNCED
                        _lastSyncTimeMillis.value = System.currentTimeMillis()
                    }
                }
            }
        activeListeners.add(completionSub)
    }

    // Direct write helpers to immediately mirror local changes to Firestore
    fun syncSummaryToCloud(summary: DailySummary) {
        val uid = activeUserId ?: return
        val data = hashMapOf(
            "dateKey" to summary.dateKey,
            "prabhupadaLectureWatched" to summary.prabhupadaLectureWatched,
            "strongMayaAttack" to summary.strongMayaAttack,
            "wakeUpTime" to summary.wakeUpTime,
            "bedTimePreviousNight" to summary.bedTimePreviousNight,
            "notes" to summary.notes,
            "updatedAtMillis" to summary.updatedAtMillis
        )
        firestore?.collection("users")?.document(uid)
            ?.collection("daily_summaries")
            ?.document(summary.dateKey)
            ?.set(data, SetOptions.merge())
    }

    fun syncSessionToCloud(session: ActivitySession) {
        val uid = activeUserId ?: return
        val data = hashMapOf(
            "id" to session.id,
            "dateKey" to session.dateKey,
            "activityType" to session.activityType,
            "subType" to session.subType,
            "startTimeMillis" to session.startTimeMillis,
            "endTimeMillis" to session.endTimeMillis,
            "durationMinutes" to session.durationMinutes,
            "note" to session.note,
            "bookId" to session.bookId,
            "bookTitle" to session.bookTitle
        )
        firestore?.collection("users")?.document(uid)
            ?.collection("activity_sessions")
            ?.document(session.id.toString())
            ?.set(data, SetOptions.merge())
    }

    fun deleteSessionFromCloud(sessionId: Long) {
        val uid = activeUserId ?: return
        firestore?.collection("users")?.document(uid)
            ?.collection("activity_sessions")
            ?.document(sessionId.toString())
            ?.delete()
    }

    fun syncBookToCloud(book: BookEntity) {
        val uid = activeUserId ?: return
        val data = hashMapOf(
            "id" to book.id,
            "title" to book.title,
            "status" to book.status.name,
            "completionCount" to book.completionCount,
            "createdAtMillis" to book.createdAtMillis,
            "lastReadTimestamp" to book.lastReadTimestamp
        )
        firestore?.collection("users")?.document(uid)
            ?.collection("books")
            ?.document(book.id.toString())
            ?.set(data, SetOptions.merge())
    }

    fun deleteBookFromCloud(bookId: Long) {
        val uid = activeUserId ?: return
        firestore?.collection("users")?.document(uid)
            ?.collection("books")
            ?.document(bookId.toString())
            ?.delete()
    }

    fun syncChecklistItemToCloud(item: ChecklistItem) {
        val uid = activeUserId ?: return
        val data = hashMapOf(
            "id" to item.id,
            "title" to item.title,
            "createdAtMillis" to item.createdAtMillis
        )
        firestore?.collection("users")?.document(uid)
            ?.collection("checklist_items")
            ?.document(item.id.toString())
            ?.set(data, SetOptions.merge())
    }

    fun deleteChecklistItemFromCloud(itemId: Long) {
        val uid = activeUserId ?: return
        firestore?.collection("users")?.document(uid)
            ?.collection("checklist_items")
            ?.document(itemId.toString())
            ?.delete()
    }

    fun syncChecklistCompletionToCloud(completion: DailyChecklistCompletion) {
        val uid = activeUserId ?: return
        val docKey = "${completion.dateKey}_${completion.itemId}"
        val data = hashMapOf(
            "dateKey" to completion.dateKey,
            "itemId" to completion.itemId,
            "isCompleted" to completion.isCompleted,
            "completedAtMillis" to completion.completedAtMillis
        )
        firestore?.collection("users")?.document(uid)
            ?.collection("checklist_completions")
            ?.document(docKey)
            ?.set(data, SetOptions.merge())
    }

    suspend fun manualSyncNow(userId: String) = withContext(Dispatchers.IO) {
        _syncStatus.value = SyncStatus.SYNCING
        try {
            uploadLocalDataToCloud(userId)
            _syncStatus.value = SyncStatus.SYNCED
            _lastSyncTimeMillis.value = System.currentTimeMillis()
        } catch (e: Exception) {
            Log.e(tag, "Manual sync failed", e)
            _syncStatus.value = SyncStatus.ERROR
        }
    }
}
