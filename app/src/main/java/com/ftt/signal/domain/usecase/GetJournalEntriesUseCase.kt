package com.ftt.signal.domain.usecase

import com.ftt.signal.db.JournalEntry
import com.ftt.signal.domain.repository.IJournalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Retrieves all journal entries as a reactive Flow. */
class GetJournalEntriesUseCase @Inject constructor(
    private val repository: IJournalRepository
) {
    operator fun invoke(): Flow<List<JournalEntry>> = repository.getAllEntries()
}
