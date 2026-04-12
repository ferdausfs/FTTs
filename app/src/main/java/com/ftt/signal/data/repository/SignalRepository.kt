package com.ftt.signal.data.repository

import com.ftt.signal.data.api.RetrofitClient
import com.ftt.signal.data.model.*
import java.util.Calendar

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
}

class SignalRepository {

    suspend fun getSignal(pair: String, apiBase: String, otcBase: String): Result<ProcessedSignal> {
        return try {
            val isOTC   = PairData.isOTC(pair)
            val base    = if (isOTC) otcBase else apiBase
            val apiPair = if (isOTC) pair.replace("-OTC", "otc") else pair
            val service = RetrofitClient.create(base)
            val resp    = service.getSignal(apiPair)

            // Check for DUMMY_FALLBACK
            if (resp.signal?.method == "DUMMY_FALLBACK" || resp.source == "DUMMY_FALLBACK") {
                val errMsg = resp.errors?.values?.firstOrNull() ?: "API exhausted"
                return Result.Error("API_LIMIT: $errMsg")
            }

            val sig = resp.signal ?: return Result.Error("No signal data")
            Result.Success(processSignal(sig, resp.pair ?: pair, pair))
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }

    private fun processSignal(sig: SignalData, respPair: String, origPair: String): ProcessedSignal {
        val confNum = sig.confidence?.replace("%", "")?.toIntOrNull() ?: 0
        val recs    = sig.recommendations ?: emptyMap()
        val tfKeys  = recs.keys.toList()
        val bestTF  = sig.bestTimeframe?.timeframe?.takeIf { recs[it] != null } ?: tfKeys.firstOrNull() ?: "5min"
        val bestRec = recs[bestTF]
        val expMins = bestRec?.expiry?.totalMinutes ?: 5
        val expSug  = bestRec?.expiry?.humanReadable ?: "${expMins}m"
        val ep      = bestRec?.entry?.price

        // TF breakdown
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

        // Session label
        val sess    = sig.session ?: sig.session
        val sesLbl  = (sig.session?.overlap
            ?: sig.session?.sessions?.firstOrNull()
            ?: "N/A").replace("OTC_24/7", "OTC 24/7")

        // ATR level
        val mcStr   = sig.marketCondition?.joinToString(",") ?: ""
        val atrLvl  = when {
            "VOLATILE" in mcStr -> "HIGH"
            "DEAD"     in mcStr -> "LOW"
            else                -> "MED"
        }

        val grade = sig.grade?.grade ?: ""

        // Label
        val label = when (sig.finalSignal?.uppercase()) {
            "BUY"      -> "BUY"
            "SELL"     -> "SELL"
            "NO_TRADE" -> "WAIT"
            else       -> "HOLD"
        }

        // Reasons
        val reasons = mutableListOf<String>()
        if (sig.entryReason != null && label != "WAIT") {
            sig.entryReason.split("·").forEach { r ->
                val t = r.trim()
                if (t.isNotEmpty() && t != "No clear setup — entry conditions not met.") reasons += t
            }
        }
        if (!sig.alignment.isNullOrEmpty() && sig.alignment != "NONE") reasons += "Alignment: ${sig.alignment}"
        sig.averageConfluence?.let { reasons += "Avg Confluence: $it/11" }

        // TF agreement
        val tfAgree = when {
            label == "BUY"  && (sig.votes?.buy  ?: 0) > 0 -> "${sig.votes!!.buy}TF"
            label == "SELL" && (sig.votes?.sell ?: 0) > 0 -> "${sig.votes!!.sell}TF"
            else -> "—"
        }

        return ProcessedSignal(
            label             = label,
            confidence        = confNum,
            grade             = grade,
            symbol            = respPair,
            timestamp         = sig.generatedAt ?: "",
            entryPrice        = ep?.let { String.format("%.5f", it.toDoubleOrNull() ?: 0.0) },
            expiryMinutes     = expMins,
            expirySuggestion  = expSug,
            tfAgreement       = tfAgree,
            buyScore          = (sig.votes?.weightedBuy ?: 0f).toInt(),
            sellScore         = (sig.votes?.weightedSell ?: 0f).toInt(),
            sessionLabel      = sesLbl,
            h1Structure       = sig.higherTFTrend ?: "NEUTRAL",
            atrLevel          = atrLvl,
            marketCondition   = sig.marketCondition?.filter { it != "UNKNOWN" } ?: emptyList(),
            reasons           = reasons,
            tfBreakdown       = tfBreakdown,
            aiValidation      = sig.aiValidation,
            filtersApplied    = sig.filtersApplied ?: emptyList(),
            newsBlackout      = sig.newsBlackout,
            averageConfluence = sig.averageConfluence,
            rawSignal         = sig,
            rawPair           = origPair
        )
    }

    // Market open helpers (replicated from HTML)
    fun isMarketOpen(pair: String): Boolean {
        val isOTC    = PairData.isOTC(pair)
        val isCrypto = PairData.isCrypto(pair)
        if (isOTC || isCrypto) return true
        val cal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        val dow  = cal.get(Calendar.DAY_OF_WEEK)  // 1=Sun..7=Sat
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        if (dow == 7) return false                   // Saturday closed
        if (dow == 1 && hour < 21) return false      // Sunday before 21:00 UTC
        if (dow == 6 && hour >= 21) return false     // Friday after 21:00 UTC
        return true
    }

    fun whyClosed(pair: String): String {
        val cal  = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        val dow  = cal.get(Calendar.DAY_OF_WEEK)
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        return when {
            dow == 7         -> "Weekend — reopens Sun 21:00 UTC"
            dow == 1         -> "Opens today at 21:00 UTC"
            dow == 6 && hour >= 21 -> "Closed for weekend"
            else             -> "Market closed"
        }
    }
}
