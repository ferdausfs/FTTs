package com.ftt.signal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ftt.signal.data.model.*
import com.ftt.signal.data.repository.Result
import com.ftt.signal.data.repository.SignalRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class SignalUiState(
    val isLoading:       Boolean         = false,
    val signalResponse:  SignalResponse?  = null,
    val error:           String?          = null,
    val lastUpdated:     String?          = null,
    val autoRefresh:     Boolean          = true,
)

data class HistoryUiState(
    val isLoading: Boolean          = false,
    val history:   HistoryResponse? = null,
    val stats:     StatsResponse?   = null,
    val error:     String?          = null,
)

class SignalViewModel : ViewModel() {

    private val repository = SignalRepository()

    // ── Signal state ─────────────────────────────────────────
    private val _signalState = MutableStateFlow(SignalUiState())
    val signalState: StateFlow<SignalUiState> = _signalState.asStateFlow()

    // ── History state ─────────────────────────────────────────
    private val _historyState = MutableStateFlow(HistoryUiState())
    val historyState: StateFlow<HistoryUiState> = _historyState.asStateFlow()

    // ── Selected pair ─────────────────────────────────────────
    private val _selectedPair = MutableStateFlow("EUR/USD")
    val selectedPair: StateFlow<String> = _selectedPair.asStateFlow()

    private var autoRefreshJob: Job? = null

    init { startAutoRefresh() }

    fun selectPair(pair: String) {
        _selectedPair.value = pair
        fetchSignal(pair)
        fetchHistory(pair)
    }

    fun fetchSignal(pair: String = _selectedPair.value) {
        viewModelScope.launch {
            _signalState.value = _signalState.value.copy(isLoading = true, error = null)
            when (val result = repository.getSignal(pair)) {
                is Result.Success -> _signalState.value = _signalState.value.copy(
                    isLoading      = false,
                    signalResponse = result.data,
                    lastUpdated    = java.text.SimpleDateFormat(
                        "HH:mm:ss", java.util.Locale.getDefault()
                    ).format(java.util.Date()),
                )
                is Result.Error -> _signalState.value = _signalState.value.copy(
                    isLoading = false,
                    error     = result.message,
                )
                else -> {}
            }
        }
    }

    fun fetchHistory(pair: String = _selectedPair.value) {
        viewModelScope.launch {
            _historyState.value = _historyState.value.copy(isLoading = true)
            val histResult  = repository.getHistory(pair)
            val statsResult = repository.getStats(pair)
            _historyState.value = HistoryUiState(
                isLoading = false,
                history   = (histResult  as? Result.Success)?.data,
                stats     = (statsResult as? Result.Success)?.data,
                error     = (histResult  as? Result.Error)?.message,
            )
        }
    }

    fun reportResult(signalId: String, result: String) {
        viewModelScope.launch {
            repository.reportResult(signalId, result)
            delay(500)
            fetchHistory(_selectedPair.value)
        }
    }

    fun toggleAutoRefresh() {
        val newState = !_signalState.value.autoRefresh
        _signalState.value = _signalState.value.copy(autoRefresh = newState)
        if (newState) startAutoRefresh() else autoRefreshJob?.cancel()
    }

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(60_000L) // 60 second auto refresh
                if (_signalState.value.autoRefresh) fetchSignal()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoRefreshJob?.cancel()
    }
}
