package com.ftt.signal.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import com.ftt.signal.ui.components.*
import com.ftt.signal.ui.theme.*
import com.ftt.signal.viewmodel.WatchlistUiState

@Composable
fun WatchlistScreen(
    state: WatchlistUiState,
    onToggleScan: () -> Unit,
    onRunScan: () -> Unit,
    onAddPair: (String) -> Unit,
    onRemovePair: (String) -> Unit,
    onSetInterval: (Int) -> Unit,
    onSetFilter: (String) -> Unit,
    onSetSort: (String) -> Unit,
    onPairClick: (String) -> Unit,
    onAddToJournal: (String) -> Unit,
    onClearNewCount: () -> Unit,
) {
    var showAddSheet by remember { mutableStateOf(false) }
    var ivText by remember(state.intervalMin) { mutableStateOf(state.intervalMin.toString()) }

    LaunchedEffect(Unit) { onClearNewCount() }

    Column(Modifier.fillMaxSize().background(Bg)) {
        // Settings bar
        Column(
            Modifier.fillMaxWidth().background(S2)
                .border(BorderStroke(1.dp, com.ftt.signal.ui.theme.Divider))
                .padding(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Watchlist Scanner", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = T1)
                    Text(if (state.active) "● Active" else "Idle", fontSize = 11.sp,
                        color = if (state.active) BuyGreen else T3)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Interval input
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            ivText, { ivText = it; it.toIntOrNull()?.let { v -> onSetInterval(v) } },
                            Modifier.width(56.dp).height(42.dp), singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = T1,
                                fontFamily = FontFamily.Monospace, textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Accent, unfocusedBorderColor = com.ftt.signal.ui.theme.Divider,
                                focusedTextColor = T1, unfocusedTextColor = T1),
                            shape = RoundedCornerShape(8.dp),
                        )
                        Text("min", fontSize = 11.sp, color = T3)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Start/Stop
                Button(
                    onClick = onToggleScan,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.active) SellRedDim else BuyGreenDim,
                        contentColor   = if (state.active) SellRed else BuyGreen),
                    modifier = Modifier.height(36.dp)
                ) { Text(if (state.active) "⏹ Stop" else "▶ Start", fontSize = 12.sp) }

                // Add
                OutlinedButton(onClick = { showAddSheet = true },
                    border = BorderStroke(1.dp, Accent.copy(0.4f)),
                    modifier = Modifier.height(36.dp)) {
                    Text("＋ Add", fontSize = 12.sp, color = Accent) }

                // Manual scan
                if (state.pairs.isNotEmpty()) {
                    OutlinedButton(onClick = onRunScan,
                        border = BorderStroke(1.dp, T3.copy(0.3f)),
                        modifier = Modifier.height(36.dp)) {
                        Text("↻ Scan", fontSize = 12.sp, color = T3) }
                }
            }
        }

        // Progress bar
        if (state.scanning || state.scanTotal > 0) {
            val pct = if (state.scanTotal > 0) state.scanProgress.toFloat() / state.scanTotal else 0f
            val done = !state.scanning && state.scanProgress >= state.scanTotal
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (done) "✓ Scan complete — ${state.scanTotal} pairs"
                         else "⟳ Scanning ${state.scanProgress}/${state.scanTotal}...",
                        fontSize = 11.sp, color = if (state.scanning) Accent else BuyGreen)
                    Text("${(pct * 100).toInt()}%", fontSize = 11.sp, color = T3)
                }
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(999.dp)).background(S3)) {
                    Box(Modifier.fillMaxWidth(pct).fillMaxHeight()
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (done) BuyGreen else Accent))
                }
            }
        }

        // Summary
        val stats = WlStats(
            buy  = state.results.values.count { it.label == "BUY" },
            sell = state.results.values.count { it.label == "SELL" },
            wait = state.results.values.count { it.label in listOf("WAIT","HOLD") },
            total = state.pairs.size
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WlSumBox("BUY", "${stats.buy}", BuyGreen, Modifier.weight(1f))
            WlSumBox("SELL", "${stats.sell}", SellRed, Modifier.weight(1f))
            WlSumBox("WAIT", "${stats.wait}", WaitYellow, Modifier.weight(1f))
            WlSumBox("TOTAL", "${stats.total}", Accent, Modifier.weight(1f))
        }

        if (state.pairs.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("👁", fontSize = 40.sp); Spacer(Modifier.height(8.dp))
                    Text("No pairs watched", fontSize = 13.sp, color = T3)
                    Spacer(Modifier.height(8.dp))
                    TextButton({ showAddSheet = true }) { Text("+ Add pairs", color = Accent) }
                }
            }
            return@Column
        }

        // Filter bar
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("all","BUY","SELL","grade","new").forEach { f ->
                val sel = state.filter == f
                val lbl = when(f) { "all" -> "All"; "BUY" -> "▲ BUY"; "SELL" -> "▼ SELL";
                    "grade" -> "★ A Grade"; "new" -> "🔥 New"; else -> f }
                Box(
                    Modifier.clip(RoundedCornerShape(999.dp))
                        .background(if (sel) AccentDim else S3)
                        .border(1.dp, if (sel) Accent.copy(0.4f) else com.ftt.signal.ui.theme.Divider, RoundedCornerShape(999.dp))
                        .clickable { onSetFilter(f) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) { Text(lbl, fontSize = 11.sp, color = if (sel) Accent else T3) }
            }
            Spacer(Modifier.width(4.dp))
            listOf("conf" to "↓ Conf","new" to "↓ New","pair" to "A–Z").forEach { (s,l) ->
                val sel = state.sort == s
                Box(
                    Modifier.clip(RoundedCornerShape(999.dp))
                        .background(if (sel) S2 else Color.Transparent)
                        .border(1.dp, if (sel) T3.copy(0.5f) else T3.copy(0.2f), RoundedCornerShape(999.dp))
                        .clickable { onSetSort(s) }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) { Text(l, fontSize = 10.sp, color = if (sel) T2 else T3) }
            }
        }

        // Pair cards
        val display = remember(state.pairs, state.results, state.filter, state.sort) {
            var p = state.pairs
            if (state.filter != "all") p = p.filter { pair ->
                val r = state.results[pair]
                when (state.filter) {
                    "BUY"   -> r?.label == "BUY"
                    "SELL"  -> r?.label == "SELL"
                    "grade" -> r?.grade in listOf("A+","A")
                    "new"   -> r?.isNew == true
                    else    -> true
                }
            }
            when (state.sort) {
                "conf" -> p.sortedByDescending { state.results[it]?.confidence ?: 0 }
                "new"  -> p.sortedWith(compareByDescending { state.results[it]?.isNew == true })
                "pair" -> p.sorted()
                else   -> p
            }
        }

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(display) { pair ->
                WlCard(
                    pair     = pair,
                    result   = state.results[pair],
                    onClick  = { onPairClick(pair) },
                    onRemove = { onRemovePair(pair) },
                    onAddJn  = { onAddToJournal(pair) },
                )
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showAddSheet) {
        PairAddSheet(
            existing = state.pairs,
            onAdd    = onAddPair,
            onDismiss = { showAddSheet = false }
        )
    }
}

