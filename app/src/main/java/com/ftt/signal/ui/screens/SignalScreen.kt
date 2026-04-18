package com.ftt.signal.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.ftt.signal.data.model.*
import com.ftt.signal.ui.components.*
import com.ftt.signal.ui.theme.*
import com.ftt.signal.viewmodel.SignalUiState
import kotlinx.coroutines.delay

@Composable
fun SignalScreen(
    state: SignalUiState,
    curPair: String,
    slPips: Float,
    tpPips: Float,
    soundOn: Boolean,
    onRefresh: () -> Unit,
    onToggleSound: () -> Unit,
    onSaveSLTP: (Float, Float) -> Unit,
    onOpenSettings: () -> Unit,
    calcSLTP: (String, String, Double, Float, Float) -> Triple<String, String, String>,
) {
    var showPicker by remember { mutableStateOf(false) }
    var toastMsg   by remember { mutableStateOf("") }
    var remainSec  by remember { mutableIntStateOf(0) }
    var totalSec   by remember { mutableIntStateOf(300) }
    val sig = state.signal

    // Countdown timer
    LaunchedEffect(sig?.timestamp, state.isCached) {
        if (sig == null) return@LaunchedEffect
        val expMs = System.currentTimeMillis() + sig.expiryMinutes * 60_000L
        totalSec  = sig.expiryMinutes * 60
        while (true) {
            remainSec = ((expMs - System.currentTimeMillis()) / 1000L).toInt().coerceAtLeast(0)
            if (remainSec == 0) { onRefresh(); break }
            delay(1000L)
        }
    }

    Box(Modifier.fillMaxSize().background(Bg)) {
        Column(Modifier.fillMaxSize()) {
            // ── Top bar ───────────────────────────────────────
            TopBar(
                curPair    = curPair,
                isLive     = state.signal != null && !state.isLoading,
                remainSec  = remainSec,
                isLoading  = state.isLoading,
                soundOn    = soundOn,
                onPairTap  = { showPicker = true },
                onRefresh  = onRefresh,
                onSound    = onToggleSound,
                onSettings = onOpenSettings,
            )

            // ── Content ───────────────────────────────────────
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when {
                    state.isLoading && sig == null -> SignalSkeleton()
                    !state.isOpen   -> ClosedCard(curPair, state.closedReason)
                    state.error != null && sig == null -> ErrorCard(state.error, onRefresh)
                    sig != null     -> {
                        // Cache strip
                        CacheStrip(state.isCached, sig.expiryMinutes, state.lastUpdated) {
                            onRefresh()
                        }

                        // Wait reasons card
                        if (sig.label == "WAIT") WaitCard(sig)

                        // Main signal card
                        SignalCard(sig, remainSec, totalSec, slPips, tpPips, calcSLTP)

                        // Signal comparison
                        state.prevSignal?.let { PrevCompareCard(it, sig) }

                        // Confidence history
                        if (state.confHistory.size > 1) ConfHistoryCard(state.confHistory)

                        // AI validation
                        sig.aiValidation?.let { AiCard(it) }
                    }
                }
                Spacer(Modifier.height(80.dp))
            }
        }

        // Toast
        if (toastMsg.isNotEmpty()) {
            Box(Modifier.fillMaxSize().padding(bottom = 90.dp), contentAlignment = Alignment.BottomCenter) {
                FttToast(toastMsg) { toastMsg = "" }
            }
        }
    }

    if (showPicker) {
        // PairPicker is handled by parent (MainNav passes selectPair)
        showPicker = false
    }
}

