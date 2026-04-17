package com.ftt.signal.domain.usecase

import com.ftt.signal.domain.repository.IJournalRepository
import javax.inject.Inject

/** Handles all journal mutation operations (mark result, note, exit). */
class UpdateJournalUseCase @Inject constructor(
    private val repository: IJournalRepository
) {
    suspend fun markResult(id: Long, result: String) = repository.markResult(id, result)
    suspend fun saveNote(id: Long, note: String)     = repository.saveNote(id, note)
    suspend fun saveExit(id: Long, exit: String, pips: Float) = repository.saveExit(id, exit, pips)
    suspend fun delete(id: Long)                     = repository.delete(id)
    suspend fun deleteAll()                          = repository.deleteAll()
}
