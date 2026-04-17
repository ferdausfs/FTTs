package com.ftt.signal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ftt.signal.db.JournalEntry
import com.ftt.signal.domain.usecase.GetJournalEntriesUseCase
import com.ftt.signal.domain.usecase.UpdateJournalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the trade journal screen.
 * Delegates all persistence to [UpdateJournalUseCase] — zero DB
 * references in the ViewModel layer.
 */
@HiltViewModel
class JournalViewModel @Inject constructor(
    getJournalEntriesUseCase: GetJournalEntriesUseCase,
    private val updateJournalUseCase: UpdateJournalUseCase,
) : ViewModel() {

    val allEntries: StateFlow<List<JournalEntry>> =
        getJournalEntriesUseCase()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun markResult(id: Long, result: String) =
        viewModelScope.launch { updateJournalUseCase.markResult(id, result) }

    fun saveNote(id: Long, note: String) =
        viewModelScope.launch { updateJournalUseCase.saveNote(id, note) }

    fun saveExit(id: Long, exit: String, pips: Float) =
        viewModelScope.launch { updateJournalUseCase.saveExit(id, exit, pips) }

    fun delete(id: Long) =
        viewModelScope.launch { updateJournalUseCase.delete(id) }

    fun clearAll() =
        viewModelScope.launch { updateJournalUseCase.deleteAll() }
}