@Composable
private fun TopBar(
    curPair: String, isLive: Boolean, remainSec: Int, isLoading: Boolean,
    soundOn: Boolean, onPairTap: () -> Unit, onRefresh: () -> Unit,
    onSound: () -> Unit, onSettings: () -> Unit,
) {
    val mm  = remainSec / 60
    val ss  = remainSec % 60
    Row(
        Modifier.fillMaxWidth().background(S2.copy(alpha = 0.92f))
            .border(BorderStroke(1.dp, Brand1.copy(alpha = 0.1f)))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Brand
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(Brand1, Brand2))),
            contentAlignment = Alignment.Center
        ) { Text("FTT", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
            color = Color.White, fontFamily = FontFamily.Monospace) }

        Column(Modifier.weight(1f)) {
            Text("FTT Signal", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = T1)
            Text("v6.5 Native", fontSize = 10.sp, color = T3, fontFamily = FontFamily.Monospace)
        }

        // Pair selector
        LiveDot(isLive)
        Box(
            Modifier.clip(RoundedCornerShape(999.dp))
                .background(AccentDim)
                .border(1.dp, Accent.copy(0.3f), RoundedCornerShape(999.dp))
                .clickable(onClick = onPairTap)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(curPair, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Accent,
                    fontFamily = FontFamily.Monospace)
                Text("▾", fontSize = 9.sp, color = T3)
            }
        }

        // Countdown pill
        if (remainSec > 0) {
            val pillCol = if (remainSec < 60) WaitYellow else T3
            Box(
                Modifier.clip(RoundedCornerShape(999.dp))
                    .background(if (remainSec < 60) WaitYellowDim else S3)
                    .border(1.dp, pillCol.copy(0.3f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) { Text(String.format("%d:%02d", mm, ss), fontSize = 11.sp, color = pillCol,
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }
        }

        // Refresh
        val spinAnim = rememberInfiniteTransition(label = "spin")
        val angle by spinAnim.animateFloat(0f, 360f,
            infiniteRepeatable(tween(800, easing = LinearEasing)), label = "a")
        IconButton(onClick = onRefresh, modifier = Modifier.size(36.dp)) {
            Text("↻", fontSize = 18.sp, color = T2,
                modifier = if (isLoading) Modifier.graphicsLayer { rotationZ = angle } else Modifier)
        }

        // Sound
        IconButton(onClick = onSound, modifier = Modifier.size(36.dp)) {
            Text(if (soundOn) "🔊" else "🔇", fontSize = 16.sp)
        }

        // Settings
        IconButton(onClick = onSettings, modifier = Modifier.size(36.dp)) {
            Text("⚙", fontSize = 16.sp, color = T3)
        }
    }
}

@Composable
private fun SignalCard(
    sig: ProcessedSignal, remainSec: Int, totalSec: Int,
    slPips: Float, tpPips: Float,
    calcSLTP: (String, String, Double, Float, Float) -> Triple<String, String, String>
) {
    val col = signalColor(sig.label)
    val bg  = signalBg(sig.label)

    FttCard {
        // Top: pair + session info + confidence ring
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(sig.symbol, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = T1,
                    fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Pill(sig.sessionLabel, Accent)
                    Pill("ATR:${sig.atrLevel}", when(sig.atrLevel) { "HIGH" -> SellRed; "LOW" -> BuyGreen; else -> WaitYellow })
                    if (sig.grade.isNotEmpty()) GradeBadge(sig.grade)
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    sig.marketCondition.take(2).forEach { mc ->
                        Pill(mc.replace("_", " "), T3)
                    }
                }
            }
            ConfRing(sig.confidence, 60.dp)
        }

        Spacer(Modifier.height(16.dp))

        // Direction
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(bg)
                .border(1.dp, col.copy(0.25f), RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                when(sig.label) { "BUY" -> "▲"; "SELL" -> "▼"; "WAIT" -> "⏳"; else -> "■" },
                fontSize = 36.sp, color = col
            )
            Column {
                Text(sig.label, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = col)
                if (sig.timestamp.isNotEmpty()) {
                    val ts = try {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                        val tsdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                        tsdf.format(sdf.parse(sig.timestamp) ?: java.util.Date())
                    } catch (e: Exception) { "" }
                    val gradeLabel = if (sig.grade.isNotEmpty()) " · Grade ${sig.grade}" else ""
                    Text("$ts$gradeLabel",
                        fontSize = 12.sp, color = T3)
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Strength meter
        StrengthMeter(sig.confidence)

        Spacer(Modifier.height(14.dp))

        // Entry / Expiry / TF row
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            InfoBox("Entry", sig.entryPrice ?: "—", col)
            InfoBox("Expiry", sig.expirySuggestion, col)
            InfoBox("TF Agree", sig.tfAgreement, col)
        }

        Spacer(Modifier.height(14.dp))

        // SL / TP widget
        if (sig.label == "BUY" || sig.label == "SELL") {
            val ep = sig.entryPrice?.toDoubleOrNull()
            SLTPWidget(sig.rawPair, sig.label, ep, slPips, tpPips, calcSLTP, {})
            Spacer(Modifier.height(14.dp))
        }

        // Countdown ring + session info row
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CountdownRing(remainSec, totalSec, 62.dp)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                InfoRow("Session", sig.sessionLabel)
                InfoRow("HTF", sig.h1Structure)
                InfoRow("ATR", sig.atrLevel)
                sig.averageConfluence?.let { InfoRow("Confluence", "$it/11") }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Category scores
        val tfa  = sig.tfBreakdown
        val bestKey = sig.rawSignal.bestTimeframe?.timeframe ?: "5min"
        val bestTfItem = tfa[bestKey]
        val catScores = bestTfItem?.categoryScores
        if (catScores != null && catScores.isNotEmpty()) {
            val maxVal = catScores.values.maxOf { maxOf(it.up ?: 0f, it.down ?: 0f) }.coerceAtLeast(1f)
            Text("Category Scores · $bestKey", fontSize = 10.sp, color = T3,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 6.dp))
            val cats = listOf("trend" to "Trend","momentum" to "Mom","macd" to "MACD",
                "stochastic" to "Stoch","bands" to "BB/CCI","adx" to "ADX",
                "patterns" to "Pattern","sr" to "S/R","fvg" to "FVG")
            cats.forEach { (key, lbl) ->
                catScores[key]?.let { sc -> ScoreBar(lbl, sc.up ?: 0f, sc.down ?: 0f, maxVal) }
            }
        } else {
            val maxScore = maxOf(sig.buyScore.toFloat(), sig.sellScore.toFloat(), 1f)
            Text("Signal Scores", fontSize = 10.sp, color = T3, fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 6.dp))
            ScoreBar("BUY ▲", sig.buyScore.toFloat(), 0f, maxScore)
            ScoreBar("SELL ▼", 0f, sig.sellScore.toFloat(), maxScore)
        }

        // Analysis reasons
        if (sig.reasons.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("Analysis", fontSize = 11.sp, color = T3, fontWeight = FontWeight.SemiBold)
            sig.reasons.forEach { r ->
                Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(5.dp).clip(RoundedCornerShape(999.dp)).background(col)
                        .align(Alignment.CenterVertically))
                    Text(r, fontSize = 11.sp, color = T2)
                }
            }
        }
    }
}

