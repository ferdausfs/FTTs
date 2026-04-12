package com.ftt.signal.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import com.ftt.signal.data.model.ProcessedSignal
import com.ftt.signal.data.model.TfBreakdownItem
import com.ftt.signal.ui.components.*
import com.ftt.signal.ui.theme.*

@Composable
fun TfScreen(signal: ProcessedSignal?) {
    Column(
        Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (signal == null) {
            Box(Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                Text("No signal loaded — refresh on Signal tab", fontSize = 12.sp, color = T3)
            }
            return@Column
        }

        val tf  = signal.tfBreakdown
        val ord = listOf("1min", "5min", "15min")
        val lbl = mapOf("1min" to "1M", "5min" to "5M", "15min" to "15M")

        // HTF conflict warning
        val htf15 = tf["15min"]?.bias ?: ""
        val lo1   = tf["1min"]?.bias ?: ""
        val lo5   = tf["5min"]?.bias ?: ""
        val hasConflict = htf15 in listOf("BUY","SELL") &&
            ((lo1 in listOf("BUY","SELL") && lo1 != htf15) ||
             (lo5 in listOf("BUY","SELL") && lo5 != htf15))

        if (hasConflict) {
            val conflicts = buildList {
                if (lo1.isNotEmpty() && lo1 != htf15) add("1M=$lo1")
                if (lo5.isNotEmpty() && lo5 != htf15) add("5M=$lo5")
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(WaitYellowDim)
                    .border(1.dp, WaitYellow.copy(0.3f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text("⚠️", fontSize = 18.sp)
                Column {
                    Text("HTF CONFLICT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WaitYellow)
                    Text("15M shows $htf15 but ${conflicts.joinToString(", ")}. High risk.",
                        fontSize = 11.sp, color = T2)
                }
            }
        }

        // 3TF compare boxes
        FttCard {
            Text("3TF Direction", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ord.forEach { key ->
                    val item = tf[key]
                    val lbl2 = lbl[key] ?: key
                    val bias = item?.bias ?: ""
                    val col  = signalColor(bias)
                    val bg   = signalBg(bias)
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                            .background(if (bias.isNotEmpty()) bg else S3)
                            .border(1.dp, if (bias.isNotEmpty()) col.copy(0.3f) else Divider, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(lbl2, fontSize = 12.sp, color = T3, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text(when(bias){"BUY"->"▲";"SELL"->"▼";else->"—"},
                            fontSize = 22.sp, color = if (bias.isNotEmpty()) col else T3)
                        Text(if (bias.isEmpty()) "—" else bias, fontSize = 11.sp, color = col,
                            fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("B:${String.format("%.1f", item?.buyVotes ?: 0f)}",
                                fontSize = 9.sp, color = BuyGreen, fontFamily = FontFamily.Monospace)
                            Text("|", fontSize = 9.sp, color = T3)
                            Text("S:${String.format("%.1f", item?.sellVotes ?: 0f)}",
                                fontSize = 9.sp, color = SellRed, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // TF breakdown table
        FttCard {
            Text("TF Breakdown", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 10.dp))
            Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                listOf("TF", "Bias", "Buy↑", "Sell↓", "ADX").forEach { h ->
                    Text(h, fontSize = 10.sp, color = T3, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f))
                }
            }
            Divider(color = Divider, thickness = 1.dp)
            ord.forEach { key ->
                val item = tf[key] ?: return@forEach
                val col  = signalColor(item.bias)
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(lbl[key] ?: key, fontSize = 11.sp, color = Accent,
                        fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f))
                    Box(Modifier.weight(1f)) { DirChip(item.bias) }
                    Text(String.format("%.1f", item.buyVotes), fontSize = 11.sp, color = BuyGreen,
                        fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                    Text(String.format("%.1f", item.sellVotes), fontSize = 11.sp, color = SellRed,
                        fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                    Text(item.adxValue?.let { String.format("%.1f", it) } ?: "—",
                        fontSize = 11.sp, color = T2, fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f))
                }
                Divider(color = Divider.copy(0.5f), thickness = 0.5.dp)
            }
        }

        // ADX detail
        FttCard {
            Text("ADX Trend Strength", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 10.dp))
            ord.forEach { key ->
                val adx  = tf[key]?.adxValue
                val info = adxInfo(adx)
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(lbl[key] ?: key, fontSize = 11.sp, color = Accent,
                        fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp),
                        fontFamily = FontFamily.Monospace)
                    Column(Modifier.weight(1f)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(info.label, fontSize = 10.sp, color = info.color)
                            Text(info.desc, fontSize = 10.sp, color = T3)
                        }
                        Spacer(Modifier.height(4.dp))
                        Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(999.dp)).background(S3)) {
                            Box(Modifier.fillMaxWidth(info.pct).fillMaxHeight()
                                .clip(RoundedCornerShape(999.dp)).background(info.color))
                        }
                    }
                    Text(adx?.let { String.format("%.1f", it) } ?: "—", fontSize = 11.sp,
                        color = info.color, fontFamily = FontFamily.Monospace,
                        modifier = Modifier.width(32.dp))
                }
            }
        }

        // Weighted scores
        FttCard {
            Text("Weighted Scores", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 10.dp))
            ord.forEach { key ->
                val item = tf[key] ?: return@forEach
                val tot  = (item.buyVotes + item.sellVotes).coerceAtLeast(0.01f)
                val bPct = (item.buyVotes / tot).coerceIn(0f, 1f)
                val sPct = (item.sellVotes / tot).coerceIn(0f, 1f)
                Row(Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(lbl[key] ?: key, fontSize = 11.sp, color = Accent,
                        modifier = Modifier.width(28.dp), fontFamily = FontFamily.Monospace)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Buy bar
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(999.dp)).background(S3)) {
                                Box(Modifier.fillMaxWidth(bPct).fillMaxHeight()
                                    .clip(RoundedCornerShape(999.dp)).background(BuyGreen))
                            }
                            Text(String.format("%.1f", item.buyVotes), fontSize = 9.sp, color = BuyGreen,
                                fontFamily = FontFamily.Monospace, modifier = Modifier.width(28.dp))
                        }
                        // Sell bar
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(999.dp)).background(S3)) {
                                Box(Modifier.fillMaxWidth(sPct).fillMaxHeight()
                                    .clip(RoundedCornerShape(999.dp)).background(SellRed))
                            }
                            Text(String.format("%.1f", item.sellVotes), fontSize = 9.sp, color = SellRed,
                                fontFamily = FontFamily.Monospace, modifier = Modifier.width(28.dp))
                        }
                    }
                }
            }
        }

        // Indicator values
        val tfWithInd = ord.filter { tf[it]?.indicators != null }
        if (tfWithInd.isNotEmpty()) {
            FttCard {
                Text("Indicator Values", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 10.dp))
                Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                    listOf("TF", "RSI", "EMA5", "EMA20", "ADX", "MACD").forEach { h ->
                        Text(h, fontSize = 9.sp, color = T3, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f))
                    }
                }
                Divider(color = Divider, thickness = 1.dp)
                tfWithInd.forEach { key ->
                    val ind = tf[key]?.indicators ?: return@forEach
                    val rsi = ind.rsiFloat()
                    val rsiCol = when { rsi != null && rsi > 70 -> SellRed; rsi != null && rsi < 30 -> BuyGreen; else -> T2 }
                    val macd   = ind.macdHistFloat()
                    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
                        Text(lbl[key] ?: key, fontSize = 10.sp, color = Accent,
                            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f))
                        Text(rsi?.let { String.format("%.1f", it) } ?: "—", fontSize = 10.sp,
                            color = rsiCol, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                        Text(ind.ema5Float()?.let { String.format("%.4f", it) } ?: "—", fontSize = 10.sp,
                            color = T2, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                        Text(ind.ema20Float()?.let { String.format("%.4f", it) } ?: "—", fontSize = 10.sp,
                            color = T2, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                        Text(ind.adxFloat()?.let { String.format("%.1f", it) } ?: "—", fontSize = 10.sp,
                            color = T2, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                        Text(macd?.let { String.format("%.4f", it) } ?: "—", fontSize = 10.sp,
                            color = if (macd != null && macd > 0) BuyGreen else if (macd != null) SellRed else T3,
                            fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                    }
                    Divider(color = Divider.copy(0.5f), thickness = 0.5.dp)
                }
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun DirChip(bias: String) {
    val col = signalColor(bias)
    Box(
        Modifier.clip(RoundedCornerShape(4.dp)).background(signalBg(bias))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(if (bias.isEmpty()) "—" else bias, fontSize = 10.sp, color = col,
            fontWeight = FontWeight.Bold)
    }
}

private data class AdxInfo(val label: String, val desc: String, val pct: Float, val color: androidx.compose.ui.graphics.Color)

private fun adxInfo(adx: Float?): AdxInfo {
    if (adx == null) return AdxInfo("—", "No data", 0f, com.ftt.signal.ui.theme.T3)
    return when {
        adx >= 50 -> AdxInfo("Very Strong", ">50", (adx / 100f).coerceIn(0f, 1f), com.ftt.signal.ui.theme.BuyGreen)
        adx >= 25 -> AdxInfo("Trending", "25-50", (adx / 100f).coerceIn(0f, 1f), com.ftt.signal.ui.theme.Accent)
        adx >= 15 -> AdxInfo("Weak Trend", "15-25", (adx / 100f).coerceIn(0f, 1f), com.ftt.signal.ui.theme.WaitYellow)
        else      -> AdxInfo("No Trend", "<15", (adx / 100f).coerceIn(0f, 1f), com.ftt.signal.ui.theme.T3)
    }
}
