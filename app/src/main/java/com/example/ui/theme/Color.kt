package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Studio ambient background matching the Beta Blue theme
val BgCanvas = Color(0xFFF8FAFC)
val BgCanvasGradientStart = Color(0xFFF8FAFC)
val BgCanvasGradientEnd = Color(0xFFEFF6FF)

// Ambient caustics & chromatic blooms (Beta Blue & Cyan luminous flares)
val CausticPurple = Color(0x353B82F6) // Converted from purple to vibrant blue bloom
val CausticCyan = Color(0x3038BDF8)
val CausticPeach = Color(0x2060A5FA)
val CausticBlue = Color(0x402563EB)

// Liquid Glass Surfaces
val CardGlass = Color(0xFFFFFFFF).copy(alpha = 0.85f)
val CardGlassBorder = Color.White.copy(alpha = 0.90f)
val CardGlassSoft = Color(0xFFFFFFFF).copy(alpha = 0.65f)
val GlassHighlightWhite = Color.White.copy(alpha = 0.92f)

// Primary Beta Blue (Original authentic beta blue theme)
val PrimaryBlue = Color(0xFF2563EB)
val PrimaryBlueEnd = Color(0xFF3B82F6)
val PrimaryBlueLight = Color(0xFFEFF6FF)
val PrimaryBlueGlow = Color(0x502563EB)
val PrimaryBlueInnerShadow = Color(0x351E3A8A)

// Seamless mapping so all screens and components switch from purple to beta blue
val PrimaryPurple = PrimaryBlue
val PrimaryPurpleEnd = PrimaryBlueEnd
val PrimaryPurpleLight = PrimaryBlueLight
val PrimaryPurpleGlow = PrimaryBlueGlow
val PrimaryPurpleInnerShadow = PrimaryBlueInnerShadow

// Secondary Cyan / Sky Liquid Glass
val SecondaryCyan = Color(0xFF0284C7)
val SecondaryCyanEnd = Color(0xFF38BDF8)
val SecondaryCyanGlow = Color(0x4D0284C7)
val SecondaryCyanInnerShadow = Color(0x380369A1)
val SecondaryCyanText = Color(0xFF0C4A6E)

// Frosted White / Silver Liquid Glass
val FrostedWhiteStart = Color(0xFFFFFFFF).copy(alpha = 0.94f)
val FrostedWhiteEnd = Color(0xFFF8FAFC).copy(alpha = 0.82f)
val FrostedWhiteGlow = Color(0x221E293B)
val FrostedWhiteInnerShadow = Color(0x20000000)

// Accent & Amber
val AccentBlue = Color(0xFF2563EB)
val AccentOrange = Color(0xFFF97316)
val AccentOrangeEnd = Color(0xFFFB923C)
val AccentOrangeGlow = Color(0x4DF97316)

// Text tokens
val TextPrimary = Color(0xFF0F172A)
val TextSecondary = Color(0xFF475569)
val TextMuted = Color(0xFF94A3B8)

// Status Badges
val BadgeGoodBg = Color(0x2510B981)
val BadgeGoodText = Color(0xFF065F46)
val BadgeLikeNewBg = Color(0x250284C7)
val BadgeLikeNewText = Color(0xFF0369A1)
val BadgeUsedBg = Color(0x25F59E0B)
val BadgeUsedText = Color(0xFF92400E)
val BadgeHeavilyUsedBg = Color(0x25EF4444)
val BadgeHeavilyUsedText = Color(0xFF991B1B)

// Neumorphic Shadows
val SoftShadowColor = Color(0x1F1E293B)
val DeepShadowColor = Color(0x260F172A)

// Specular Rim & Gradient Brushes (Liquid Glass Sapphire Rim)
val GlassRimBrush = Brush.linearGradient(
    listOf(
        Color.White.copy(alpha = 0.95f),
        Color.White.copy(alpha = 0.45f),
        Color(0x7593C5FD), // Ice Blue rim highlight
        Color(0x603B82F6)  // Azure specular highlight
    )
)

val BlueGradient = Brush.horizontalGradient(
    listOf(PrimaryBlue, PrimaryBlueEnd)
)

val BlueButtonBrush = Brush.linearGradient(
    listOf(PrimaryBlue, PrimaryBlueEnd)
)

val PurpleGradient = BlueGradient
val PurpleButtonBrush = BlueButtonBrush

val CyanButtonBrush = Brush.linearGradient(
    listOf(Color(0xFF38BDF8).copy(alpha = 0.9f), Color(0xFF0284C7).copy(alpha = 0.9f))
)

val FrostedWhiteBrush = Brush.verticalGradient(
    listOf(FrostedWhiteStart, FrostedWhiteEnd)
)
