package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Crumia Official Brand Gradient Palette (Blue -> Cyan -> Indigo -> Violet -> Purple)
val CrumiaCyan = Color(0xFF38BDF8)
val CrumiaBlue = Color(0xFF3B82F6)
val CrumiaIndigo = Color(0xFF6366F1)
val CrumiaViolet = Color(0xFF8B5CF6)
val CrumiaPurple = Color(0xFFD946EF)

// Chrome-Inspired Material You Warm Light Palette
val ChromeWarmBackground = Color(0xFFF3ECE0)
val ChromeWarmSurface = Color(0xFFFFFFFF)
val ChromeWarmPill = Color(0xFFE5DCCB)
val ChromeWarmAccent = Color(0xFF725B00)
val ChromeWarmOutline = Color(0xFFD8CEBC)

// Chrome-Inspired Dark Palette
val DarkBackground = Color(0xFF1A1A1E)
val DarkSurface = Color(0xFF242429)
val DarkSurfaceElevated = Color(0xFF2E2E35)
val DarkSurfaceBorder = Color(0xFF3D3D47)
val DarkOnSurface = Color(0xFFF1F3F4)
val DarkOnSurfaceMuted = Color(0xFF9AA0A6)

// Light Text
val LightOnSurface = Color(0xFF1F1F1F)
val LightOnSurfaceMuted = Color(0xFF5F6368)

// Incognito Palette
val IncognitoBackground = Color(0xFF16151A)
val IncognitoSurface = Color(0xFF24222B)
val IncognitoSurfaceElevated = Color(0xFF302D3A)
val IncognitoAccent = Color(0xFFC084FC)

// Semantic Accents
val ShieldEmerald = Color(0xFF10B981)
val SecurityAmber = Color(0xFFF59E0B)
val DangerCrimson = Color(0xFFEF4444)

val CrumiaBrandBrush = Brush.linearGradient(
    colors = listOf(CrumiaBlue, CrumiaCyan, CrumiaIndigo, CrumiaViolet, CrumiaPurple)
)

val CrumiaSubtleBrush = Brush.linearGradient(
    colors = listOf(
        CrumiaCyan.copy(alpha = 0.14f),
        CrumiaViolet.copy(alpha = 0.14f)
    )
)
