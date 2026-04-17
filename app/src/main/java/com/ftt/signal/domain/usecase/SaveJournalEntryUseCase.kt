package com.ftt.signal.domain.usecase

import com.ftt.signal.db.JournalEntry
import com.ftt.signal.domain.repository.IJournalRepository
import javax.inject.Inject

/** Persists a new journal entry (auto-save on new BUY/SELL signal). */
class SaveJournalEntryUseCase @Inject constructor(
    private val repository: IJournalRepository
) {
    suspend operator fun invoke(entry: JournalEntry): Long = repository.insert(entry)
}
