package com.ftt.signal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ftt.signal.data.model.ProcessedSignal
import com.ftt.signal.data.repository.Result
import com.ftt.signal.db.JournalEntry
import com.ftt.signal.domain.usecase.GetSignalUseCase
import com.ftt.signal.domain.usecase.SaveJournalEntryUseCase
import com.ftt.signal.prefs.AppPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class WlResult(
    val label:     String          = "",
    val confidence:Int             = 0,
    val grade:     String          = "",
    val timestamp: String          = "",
    val expiryAt:  Long            = 0L,
    val isNew:     Boolean         = false,
    val loading:   Boolean         = false,
    val error:     String          = "",
    val signal:    ProcessedSignal? = null,
)

data class WatchlistUiState(
    val pairs:        List<String>            = emptyList(),
    val results:      Map<String, WlResult>   = emptyMap(),
    val active:       Boolean                 = false,
    val scanning:     Boolean                 = false,
    val scanProgress: Int                     = 0,
    val scanTotal:    Int                     = 0,
    val intervalMin:  Int                     = 1,
    val filter:       String                  = "all",
    val sort:         String                  = "conf",
    val newCount:     Int                     = 0,
)

/**
 * ViewModel for the watchlist / multi-pair scan screen.
 * Uses [GetSignalUseCase] and [SaveJournalEntryUseCase] instead of
 * accessing the repository or DAO directly.
 */
@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val getSignalUseCase:       GetSignalUseCase,
    private val saveJournalEntryUseCase: SaveJournalEntryUseCase,
    private val prefs:                  AppPrefs,
) : ViewModel() {

    private val _state = MutableStateFlow(WatchlistUiState())
    val state: StateFlow<WatchlistUiState> = _state.asStateFlow()

    val apiBase    = prefs.apiBase.stateIn(viewModelScope, SharingStarted.Eagerly, AppPrefs.DEFAULT_API)
    val otcApiBase = prefs.otcApiBase.stateIn(viewModelScope, SharingStarted.Eagerly, AppPrefs.DEFAULT_OTC_API)

    private var scanTimer: Job? = null

    init {
        // Sync persisted pairs list from DataStore
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

    // ── Pair management ───────────────────────────────────────

    fun addPair(pair: String) {
        val cur = _state.value.pairs
        if (cur.contains(pair)) return
        savePairs(cur + pair)
    }

    fun removePair(pair: String) {
        savePairs(_state.value.pairs.filter { it != pair })
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

    // ── Scan lifecycle ────────────────────────────────────────

    fun toggleScan() { if (_state.value.active) stopScan() else startScan() }

    fun startScan() {
        _state.update { it.copy(active = true) }
        runScan()
        val intervalMs = _state.value.intervalMin * 60_000L
        scanTimer = viewModelScope.launch {
            while (isActive) { delay(intervalMs); runScan() }
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
            // Mark every pair as loading
            _state.update { st ->
                val loading = st.results.toMutableMap()
                pairs.forEach { p ->
                    loading[p] = loading[p]?.copy(loading = true) ?: WlResult(loading = true)
                }
                st.copy(scanning = true, scanProgress = 0, scanTotal = pairs.size, results = loading)
            }

            var newCount = _state.value.newCount

            // Stagger requests 150ms apart to avoid rate-limiting
            val jobs = pairs.mapIndexed { i, pair ->
                async {
                    delay(i * 150L)
                    if (!getSignalUseCase.isMarketOpen(pair)) {
                        return@async pair to WlResult(label = "CLOSED", loading = false)
                    }
                    when (val r = getSignalUseCase(pair, apiBase.value, otcApiBase.value)) {
                        is Result.Success -> {
                            val sig  = r.data
                            val prev = _state.value.results[pair]
                            val isNew = prev != null
                                && prev.timestamp.isNotEmpty()
                                && prev.timestamp != sig.timestamp
                            if (isNew) newCount++
                            // Auto-journal new BUY/SELL signals
                            if (isNew && (sig.label == "BUY" || sig.label == "SELL")) {
                                saveJournalEntryUseCase(
                                    JournalEntry(
                                        pair          = pair,
                                        dir           = sig.label,
                                        conf          = sig.confidence,
                                        grade         = sig.grade,
                                        entryPrice    = sig.entryPrice,
                                        exitPrice     = null,
                                        pips          = null,
                                        result        = "PENDING",
                                        session       = sig.sessionLabel,
                                        expiryMinutes = sig.expiryMinutes,
                                        expiryAt      = System.currentTimeMillis() + sig.expiryMinutes * 60_000L,
                                    )
                                )
                            }
                            pair to WlResult(
                                label      = sig.label,
                                confidence = sig.confidence,
                                grade      = sig.grade,
                                timestamp  = sig.timestamp,
                                isNew      = isNew,
                                loading    = false,
                                expiryAt   = if (isNew)
                                    System.currentTimeMillis() + sig.expiryMinutes * 60_000L
                                else prev?.expiryAt ?: 0L,
                                signal     = sig,
                            )
                        }
                        is Result.Error -> pair to WlResult(label = "ERR", loading = false, error = r.message)
                    }
                }
            }

            // Update state as each job completes
            jobs.forEach { job ->
                val (pair, result) = job.await()
                _state.update { st ->
                    st.copy(
                        results      = st.results.toMutableMap().also { it[pair] = result },
                        scanProgress = st.scanProgress + 1,
                        newCount     = newCount,
                    )
                }
            }

            _state.update { it.copy(scanning = false, newCount = newCount) }
        }
    }

    // ── Misc helpers ──────────────────────────────────────────

    fun setFilter(f: String) = _state.update { it.copy(filter = f) }
    fun setSort(s: String)   = _state.update { it.copy(sort = s) }
    fun clearNewCount()      = _state.update { it.copy(newCount = 0) }

    fun addToJournal(pair: String) {
        val sig = _state.value.results[pair]?.signal ?: return
        viewModelScope.launch {
            saveJournalEntryUseCase(
                JournalEntry(
                    pair          = pair,
                    dir           = sig.label,
                    conf          = sig.confidence,
                    grade         = sig.grade,
                    entryPrice    = sig.entryPrice,
                    exitPrice     = null,
                    pips          = null,
                    result        = "PENDING",
                    session       = sig.sessionLabel,
                    expiryMinutes = sig.expiryMinutes,
                    expiryAt      = System.currentTimeMillis() + sig.expiryMinutes * 60_000L,
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        scanTimer?.cancel()
    }
}
