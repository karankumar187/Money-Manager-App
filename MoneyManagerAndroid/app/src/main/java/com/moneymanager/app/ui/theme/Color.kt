package com.moneymanager.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── Exact match to iOS Theme.swift ────────────────────────────────────────
// Backgrounds
val BgPrimary   = Color(0xFF161618)   // near-black canvas
val BgCard      = Color(0xFF222226)   // card surface
val BgCardAlt   = Color(0xFF2C2C30)   // elevated card / input
val NavBg       = Color(0xFF1A1A1E)   // bottom nav bar

// Text
val TextPrimary   = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF8E8E93)
val TextTertiary  = Color(0xFF3A3A44)

// Accents
val Accent1    = Color(0xFFFF3B30)   // red — primary action
val Accent2    = Color(0xFFFF6961)   // light red highlight
val AccentBlue = Color(0xFF5AC8FA)

// Finance
val IncomeGreen = Color(0xFF30D158)
val ExpenseRed  = Color(0xFFFF453A)

// Category palette
val CatOrange = Color(0xFFFF9F0A)
val CatPurple = Color(0xFF7B61FF)
val CatBlue   = Color(0xFF5AC8FA)
val CatGreen  = Color(0xFF30D158)
val CatRed    = Color(0xFFFF453A)
val CatYellow = Color(0xFFFFD60A)
val CatPink   = Color(0xFFFF375F)
val CatTeal   = Color(0xFF5AC8FA)
val CatGray   = Color(0xFF636366)

// Gradients (represented as start/end pairs)
val AccentGradientStart = Color(0xFFFF3B30)
val AccentGradientEnd   = Color(0xFFFF6B35)
val IncomeGradientStart = Color(0xFF30D158)
val IncomeGradientEnd   = Color(0xFF34C759)
val ExpenseGradientStart = Color(0xFFFF453A)
val ExpenseGradientEnd   = Color(0xFFFF3B30)

// Avatar palette (same order as iOS)
val AvatarPalette = listOf(CatPurple, AccentBlue, CatOrange, IncomeGreen, CatPink, CatYellow)

fun avatarColor(name: String): Color = AvatarPalette[Math.abs(name.hashCode()) % AvatarPalette.size]

fun hexColor(hex: String): Color {
    val clean = hex.trimStart('#')
    val value = clean.toLongOrNull(16) ?: 0xFF888888L
    return Color((0xFF000000L or value).toInt())
}
