package com.ftt.signal.data.model

import com.google.gson.annotations.SerializedName

// ── Top-level API response ────────────────────────────────────
data class SignalResponse(
    @SerializedName("pair")         val pair:         String?,
    @SerializedName("assetType")    val assetType:    String?,
    @SerializedName("isOTC")        val isOTC:        Boolean?,
    @SerializedName("marketStatus") val marketStatus: String?,
    @SerializedName("session")      val session:      SessionInfo?,
    @SerializedName("signal")       val signal:       SignalData?,
    @SerializedName("source")       val source:       String?,
    @SerializedName("timestamp")    val timestamp:    String?,
    @SerializedName("dataNote")     val dataNote:     String?,
    @SerializedName("error")        val error:        Boolean?,
    @SerializedName("message")      val message:      String?,
)

data class SessionInfo(
    @SerializedName("sessions") val sessions: List<String>?,
    @SerializedName("overlap")  val overlap:  String?,
    @SerializedName("quality")  val quality:  String?,
    @SerializedName("hour")     val hour:     Int?,
)

// ── Core signal payload ───────────────────────────────────────
data class SignalData(
    @SerializedName("finalSignal")    val finalSignal:    String?,
    @SerializedName("confidence")     val confidence:     String?,
    @SerializedName("grade")          val grade:          GradeInfo?,
    @SerializedName("assetType")      val assetType:      String?,
    @SerializedName("marketRegime")   val marketRegime:   String?,
    @SerializedName("regimeAdvice")   val regimeAdvice:   String?,
    @SerializedName("alignment")      val alignment:      String?,
    @SerializedName("higherTFTrend")  val higherTFTrend:  String?,
    @SerializedName("entryReason")    val entryReason:    String?,
    @SerializedName("filtersApplied") val filtersApplied: List<String>?,
    @SerializedName("aiValidation")   val aiValidation:   AiValidation?,
    @SerializedName("bestTimeframe")  val bestTimeframe:  BestTimeframe?,
    @SerializedName("votes")          val votes:          VoteData?,
    @SerializedName("recommendations")val recommendations:Map<String, TFRecommendation>?,
    @SerializedName("sessionWeight")  val sessionWeight:  Double?,
    @SerializedName("candleQuality")  val candleQuality:  Double?,
    @SerializedName("method")         val method:         String?,
    @SerializedName("generatedAt")    val generatedAt:    String?,
    @SerializedName("isOTC")          val isOTC:          Boolean?,
    @SerializedName("otcNote")        val otcNote:        String?,
    @SerializedName("otcPatterns")    val otcPatterns:    OtcPatterns?,
)

data class GradeInfo(
    @SerializedName("grade")       val grade:       String?,
    @SerializedName("label")       val label:       String?,
    @SerializedName("description") val description: String?,
)

// ── AI Validation ─────────────────────────────────────────────
data class AiValidation(
    @SerializedName("cerebras")       val cerebras:       AiResult?,
    @SerializedName("groq")           val groq:           AiResult?,
    @SerializedName("combined")       val combined:       AiCombined?,
    @SerializedName("combinedAgreed") val combinedAgreed: Boolean?,
    @SerializedName("agrees")         val agrees:         Boolean?,
    @SerializedName("status")         val status:         String?,
    @SerializedName("signal")         val signal:         String?,
    @SerializedName("confidence")     val confidence:     Int?,
    @SerializedName("reason")         val reason:         String?,
    @SerializedName("concerns")       val concerns:       String?,
)

data class AiResult(
    @SerializedName("status")     val status:     String?,
    @SerializedName("signal")     val signal:     String?,
    @SerializedName("confidence") val confidence: Int?,
    @SerializedName("reason")     val reason:     String?,
    @SerializedName("concerns")   val concerns:   String?,
    @SerializedName("model")      val model:      String?,
)

data class AiCombined(
    @SerializedName("status")     val status:     String?,
    @SerializedName("signal")     val signal:     String?,
    @SerializedName("confidence") val confidence: Int?,
    @SerializedName("reason")     val reason:     String?,
    @SerializedName("concerns")   val concerns:   String?,
    @SerializedName("agreement")  val agreement:  String?,
    @SerializedName("model")      val model:      String?,
)

