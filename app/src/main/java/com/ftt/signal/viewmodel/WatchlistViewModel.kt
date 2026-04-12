package com.ftt.signal.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.ftt.signal.data.model.ProcessedSignal
import com.ftt.signal.data.repository.Result
import com.ftt.signal.data.repository.SignalRepository
import com.ftt.signal.db.AppDatabase
import com.ftt.signal.db.JournalEntry
import com.ftt.signal.prefs.AppPrefs
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class WlResult(
    val label: String    = "",
    val confidence: Int  = 0,
    val grade: String    = "",
    val timestamp: String = "",
    val expiryAt: Long   = 0L,
    val isNew: Boolean   = false,
    val loading: Boolean = false,
    val error: String    = "",
    val signal: ProcessedSignal? = null
)

data class WatchlistUiState(
    val pairs: List<String>           = emptyList(),
    val results: Map<String, WlResult> = emptyMap(),
    val active: Boolean               = false,
    val scanning: Boolean             = false,
    val scanProgress: Int             = 0,
    val scanTotal: Int                = 0,
    val intervalMin: Int              = 1,
    val filter: String                = "all",
    val sort: String                  = "conf",
    val newCount: Int                 = 0,
    val apiUsed: Int                  = 0,
    val apiLimit: Int                 = 1600,
)

class WatchlistViewModel(application: Application) : AndroidViewModel(application) {
    private val repo  = SignalRepository()
    private val prefs = AppPrefs(application)
    private val dao   = AppDatabase.get(application).journalDao()

    private val _state = MutableStateFlow(WatchlistUiState())
    val state: StateFlow<WatchlistUiState> = _state.asStateFlow()

    val apiBase    = prefs.apiBase.stateIn(viewModelScope, SharingStarted.Eagerly, AppPrefs.DEFAULT_API)
    val otcApiBase = prefs.otcApiBase.stateIn(viewModelScope, SharingStarted.Eagerly, AppPrefs.DEFAULT_OTC_API)

    private var scanTimer: Job? = null

    init {
        viewModelScope.launch {
            prefs.wlPairsStr.collect { str ->
                val pairs = if (str.isBlank()) emptyList()
                else str.split(",").filter { it.isNotBlank() }
                _state.update { it.copy(pairs = pairs) }
            }
        }
        viewModelScope.launch {
            prefs.wlInterval.collect { iv -> _state.update { it.copy(intervalMin = iv) } }
        }
    }

    fun addPair(pair: String) {
        val cur = _state.value.pairs
        if (cur.contains(pair)) return
        val new = cur + pair
        savePairs(new)
    }

    fun removePair(pair: String) {
        val new = _state.value.pairs.filter { it != pair }
        savePairs(new)
        _state.update { it.copy(results = it.results - pair) }
    }

    private fun savePairs(pairs: List<String>) {
        viewModelScope.launch {
            prefs.set(AppPrefs.WL_PAIRS, pairs.joinToString(","))
            _state.update { it.copy(pairs = pairs) }
        }
    }

    fun setInterval(min: Int) {
        viewModelScope.launch {
            prefs.set(AppPrefs.WL_INTERVAL, min)
            _state.update { it.copy(intervalMin = min) }
            if (_state.value.active) { stopScan(); startScan() }
        }
    }

    fun toggleScan() {
        if (_state.value.active) stopScan() else startScan()
    }

    fun startScan() {
        _state.update { it.copy(active = true) }
        runScan()
        val iv = _state.value.intervalMin * 60_000L
        scanTimer = viewModelScope.launch {
            while (isActive) { delay(iv); runScan() }
        }
    }

    fun stopScan() {
        scanTimer?.cancel()
        scanTimer = null
        _state.update { it.copy(active = false) }
    }

    fun runScan() {
        val pairs = _state.value.pairs
        if (pairs.isEmpty()) return
        viewModelScope.launch {
            _state.update { st ->
                val loadingResults = st.results.toMutableMap()
                pairs.forEach { p -> loadingResults[p] = loadingResults[p]?.copy(loading = true) ?: WlResult(loading = true) }
                st.copy(scanning = true, scanProgress = 0, scanTotal = pairs.size, results = loadingResults)
            }

            var newCount = _state.value.newCount
            val jobs = pairs.mapIndexed { i, pair ->
                async {
                    delay(i * 150L) // stagger to avoid rate limit
                    if (!repo.isMarketOpen(pair)) {
                        return@async pair to WlResult(label = "CLOSED", loading = false)
                    }
                    when (val r = repo.getSignal(pair, apiBase.value, otcApiBase.value)) {
                        is Result.Success -> {
                            val sig  = r.data
                            val prev = _state.value.results[pair]
                            val isNew = prev != null && prev.timestamp.isNotEmpty() && prev.timestamp != sig.timestamp
                            if (isNew) newCount++
                            if (isNew && (sig.label == "BUY" || sig.label == "SELL")) {
                                dao.insert(JournalEntry(
                                    pair = pair, dir = sig.label, conf = sig.confidence,
                                    grade = sig.grade, entryPrice = sig.entryPrice,
                                    exitPrice = null, pips = null, result = "PENDING",
                                    session = sig.sessionLabel, expiryMinutes = sig.expiryMinutes,
                                    expiryAt = System.currentTimeMillis() + sig.expiryMinutes * 60_000L
                                ))
                            }
                            pair to WlResult(
                                label = sig.label, confidence = sig.confidence, grade = sig.grade,
                                timestamp = sig.timestamp, isNew = isNew, loading = false,
                                expiryAt = if (isNew) System.currentTimeMillis() + sig.expiryMinutes * 60_000L
                                else prev?.expiryAt ?: 0L,
                                signal = sig
                            )
                        }
                        is Result.Error -> pair to WlResult(
                            label = "ERR", loading = false, error = r.message
                        )
                    }
                }
            }

            jobs.forEach { job ->
                val (pair, result) = job.await()
                _state.update { st ->
                    val newResults = st.results.toMutableMap()
                    newResults[pair] = result
                    st.copy(
                        results = newResults,
                        scanProgress = st.scanProgress + 1,
                        newCount = newCount
                    )
                }
            }

            _state.update { it.copy(scanning = false, newCount = newCount) }
        }
    }

    fun setFilter(f: String) = _state.update { it.copy(filter = f) }
    fun setSort(s: String)   = _state.update { it.copy(sort = s) }
    fun clearNewCount()      = _state.update { it.copy(newCount = 0) }

    fun addToJournal(pair: String) {
        val sig = _state.value.results[pair]?.signal ?: return
        viewModelScope.launch {
            dao.insert(JournalEntry(
                pair = pair, dir = sig.label, conf = sig.confidence, grade = sig.grade,
                entryPrice = sig.entryPrice, exitPrice = null, pips = null, result = "PENDING",
                session = sig.sessionLabel, expiryMinutes = sig.expiryMinutes,
                expiryAt = System.currentTimeMillis() + sig.expiryMinutes * 60_000L
            ))
        }
    }
}
