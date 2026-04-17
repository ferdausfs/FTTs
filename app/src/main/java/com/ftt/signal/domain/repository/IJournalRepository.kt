package com.ftt.signal.domain.repository

import com.ftt.signal.db.JournalEntry
import kotlinx.coroutines.flow.Flow

/**
 * Domain-layer contract for trade-journal persistence.
 */
interface IJournalRepository {
    fun getAllEntries(): Flow<List<JournalEntry>>
    suspend fun insert(entry: JournalEntry): Long
    suspend fun markResult(id: Long, result: String)
    suspend fun saveNote(id: Long, note: String)
    suspend fun saveExit(id: Long, exitPrice: String, pips: Float)
    suspend fun delete(id: Long)
    suspend fun deleteAll()
}