// ── Timeframe data ────────────────────────────────────────────
data class BestTimeframe(
    @SerializedName("timeframe")    val timeframe:    String?,
    @SerializedName("direction")    val direction:    String?,
    @SerializedName("score")        val score:        Double?,
    @SerializedName("confluence")   val confluence:   Int?,
    @SerializedName("expiry")       val expiry:       ExpiryInfo?,
    @SerializedName("reason")       val reason:       String?,
)

data class TFRecommendation(
    @SerializedName("direction")  val direction:  String?,
    @SerializedName("score")      val score:      ScoreData?,
    @SerializedName("confluence") val confluence: String?,
    @SerializedName("expiry")     val expiry:     ExpiryInfo?,
    @SerializedName("entry")      val entry:      EntryInfo?,
    @SerializedName("patterns")   val patterns:   List<String>?,
)

data class ExpiryInfo(
    @SerializedName("candles")       val candles:       Int?,
    @SerializedName("candleSize")    val candleSize:    String?,
    @SerializedName("totalMinutes")  val totalMinutes:  Int?,
    @SerializedName("expiryTime")    val expiryTime:    String?,
    @SerializedName("humanReadable") val humanReadable: String?,
    @SerializedName("countdown")     val countdown:     CountdownInfo?,
)

data class CountdownInfo(
    @SerializedName("secondsLeft") val secondsLeft: Int?,
    @SerializedName("label")       val label:       String?,
)

data class EntryInfo(
    @SerializedName("price")          val price:          Double?,
    @SerializedName("candleTime")     val candleTime:     String?,
    @SerializedName("candleDirection")val candleDirection:String?,
)

data class ScoreData(
    @SerializedName("up")   val up:   Double?,
    @SerializedName("down") val down: Double?,
    @SerializedName("diff") val diff: Double?,
)

data class VoteData(
    @SerializedName("BUY")          val buy:          Int?,
    @SerializedName("SELL")         val sell:         Int?,
    @SerializedName("NO_TRADE")     val noTrade:      Int?,
    @SerializedName("total")        val total:        Int?,
    @SerializedName("weightedBuy")  val weightedBuy:  Double?,
    @SerializedName("weightedSell") val weightedSell: Double?,
)

// ── OTC Patterns ──────────────────────────────────────────────
data class OtcPatterns(
    @SerializedName("signals")         val signals:         List<String>?,
    @SerializedName("confluenceBonus") val confluenceBonus: Int?,
    @SerializedName("timeContext")     val timeContext:     TimeContext?,
)

data class TimeContext(
    @SerializedName("quality") val quality: String?,
    @SerializedName("reason")  val reason:  String?,
)

// ── History ───────────────────────────────────────────────────
data class HistoryResponse(
    @SerializedName("pair")     val pair:     String?,
    @SerializedName("total")    val total:    Int?,
    @SerializedName("winRate")  val winRate:  Double?,
    @SerializedName("signals")  val signals:  List<HistorySignal>?,
)

data class HistorySignal(
    @SerializedName("id")         val id:         String?,
    @SerializedName("pair")       val pair:       String?,
    @SerializedName("direction")  val direction:  String?,
    @SerializedName("confidence") val confidence: String?,
    @SerializedName("grade")      val grade:      String?,
    @SerializedName("result")     val result:     String?,
    @SerializedName("timestamp")  val timestamp:  String?,
    @SerializedName("bestTF")     val bestTF:     String?,
    @SerializedName("isOTC")      val isOTC:      Boolean?,
)

// ── Stats ─────────────────────────────────────────────────────
data class StatsResponse(
    @SerializedName("pair")      val pair:      String?,
    @SerializedName("stats")     val stats:     PairStats?,
    @SerializedName("message")   val message:   String?,
)

data class PairStats(
    @SerializedName("pair")         val pair:         String?,
    @SerializedName("totalSignals") val totalSignals: Int?,
    @SerializedName("wins")         val wins:         Int?,
    @SerializedName("losses")       val losses:       Int?,
    @SerializedName("winRate")      val winRate:      Double?,
    @SerializedName("bySession")    val bySession:    Map<String, SessionStat>?,
    @SerializedName("byTF")         val byTF:         Map<String, SessionStat>?,
    @SerializedName("dynamicConfidenceAdjustment") val dynamicAdj: String?,
)

data class SessionStat(
    @SerializedName("wins")    val wins:    Int?,
    @SerializedName("losses")  val losses:  Int?,
    @SerializedName("winRate") val winRate: Double?,
)
