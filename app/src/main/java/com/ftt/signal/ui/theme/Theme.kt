package com.ftt.signal.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Brand Palette ─────────────────────────────────────────────
val Bg          = Color(0xFF000000)
val S1          = Color(0xFF242529)
val S2          = Color(0xFF10121C)
val S3          = Color(0xFF161824)
val S4          = Color(0xFF1E2130)

val BuyGreen    = Color(0xFF22D472)
val BuyGreenDim = Color(0x1422D472)
val BuyGreenMid = Color(0x2622D472)
val SellRed     = Color(0xFFFF5F6B)
val SellRedDim  = Color(0x14FF5F6B)
val SellRedMid  = Color(0x26FF5F6B)
val WaitYellow  = Color(0xFFFFB930)
val WaitYellowDim = Color(0x12FFB930)

val Accent      = Color(0xFF4F9EFF)
val AccentDim   = Color(0x0F4F9EFF)
val Brand1      = Color(0xFF4776E6)
val Brand2      = Color(0xFF8E54E9)

val T1          = Color(0xFFFFFFFF)
val T2          = Color(0xFFB0BAC9)
val T3          = Color(0xFF8892A4)
val TMuted      = Color(0xFF4A5568)
val Divider     = Color(0xFF2D3448)

val GradeA      = Color(0xFF22D472)
val GradeB      = Color(0xFF4F9EFF)
val GradeC      = Color(0xFFFFB020)
val GradeD      = Color(0xFFFF6B35)
val GradeF      = Color(0xFFFF5F6B)

fun signalColor(label: String) = when (label.uppercase()) {
    "BUY"  -> BuyGreen
    "SELL" -> SellRed
    "WAIT" -> WaitYellow
    else   -> T3
}

fun signalBg(label: String) = when (label.uppercase()) {
    "BUY"  -> BuyGreenDim
    "SELL" -> SellRedDim
    "WAIT" -> WaitYellowDim
    else   -> S3
}

fun gradeColor(g: String) = when (g.uppercase()) {
    "A+", "A" -> GradeA
    "B"       -> GradeB
    "C"       -> GradeC
    "D"       -> GradeD
    else      -> GradeF
}

fun confColor(c: Int) = when {
    c >= 70 -> BuyGreen
    c >= 50 -> Accent
    c >= 35 -> WaitYellow
    else    -> SellRed
}

private val DarkScheme = darkColorScheme(
    primary          = Brand1,
    onPrimary        = T1,
    secondary        = BuyGreen,
    background       = Bg,
    onBackground     = T1,
    surface          = S2,
    onSurface        = T1,
    surfaceVariant   = S3,
    onSurfaceVariant = T2,
    outline          = Divider,
    error            = SellRed,
)

val FttTypography = Typography(
    headlineLarge  = TextStyle(fontWeight = FontWeight.Bold,     fontSize = 26.sp, color = T1),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = T1),
    titleLarge     = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = T1),
    titleMedium    = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 14.sp, color = T1),
    bodyLarge      = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 14.sp, color = T1),
    bodyMedium     = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 13.sp, color = T2),
    bodySmall      = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 11.sp, color = T3),
    labelLarge     = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 12.sp, color = T3,
        letterSpacing = 0.5.sp),
    labelSmall     = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 10.sp, color = TMuted,
        fontFamily = FontFamily.Monospace),
)

@Composable
fun FttTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkScheme, typography = FttTypography, content = content)
}
