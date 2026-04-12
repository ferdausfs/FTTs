package com.ftt.signal.data.model

import com.google.gson.*
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

// ── Raw API response ──────────────────────────────────────────
data class ApiSignalResponse(
    @SerializedName("pair")         val pair: String?,
    @SerializedName("assetType")    val assetType: String?,
    @SerializedName("isOTC")        val isOTC: Boolean?,
    @SerializedName("marketStatus") val marketStatus: String?,
    @SerializedName("session")      val session: SessionInfo?,
    @SerializedName("signal")       val signal: SignalData?,
    @SerializedName("source")       val source: String?,
    @SerializedName("timestamp")    val timestamp: String?,
    @SerializedName("errors")       val errors: Map<String, String>?,
)

data class SessionInfo(
    @SerializedName("sessions") val sessions: List<String>?,
    @SerializedName("overlap")  val overlap: String?,
    @SerializedName("quality")  val quality: String?,
    @SerializedName("hour")     val hour: Int?
)

data class SignalData(
    @SerializedName("finalSignal")       val finalSignal: String?,
    @SerializedName("confidence")        val confidence: String?,
    @SerializedName("grade")             val grade: GradeField?,
    @SerializedName("recommendations")   val recommendations: Map<String, Recommendation>?,
    @SerializedName("timeframeAnalysis") val timeframeAnalysis: Map<String, TfAnalysis>?,
    @SerializedName("bestTimeframe")     val bestTimeframe: BestTimeframe?,
    @SerializedName("votes")             val votes: Votes?,
    @SerializedName("entryReason")       val entryReason: String?,
    @SerializedName("alignment")         val alignment: String?,
    @SerializedName("averageConfluence") val averageConfluence: Int?,
    @SerializedName("higherTFTrend")     val higherTFTrend: String?,
    @SerializedName("marketCondition")   val marketCondition: List<String>?,
    @SerializedName("aiValidation")      val aiValidation: AiValidation?,
    @SerializedName("filtersApplied")    val filtersApplied: List<String>?,
    @SerializedName("newsBlackout")      val newsBlackout: NewsBlackout?,
    @SerializedName("session")           val session: SessionInfo?,
    @SerializedName("generatedAt")       val generatedAt: String?,
    @SerializedName("method")            val method: String?
)

// Grade can be String or Object in API
data class GradeField(val grade: String)

class GradeDeserializer : JsonDeserializer<GradeField?> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, ctx: JsonDeserializationContext?): GradeField? {
        json ?: return null
        return when {
            json.isJsonPrimitive -> GradeField(json.asString)
            json.isJsonObject    -> GradeField(json.asJsonObject.get("grade")?.asString ?: "")
            else                 -> null
        }
    }
}

data class Recommendation(
    @SerializedName("direction") val direction: String?,
    @SerializedName("score")     val score: ScoreData?,
    @SerializedName("expiry")    val expiry: ExpiryData?,
    @SerializedName("entry")     val entry: EntryData?
)

data class ScoreData(
    @SerializedName("up")   val up: Float?,
    @SerializedName("down") val down: Float?
)

data class ExpiryData(
    @SerializedName("totalMinutes")  val totalMinutes: Int?,
    @SerializedName("humanReadable") val humanReadable: String?,
    @SerializedName("countdown")     val countdown: String?
)

data class EntryData(@SerializedName("price") val price: String?)

data class TfAnalysis(
    @SerializedName("indicators")     val indicators: Indicators?,
    @SerializedName("categoryScores") val categoryScores: Map<String, ScoreData>?
)

data class Indicators(
    @SerializedName("rsi")      val rsi: JsonElement?,
    @SerializedName("ema5")     val ema5: JsonElement?,
    @SerializedName("ema20")    val ema20: JsonElement?,
    @SerializedName("adx")      val adx: JsonElement?,
    @SerializedName("macdHist") val macdHist: JsonElement?
) {
    fun rsiFloat()     = rsi?.asFloatOrNull()
    fun ema5Float()    = ema5?.asFloatOrNull()
    fun ema20Float()   = ema20?.asFloatOrNull()
    fun adxFloat()     = adx?.asFloatOrNull()
    fun macdHistFloat()= macdHist?.asFloatOrNull()
}

