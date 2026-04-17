package com.ftt.signal.data.repository

import com.ftt.signal.data.api.RetrofitClient
import com.ftt.signal.data.model.*
import com.ftt.signal.domain.repository.ISignalRepository
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [ISignalRepository].
 * Injected as a Singleton so the in-memory cache is shared across
 * all callers (SignalViewModel + WatchlistViewModel).
 */
@Singleton
class SignalRepositoryImpl @Inject constructor() : ISignalRepository {

    override suspend fun getSignal(
        pair: String,
        apiBase: String,
        otcBase: String
    ): Result<ProcessedSignal> {
        return try {
            val isOTC   = PairData.isOTC(pair)
            val base    = if (isOTC) otcBase else apiBase
            val apiPair = if (isOTC) pair.replace("-OTC", "otc") else pair
            val service = RetrofitClient.create(base)
            val resp    = service.getSignal(apiPair)

            // Detect DUMMY_FALLBACK (API limit exceeded)
            if (resp.signal?.method == "DUMMY_FALLBACK" || resp.source == "DUMMY_FALLBACK") {
                val errMsg = resp.errors?.values?.firstOrNull() ?: "API limit reached"
                return Result.Error("API_LIMIT: $errMsg")
            }

            val sig = resp.signal ?: return Result.Error("No signal data in response")
            Result.Success(processSignal(sig, resp.pair ?: pair, pair))
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown network error")
        }
    }

    override fun isMarketOpen(pair: String): Boolean {
        if (PairData.isOTC(pair) || PairData.isCrypto(pair)) return true
        val cal  = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        val dow  = cal.get(Calendar.DAY_OF_WEEK)
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        if (dow == 7) return false
        if (dow == 1 && hour < 21) return false
        if (dow == 6 && hour >= 21) return false
        return true
    }

    override fun whyClosed(pair: String): String {
        val cal  = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        val dow  = cal.get(Calendar.DAY_OF_WEEK)
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        return when {
            dow == 7              -> "Weekend — reopens Sun 21:00 UTC"
            dow == 1              -> "Opens today at 21:00 UTC"
            dow == 6 && hour >= 21 -> "Closed for weekend"
            else                  -> "Market closed"
        }
    }

    // ── Internal signal processing ────────────────────────────
    private fun processSignal(sig: SignalData, respPair: String, origPair: String): ProcessedSignal {
        val confNum = sig.confidence?.replace("%", "")?.toIntOrNull() ?: 0
        val recs    = sig.recommendations ?: emptyMap()
        val tfKeys  = recs.keys.toList()
        val bestTF  = sig.bestTimeframe?.timeframe?.takeIf { recs[it] != null }
            ?: tfKeys.firstOrNull() ?: "5min"
        val bestRec = recs[bestTF]
        val expMins = bestRec?.expiry?.totalMinutes ?: 5
        val expSug  = bestRec?.expiry?.humanReadable ?: "${expMins}m"
        val ep      = bestRec?.entry?.price

        val tfa = sig.timeframeAnalysis ?: emptyMap()
        val tfBreakdown = tfKeys.associate { tf ->
            val rec  = recs[tf]!!
            val tfAn = tfa[tf]
            tf to TfBreakdownItem(
                bias           = rec.direction ?: "",
                buyVotes       = rec.score?.up ?: 0f,
                sellVotes      = rec.score?.down ?: 0f,
                adxValue       = tfAn?.indicators?.adxFloat(),
                categoryScores = tfAn?.categoryScores,
                indicators     = tfAn?.indicators
            )
        }

        val sesLbl = (sig.session?.overlap
            ?: sig.session?.sessions?.firstOrNull()
            ?: "N/A").replace("OTC_24/7", "OTC 24/7")

        val mcStr  = sig.marketCondition?.joinToString(",") ?: ""
        val atrLvl = when {
            "VOLATILE" in mcStr -> "HIGH"
            "DEAD"     in mcStr -> "LOW"
            else                -> "MED"
        }

        val grade = sig.grade?.grade ?: ""
        val label = when (sig.finalSignal?.uppercase()) {
            "BUY"      -> "BUY"
            "SELL"     -> "SELL"
            "NO_TRADE" -> "WAIT"
            else       -> "HOLD"
        }

        val reasons = mutableListOf<String>()
        if (sig.entryReason != null && label != "WAIT") {
            sig.entryReason.split("·").forEach { r ->
                val t = r.trim()
                if (t.isNotEmpty() && t != "No clear setup — entry conditions not met.") reasons += t
            }
        }
        if (!sig.alignment.isNullOrEmpty() && sig.alignment != "NONE") reasons += "Alignment: ${sig.alignment}"
        sig.averageConfluence?.let { reasons += "Avg Confluence: $it/11" }

        val tfAgree = when {
            label == "BUY"  && (sig.votes?.buy  ?: 0) > 0 -> "${sig.votes!!.buy}TF"
            label == "SELL" && (sig.votes?.sell ?: 0) > 0 -> "${sig.votes!!.sell}TF"
            else -> "—"
        }

        return ProcessedSignal(
            label            = label,
            confidence       = confNum,
            grade            = grade,
            symbol           = respPair,
            timestamp        = sig.generatedAt ?: "",
            entryPrice       = ep?.let { String.format("%.5f", it.toDoubleOrNull() ?: 0.0) },
            expiryMinutes    = expMins,
            expirySuggestion = expSug,
            tfAgreement      = tfAgree,
            buyScore         = (sig.votes?.weightedBuy ?: 0f).toInt(),
            sellScore        = (sig.votes?.weightedSell ?: 0f).toInt(),
            sessionLabel     = sesLbl,
            h1Structure      = sig.higherTFTrend ?: "NEUTRAL",
            atrLevel         = atrLvl,
            marketCondition  = sig.marketCondition?.filter { it != "UNKNOWN" } ?: emptyList(),
            reasons          = reasons,
            tfBreakdown      = tfBreakdown,
            aiValidation     = sig.aiValidation,
            filtersApplied   = sig.filtersApplied ?: emptyList(),
            newsBlackout     = sig.newsBlackout,
            averageConfluence = sig.averageConfluence,
            rawSignal        = sig,
            rawPair          = origPair
        )
    }
}
