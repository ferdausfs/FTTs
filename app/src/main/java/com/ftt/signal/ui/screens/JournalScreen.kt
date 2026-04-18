package com.ftt.signal.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.ftt.signal.db.JournalEntry
import com.ftt.signal.ui.components.*
import com.ftt.signal.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun JournalScreen(
    entries: List<JournalEntry>,
    onMark: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
    onNote: (Long, String) -> Unit,
    onClearAll: () -> Unit,
) {
    var tab     by remember { mutableStateOf("list") }
    var filter  by remember { mutableStateOf("all") }
    var confirm by remember { mutableStateOf<Pair<Long, String>?>(null) }
    var clearConfirm by remember { mutableStateOf(false) }

    val filtered = remember(entries, filter) {
        when (filter) {
            "WIN"     -> entries.filter { it.result == "WIN" }
            "LOSS"    -> entries.filter { it.result == "LOSS" }
            "PENDING" -> entries.filter { it.result == "PENDING" }
            "BUY"     -> entries.filter { it.dir == "BUY" }
            "SELL"    -> entries.filter { it.dir == "SELL" }
            else      -> entries
        }
    }

    Column(Modifier.fillMaxSize().background(Bg)) {
        // Sub-tabs
        ScrollableTabRow(
            selectedTabIndex = listOf("list","calendar","timeline","monthly").indexOf(tab).coerceAtLeast(0),
            containerColor   = S2,
            contentColor     = Accent,
            edgePadding      = 12.dp,
            divider          = { Divider(color = com.ftt.signal.ui.theme.Divider) },
        ) {
            listOf("📋 List","📅 Cal","⏱ TL","📊 Monthly").forEachIndexed { i, label ->
                val tabs = listOf("list","calendar","timeline","monthly")
                Tab(selected = tab == tabs[i], onClick = { tab = tabs[i] },
                    text = { Text(label, fontSize = 12.sp) })
            }
        }

        when (tab) {
            "list"     -> JournalList(filtered, filter, onMark, onDelete, onNote,
                onFilterChange = { filter = it }, onClearAll = { clearConfirm = true })
            "calendar" -> JournalCalendar(entries)
            "timeline" -> JournalTimeline(entries.take(30))
            "monthly"  -> JournalMonthly(entries)
        }
    }

    // Confirm mark
    confirm?.let { (id, result) ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Mark as $result?") },
            confirmButton = { TextButton({ onMark(id, result); confirm = null }) { Text("Yes") } },
            dismissButton = { TextButton({ confirm = null }) { Text("Cancel") } },
            containerColor = S2,
        )
    }

    // Confirm clear all
    if (clearConfirm) {
        AlertDialog(
            onDismissRequest = { clearConfirm = false },
            title = { Text("Clear all journal?") },
            text  = { Text("This cannot be undone.", color = T3) },
            confirmButton = { TextButton({ onClearAll(); clearConfirm = false }) {
                Text("Clear", color = SellRed) } },
            dismissButton = { TextButton({ clearConfirm = false }) { Text("Cancel") } },
            containerColor = S2,
        )
    }
}

