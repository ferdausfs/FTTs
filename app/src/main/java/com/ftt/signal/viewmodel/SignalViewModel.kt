package com.ftt.signal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ftt.signal.NotificationHelper
import com.ftt.signal.data.model.*
import com.ftt.signal.data.repository.Result
import com.ftt.signal.db.JournalEntry
import com.ftt.signal.domain.usecase.GetSignalUseCase
import com.ftt.signal.domain.usecase.SaveJournalEntryUseCase
import com.ftt.signal.prefs.AppPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class SignalUiState(
    val isLoading:    Boolean           = false,
    val signal:       ProcessedSignal?  = null,
    val prevSignal:   ProcessedSignal?  = null,
    val error:        String?           = null,
    val isCached:     Boolean           = false,
    val lastUpdated:  String?           = null,
    val autoRefresh:  Boolean           = true,
    val isOpen:       Boolean           = true,
    val closedReason: String            = "",
    val confHistory:  List<Pair<Int, String>> = emptyList(),
)

/**
 * ViewModel for the main signal screen.
 *
 * Injected via Hilt — no manual ViewModelProvider needed.
 * Owns the in-memory signal cache so refreshes are fast.
 */
@HiltViewModel
class SignalViewModel @Inject constructor(
    private val getSignalUseCase:      GetSignalUseCase,
    private val saveJournalEntryUseCase: SaveJournalEntryUseCase,
    private val prefs:                 AppPrefs,
    private val notificationHelper:    NotificationHelper,
) : ViewModel() {

    private val _state = MutableStateFlow(SignalUiState())
    val state: StateFlow<SignalUiState> = _state.asStateFlow()

    val curPair    = prefs.curPair.stateIn(viewModelScope, SharingStarted.Eagerly, "EUR/USD")
    val soundOn    = prefs.soundOn.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val slPips     = prefs.slPips.stateIn(viewModelScope, SharingStarted.Eagerly, 15f)
    val tpPips     = prefs.tpPips.stateIn(viewModelScope, SharingStarted.Eagerly, 30f)
    val apiBase    = prefs.apiBase.stateIn(viewModelScope, SharingStarted.Eagerly, AppPrefs.DEFAULT_API)
    val otcApiBase = prefs.otcApiBase.stateIn(viewModelScope, SharingStarted.Eagerly, AppPrefs.DEFAULT_OTC_API)

    /** Per-session in-memory cache: pair → (signal, expiryMs) */
    private val signalCache  = mutableMapOf<String, ProcessedSignal>()
    private val cacheExpiry  = mutableMapOf<String, Long>()
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
            // Gate on market hours for Forex pairs
            if (!getSignalUseCase.isMarketOpen(pair)) {
                _state.update {
                    it.copy(
                        isLoading    = false,
                        isOpen       = false,
                        closedReason = getSignalUseCase.whyClosed(pair)
                    )
                }
                return@launch
            }

            // Serve from cache when still fresh
            val cached    = signalCache[pair]
            val expiry    = cacheExpiry[pair] ?: 0L
            val cacheHit  = !forceRefresh && cached != null && System.currentTimeMillis() < expiry

            if (cacheHit && cached != null) {
                _state.update { it.copy(isLoading = false, signal = cached, isCached = true, isOpen = true, error = null) }
                return@launch
            }

            _state.update { it.copy(isLoading = true, isOpen = true, error = null) }

            when (val result = getSignalUseCase(pair, apiBase.value, otcApiBase.value)) {
                is Result.Success -> {
                    val sig   = result.data
                    val prev  = signalCache[pair]
                    val isNew = prev?.timestamp != sig.timestamp

                    // Update cache
                    signalCache[pair] = sig
                    cacheExpiry[pair] = System.currentTimeMillis() + sig.expiryMinutes * 60_000L

                    val newHist = (_state.value.confHistory + (sig.confidence to sig.label)).takeLast(8)

                    _state.update {
                        it.copy(
                            isLoading   = false,
                            signal      = sig,
                            prevSignal  = if (isNew) prev else it.prevSignal,
                            isCached    = false,
                            error       = null,
                            isOpen      = true,
                            lastUpdated = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
                            confHistory = newHist,
                        )
                    }

                    // Auto-save new actionable signals to journal + notify
                    if (isNew && (sig.label == "BUY" || sig.label == "SELL")) {
                        saveToJournal(sig)
                        notificationHelper.notifySignal(sig.symbol, sig.label, sig.grade)
                    }
                }
                is Result.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
            }
        }
    }

    private suspend fun saveToJournal(sig: ProcessedSignal) {
        saveJournalEntryUseCase(
            JournalEntry(
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
        )
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

    fun saveLotPip(lot: Float, pip: Float) {
        viewModelScope.launch {
            prefs.set(AppPrefs.LOT_SIZE,  lot)
            prefs.set(AppPrefs.PIP_VALUE, pip)
        }
    }

    fun saveApiSettings(api: String, otc: String) {
        viewModelScope.launch {
            prefs.set(AppPrefs.API_BASE,     api.trim())
            prefs.set(AppPrefs.OTC_API_BASE, otc.trim())
            signalCache.clear()
            cacheExpiry.clear()
        }
    }

    /** SL/TP price calculator — pure math, no IO. */
    fun calcSLTP(pair: String, dir: String, ep: Double, sl: Float, tp: Float): Triple<String, String, String> {
        val ps = when {
            "JPY" in pair           -> 0.01
            "XAU" in pair           -> 0.1
            "BTC" in pair           -> 1.0
            "ETH" in pair           -> 0.1
            PairData.isCrypto(pair) -> 0.01
            else                    -> 0.0001
        }
        val slPrice = if (dir == "BUY") ep - sl * ps else ep + sl * ps
        val tpPrice = if (dir == "BUY") ep + tp * ps else ep - tp * ps
        val dec = when { "JPY" in pair -> 3; "XAU" in pair -> 2; "BTC" in pair -> 1; else -> 5 }
        val fmt = "%.${dec}f"
        val rr  = String.format("%.1f", tp / sl)
        return Triple(String.format(fmt, slPrice), String.format(fmt, tpPrice), rr)
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

    override fun onCleared() {
        super.onCleared()
        autoRefreshJob?.cancel()
    }
}
