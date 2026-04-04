package com.moneymanager.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val DarkColorScheme = darkColorScheme(
    primary         = Accent1,
    onPrimary       = Color.White,
    primaryContainer = Color(0xFF3A1A18),
    secondary       = AccentBlue,
    onSecondary     = Color.White,
    background      = BgPrimary,
    onBackground    = TextPrimary,
    surface         = BgCard,
    onSurface       = TextPrimary,
    surfaceVariant  = BgCardAlt,
    onSurfaceVariant = TextSecondary,
    error           = ExpenseRed,
    outline         = TextTertiary,
    outlineVariant  = Color(0xFF2A2A2E)
)

val AppTypography = androidx.compose.material3.Typography(
    displayLarge  = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp, color = TextPrimary),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, color = TextPrimary),
    titleLarge    = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = TextPrimary),
    titleMedium   = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = TextPrimary),
    bodyLarge     = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, color = TextPrimary),
    bodyMedium    = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, color = TextSecondary),
    bodySmall     = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, color = TextSecondary),
    labelSmall    = TextStyle(fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.2.sp, color = TextSecondary),
)

@Composable
fun MoneyManagerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = AppTypography,
        content     = content
    )
}
