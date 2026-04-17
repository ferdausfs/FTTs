package com.ftt.signal.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.ftt.signal.NotificationHelper
import com.ftt.signal.data.repository.Result
import com.ftt.signal.db.JournalEntry
import com.ftt.signal.domain.usecase.GetSignalUseCase
import com.ftt.signal.domain.usecase.SaveJournalEntryUseCase
import com.ftt.signal.prefs.AppPrefs
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Background periodic worker that:
 *  1. Reads the user's current pair from DataStore
 *  2. Fetches the latest signal
 *  3. If a new BUY/SELL arrives → saves to journal + fires a notification
 *
 * Annotated with @HiltWorker so Hilt can inject dependencies.
 * Scheduled every 15 minutes from [FttApplication].
 */
@HiltWorker
class SignalSyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val getSignalUseCase:       GetSignalUseCase,
    private val saveJournalEntryUseCase: SaveJournalEntryUseCase,
    private val prefs:                  AppPrefs,
    private val notificationHelper:     NotificationHelper,
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "ftt_signal_sync"
    }

    override suspend fun doWork(): Result {
        return try {
            val pair    = prefs.curPair.first()
            val apiBase = prefs.apiBase.first()
            val otcBase = prefs.otcApiBase.first()

            // Skip silently when market is closed — no retry needed
            if (!getSignalUseCase.isMarketOpen(pair)) return Result.success()

            when (val result = getSignalUseCase(pair, apiBase, otcBase)) {
                is com.ftt.signal.data.repository.Result.Success -> {
                    val sig = result.data
                    if (sig.label == "BUY" || sig.label == "SELL") {
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
                        notificationHelper.notifySignal(sig.symbol, sig.label, sig.grade)
                    }
                    Result.success()
                }
                is com.ftt.signal.data.repository.Result.Error -> {
                    // Retry on network errors, not on API-limit errors
                    if (result.message.startsWith("API_LIMIT")) Result.success()
                    else Result.retry()
                }
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
