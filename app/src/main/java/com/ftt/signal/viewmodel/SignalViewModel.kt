package com.ftt.signal.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.ftt.signal.data.model.*
import com.ftt.signal.data.repository.Result
import com.ftt.signal.data.repository.SignalRepository
import com.ftt.signal.db.AppDatabase
import com.ftt.signal.db.JournalEntry
import com.ftt.signal.prefs.AppPrefs
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class SignalUiState(
    val isLoading:   Boolean          = false,
    val signal:      ProcessedSignal?  = null,
    val prevSignal:  ProcessedSignal?  = null,
    val error:       String?           = null,
    val isCached:    Boolean           = false,
    val lastUpdated: String?           = null,
    val autoRefresh: Boolean           = true,
    val isOpen:      Boolean           = true,
    val closedReason:String            = "",
    val confHistory: List<Pair<Int, String>> = emptyList(),
)

class SignalViewModel(application: Application) : AndroidViewModel(application) {
    private val repo    = SignalRepository()
    private val prefs   = AppPrefs(application)
    private val dao     = AppDatabase.get(application).journalDao()

    private val _state = MutableStateFlow(SignalUiState())
    val state: StateFlow<SignalUiState> = _state.asStateFlow()

    val curPair    = prefs.curPair.stateIn(viewModelScope, SharingStarted.Eagerly, "EUR/USD")
    val soundOn    = prefs.soundOn.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val slPips     = prefs.slPips.stateIn(viewModelScope, SharingStarted.Eagerly, 15f)
    val tpPips     = prefs.tpPips.stateIn(viewModelScope, SharingStarted.Eagerly, 30f)
    val apiBase    = prefs.apiBase.stateIn(viewModelScope, SharingStarted.Eagerly, AppPrefs.DEFAULT_API)
    val otcApiBase = prefs.otcApiBase.stateIn(viewModelScope, SharingStarted.Eagerly, AppPrefs.DEFAULT_OTC_API)

    // Session-level cache
    private val signalCache   = mutableMapOf<String, ProcessedSignal>()
    private val cacheExpiry   = mutableMapOf<String, Long>()
    private var autoRefreshJob: Job? = null

    init { startAutoRefresh() }

    fun selectPair(pair: String) {
        viewModelScope.launch {
            prefs.set(AppPrefs.CUR_PAIR, pair)
            fetchSignal(pair, forceRefresh = false)
        }
    }

    fun fetchSignal(pair: String = curPair.value, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            // Check if market is open
            if (!repo.isMarketOpen(pair)) {
                _state.update { it.copy(
                    isLoading = false, isOpen = false,
                    closedReason = repo.whyClosed(pair)
                )}
                return@launch
            }

            // Check cache
            val cached     = signalCache[pair]
            val expiry     = cacheExpiry[pair] ?: 0L
            val isCacheOk  = !forceRefresh && cached != null && System.currentTimeMillis() < expiry

            if (isCacheOk && cached != null) {
                _state.update { it.copy(
                    isLoading = false, signal = cached, isCached = true, isOpen = true,
                    error = null
                )}
                return@launch
            }

            _state.update { it.copy(isLoading = true, isOpen = true, error = null) }

            when (val result = repo.getSignal(pair, apiBase.value, otcApiBase.value)) {
                is Result.Success -> {
                    val sig       = result.data
                    val prev      = signalCache[pair]
                    val isNew     = prev?.timestamp != sig.timestamp
                    val expMin    = sig.expiryMinutes
                    signalCache[pair]  = sig
                    cacheExpiry[pair]  = System.currentTimeMillis() + expMin * 60_000L

                    val newHist = (_state.value.confHistory + (sig.confidence to sig.label))
                        .takeLast(8)

                    _state.update { it.copy(
                        isLoading   = false, signal = sig, prevSignal = if (isNew) prev else it.prevSignal,
                        isCached    = false, error = null, isOpen = true,
                        lastUpdated = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()),
                        confHistory = newHist,
                    )}

                    // Auto-save to journal if new BUY/SELL
                    if (isNew && (sig.label == "BUY" || sig.label == "SELL")) {
                        saveToJournal(sig)
                    }
                }
                is Result.Error -> _state.update { it.copy(
                    isLoading = false, error = result.message
                )}
            }
        }
    }

    private suspend fun saveToJournal(sig: ProcessedSignal) {
        val entry = JournalEntry(
            pair          = sig.rawPair,
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
            timestamp     = System.currentTimeMillis(),
        )
        dao.insert(entry)
    }

    fun toggleAutoRefresh() {
        val new = !_state.value.autoRefresh
        _state.update { it.copy(autoRefresh = new) }
        if (new) startAutoRefresh() else autoRefreshJob?.cancel()
    }

    fun toggleSound() { viewModelScope.launch { prefs.set(AppPrefs.SOUND_ON, !soundOn.value) } }

    fun saveSLTP(sl: Float, tp: Float) {
        viewModelScope.launch {
            prefs.set(AppPrefs.SL_PIPS, sl)
            prefs.set(AppPrefs.TP_PIPS, tp)
        }
    }

    fun saveApiSettings(api: String, otc: String) {
        viewModelScope.launch {
            prefs.set(AppPrefs.API_BASE, api.trim())
            prefs.set(AppPrefs.OTC_API_BASE, otc.trim())
            signalCache.clear(); cacheExpiry.clear()
        }
    }

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(60_000L)
                if (_state.value.autoRefresh) fetchSignal(forceRefresh = false)
            }
        }
    }

    // SL/TP calc
    fun calcSLTP(pair: String, dir: String, ep: Double, sl: Float, tp: Float): Triple<String, String, String> {
        val ps = when {
            "JPY" in pair -> 0.01
            "XAU" in pair -> 0.1
            "BTC" in pair -> 1.0
            "ETH" in pair -> 0.1
            PairData.isCrypto(pair) -> 0.01
            else -> 0.0001
        }
        val slPrice = if (dir == "BUY") ep - sl * ps else ep + sl * ps
        val tpPrice = if (dir == "BUY") ep + tp * ps else ep - tp * ps
        val dec = when { "JPY" in pair -> 3; "XAU" in pair -> 2; "BTC" in pair -> 1; else -> 5 }
        val fmt = "%.${dec}f"
        val rr  = String.format("%.1f", tp / sl)
        return Triple(String.format(fmt, slPrice), String.format(fmt, tpPrice), rr)
    }

    override fun onCleared() { super.onCleared(); autoRefreshJob?.cancel() }
}

    fun saveLotPip(lot: Float, pip: Float) {
        viewModelScope.launch {
            prefs.set(AppPrefs.LOT_SIZE,  lot)
            prefs.set(AppPrefs.PIP_VALUE, pip)
        }
    }
