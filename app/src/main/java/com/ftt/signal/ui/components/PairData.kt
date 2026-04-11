package com.ftt.signal.ui.components

// ── Pair lists ────────────────────────────────────────────────
object PairData {

    val FOREX_MAJORS = listOf(
        "EUR/USD", "GBP/USD", "USD/JPY", "USD/CHF",
        "USD/CAD", "AUD/USD", "NZD/USD",
    )

    val FOREX_MINORS = listOf(
        "EUR/GBP", "EUR/JPY", "EUR/CHF", "EUR/AUD", "EUR/CAD",
        "GBP/JPY", "GBP/CHF", "GBP/AUD", "GBP/CAD",
        "AUD/JPY", "AUD/CAD", "NZD/JPY", "CAD/JPY", "CHF/JPY",
    )

    val CRYPTO = listOf(
        "BTC/USD", "ETH/USD", "BNB/USD", "XRP/USD",
        "SOL/USD", "ADA/USD", "DOGE/USD",
    )

    val OTC_MAJOR = listOf(
        "EUR/USD-OTC", "GBP/USD-OTC", "USD/JPY-OTC",
        "USD/CHF-OTC", "USD/CAD-OTC", "AUD/USD-OTC", "NZD/USD-OTC",
    )

    val OTC_MINOR = listOf(
        "EUR/GBP-OTC", "EUR/JPY-OTC", "GBP/JPY-OTC",
        "AUD/JPY-OTC", "CAD/JPY-OTC",
    )

    val CATEGORIES = listOf(
        PairCategory("Forex Majors", FOREX_MAJORS),
        PairCategory("Forex Minors", FOREX_MINORS),
        PairCategory("Crypto",       CRYPTO),
        PairCategory("OTC Majors",   OTC_MAJOR),
        PairCategory("OTC Minors",   OTC_MINOR),
    )

    fun isOTC(pair: String) = pair.endsWith("-OTC")
    fun displayName(pair: String) = pair // already formatted
}

data class PairCategory(
    val name:  String,
    val pairs: List<String>,
)
