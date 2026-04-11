package com.ftt.signal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ftt.signal.ui.theme.*

// ── Section card ──────────────────────────────────────────────
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content:  @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(16.dp),
        content = content,
    )
}

// ── Label + value row ─────────────────────────────────────────
@Composable
fun InfoRow(
    label:      String,
    value:      String,
    valueColor: Color = TextPrimary,
    modifier:   Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(text = value, style = MaterialTheme.typography.bodyLarge,  color = valueColor,
            fontWeight = FontWeight.SemiBold)
    }
}

// ── Signal direction badge ────────────────────────────────────
@Composable
fun DirectionBadge(direction: String?, modifier: Modifier = Modifier) {
    val color = signalColor(direction)
    val bg    = signalBgColor(direction)
    val label = when (direction?.uppercase()) {
        "BUY"      -> "▲ BUY"
        "SELL"     -> "▼ SELL"
        "NO_TRADE" -> "— WAIT"
        else       -> "—"
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp,
            letterSpacing = 0.5.sp)
    }
}

// ── Grade badge ───────────────────────────────────────────────
@Composable
fun GradeBadge(grade: String?, modifier: Modifier = Modifier) {
    if (grade == null) return
    val color = gradeColor(grade)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = grade, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

// ── Confidence bar ────────────────────────────────────────────
@Composable
fun ConfidenceBar(
    confidenceStr: String?,
    direction:     String?,
    modifier:      Modifier = Modifier,
) {
    val pct = confidenceStr?.replace("%", "")?.trim()?.toFloatOrNull()?.div(100f) ?: 0f
    val color = signalColor(direction)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Confidence", style = MaterialTheme.typography.labelLarge)
            Text(
                confidenceStr ?: "—",
                style = MaterialTheme.typography.bodyLarge,
                color = color,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(SurfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(pct.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}

// ── AI result chip ────────────────────────────────────────────
@Composable
fun AiChip(label: String, signal: String?, confidence: Int?, modifier: Modifier = Modifier) {
    val color = signalColor(signal)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceVariant)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        Text(
            signal ?: "—",
            color      = color,
            fontWeight = FontWeight.Bold,
            fontSize   = 12.sp,
        )
        if (confidence != null) {
            Text(
                "$confidence%",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
    }
}

// ── Section header ────────────────────────────────────────────
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text     = title.uppercase(),
        style    = MaterialTheme.typography.labelLarge,
        color    = TextMuted,
        modifier = modifier.padding(bottom = 8.dp),
        letterSpacing = 1.2.sp,
    )
}

// ── Divider ───────────────────────────────────────────────────
@Composable
fun FttDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier  = modifier.padding(vertical = 8.dp),
        color     = Divider,
        thickness = 0.5.dp,
    )
}

// ── Win/Loss chip ─────────────────────────────────────────────
@Composable
fun ResultChip(result: String?, modifier: Modifier = Modifier) {
    val color = winLossColor(result)
    val label = when (result?.uppercase()) {
        "WIN"  -> "WIN"
        "LOSS" -> "LOSS"
        else   -> "PENDING"
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = color, fontWeight = FontWeight.Bold, fontSize = 10.sp,
            letterSpacing = 0.5.sp)
    }
}

// ── Loading indicator ─────────────────────────────────────────
@Composable
fun FttLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Accent, strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
    }
}

// ── Error state ───────────────────────────────────────────────
@Composable
fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier              = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.spacedBy(12.dp),
    ) {
        Text("⚠", fontSize = 32.sp)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Button(
            onClick   = onRetry,
            colors    = ButtonDefaults.buttonColors(containerColor = Accent),
            shape     = RoundedCornerShape(8.dp),
        ) {
            Text("Retry", color = TextPrimary)
        }
    }
}
