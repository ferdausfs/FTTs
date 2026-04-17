package com.ftt.signal.domain.usecase

import com.ftt.signal.data.model.ProcessedSignal
import com.ftt.signal.data.repository.Result
import com.ftt.signal.domain.repository.ISignalRepository
import javax.inject.Inject

/**
 * Fetches a trading signal for the given currency pair.
 * Wraps the repository call so ViewModels stay thin.
 */
class GetSignalUseCase @Inject constructor(
    private val repository: ISignalRepository
) {
    suspend operator fun invoke(
        pair: String,
        apiBase: String,
        otcBase: String
    ): Result<ProcessedSignal> = repository.getSignal(pair, apiBase, otcBase)

    fun isMarketOpen(pair: String) = repository.isMarketOpen(pair)
    fun whyClosed(pair: String)    = repository.whyClosed(pair)
}
