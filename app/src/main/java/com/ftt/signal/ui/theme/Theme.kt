package com.ftt.signal.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Brand Colors ──────────────────────────────────────────────
val Background     = Color(0xFF0D0F14)
val Surface        = Color(0xFF161A23)
val SurfaceVariant = Color(0xFF1E2330)
val CardBg         = Color(0xFF1A1F2E)

val BuyGreen       = Color(0xFF00D4A3)
val BuyGreenDim    = Color(0xFF00D4A320)
val SellRed        = Color(0xFFFF4757)
val SellRedDim     = Color(0xFFFF475720)
val NoTradeGray    = Color(0xFF8892A4)

val Accent         = Color(0xFF6C63FF)
val AccentDim      = Color(0xFF6C63FF20)
val Gold           = Color(0xFFFFD700)
val GoldDim        = Color(0xFFFFD70020)

val TextPrimary    = Color(0xFFECEFF4)
val TextSecondary  = Color(0xFF8892A4)
val TextMuted      = Color(0xFF4A5568)
val Divider        = Color(0xFF2D3448)

val GradeA         = Color(0xFF00D4A3)
val GradeB         = Color(0xFF3D9BE9)
val GradeC         = Color(0xFFFFB020)
val GradeD         = Color(0xFFFF6B35)
val GradeF         = Color(0xFFFF4757)

// ── Dark Color Scheme ─────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary          = Accent,
    onPrimary        = TextPrimary,
    primaryContainer = AccentDim,
    secondary        = BuyGreen,
    onSecondary      = Background,
    background       = Background,
    onBackground     = TextPrimary,
    surface          = Surface,
    onSurface        = TextPrimary,
    surfaceVariant   = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline          = Divider,
    error            = SellRed,
    onError          = TextPrimary,
)

// ── Typography ────────────────────────────────────────────────
val FttsTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize   = 28.sp,
        color      = TextPrimary,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 22.sp,
        color      = TextPrimary,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 18.sp,
        color      = TextPrimary,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize   = 15.sp,
        color      = TextPrimary,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize   = 14.sp,
        color      = TextPrimary,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize   = 13.sp,
        color      = TextSecondary,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize   = 11.sp,
        color      = TextMuted,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize   = 12.sp,
        letterSpacing = 0.8.sp,
        color      = TextSecondary,
    ),
)

// ── App Theme ─────────────────────────────────────────────────
@Composable
fun FttsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = FttsTypography,
        content     = content,
    )
}

// ── Signal color helpers ──────────────────────────────────────
fun signalColor(direction: String?) = when (direction?.uppercase()) {
    "BUY"      -> BuyGreen
    "SELL"     -> SellRed
    "NO_TRADE" -> NoTradeGray
    else       -> NoTradeGray
}

fun signalBgColor(direction: String?) = when (direction?.uppercase()) {
    "BUY"      -> BuyGreenDim
    "SELL"     -> SellRedDim
    "NO_TRADE" -> Color(0xFF8892A415)
    else       -> Color(0xFF8892A415)
}

fun gradeColor(grade: String?) = when (grade?.uppercase()) {
    "A+", "A" -> GradeA
    "B"       -> GradeB
    "C"       -> GradeC
    "D"       -> GradeD
    else      -> GradeF
}

fun winLossColor(result: String?) = when (result?.uppercase()) {
    "WIN"  -> BuyGreen
    "LOSS" -> SellRed
    else   -> NoTradeGray
}
