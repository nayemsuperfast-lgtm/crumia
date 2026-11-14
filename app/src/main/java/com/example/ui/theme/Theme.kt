package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.model.ThemeMode

private val CrumiaDarkColorScheme = darkColorScheme(
    primary = CrumiaCyan,
    onPrimary = Color(0xFF062538),
    primaryContainer = Color(0xFF2B2D3B),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = CrumiaViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF332D47),
    onSecondaryContainer = Color(0xFFEDE9FE),
    tertiary = CrumiaPurple,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkOnSurfaceMuted,
    outline = DarkSurfaceBorder,
    error = DangerCrimson,
    onError = Color.White
)

private val CrumiaLightColorScheme = lightColorScheme(
    primary = ChromeWarmAccent,
    onPrimary = Color.White,
    primaryContainer = ChromeWarmPill,
    onPrimaryContainer = Color(0xFF241A00),
    secondary = CrumiaIndigo,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8EAFE),
    onSecondaryContainer = Color(0xFF1E1B4B),
    tertiary = CrumiaCyan,
    onTertiary = Color(0xFF082F49),
    background = ChromeWarmBackground,
    onBackground = LightOnSurface,
    surface = ChromeWarmSurface,
    onSurface = LightOnSurface,
    surfaceVariant = ChromeWarmPill,
    onSurfaceVariant = LightOnSurfaceMuted,
    outline = ChromeWarmOutline,
    error = DangerCrimson,
    onError = Color.White
)

private val CrumiaIncognitoColorScheme = darkColorScheme(
    primary = IncognitoAccent,
    onPrimary = Color(0xFF1E0B36),
    primaryContainer = IncognitoSurfaceElevated,
    onPrimaryContainer = Color(0xFFF3E8FF),
    secondary = CrumiaCyan,
    onSecondary = Color(0xFF041C2C),
    secondaryContainer = Color(0xFF2B2836),
    onSecondaryContainer = Color(0xFFE0E7FF),
    tertiary = CrumiaPurple,
    onTertiary = Color.White,
    background = IncognitoBackground,
    onBackground = DarkOnSurface,
    surface = IncognitoSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = IncognitoSurfaceElevated,
    onSurfaceVariant = Color(0xFFB4B0C2),
    outline = Color(0xFF3F3B4D),
    error = DangerCrimson,
    onError = Color.White
)

val CrumiaShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun CrumiaTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    isIncognito: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val colorScheme = when {
        isIncognito -> CrumiaIncognitoColorScheme
        useDark -> CrumiaDarkColorScheme
        else -> CrumiaLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CrumiaTypography,
        shapes = CrumiaShapes,
        content = content
    )
}
