package com.ftt.signal.domain.repository

import com.ftt.signal.data.model.ProcessedSignal
import com.ftt.signal.data.repository.Result

/**
 * Domain-layer contract for signal fetching.
 * Implementations live in data/ and are bound via Hilt.
 */
interface ISignalRepository {
    suspend fun getSignal(pair: String, apiBase: String, otcBase: String): Result<ProcessedSignal>
    fun isMarketOpen(pair: String): Boolean
    fun whyClosed(pair: String): String
}
