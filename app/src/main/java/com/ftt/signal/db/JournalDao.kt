package com.ftt.signal.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal ORDER BY timestamp DESC")
    fun getAll(): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal WHERE id = :id")
    suspend fun getById(id: Long): JournalEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: JournalEntry): Long

    @Update
    suspend fun update(entry: JournalEntry)

    @Query("UPDATE journal SET result = :result WHERE id = :id")
    suspend fun markResult(id: Long, result: String)

    @Query("UPDATE journal SET note = :note WHERE id = :id")
    suspend fun saveNote(id: Long, note: String)

    @Query("UPDATE journal SET exitPrice = :exitPrice, pips = :pips WHERE id = :id")
    suspend fun saveExit(id: Long, exitPrice: String, pips: Float)

    @Query("DELETE FROM journal WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM journal")
    suspend fun deleteAll()
}
