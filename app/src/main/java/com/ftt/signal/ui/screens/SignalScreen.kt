package com.ftt.signal.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ftt.signal.data.model.*
import com.ftt.signal.ui.components.*
import com.ftt.signal.ui.theme.*
import com.ftt.signal.viewmodel.SignalUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignalScreen(
    uiState:      SignalUiState,
    selectedPair: String,
    onPairSelect: (String) -> Unit,
    onRefresh:    () -> Unit,
    onToggleAuto: () -> Unit,
) {
    var showPairPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // ── Top Bar ──────────────────────────────────────────
        TopBar(
            selectedPair  = selectedPair,
            autoRefresh   = uiState.autoRefresh,
            lastUpdated   = uiState.lastUpdated,
            onPickerClick = { showPairPicker = true },
            onRefresh     = onRefresh,
            onToggleAuto  = onToggleAuto,
        )

        // ── Content ──────────────────────────────────────────
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading && uiState.signalResponse == null ->
                    FttLoading(modifier = Modifier.align(Alignment.Center))

                uiState.error != null && uiState.signalResponse == null ->
                    ErrorState(
                        message = uiState.error,
                        onRetry = onRefresh,
                        modifier = Modifier.align(Alignment.Center),
                    )

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        uiState.signalResponse?.let { resp ->
                            MarketStatusBar(resp)
                            resp.signal?.let { sig ->
                                MainSignalCard(sig, selectedPair)
                                AIValidationCard(sig.aiValidation)
                                TimeframeCard(sig)
                                OtcPatternsCard(sig)
                                FiltersCard(sig.filtersApplied)
                            }
                        }
                        Spacer(Modifier.height(80.dp))
                    }
                }
            }

            // Loading overlay while refreshing
            if (uiState.isLoading && uiState.signalResponse != null) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    color    = Accent,
                    trackColor = SurfaceVariant,
                )
            }
        }
    }

    // ── Pair Picker Sheet ────────────────────────────────────
    if (showPairPicker) {
        PairPickerSheet(
            selectedPair = selectedPair,
            onSelect     = { pair ->
                showPairPicker = false
                onPairSelect(pair)
            },
            onDismiss    = { showPairPicker = false },
        )
    }
}