fun JsonElement.asFloatOrNull(): Float? = try {
    if (isJsonPrimitive) asString.toFloatOrNull() else null
} catch (e: Exception) { null }

data class BestTimeframe(
    @SerializedName("timeframe") val timeframe: String?,
    @SerializedName("score")     val score: Float?
)

data class Votes(
    @SerializedName("BUY")          val buy: Int?,
    @SerializedName("SELL")         val sell: Int?,
    @SerializedName("weightedBuy")  val weightedBuy: Float?,
    @SerializedName("weightedSell") val weightedSell: Float?
)

data class AiValidation(
    @SerializedName("approved")   val approved: Boolean?,
    @SerializedName("reason")     val reason: String?,
    @SerializedName("confidence") val confidence: String?
)

data class NewsBlackout(
    @SerializedName("blocked") val blocked: Boolean?,
    @SerializedName("label")   val label: String?
)

// ── Processed / display model (built from raw API) ────────────
data class ProcessedSignal(
    val label: String,         // BUY / SELL / WAIT / HOLD
    val confidence: Int,
    val grade: String,
    val symbol: String,
    val timestamp: String,
    val entryPrice: String?,
    val expiryMinutes: Int,
    val expirySuggestion: String,
    val tfAgreement: String,
    val buyScore: Int,
    val sellScore: Int,
    val sessionLabel: String,
    val h1Structure: String,
    val atrLevel: String,      // HIGH / MED / LOW
    val marketCondition: List<String>,
    val reasons: List<String>,
    val tfBreakdown: Map<String, TfBreakdownItem>,
    val aiValidation: AiValidation?,
    val filtersApplied: List<String>,
    val newsBlackout: NewsBlackout?,
    val averageConfluence: Int?,
    val rawSignal: SignalData,
    val rawPair: String
)

data class TfBreakdownItem(
    val bias: String,
    val buyVotes: Float,
    val sellVotes: Float,
    val adxValue: Float?,
    val categoryScores: Map<String, ScoreData>?,
    val indicators: Indicators?
)

// ── Pair data ─────────────────────────────────────────────────
object PairData {
    val FX = listOf(
        "EUR/USD","GBP/USD","USD/JPY","USD/CHF","AUD/USD","NZD/USD","USD/CAD",
        "EUR/GBP","GBP/JPY","EUR/JPY","EUR/CHF","AUD/JPY","GBP/CHF","CAD/JPY",
        "USD/SGD","USD/NOK","USD/SEK","EUR/AUD","GBP/AUD","AUD/NZD","USD/MXN","USD/ZAR"
    )
    val CRYPTO = listOf(
        "BTC/USD","ETH/USD","BNB/USD","XRP/USD","SOL/USD","ADA/USD","DOGE/USD",
        "AVAX/USD","DOT/USD","LINK/USD","BTC/EUR","ETH/EUR","BTC/GBP","ETH/BTC"
    )
    val OTC = listOf(
        "EUR/USD-OTC","GBP/USD-OTC","USD/JPY-OTC","AUD/USD-OTC","USD/CAD-OTC",
        "USD/CHF-OTC","NZD/USD-OTC","EUR/GBP-OTC","EUR/JPY-OTC","GBP/JPY-OTC",
        "EUR/AUD-OTC","GBP/AUD-OTC","AUD/JPY-OTC","EUR/CHF-OTC","GBP/CHF-OTC",
        "CAD/JPY-OTC","AUD/NZD-OTC","AUD/CHF-OTC"
    )
    val ALL = FX + CRYPTO + OTC

    fun isOTC(p: String)    = p.endsWith("-OTC")
    fun isCrypto(p: String) = CRYPTO.contains(p)
    fun category(p: String) = when {
        isOTC(p)    -> "OTC"
        isCrypto(p) -> "Crypto"
        else        -> "Forex"
    }
}