@Composable
private fun InfoBox(label: String, value: String, valCol: Color = T1) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = T3)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = valCol,
            fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 11.sp, color = T3, modifier = Modifier.width(70.dp))
        Text(value, fontSize = 11.sp, color = T1, fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun SLTPWidget(
    pair: String, dir: String, ep: Double?,
    slPips: Float, tpPips: Float,
    calcSLTP: (String, String, Double, Float, Float) -> Triple<String, String, String>,
    onSave: () -> Unit
) {
    var sl by remember { mutableStateOf(slPips.toString()) }
    var tp by remember { mutableStateOf(tpPips.toString()) }
    val slCalc = sl.toFloatOrNull() ?: slPips
    val tpCalc = tp.toFloatOrNull() ?: tpPips
    val calc   = ep?.let { calcSLTP(pair, dir, it, slCalc, tpCalc) }

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(S3).border(1.dp, Divider, RoundedCornerShape(10.dp)).padding(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("SL / TP", fontSize = 11.sp, color = T3, fontWeight = FontWeight.SemiBold)
            Text("RR 1:${calc?.third ?: String.format("%.1f", tpCalc/slCalc)}",
                fontSize = 11.sp, color = Accent, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(
                Modifier.weight(1f)
                    .clip(RoundedCornerShape(8.dp)).background(SellRedDim)
                    .border(1.dp, SellRed.copy(0.3f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Text("Stop Loss", fontSize = 10.sp, color = T3)
                Text(calc?.first ?: "—", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    color = SellRed, fontFamily = FontFamily.Monospace)
                Text("${sl}p", fontSize = 10.sp, color = T3)
            }
            Text(if (dir == "BUY") "▲" else "▼", fontSize = 20.sp, color = signalColor(dir),
                modifier = Modifier.align(Alignment.CenterVertically))
            Column(
                Modifier.weight(1f)
                    .clip(RoundedCornerShape(8.dp)).background(BuyGreenDim)
                    .border(1.dp, BuyGreen.copy(0.3f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Text("Take Profit", fontSize = 10.sp, color = T3)
                Text(calc?.second ?: "—", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    color = BuyGreen, fontFamily = FontFamily.Monospace)
                Text("${tp}p", fontSize = 10.sp, color = T3)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(sl, { sl = it }, Modifier.weight(1f).height(48.dp),
                label = { Text("SL", fontSize = 10.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SellRed, unfocusedBorderColor = Divider,
                    focusedTextColor = T1, unfocusedTextColor = T1),
                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace))
            OutlinedTextField(tp, { tp = it }, Modifier.weight(1f).height(48.dp),
                label = { Text("TP", fontSize = 10.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BuyGreen, unfocusedBorderColor = Divider,
                    focusedTextColor = T1, unfocusedTextColor = T1),
                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace))
            Button(onClick = onSave, modifier = Modifier.height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentDim,
                    contentColor = Accent)) { Text("Set", fontSize = 12.sp) }
        }
    }
}

@Composable
private fun WaitCard(sig: ProcessedSignal) {
    val reasons = buildList {
        sig.filtersApplied.forEach { add(it) }
        if (sig.newsBlackout?.blocked == true) add("News Blackout: ${sig.newsBlackout.label}")
        if (sig.rawSignal.alignment == "MIXED") add("Mixed TF alignment")
        if (sig.confidence < 65) add("Low confidence: ${sig.confidence}% (floor 65%)")
        if (isEmpty()) add("Score below threshold")
    }
    FttCard {
        Text("⏳ Why WAIT?", fontSize = 13.sp, fontWeight = FontWeight.Bold,
            color = WaitYellow, modifier = Modifier.padding(bottom = 8.dp))
        reasons.forEach { r ->
            Row(Modifier.padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("→", fontSize = 11.sp, color = WaitYellow)
                Text(r, fontSize = 11.sp, color = T2)
            }
        }
        Text("↻ Auto-refresh in ${sig.expiryMinutes} min",
            fontSize = 10.sp, color = T3, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun AiCard(ai: AiValidation) {
    val col = if (ai.approved == true) BuyGreen else SellRed
    FttCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🤖", fontSize = 16.sp)
            Text("AI Validation", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = T1)
            Spacer(Modifier.weight(1f))
            Pill(if (ai.approved == true) "APPROVED" else "REJECTED", col)
        }
        if (!ai.reason.isNullOrEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(ai.reason, fontSize = 11.sp, color = T2)
        }
        if (!ai.confidence.isNullOrEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text("Confidence: ${ai.confidence}", fontSize = 10.sp, color = T3,
                fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun PrevCompareCard(prev: ProcessedSignal, cur: ProcessedSignal) {
    val diff = cur.confidence - prev.confidence
    FttCard {
        Text("Signal Comparison", fontSize = 12.sp, color = T3, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("PREVIOUS", fontSize = 9.sp, color = T3, fontFamily = FontFamily.Monospace)
                Text(prev.label, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    color = signalColor(prev.label))
                Text("${prev.confidence}%", fontSize = 11.sp, color = T3,
                    fontFamily = FontFamily.Monospace)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("→", fontSize = 18.sp, color = T3)
                Text(if (diff > 0) "+$diff%" else "$diff%", fontSize = 11.sp,
                    color = if (diff > 0) BuyGreen else if (diff < 0) SellRed else T3,
                    fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("CURRENT", fontSize = 9.sp, color = T3, fontFamily = FontFamily.Monospace)
                Text(cur.label, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    color = signalColor(cur.label))
                Text("${cur.confidence}%", fontSize = 11.sp, color = T3,
                    fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun ConfHistoryCard(hist: List<Pair<Int, String>>) {
    val maxConf = hist.maxOf { it.first }.coerceAtLeast(50)
    FttCard {
        Text("Confidence History", fontSize = 11.sp, color = T3, modifier = Modifier.padding(bottom = 8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom) {
            hist.forEachIndexed { i, (c, l) ->
                val col   = signalColor(l)
                val isLast = i == hist.lastIndex
                val pct   = c.toFloat() / maxConf
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.fillMaxWidth().height((42 * pct).dp)
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(col.copy(if (isLast) 1f else 0.4f)))
                    Text("$c", fontSize = 8.sp, color = T3, textAlign = TextAlign.Center,
                        fontFamily = FontFamily.Monospace)
                    Text(when(l){"BUY"->"▲";"SELL"->"▼";"WAIT"->"⏳";else->"■"},
                        fontSize = 9.sp, color = col, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun CacheStrip(isCached: Boolean, expiryMin: Int, lastUpdated: String?, onRefresh: () -> Unit) {
    val col  = if (isCached) WaitYellow else BuyGreen
    val text = if (isCached) "Cached signal · $lastUpdated" else "New signal · auto-refresh ${expiryMin}m"
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(col.copy(0.08f))
            .border(1.dp, col.copy(0.2f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).clip(RoundedCornerShape(999.dp)).background(col))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 11.sp, color = col, modifier = Modifier.weight(1f))
        if (isCached) {
            Text("↻", fontSize = 14.sp, color = T3, modifier = Modifier.clickable(onClick = onRefresh))
        }
    }
}

@Composable
private fun ClosedCard(pair: String, reason: String) {
    Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🔒", fontSize = 44.sp)
            Spacer(Modifier.height(12.dp))
            Text(pair, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
                color = T1, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(6.dp))
            Text("MARKET CLOSED", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WaitYellow)
            Spacer(Modifier.height(8.dp))
            Text(reason, fontSize = 12.sp, color = T2, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ErrorCard(error: String, onRetry: () -> Unit) {
    val isApiLimit = error.startsWith("API_LIMIT:")
    FttCard {
        if (isApiLimit) {
            Text("📡", fontSize = 32.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(8.dp))
            Text("API LIMIT REACHED", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                color = WaitYellow, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(6.dp))
            Text(error.removePrefix("API_LIMIT:").trim(),
                fontSize = 11.sp, color = T2, textAlign = TextAlign.Center)
        } else {
            Text("⚠ Failed", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SellRed)
            Spacer(Modifier.height(4.dp))
            Text(error, fontSize = 11.sp, color = T2)
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry, modifier = Modifier.align(Alignment.CenterHorizontally),
            colors = ButtonDefaults.buttonColors(containerColor = AccentDim, contentColor = Accent)
        ) { Text("Retry") }
    }
}

@Composable
private fun SignalSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(3) {
            ShimmerBox(Modifier.fillMaxWidth().height(120.dp))
        }
    }
}