// ── Top Bar ───────────────────────────────────────────────────
@Composable
private fun TopBar(
    selectedPair:  String,
    autoRefresh:   Boolean,
    lastUpdated:   String?,
    onPickerClick: () -> Unit,
    onRefresh:     () -> Unit,
    onToggleAuto:  () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text("FTTs Signal", style = MaterialTheme.typography.titleMedium,
                color = TextPrimary, fontWeight = FontWeight.Bold)
            if (lastUpdated != null) {
                Text("Updated $lastUpdated", style = MaterialTheme.typography.bodySmall,
                    color = TextMuted)
            }
        }

        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Auto refresh toggle
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (autoRefresh) AccentDim else SurfaceVariant)
                    .border(1.dp, if (autoRefresh) Accent.copy(.4f) else Divider, RoundedCornerShape(8.dp))
                    .clickable(onClick = onToggleAuto)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = if (autoRefresh) "AUTO ON" else "AUTO OFF",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                    color = if (autoRefresh) Accent else TextSecondary,
                )
            }

            // Pair selector
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceVariant)
                    .clickable(onClick = onPickerClick)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(selectedPair, fontWeight = FontWeight.Bold,
                        color = TextPrimary, fontSize = 13.sp)
                    Text("▼", fontSize = 9.sp, color = TextSecondary)
                }
            }

            // Refresh button
            IconButton(onClick = onRefresh, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh",
                    tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ── Market Status Bar ─────────────────────────────────────────
@Composable
private fun MarketStatusBar(resp: SignalResponse) {
    val isOpen  = resp.marketStatus?.contains("OPEN", ignoreCase = true) == true
    val isOTC   = resp.isOTC == true
    val session = resp.session

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceVariant)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isOpen) BuyGreen else SellRed)
            )
            Text(
                text  = if (isOTC) "OTC 24/7" else if (isOpen) "Market Open" else "Market Closed",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isOpen) BuyGreen else SellRed,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            session?.sessions?.take(2)?.forEach { s ->
                Text(s, style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Surface)
                        .padding(horizontal = 6.dp, vertical = 2.dp))
            }
            session?.quality?.let { q ->
                Text(q, style = MaterialTheme.typography.bodySmall,
                    color = when (q) {
                        "HIGHEST" -> Gold
                        "HIGH"    -> BuyGreen
                        "MEDIUM"  -> GradeC
                        else      -> TextMuted
                    },
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

// ── Main Signal Card ──────────────────────────────────────────
@Composable
private fun MainSignalCard(sig: SignalData, pair: String) {
    SectionCard {
        // Pair + OTC tag
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Column {
                Text(pair, style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary, fontWeight = FontWeight.Bold)
                sig.marketRegime?.let { regime ->
                    Text(regime, style = MaterialTheme.typography.bodySmall,
                        color = when (regime) {
                            "TRENDING"  -> BuyGreen
                            "RANGING"   -> GradeC
                            "BREAKOUT"  -> Accent
                            "VOLATILE"  -> SellRed
                            else        -> TextMuted
                        })
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (sig.isOTC == true) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Gold.copy(.15f))
                            .border(1.dp, Gold.copy(.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text("OTC", color = Gold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
                sig.grade?.grade?.let { GradeBadge(it) }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Direction
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            DirectionBadge(
                direction = sig.finalSignal,
                modifier  = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            )
        }

        Spacer(Modifier.height(16.dp))

        // Confidence bar
        ConfidenceBar(sig.confidence, sig.finalSignal)

        FttDivider()

        // Key info
        sig.bestTimeframe?.let { btf ->
            InfoRow("Best Timeframe", btf.timeframe ?: "—")
            btf.expiry?.let { exp ->
                InfoRow("Expiry", exp.humanReadable ?: "—", valueColor = Gold)
                exp.countdown?.let { cd ->
                    InfoRow("Next candle", cd.label ?: "—", valueColor = TextSecondary)
                }
            }
        }

        InfoRow("Alignment", sig.alignment ?: "—",
            valueColor = when (sig.alignment) {
                "ALL_BULLISH"    -> BuyGreen
                "ALL_BEARISH"    -> SellRed
                "MOSTLY_BULLISH" -> BuyGreen.copy(.7f)
                "MOSTLY_BEARISH" -> SellRed.copy(.7f)
                else             -> TextMuted
            }
        )

        InfoRow("HTF Trend", sig.higherTFTrend ?: "—",
            valueColor = when {
                sig.higherTFTrend?.contains("BUY") == true -> BuyGreen
                sig.higherTFTrend?.contains("SELL") == true -> SellRed
                else -> TextMuted
            }
        )

        // Session + candle quality multipliers
        sig.sessionWeight?.let { sw ->
            InfoRow("Session Weight", "×%.2f".format(sw),
                valueColor = if (sw >= 1.0) BuyGreen else GradeC)
        }
        sig.candleQuality?.let { cq ->
            InfoRow("Candle Quality", "×%.2f".format(cq),
                valueColor = if (cq >= 1.0) BuyGreen else GradeC)
        }

        // Entry reason
        sig.entryReason?.let { reason ->
            FttDivider()
            Text("Entry Reason", style = MaterialTheme.typography.labelLarge, color = TextMuted)
            Spacer(Modifier.height(4.dp))
            Text(reason, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }

        // Regime advice
        sig.regimeAdvice?.let { advice ->
            Spacer(Modifier.height(4.dp))
            Text(advice, style = MaterialTheme.typography.bodySmall, color = Accent)
        }
    }
}

// ── AI Validation Card ────────────────────────────────────────
@Composable
private fun AIValidationCard(ai: AiValidation?) {
    if (ai == null || ai.status == "SKIPPED") return

    SectionCard {
        SectionHeader("AI Validation")

        val combined = ai.combined
        if (combined != null) {
            // Combined result
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Dual AI Result", style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary)
                    combined.agreement?.let { agr ->
                        Text(agr.replace("_", " "), style = MaterialTheme.typography.bodySmall,
                            color = if (agr.contains("AGREE")) BuyGreen else GradeC)
                    }
                }
                DirectionBadge(combined.signal)
            }

            combined.reason?.let { r ->
                Spacer(Modifier.height(6.dp))
                Text(r, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            combined.concerns?.let { c ->
                Spacer(Modifier.height(4.dp))
                Text("⚠ $c", style = MaterialTheme.typography.bodySmall, color = GradeC)
            }

            FttDivider()
        }

        // Individual AI results
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ai.cerebras?.let { c ->
                AiChip("Cerebras", c.signal, c.confidence, modifier = Modifier.weight(1f))
            }
            ai.groq?.let { g ->
                AiChip("Groq", g.signal, g.confidence, modifier = Modifier.weight(1f))
            }
        }
    }
}

// ── Timeframe breakdown card ──────────────────────────────────
@Composable
private fun TimeframeCard(sig: SignalData) {
    val recs = sig.recommendations ?: return
    if (recs.isEmpty()) return

    SectionCard {
        SectionHeader("Timeframe Analysis")
        recs.entries.sortedByDescending { it.key }.forEach { (tf, rec) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(tf, style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text(rec.confluence ?: "—", style = MaterialTheme.typography.bodySmall,
                        color = TextMuted)
                    rec.expiry?.humanReadable?.let { exp ->
                        Text("Expiry: $exp", style = MaterialTheme.typography.bodySmall,
                            color = Gold)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    DirectionBadge(rec.direction)
                    rec.entry?.price?.let { p ->
                        Text("@ %.5f".format(p), style = MaterialTheme.typography.bodySmall,
                            color = TextMuted, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            rec.patterns?.takeIf { it.isNotEmpty() }?.let { patterns ->
                Text("⬟ " + patterns.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall, color = Accent)
            }
            if (recs.keys.last() != tf) FttDivider()
        }
    }
}

// ── OTC Patterns Card ─────────────────────────────────────────
@Composable
private fun OtcPatternsCard(sig: SignalData) {
    val otc = sig.otcPatterns ?: return
    if (sig.isOTC != true) return

    SectionCard {
        SectionHeader("OTC Pattern Analysis")

        otc.signals?.takeIf { it.isNotEmpty() }?.let { signals ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                signals.take(3).forEach { s ->
                    Text(
                        s.replace("_", " "),
                        style = MaterialTheme.typography.bodySmall,
                        color = Gold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Gold.copy(.1f))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        otc.timeContext?.let { tc ->
            InfoRow("Time Quality", tc.quality ?: "—",
                valueColor = when (tc.quality) {
                    "GOOD"     -> BuyGreen
                    "AVOID"    -> SellRed
                    "MODERATE" -> GradeC
                    else       -> TextSecondary
                })
            tc.reason?.let { r -> Text(r, style = MaterialTheme.typography.bodySmall, color = TextMuted) }
        }

        otc.confluenceBonus?.takeIf { it != 0 }?.let { bonus ->
            InfoRow("Confluence Bonus", if (bonus > 0) "+$bonus" else "$bonus",
                valueColor = if (bonus > 0) BuyGreen else SellRed)
        }
    }
}

// ── Filters Applied Card ──────────────────────────────────────
@Composable
private fun FiltersCard(filters: List<String>?) {
    if (filters.isNullOrEmpty()) return

    SectionCard {
        SectionHeader("Filters Applied")
        filters.forEach { f ->
            val color = when {
                f.contains("BOOST", ignoreCase = true)  -> BuyGreen
                f.contains("PENALTY", ignoreCase = true) -> GradeC
                f.contains("FLOOR", ignoreCase = true)   -> SellRed
                f.contains("BLOCK", ignoreCase = true)   -> SellRed
                else -> TextSecondary
            }
            Row(
                modifier = Modifier.padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("•", color = color, fontSize = 12.sp)
                Text(f, style = MaterialTheme.typography.bodySmall, color = color)
            }
        }
    }
}