@Composable
private fun JournalList(
    entries: List<JournalEntry>, filter: String,
    onMark: (Long, String) -> Unit, onDelete: (Long) -> Unit, onNote: (Long, String) -> Unit,
    onFilterChange: (String) -> Unit, onClearAll: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        // Filter bar
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("all" to "All", "WIN" to "✅ WIN", "LOSS" to "❌ LOSS",
                "PENDING" to "⏳ Pending", "BUY" to "▲ BUY", "SELL" to "▼ SELL").forEach { (f, l) ->
                val sel = filter == f
                Box(
                    Modifier.clip(RoundedCornerShape(999.dp))
                        .background(if (sel) AccentDim else S3)
                        .border(1.dp, if (sel) Accent.copy(0.4f) else com.ftt.signal.ui.theme.Divider, RoundedCornerShape(999.dp))
                        .clickable { onFilterChange(f) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) { Text(l, fontSize = 11.sp, color = if (sel) Accent else T3,
                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal) }
            }
        }

        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📓", fontSize = 44.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("No journal entries", fontSize = 13.sp, color = T3)
                }
            }
            return@Column
        }

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(entries, key = { it.id }) { entry ->
                JournalCard(entry, onMark, onDelete, onNote)
            }
            item {
                TextButton(onClick = onClearAll, modifier = Modifier.fillMaxWidth()) {
                    Text("🗑 Clear All", color = SellRed, fontSize = 12.sp)
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun JournalCard(
    e: JournalEntry, onMark: (Long, String) -> Unit,
    onDelete: (Long) -> Unit, onNote: (Long, String) -> Unit,
) {
    var noteText by remember(e.id) { mutableStateOf(e.note) }
    var expanded by remember { mutableStateOf(false) }
    val resCol = when(e.result) { "WIN" -> BuyGreen; "LOSS" -> SellRed; else -> WaitYellow }
    val dirCol = signalColor(e.dir)
    val ts = remember(e.timestamp) {
        SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(e.timestamp))
    }
    val now = System.currentTimeMillis()
    val rem = ((e.expiryAt - now) / 1000L).toInt().coerceAtLeast(0)

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(S2).border(1.dp,
                when(e.result) { "WIN" -> BuyGreen.copy(0.2f); "LOSS" -> SellRed.copy(0.2f); else -> com.ftt.signal.ui.theme.Divider },
                RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded }
            .padding(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(e.pair, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = T1,
                        fontFamily = FontFamily.Monospace)
                    if (e.autoResolved) Pill("⚡ auto", WaitYellow)
                    if (e.grade.isNotEmpty()) GradeBadge(e.grade)
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DirBadge(e.dir)
                    Text("${e.conf}%", fontSize = 11.sp, color = T3, fontFamily = FontFamily.Monospace)
                    e.entryPrice?.let { Text("Entry: $it", fontSize = 11.sp, color = T3, fontFamily = FontFamily.Monospace) }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    Modifier.clip(RoundedCornerShape(8.dp))
                        .background(resCol.copy(0.12f))
                        .border(1.dp, resCol.copy(0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(when(e.result){"WIN"->"✅ WIN";"LOSS"->"❌ LOSS";else->"⏳"},
                        fontSize = 11.sp, color = resCol, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                if (e.result == "PENDING" && rem > 0) {
                    Text(String.format("%d:%02d", rem/60, rem%60), fontSize = 10.sp,
                        color = if (rem < 60) SellRed else T3, fontFamily = FontFamily.Monospace)
                }
                Text(ts, fontSize = 9.sp, color = T3)
            }
        }

        if (expanded) {
            Spacer(Modifier.height(10.dp))
            Divider(color = com.ftt.signal.ui.theme.Divider.copy(0.5f))
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Session: ${e.session}", fontSize = 10.sp, color = T3)
                Text("Expiry: ${e.expiryMinutes}m", fontSize = 10.sp, color = T3)
                e.exitPrice?.let { Text("Exit: $it", fontSize = 10.sp, color = T2) }
                e.pips?.let { Text("${if(it>=0)"+""else""}${String.format("%.1f",it)}p", fontSize = 10.sp,
                    color = if (it >= 0) BuyGreen else SellRed) }
            }
            Spacer(Modifier.height(8.dp))
            // Note
            OutlinedTextField(
                value = noteText, onValueChange = { noteText = it; onNote(e.id, it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Add note...", fontSize = 11.sp, color = T3) },
                minLines = 1, maxLines = 3,
                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = T1),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent, unfocusedBorderColor = com.ftt.signal.ui.theme.Divider,
                    focusedTextColor = T1, unfocusedTextColor = T1),
                shape = RoundedCornerShape(8.dp),
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (e.result != "WIN")
                    OutlinedButton(onClick = { onMark(e.id, "WIN") },
                        border = BorderStroke(1.dp, BuyGreen.copy(0.4f)),
                        modifier = Modifier.height(34.dp)) {
                        Text("✅", fontSize = 13.sp) }
                if (e.result != "LOSS")
                    OutlinedButton(onClick = { onMark(e.id, "LOSS") },
                        border = BorderStroke(1.dp, SellRed.copy(0.4f)),
                        modifier = Modifier.height(34.dp)) {
                        Text("❌", fontSize = 13.sp) }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { onDelete(e.id) }, modifier = Modifier.size(34.dp)) {
                    Text("🗑", fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun JournalCalendar(entries: List<JournalEntry>) {
    var viewDate  by remember { mutableStateOf(Calendar.getInstance()) }
    var selDay    by remember { mutableIntStateOf(-1) }
    val sdf       = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    val months    = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")

    val yr = viewDate.get(Calendar.YEAR)
    val mo = viewDate.get(Calendar.MONTH)

    val dayMap = remember(entries, yr, mo) {
        val m = mutableMapOf<Int, List<JournalEntry>>()
        entries.forEach { e ->
            val cal = Calendar.getInstance().apply { timeInMillis = e.timestamp }
            if (cal.get(Calendar.YEAR) == yr && cal.get(Calendar.MONTH) == mo) {
                val d = cal.get(Calendar.DAY_OF_MONTH)
                m[d] = (m[d] ?: emptyList()) + e
            }
        }
        m
    }

    val firstDow = Calendar.getInstance().apply { set(yr, mo, 1) }.get(Calendar.DAY_OF_WEEK) - 1
    val daysInMonth = Calendar.getInstance().apply { set(yr, mo, 1) }.getActualMaximum(Calendar.DAY_OF_MONTH)
    val today = Calendar.getInstance()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        // Nav
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton({ viewDate = (viewDate.clone() as Calendar).apply { add(Calendar.MONTH, -1) } }) {
                Text("◀", fontSize = 16.sp, color = T2) }
            Text("${months[mo]} $yr", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = T1)
            IconButton({ viewDate = (viewDate.clone() as Calendar).apply { add(Calendar.MONTH, 1) } }) {
                Text("▶", fontSize = 16.sp, color = T2) }
        }
        Spacer(Modifier.height(8.dp))
        // Weekday headers
        Row(Modifier.fillMaxWidth()) {
            listOf("S","M","T","W","T","F","S").forEach { d ->
                Text(d, Modifier.weight(1f), fontSize = 10.sp, color = T3, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(4.dp))
        // Grid
        val totalCells = firstDow + daysInMonth
        val rows = (totalCells + 6) / 7
        repeat(rows) { row ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { col ->
                    val cellIdx = row * 7 + col
                    val day = cellIdx - firstDow + 1
                    if (day < 1 || day > daysInMonth) {
                        Box(Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val dayEntries = dayMap[day] ?: emptyList()
                        val isToday = today.get(Calendar.YEAR) == yr &&
                                today.get(Calendar.MONTH) == mo &&
                                today.get(Calendar.DAY_OF_MONTH) == day
                        val isSel = selDay == day
                        Box(
                            Modifier.weight(1f).aspectRatio(1f).padding(2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(when { isSel -> AccentDim; isToday -> S3; else -> Color.Transparent })
                                .border(if (isToday) 1.dp else 0.dp, Accent.copy(0.5f), RoundedCornerShape(8.dp))
                                .clickable { selDay = if (selDay == day) -1 else day },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$day", fontSize = 11.sp,
                                    color = if (dayEntries.isNotEmpty()) T1 else T3)
                                Row(horizontalArrangement = Arrangement.Center) {
                                    dayEntries.take(3).forEach { e ->
                                        Box(Modifier.size(4.dp).clip(RoundedCornerShape(999.dp))
                                            .background(when(e.result){"WIN"->BuyGreen;"LOSS"->SellRed;else->WaitYellow}))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selected day detail
        if (selDay > 0 && dayMap.containsKey(selDay)) {
            Spacer(Modifier.height(12.dp))
            val dayList = dayMap[selDay]!!
            val wins  = dayList.count { it.result == "WIN" }
            val losses = dayList.count { it.result == "LOSS" }
            FttCard {
                Text("${months[mo]} $selDay · ${dayList.size} trades · ✅$wins ❌$losses",
                    fontSize = 11.sp, color = T3, modifier = Modifier.padding(bottom = 8.dp))
                dayList.forEach { e ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)
                        .border(BorderStroke(0.dp, Color.Transparent))
                        .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(if(e.dir=="BUY")"▲" else "▼", fontSize = 12.sp, color = signalColor(e.dir))
                        Text(e.pair, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = T1,
                            fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                        Text("${e.conf}%", fontSize = 10.sp, color = T3)
                        Text(e.result, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            color = when(e.result){"WIN"->BuyGreen;"LOSS"->SellRed;else->WaitYellow})
                    }
                    Divider(color = com.ftt.signal.ui.theme.Divider.copy(0.3f))
                }
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun JournalTimeline(entries: List<JournalEntry>) {
    if (entries.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("⏱", fontSize = 40.sp); Text("No trades", fontSize = 13.sp, color = T3) }
        }
        return
    }
    val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)) {
        items(entries, key = { it.id }) { e ->
            val resCol = when(e.result){"WIN"->BuyGreen;"LOSS"->SellRed;else->WaitYellow}
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(12.dp).clip(RoundedCornerShape(999.dp)).background(resCol))
                    Box(Modifier.width(2.dp).height(56.dp).background(com.ftt.signal.ui.theme.Divider))
                }
                Column(Modifier.weight(1f).padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(10.dp)).background(S2)
                    .border(1.dp, com.ftt.signal.ui.theme.Divider, RoundedCornerShape(10.dp)).padding(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${e.pair} ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = T1,
                            fontFamily = FontFamily.Monospace)
                        Text(e.result, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = resCol)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DirBadge(e.dir, Modifier.height(20.dp))
                        Text("${e.conf}%", fontSize = 10.sp, color = T3)
                        if (e.session.isNotEmpty()) Text(e.session, fontSize = 10.sp, color = T3)
                    }
                    Text(sdf.format(Date(e.timestamp)), fontSize = 9.sp, color = T3,
                        modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun JournalMonthly(entries: List<JournalEntry>) {
    val months = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
    val byMonth = remember(entries) {
        val map = mutableMapOf<String, Triple<Int,Int,Float>>() // key -> (wins, losses, pips)
        entries.forEach { e ->
            val cal = Calendar.getInstance().apply { timeInMillis = e.timestamp }
            val key = "${cal.get(Calendar.YEAR)}-${String.format("%02d", cal.get(Calendar.MONTH)+1)}"
            val (w,l,p) = map[key] ?: Triple(0, 0, 0f)
            map[key] = Triple(
                if (e.result == "WIN") w+1 else w,
                if (e.result == "LOSS") l+1 else l,
                p + (e.pips ?: 0f)
            )
        }
        map.entries.sortedByDescending { it.key }
    }

    if (byMonth.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📊", fontSize = 40.sp); Text("No trades", fontSize = 13.sp, color = T3) }
        }
        return
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(byMonth) { (key, stats) ->
            val (w, l, pips) = stats
            val total = w + l
            val wr = if (total > 0) (w * 100 / total) else null
            val wrCol = when { wr == null -> T3; wr >= 60 -> BuyGreen; wr >= 40 -> WaitYellow; else -> SellRed }
            val parts = key.split("-")
            val label = "${months[(parts[1].toIntOrNull() ?: 1) - 1]} ${parts[0]}"
            FttCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = T1,
                        modifier = Modifier.weight(1f))
                    Text(if (wr != null) "$wr%" else "—", fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold, color = wrCol)
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatBox("Total", "${w+l}", T1)
                    StatBox("Wins", "$w", BuyGreen)
                    StatBox("Losses", "$l", SellRed)
                    StatBox("Pips", "${if(pips>=0)"+" else ""}${String.format("%.1f",pips)}",
                        if (pips >= 0) BuyGreen else SellRed)
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(999.dp)).background(S3)) {
                    Box(Modifier.fillMaxWidth((wr ?: 0) / 100f).fillMaxHeight()
                        .clip(RoundedCornerShape(999.dp)).background(wrCol))
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun StatBox(label: String, value: String, col: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = col)
        Text(label, fontSize = 10.sp, color = T3)
    }
}
