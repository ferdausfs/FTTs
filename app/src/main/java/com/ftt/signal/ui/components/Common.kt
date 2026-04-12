package com.ftt.signal.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
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
import com.ftt.signal.ui.theme.*

// ── Grade badge ───────────────────────────────────────────────
@Composable
fun GradeBadge(grade: String, modifier: Modifier = Modifier) {
    val col = gradeColor(grade)
    Box(
        modifier
            .clip(RoundedCornerShape(4.dp))
            .background(col.copy(alpha = 0.15f))
            .border(1.dp, col.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(grade, fontSize = 10.sp, color = col, fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace)
    }
}

// ── Signal direction badge ────────────────────────────────────
@Composable
fun DirBadge(label: String, modifier: Modifier = Modifier) {
    val col = signalColor(label)
    val bg  = signalBg(label)
    val ico = when (label) { "BUY" -> "▲"; "SELL" -> "▼"; "WAIT" -> "⏳"; else -> "■" }
    Box(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, col.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text("$ico $label", fontSize = 11.sp, color = col, fontWeight = FontWeight.Bold)
    }
}

// ── Small colored pill ────────────────────────────────────────
@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, fontSize = 10.sp, color = color, fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace)
    }
}

// ── Confidence ring (donut) ───────────────────────────────────
@Composable
fun ConfRing(conf: Int, size: Dp = 56.dp) {
    val col   = confColor(conf)
    val label = when { conf >= 85 -> "Elite"; conf >= 70 -> "Strong"; conf >= 50 -> "Good";
        conf >= 35 -> "Mod"; else -> "Weak" }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(size), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(size)) {
                val stroke = size.toPx() * 0.12f
                val inset  = stroke / 2f
                val sweep  = 360f * conf / 100f
                drawArc(S3, -90f, 360f, false,
                    topLeft = Offset(inset, inset),
                    size    = Size(this.size.width - stroke, this.size.height - stroke),
                    style   = Stroke(stroke, cap = StrokeCap.Round))
                drawArc(col, -90f, sweep, false,
                    topLeft = Offset(inset, inset),
                    size    = Size(this.size.width - stroke, this.size.height - stroke),
                    style   = Stroke(stroke, cap = StrokeCap.Round))
            }
            Text("$conf%", fontSize = (size.value * 0.22f).sp, fontWeight = FontWeight.Bold,
                color = col, fontFamily = FontFamily.Monospace)
        }
        Text(label, fontSize = 9.sp, color = T3)
    }
}

// ── Countdown ring ────────────────────────────────────────────
@Composable
fun CountdownRing(remainSec: Int, totalSec: Int, size: Dp = 62.dp) {
    val pct = if (totalSec > 0) remainSec.toFloat() / totalSec else 0f
    val col = when { pct > 0.5f -> BuyGreen; pct > 0.25f -> WaitYellow; else -> SellRed }
    val mm  = remainSec / 60
    val ss  = remainSec % 60
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(size), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(size)) {
                val stroke = size.toPx() * 0.1f
                val inset  = stroke / 2f
                val sweep  = 360f * pct
                drawArc(S3, -90f, 360f, false,
                    topLeft = Offset(inset, inset),
                    size    = Size(this.size.width - stroke, this.size.height - stroke),
                    style   = Stroke(stroke))
                drawArc(col, -90f, sweep, false,
                    topLeft = Offset(inset, inset),
                    size    = Size(this.size.width - stroke, this.size.height - stroke),
                    style   = Stroke(stroke, cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(String.format("%d:%02d", mm, ss),
                    fontSize = (size.value * 0.19f).sp, fontWeight = FontWeight.Bold,
                    color = col, fontFamily = FontFamily.Monospace)
                Text("next", fontSize = 8.sp, color = T3)
            }
        }
    }
}

// ── Signal strength meter ─────────────────────────────────────
@Composable
fun StrengthMeter(conf: Int) {
    val col   = confColor(conf)
    val label = when { conf >= 85 -> "Elite"; conf >= 70 -> "Strong"; conf >= 50 -> "Good";
        conf >= 35 -> "Moderate"; else -> "Weak" }
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Signal Strength", fontSize = 10.sp, color = T3)
            Text("$label ($conf%)", fontSize = 10.sp, color = col, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp)).background(S3)) {
            Box(Modifier.fillMaxWidth(conf.coerceIn(0, 100) / 100f).fillMaxHeight()
                .clip(RoundedCornerShape(999.dp)).background(col))
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Weak", fontSize = 9.sp, color = SellRed)
            Text("Mod", fontSize = 9.sp, color = WaitYellow)
            Text("Good", fontSize = 9.sp, color = Accent)
            Text("Strong", fontSize = 9.sp, color = BuyGreen)
            Text("Elite", fontSize = 9.sp, color = BuyGreen)
        }
    }
}

// ── Score bar row ─────────────────────────────────────────────
@Composable
fun ScoreBar(label: String, buy: Float, sell: Float, maxVal: Float) {
    val isBuy  = buy >= sell
    val val_   = if (isBuy) buy else sell
    val pct    = if (maxVal > 0) (val_ / maxVal).coerceIn(0f, 1f) else 0f
    val col    = if (isBuy && val_ > 0) BuyGreen else if (!isBuy && val_ > 0) SellRed else T3
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 9.sp, color = col, modifier = Modifier.width(52.dp),
            fontWeight = FontWeight.Medium)
        Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(999.dp)).background(S3)) {
            Box(Modifier.fillMaxWidth(pct).fillMaxHeight()
                .clip(RoundedCornerShape(999.dp)).background(col))
        }
        Text(String.format("%.1f", val_), fontSize = 9.sp, color = col,
            modifier = Modifier.width(28.dp), textAlign = TextAlign.End,
            fontFamily = FontFamily.Monospace)
    }
}

// ── Section card ──────────────────────────────────────────────
@Composable
fun FttCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(S2)
            .border(1.dp, Divider, RoundedCornerShape(14.dp))
            .padding(16.dp),
        content = content
    )
}

// ── Loading shimmer ───────────────────────────────────────────
@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    val anim = rememberInfiniteTransition(label = "shimmer")
    val alpha by anim.animateFloat(0.3f, 0.7f,
        infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a")
    Box(modifier.clip(RoundedCornerShape(8.dp)).background(S3.copy(alpha = alpha)))
}

// ── Toast-style snackbar ──────────────────────────────────────
@Composable
fun FttToast(message: String, onDismiss: () -> Unit) {
    LaunchedEffect(message) {
        kotlinx.coroutines.delay(2200)
        onDismiss()
    }
    Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center) {
        Box(
            Modifier.clip(RoundedCornerShape(999.dp))
                .background(S1)
                .border(1.dp, Divider, RoundedCornerShape(999.dp))
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Text(message, fontSize = 12.sp, color = T1, fontWeight = FontWeight.Medium)
        }
    }
}

// ── Live dot ─────────────────────────────────────────────────
@Composable
fun LiveDot(live: Boolean) {
    val anim   = rememberInfiniteTransition(label = "dot")
    val radius by anim.animateFloat(0f, 7f,
        infiniteRepeatable(tween(2000), RepeatMode.Restart), label = "r")
    val col = if (live) BuyGreen else WaitYellow
    Box(Modifier.size(14.dp), contentAlignment = Alignment.Center) {
        if (live) Canvas(Modifier.size(14.dp)) {
            drawCircle(col.copy(alpha = (1f - radius / 7f) * 0.5f), radius = radius.dp.toPx())
            drawCircle(col, radius = 3.5.dp.toPx())
        } else Canvas(Modifier.size(8.dp)) { drawCircle(col) }
    }
}
