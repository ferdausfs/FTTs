package com.ftt.signal.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room database definition.
 *
 * The singleton instance is now managed by Hilt via [DatabaseModule].
 * The old manual companion-object singleton has been removed to avoid
 * double-instance bugs.
 */
@Database(entities = [JournalEntry::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun journalDao(): JournalDao
}
