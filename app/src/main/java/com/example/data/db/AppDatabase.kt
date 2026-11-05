package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.ActivitySession
import com.example.data.model.BookEntity
import com.example.data.model.ChecklistItem
import com.example.data.model.DailyChecklistCompletion
import com.example.data.model.DailySummary

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ActivitySession::class,
        DailySummary::class,
        ChecklistItem::class,
        DailyChecklistCompletion::class,
        BookEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao
    abstract fun bookDao(): BookDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `books` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `author` TEXT NOT NULL DEFAULT '',
                        `status` TEXT NOT NULL DEFAULT 'NOT_READ',
                        `completionCount` INTEGER NOT NULL DEFAULT 0,
                        `createdAtMillis` INTEGER NOT NULL DEFAULT 0,
                        `lastReadTimestamp` INTEGER
                    )
                """.trimIndent())
                try {
                    db.execSQL("ALTER TABLE `activity_sessions` ADD COLUMN `bookId` INTEGER")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `activity_sessions` ADD COLUMN `bookTitle` TEXT")
                } catch (e: Exception) {}
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `books_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `status` TEXT NOT NULL DEFAULT 'NOT_READ',
                        `completionCount` INTEGER NOT NULL DEFAULT 0,
                        `createdAtMillis` INTEGER NOT NULL DEFAULT 0,
                        `lastReadTimestamp` INTEGER
                    )
                """.trimIndent())
                try {
                    db.execSQL("""
                        INSERT INTO `books_new` (`id`, `title`, `status`, `completionCount`, `createdAtMillis`, `lastReadTimestamp`)
                        SELECT `id`, `title`, `status`, `completionCount`, `createdAtMillis`, `lastReadTimestamp` FROM `books`
                    """.trimIndent())
                    db.execSQL("DROP TABLE `books`")
                    db.execSQL("ALTER TABLE `books_new` RENAME TO `books`")
                } catch (e: Exception) {
                    // Fallback to table creation if books didn't exist
                    db.execSQL("ALTER TABLE `books_new` RENAME TO `books`")
                }
            }
        }

        val MIGRATION_3_5 = object : Migration(3, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `books` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `status` TEXT NOT NULL DEFAULT 'NOT_READ',
                        `completionCount` INTEGER NOT NULL DEFAULT 0,
                        `createdAtMillis` INTEGER NOT NULL DEFAULT 0,
                        `lastReadTimestamp` INTEGER
                    )
                """.trimIndent())
                try {
                    db.execSQL("ALTER TABLE `activity_sessions` ADD COLUMN `bookId` INTEGER")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `activity_sessions` ADD COLUMN `bookTitle` TEXT")
                } catch (e: Exception) {}
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sadhana_tracker.db"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_3_5)
                    .fallbackToDestructiveMigrationFrom(1, 2)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
