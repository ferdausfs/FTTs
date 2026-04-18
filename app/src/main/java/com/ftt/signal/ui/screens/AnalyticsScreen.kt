package com.ftt.signal.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.ftt.signal.db.JournalEntry
import com.ftt.signal.ui.components.FttCard
import com.ftt.signal.ui.components.Pill
import com.ftt.signal.ui.theme.*
import java.util.*

@Composable
fun AnalyticsScreen(
    entries: List<JournalEntry>,
    lotSize: Float,
    pipValue: Float,
    onSavePL: (Float, Float) -> Unit,
) {
    val closed  = entries.filter { it.result in listOf("WIN","LOSS") }
    val wins    = closed.count { it.result == "WIN" }
    val losses  = closed.count { it.result == "LOSS" }
    val pending = entries.count { it.result == "PENDING" }
    val wr      = if (closed.isNotEmpty()) wins * 100 / closed.size else null
    val wrCol   = when { wr == null -> T3; wr >= 60 -> BuyGreen; wr >= 40 -> WaitYellow; else -> SellRed }

    // Streak
    var streak = 0; var streakType = ""
    for (e in entries) {
        if (e.result !in listOf("WIN","LOSS")) break
        if (streak == 0) { streakType = e.result; streak = 1 }
        else if (e.result == streakType) streak++ else break
    }

    // Pips
    val winPips  = closed.filter { it.result == "WIN"  && it.pips != null }.sumOf { it.pips!!.toDouble() }
    val lossPips = closed.filter { it.result == "LOSS" && it.pips != null }.sumOf { it.pips!!.toDouble() }
    val netPips  = winPips + lossPips
    val pipsCount = closed.count { it.pips != null }
    val avgPip   = if (pipsCount > 0) netPips / pipsCount else 0.0

    var lotText by remember { mutableStateOf(lotSize.toString()) }
    var pipText by remember { mutableStateOf(pipValue.toString()) }
    val estPL   = netPips * (lotText.toDoubleOrNull() ?: 0.1) * (pipText.toDoubleOrNull() ?: 10.0)

    Column(Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // Win rate donut
        FttCard {
            Text("Win Rate", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
                    val sweep = 360f * (wr ?: 0) / 100f
                    Canvas(Modifier.size(84.dp)) {
                        val stroke = 10.dp.toPx()
                        val inset  = stroke / 2f
                        drawArc(S3, -90f, 360f, false,
                            topLeft = Offset(inset, inset),
                            size = Size(this.size.width - stroke, this.size.height - stroke),
                            style = Stroke(stroke))
                        drawArc(wrCol, -90f, sweep, false,
                            topLeft = Offset(inset, inset),
                            size = Size(this.size.width - stroke, this.size.height - stroke),
                            style = Stroke(stroke, cap = StrokeCap.Round))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (wr != null) "$wr%" else "—", fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold, color = wrCol)
                        Text("WR", fontSize = 10.sp, color = T3)
                    }
                }
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatTile("Wins",    "$wins",    BuyGreen)
                    StatTile("Losses",  "$losses",  SellRed)
                    StatTile("Pending", "$pending", WaitYellow)
                    StatTile("Total",   "${entries.size}", T1)
                }
            }
        }

        // Streak + stats
        FttCard {
            Text("Streak & Stats", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StreakBox(streak, streakType)
                StatTile("Closed", "${closed.size}", T1)
                val avgSign = if (avgPip >= 0) "+" else ""
                StatTile("Avg Pip", "$avgSign${String.format("%.1f", avgPip)}",
                    if (avgPip >= 0) BuyGreen else SellRed)
            }
        }

        // P&L
        FttCard {
            Text("P&L Calculator", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Lot Size", fontSize = 10.sp, color = T3)
                    OutlinedTextField(lotText, { lotText = it }, Modifier.fillMaxWidth().height(52.dp),
                        singleLine = true, shape = RoundedCornerShape(8.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, color = T1,
                            fontFamily = FontFamily.Monospace),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Accent, unfocusedBorderColor = com.ftt.signal.ui.theme.Divider,
                            focusedTextColor = T1, unfocusedTextColor = T1))
                }
                Column(Modifier.weight(1f)) {
                    Text("Pip $", fontSize = 10.sp, color = T3)
                    OutlinedTextField(pipText, { pipText = it }, Modifier.fillMaxWidth().height(52.dp),
                        singleLine = true, shape = RoundedCornerShape(8.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, color = T1,
                            fontFamily = FontFamily.Monospace),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Accent, unfocusedBorderColor = com.ftt.signal.ui.theme.Divider,
                            focusedTextColor = T1, unfocusedTextColor = T1))
                }
                Column(Modifier.padding(top = 16.dp)) {
                    Button(onClick = {
                        onSavePL(lotText.toFloatOrNull() ?: 0.1f, pipText.toFloatOrNull() ?: 10f)
                    }, modifier = Modifier.height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentDim, contentColor = Accent)
                    ) { Text("Set", fontSize = 12.sp) }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                val plSign = if (estPL >= 0) "+" else ""
                PlBox("Est P&L", "$plSign${String.format("%.2f", estPL)}$",
                    if (estPL >= 0) BuyGreen else SellRed)
                PlBox("Win Pips", "+${String.format("%.1f",winPips)}", BuyGreen)
                PlBox("Loss Pips", "${String.format("%.1f",lossPips)}", SellRed)
            }
        }

        // Session performance
        val sessMap = mutableMapOf<String, Pair<Int,Int>>()
        closed.forEach { e ->
            val s = e.session.ifEmpty { "London" }
            val (w,l) = sessMap[s] ?: (0 to 0)
            sessMap[s] = if (e.result == "WIN") (w+1 to l) else (w to l+1)
        }
        if (sessMap.isNotEmpty()) {
            FttCard {
                Text("Session Performance", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp))
                listOf("London","NY","Asia","London/NY").forEach { sess ->
                    val (sw, sl) = sessMap[sess] ?: return@forEach
                    val tot = sw + sl
                    val sWr = if (tot > 0) sw * 100 / tot else null
                    val sc  = when { sWr == null -> T3; sWr >= 60 -> BuyGreen; sWr >= 40 -> WaitYellow; else -> SellRed }
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(sess, fontSize = 12.sp, color = T2, modifier = Modifier.width(80.dp))
                        Column(Modifier.weight(1f)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                val wrLabel = if (sWr != null) " · $sWr% WR" else ""
                                Text("$tot trades$wrLabel",
                                    fontSize = 10.sp, color = T3)
                            }
                            Spacer(Modifier.height(3.dp))
                            Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(999.dp)).background(S3)) {
                                Box(Modifier.fillMaxWidth((sWr ?: 0) / 100f).fillMaxHeight()
                                    .clip(RoundedCornerShape(999.dp)).background(sc))
                            }
                        }
                        Text(if (sWr != null) "$sWr%" else "—", fontSize = 12.sp,
                            fontWeight = FontWeight.Bold, color = sc,
                            modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
                    }
                }
            }
        }

        // Best hours heatmap
        if (closed.isNotEmpty()) {
            val hourMap = mutableMapOf<Int, Pair<Int,Int>>()
            closed.forEach { e ->
                val h  = Calendar.getInstance().apply { timeInMillis = e.timestamp }.get(Calendar.HOUR_OF_DAY)
                val b  = (h / 3) * 3
                val (w,l) = hourMap[b] ?: (0 to 0)
                hourMap[b] = if (e.result == "WIN") (w+1 to l) else (w to l+1)
            }
            val maxTot = (0..7).mapNotNull { hourMap[it*3] }.maxOfOrNull { it.first + it.second } ?: 1
            FttCard {
                Text("Best Hours (UTC)", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom) {
                    (0..7).forEach { i ->
                        val h = i * 3
                        val (hw, hl) = hourMap[h] ?: (0 to 0)
                        val tot  = hw + hl
                        val hWr  = if (tot > 0) hw * 100 / tot else null
                        val hCol = when { hWr == null -> S3; hWr >= 60 -> BuyGreen; hWr >= 40 -> WaitYellow; else -> SellRed }
                        val barH = if (tot > 0) ((tot.toFloat() / maxTot) * 44).coerceAtLeast(4f) else 3f
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.BottomCenter) {
                                Box(Modifier.fillMaxWidth().height(barH.dp).clip(RoundedCornerShape(3.dp)).background(hCol))
                            }
                            Text("${h}h", fontSize = 8.sp, color = T3, textAlign = TextAlign.Center)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("UTC · 3h blocks", fontSize = 9.sp, color = T3,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        }

        // Pair performance
        val pairMap = mutableMapOf<String, Triple<Int,Int,Int>>() // wins, losses, pending
        entries.forEach { e ->
            val (w,l,p) = pairMap[e.pair] ?: Triple(0,0,0)
            pairMap[e.pair] = when(e.result) {
                "WIN" -> Triple(w+1, l, p); "LOSS" -> Triple(w, l+1, p); else -> Triple(w, l, p+1)
            }
        }
        if (pairMap.isNotEmpty()) {
            val sorted = pairMap.entries.map { (pair, v) ->
                val (w,l,p) = v; val tot = w+l
                PairPerf(pair, w, l, p, if(tot>0) w*100/tot else null)
            }.sortedByDescending { it.wr ?: -1 }.take(10)

            FttCard {
                Text("Pair Performance", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp))
                sorted.forEach { pp ->
                    val pc = when { pp.wr == null -> T3; pp.wr >= 60 -> BuyGreen; pp.wr >= 40 -> WaitYellow; else -> SellRed }
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(pp.pair, fontSize = 11.sp, color = T2, fontFamily = FontFamily.Monospace,
                            modifier = Modifier.width(80.dp))
                        Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(999.dp)).background(S3)) {
                            Box(Modifier.fillMaxWidth((pp.wr ?: 0) / 100f).fillMaxHeight()
                                .clip(RoundedCornerShape(999.dp)).background(pc))
                        }
                        Text(if (pp.wr != null) "${pp.wr}%" else "—", fontSize = 10.sp,
                            color = pc, modifier = Modifier.width(34.dp), textAlign = TextAlign.End,
                            fontFamily = FontFamily.Monospace)
                        Text("${pp.wins+pp.losses}T", fontSize = 9.sp, color = T3,
                            modifier = Modifier.width(28.dp), fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun StatTile(label: String, value: String, col: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = col)
        Text(label, fontSize = 10.sp, color = T3)
    }
}

@Composable
private fun StreakBox(streak: Int, type: String) {
    val col  = if (type == "WIN") BuyGreen else if (type == "LOSS") SellRed else T3
    val ico  = if (type == "WIN") "✅" else if (type == "LOSS") "❌" else ""
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$streak", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = col)
        Text("streak", fontSize = 10.sp, color = T3)
        if (ico.isNotEmpty()) Text(ico.repeat(streak.coerceAtMost(5)), fontSize = 10.sp)
    }
}

@Composable
private fun PlBox(label: String, value: String, col: androidx.compose.ui.graphics.Color) {
    Column(
        Modifier.clip(RoundedCornerShape(10.dp)).background(S3)
            .border(1.dp, com.ftt.signal.ui.theme.Divider, RoundedCornerShape(10.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 10.sp, color = T3)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = col,
            fontFamily = FontFamily.Monospace)
    }
}

private data class PairPerf(val pair: String, val wins: Int, val losses: Int, val pending: Int, val wr: Int?)
