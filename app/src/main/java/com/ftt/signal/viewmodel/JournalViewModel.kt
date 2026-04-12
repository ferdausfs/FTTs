package com.ftt.signal.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.ftt.signal.db.AppDatabase
import com.ftt.signal.db.JournalEntry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class JournalViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.get(application).journalDao()
    val allEntries: StateFlow<List<JournalEntry>> =
        dao.getAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun markResult(id: Long, result: String) = viewModelScope.launch { dao.markResult(id, result) }
    fun saveNote(id: Long, note: String)     = viewModelScope.launch { dao.saveNote(id, note) }
    fun delete(id: Long)                     = viewModelScope.launch { dao.delete(id) }
    fun clearAll()                           = viewModelScope.launch { dao.deleteAll() }
    fun saveExit(id: Long, exit: String, pips: Float) =
        viewModelScope.launch { dao.saveExit(id, exit, pips) }
}