@Composable
private fun WlSumBox(label: String, value: String, col: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(8.dp)).background(S2)
        .border(1.dp, com.ftt.signal.ui.theme.Divider, RoundedCornerShape(8.dp))
        .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = col)
        Text(label, fontSize = 9.sp, color = T3)
    }
}

@Composable
private fun WlCard(
    pair: String, result: com.ftt.signal.viewmodel.WlResult?,
    onClick: () -> Unit, onRemove: () -> Unit, onAddJn: () -> Unit,
) {
    val lbl    = result?.label ?: ""
    val col    = signalColor(lbl)
    val bg     = signalBg(lbl)
    val conf   = result?.confidence ?: 0
    val now    = System.currentTimeMillis()
    val rem    = if ((result?.expiryAt ?: 0) > now) ((result!!.expiryAt - now) / 1000L).toInt() else 0

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(if (result?.isNew == true) S2 else S2)
            .border(1.dp,
                if (result?.isNew == true) col.copy(0.4f) else if (lbl.isNotEmpty()) col.copy(0.15f) else com.ftt.signal.ui.theme.Divider,
                RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // Direction icon
            Text(when(lbl){"BUY"->"▲";"SELL"->"▼";"WAIT"->"⏳";"CLOSED"->"🔒";"ERR"->"⚠";""->"⟳";else->"■"},
                fontSize = 20.sp, color = if (lbl.isNotEmpty()) col else T3,
                modifier = Modifier.width(28.dp))
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(pair, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = T1,
                        fontFamily = FontFamily.Monospace)
                    if (result?.isNew == true) Pill("NEW", SellRed)
                    if (result?.grade?.isNotEmpty() == true) GradeBadge(result.grade)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (result?.loading == true) "Scanning..." else lbl.ifEmpty { "—" },
                        fontSize = 12.sp, color = col)
                    if (rem > 0) Text("⏰ ${rem/60}:${String.format("%02d",rem%60)}",
                        fontSize = 10.sp, color = if (rem < 60) SellRed else T3,
                        fontFamily = FontFamily.Monospace)
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (lbl in listOf("BUY","SELL") && conf > 0) {
                        IconButton(onClick = onAddJn, modifier = Modifier.size(28.dp)) {
                            Text("+J", fontSize = 10.sp, color = Accent) }
                    }
                    IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                        Text("✕", fontSize = 12.sp, color = T3) }
                }
                result?.timestamp?.takeIf { it.isNotEmpty() }?.let {
                    try {
                        val ts = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                            .format(java.util.Date(java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).parse(it)!!.time))
                        Text(ts, fontSize = 9.sp, color = T3, fontFamily = FontFamily.Monospace)
                    } catch (e: Exception) {}
                }
            }
        }

        if (conf > 0) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Confidence", fontSize = 9.sp, color = T3, modifier = Modifier.width(70.dp))
                Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(999.dp)).background(S3)) {
                    Box(Modifier.fillMaxWidth(conf / 100f).fillMaxHeight()
                        .clip(RoundedCornerShape(999.dp)).background(confColor(conf)))
                }
                Text("$conf%", fontSize = 10.sp, color = confColor(conf),
                    fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PairAddSheet(
    existing: List<String>,
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        val q = query.uppercase().replace(Regex("[^A-Z0-9]"), "")
        com.ftt.signal.data.model.PairData.ALL.filter {
            it.replace(Regex("[/\\-]"), "").contains(q) || q.isEmpty()
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor  = S2,
        shape           = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Text("Add to Watchlist", style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 12.dp))
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(),
                placeholder = { Text("Search pair...", color = T3, fontSize = 13.sp) },
                singleLine = true, shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent, unfocusedBorderColor = com.ftt.signal.ui.theme.Divider,
                    focusedTextColor = T1, unfocusedTextColor = T1))
            Spacer(Modifier.height(10.dp))
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
                modifier = Modifier.heightIn(max = 340.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                androidx.compose.foundation.lazy.grid.items(filtered) { pair ->
                    val inList = existing.contains(pair)
                    Box(
                        Modifier.clip(RoundedCornerShape(8.dp))
                            .background(if (inList) BuyGreenDim else S3)
                            .border(1.dp, if (inList) BuyGreen.copy(0.4f) else com.ftt.signal.ui.theme.Divider, RoundedCornerShape(8.dp))
                            .clickable { if (!inList) onAdd(pair) }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(pair.replace("-OTC",""), fontSize = 10.sp,
                            color = if (inList) BuyGreen else T1,
                            fontWeight = if (inList) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
        }
    }
}

private data class WlStats(val buy: Int, val sell: Int, val wait: Int, val total: Int)
