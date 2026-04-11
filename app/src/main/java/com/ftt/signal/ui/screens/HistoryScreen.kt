package com.ftt.signal.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ftt.signal.data.model.HistorySignal
import com.ftt.signal.ui.components.*
import com.ftt.signal.ui.theme.*
import com.ftt.signal.viewmodel.HistoryUiState

@Composable
fun HistoryScreen(
    uiState:      HistoryUiState,
    selectedPair: String,
    onRefresh:    () -> Unit,
    onReport:     (String, String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().background(Background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("History", style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary, fontWeight = FontWeight.Bold)
                Text(selectedPair, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            IconButton(onClick = onRefresh) {
                Text("↻", fontSize = 20.sp, color = TextSecondary)
            }
        }

        when {
            uiState.isLoading -> FttLoading(modifier = Modifier.padding(32.dp))
            uiState.error != null -> ErrorState(uiState.error, onRefresh)
            else -> {
                LazyColumn(
                    modifier            = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding      = PaddingValues(16.dp),
                ) {
                    // Stats summary
                    uiState.stats?.stats?.let { stats ->
                        item { StatsSummaryCard(stats) }
                    }

                    // Signal list
                    val signals = uiState.history?.signals ?: emptyList()
                    if (signals.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(48.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text("📊", fontSize = 40.sp)
                                    Text("No signal history yet",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary)
                                    Text("Signals will appear here after trading",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted)
                                }
                            }
                        }
                    } else {
                        item {
                            SectionHeader("Signal History (${signals.size})")
                        }
                        items(signals) { sig ->
                            HistorySignalCard(sig, onReport)
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

// ── Stats Summary Card ────────────────────────────────────────
@Composable
private fun StatsSummaryCard(stats: com.ftt.signal.data.model.PairStats) {
    SectionCard {
        SectionHeader("Performance Summary")

        // Win rate big number
        val wr = stats.winRate
        val wrPct = if (wr != null) "%.1f%%".format(wr * 100) else "—"
        val wrColor = when {
            wr == null      -> TextMuted
            wr >= 0.65      -> BuyGreen
            wr >= 0.50      -> GradeC
            else            -> SellRed
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            StatBox("Win Rate", wrPct, wrColor)
            StatBox("Wins",   stats.wins?.toString()   ?: "—", BuyGreen)
            StatBox("Losses", stats.losses?.toString() ?: "—", SellRed)
            StatBox("Total",  stats.totalSignals?.toString() ?: "—", TextSecondary)
        }

        // Dynamic confidence adjustment
        stats.dynamicAdj?.let { adj ->
            FttDivider()
            InfoRow(
                label = "Dynamic Confidence Adj",
                value = adj,
                valueColor = when {
                    adj.startsWith("+") -> BuyGreen
                    adj.startsWith("-") -> SellRed
                    else -> TextMuted
                }
            )
        }

        // Best session
        stats.bySession?.maxByOrNull { it.value.winRate ?: 0.0 }?.let { (sess, stat) ->
            InfoRow(
                label = "Best Session",
                value = "$sess (${(stat.winRate ?: 0.0).times(100).toInt()}%)",
                valueColor = Gold,
            )
        }

        // Best TF
        stats.byTF?.maxByOrNull { it.value.winRate ?: 0.0 }?.let { (tf, stat) ->
            InfoRow(
                label = "Best Timeframe",
                value = "$tf (${(stat.winRate ?: 0.0).times(100).toInt()}%)",
                valueColor = Accent,
            )
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextMuted)
    }
}

// ── History Signal Card ───────────────────────────────────────
@Composable
private fun HistorySignalCard(sig: HistorySignal, onReport: (String, String) -> Unit) {
    var showReportMenu by remember { mutableStateOf(false) }

    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(sig.pair ?: "—", style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary, fontWeight = FontWeight.Bold)
                    if (sig.isOTC == true) {
                        Text("OTC", fontSize = 10.sp, color = Gold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Gold.copy(.1f))
                                .padding(horizontal = 5.dp, vertical = 2.dp))
                    }
                }
                Text(sig.timestamp?.take(16)?.replace("T", " ") ?: "—",
                    style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ResultChip(sig.result)
                DirectionBadge(sig.direction)
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InfoRow("Confidence", sig.confidence ?: "—",
                modifier = Modifier.weight(1f))
            InfoRow("TF", sig.bestTF ?: "—",
                modifier = Modifier.weight(1f))
        }

        sig.grade?.let { g ->
            InfoRow("Grade", g, valueColor = gradeColor(g))
        }

        // OTC report buttons — only if result is null/pending
        if (sig.isOTC == true && sig.result == null && sig.id != null) {
            FttDivider()
            Text("Report Result (OTC Manual)",
                style = MaterialTheme.typography.bodySmall, color = TextMuted)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onReport(sig.id, "WIN") },
                    colors  = ButtonDefaults.buttonColors(containerColor = BuyGreen.copy(.8f)),
                    shape   = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(36.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Text("WIN", color = Background, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Button(
                    onClick = { onReport(sig.id, "LOSS") },
                    colors  = ButtonDefaults.buttonColors(containerColor = SellRed.copy(.8f)),
                    shape   = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(36.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Text("LOSS", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
