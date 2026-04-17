package com.ftt.signal.data.repository

import com.ftt.signal.db.JournalDao
import com.ftt.signal.db.JournalEntry
import com.ftt.signal.domain.repository.IJournalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [IJournalRepository] backed by Room.
 */
@Singleton
class JournalRepositoryImpl @Inject constructor(
    private val dao: JournalDao
) : IJournalRepository {

    override fun getAllEntries(): Flow<List<JournalEntry>>    = dao.getAll()
    override suspend fun insert(entry: JournalEntry): Long   = dao.insert(entry)
    override suspend fun markResult(id: Long, result: String) = dao.markResult(id, result)
    override suspend fun saveNote(id: Long, note: String)    = dao.saveNote(id, note)
    override suspend fun saveExit(id: Long, exitPrice: String, pips: Float) = dao.saveExit(id, exitPrice, pips)
    override suspend fun delete(id: Long)                    = dao.delete(id)
    override suspend fun deleteAll()                         = dao.deleteAll()
}
