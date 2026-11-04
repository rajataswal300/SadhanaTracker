# Book Reading Library & Tracking System — Implementation Plan

Add comprehensive book cataloging, status management, re-reading counters, and reading session integration to the **Book Reading** view in the Daily Tracker.

---

## User Review & Critical Decisions

Based on user feedback collected during the planning phase:
1. **Access Point**: Dedicated **Book Library Modal Sheet** accessible directly via a *"My Library"* / *"Book List"* button inside the Reading card on the dashboard.
2. **Timer Session Linking**: An **optional book selector** on the active Reading timer card allowing users to tag their reading session with one of their currently reading or pending books (or read without tagging).
3. **Completion & Re-reading**: Marking a book as finished increments its **`completionCount` by 1**, moves it to Completed, and allows immediate or subsequent **Re-reading** (which moves it back to *Currently Reading* with count preserved).

---

## 1. Overview & Core Concept

Currently, the Book Reading section tracks cumulative reading time, pages read, session history, and provides active timer/stopwatch controls. 

This enhancement transforms the reading feature into a structured reading tracker:
- **Feed & Catalog Books**: Users can input books they want to read now or add anytime later.
- **Three-Tier Status Lifecycle**:
  - `NOT_READ` (Pending / To Read)
  - `READING` (Currently Reading — multiple books can be in progress simultaneously)
  - `COMPLETED` (Read one or more times)
- **Re-Reading Counter**: Tracks how many times each book has been completed (e.g., *"Completed 3 times"*).
- **Session Attribution**: When starting a reading timer, users can choose which book they are reading. Completed session logs display the book title.
- **Quick Action Workflows**:
  - *Start Reading*: Moves a pending or completed book into *Currently Reading*.
  - *Mark as Finished*: Increments completion count ($+1$), clears active reading status, and moves to *Completed*.
  - *Re-Read*: Reactivates a completed book back to *Currently Reading*.
  - *Edit / Delete*: Update book titles or remove books with confirmation.

---

## 2. User Experience & Visual Design

### A. Dashboard Reading Card Enhancements (`BookReadingCard.kt`)
- **Currently Reading Indicator**:
  - If books are marked as *Currently Reading*, displays a carousel/chip row: e.g., 📖 *"Bhagavad Gita (2nd time)"*, 📖 *"Srimad Bhagavatam Canto 1"*.
- **"Book Library" Button**:
  - Prominent outlined button with badge showing total books and active reading count (e.g., `📚 Library (2 in progress)`).
- **Session Book Tag Dropdown/Chips**:
  - When timer is stopped or running, user can optionally tap a chip: e.g., *"Reading: [ Select Book ▾ ]"*.
  - Selected book is logged in `ActivitySession` (`bookTitle`).

### B. Book Library Modal Bottom Sheet (`BookLibrarySheet.kt`)
- **Header**: Title *"Book Library"*, close button, search bar for large collections.
- **Filter Segmented Tabs**:
  - **All** (count)
  - **Currently Reading** (count with pulse indicator)
  - **Pending / To Read** (count)
  - **Completed** (count)
- **Add Book Input Bar**:
  - Outlined text field with quick *"Add Book"* button and keyboard action `Done`.
  - Automatically adds book with `status = NOT_READ`, `completionCount = 0`.
- **Book Item Cards**:
  - Book title (bold, readable M3 typography).
  - Status pill / badge:
    - 🔵 *In Progress* (green/primary badge)
    - ⏳ *To Read* (neutral/outline badge)
    - ✅ *Completed* (surface variant badge with completion count, e.g. *"Completed 2x"*)
  - Contextual Quick Actions:
    - If `NOT_READ`: `[Start Reading]` (changes to `READING`).
    - If `READING`: `[Mark as Finished]` (increments count, sets `COMPLETED`).
    - If `COMPLETED`: `[Re-read]` (sets `READING`), `[+1 Read]` (manual increment).
    - Overflow menu: *Edit Title*, *Delete Book*.

---

## 3. Key Product Decisions & Trade-Offs

| Decision | Chosen Approach | Rationale |
| :--- | :--- | :--- |
| **Storage Architecture** | Room SQLite Database table `books` | Persists books locally alongside existing `ActivitySession` and `DailySummary` tables. Zero network dependency, instant reactive updates via `Flow`. |
| **Multiple Active Books** | Supported simultaneously | Users frequently alternate between multiple books (e.g., morning scripture study vs. evening philosophy). |
| **Timer Tagging** | Optional book tag | Prevents blocking the timer start if the user wants a quick untagged reading session, while rewarding tagging when selected. |
| **Re-reading Count Lifecycle** | `completionCount: Int` incremented on finish | Preserves accurate historical count without losing the book entity or creating duplicate entries. |

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────┐
│                   BookReadingCard                      │
│   (Active Timer, Session Stats, Active Book Chips)     │
└──────────┬───────────────────────────────┬─────────────┘
           │ (Open Library)                │ (Tag Session)
           ▼                               ▼
┌─────────────────────────┐     ┌────────────────────────┐
│    BookLibrarySheet     │     │    ActivitySession     │
│  - Filter Tabs          │     │    - bookId: Long?     │
│  - Add Book Form        │     │    - bookTitle: String?│
│  - Book List & Actions  │     └──────────┬─────────────┘
└──────────┬──────────────┘                │
           ▼                               │
┌──────────────────────────────────────────┴─────────────┐
│                 DailyTrackerViewModel                  │
│   - books: StateFlow<List<BookEntity>>                 │
│   - activeBooks: StateFlow<List<BookEntity>>           │
│   - addBook(), updateStatus(), incrementCompletion()   │
└──────────────────────────┬─────────────────────────────┘
                           ▼
┌────────────────────────────────────────────────────────┐
│                    TrackerRepository                   │
│   - observeBooks(), insertBook(), updateBook()         │
└──────────────────────────┬─────────────────────────────┘
                           ▼
┌────────────────────────────────────────────────────────┐
│                  Room: BookDao & DB                    │
│   - Table: `books`                                     │
│     (id, title, status, completionCount, createdAt)    │
└────────────────────────────────────────────────────────┘
```

### A. Room Entity: `BookEntity`
```kotlin
enum class BookStatus {
    NOT_READ,    // Pending / To Read
    READING,     // Currently reading
    COMPLETED    // Finished (at least once)
}

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val author: String = "",
    val status: BookStatus = BookStatus.NOT_READ,
    val completionCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastReadTimestamp: Long? = null
)
```

### B. Room DAO: `BookDao`
- `getAllBooks(): Flow<List<BookEntity>>`
- `getBooksByStatus(status: BookStatus): Flow<List<BookEntity>>`
- `insertBook(book: BookEntity): Long`
- `updateBook(book: BookEntity)`
- `deleteBook(book: BookEntity)`
- `incrementCompletion(bookId: Long, timestamp: Long)`

### C. Migration & Database Update
- Add `BookEntity::class` to `AppDatabase.kt` entity list.
- Bump database version (with existing destructive fallback or automatic migration).

### D. Automated Testing Strategy
- Add unit tests in `ExampleRobolectricTest.kt`:
  - Verify inserting a book defaults to `NOT_READ` and `completionCount = 0`.
  - Verify marking as `READING` updates status.
  - Verify finishing a book increments `completionCount` by 1 and marks `COMPLETED`.
  - Verify re-reading preserves previous `completionCount` and sets status back to `READING`.
  - Verify filtering by status (`NOT_READ`, `READING`, `COMPLETED`).
