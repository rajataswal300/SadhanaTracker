package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BookEntity
import com.example.data.model.BookStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY createdAtMillis DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE status = :status ORDER BY lastReadTimestamp DESC, createdAtMillis DESC")
    fun getBooksByStatus(status: BookStatus): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    suspend fun getBookById(id: Long): BookEntity?

    @Query("SELECT COUNT(*) FROM books")
    suspend fun getBooksCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Update
    suspend fun updateBook(book: BookEntity)

    @Delete
    suspend fun deleteBook(book: BookEntity)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteBookById(id: Long)

    @Query("UPDATE books SET title = :newTitle WHERE id = :id")
    suspend fun updateBookTitle(id: Long, newTitle: String)

    @Query("UPDATE books SET status = :newStatus, lastReadTimestamp = :timestamp WHERE id = :id")
    suspend fun updateBookStatus(id: Long, newStatus: BookStatus, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE books SET status = :newStatus, completionCount = completionCount + 1, lastReadTimestamp = :timestamp WHERE id = :id")
    suspend fun markBookCompleted(id: Long, newStatus: BookStatus = BookStatus.COMPLETED, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE books SET completionCount = completionCount + 1, lastReadTimestamp = :timestamp WHERE id = :id")
    suspend fun incrementCompletionCount(id: Long, timestamp: Long = System.currentTimeMillis())
}
