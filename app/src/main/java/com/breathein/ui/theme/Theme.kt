package com.breathein.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Dark, calm and minimal: near-black surfaces, white as the only accent.
private val BreatheInColors = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF262626),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFBDBDBD),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF262626),
    onSecondaryContainer = Color.White,
    tertiary = Color(0xFFBDBDBD),
    background = Color(0xFF0A0A0A),
    onBackground = Color.White,
    surface = Color(0xFF0A0A0A),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFF9E9E9E),
    surfaceContainerLowest = Color(0xFF0A0A0A),
    surfaceContainerLow = Color(0xFF121212),
    surfaceContainer = Color(0xFF161616),
    surfaceContainerHigh = Color(0xFF1C1C1C),
    surfaceContainerHighest = Color(0xFF232323),
    outline = Color(0xFF3A3A3A),
    outlineVariant = Color(0xFF262626),
    error = Color(0xFFFF8A80),
)

private val BreatheInShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

// Lighter display and headline weights read calmer than the bold defaults.
private val BreatheInTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontWeight = FontWeight.Light),
        displayMedium = displayMedium.copy(fontWeight = FontWeight.Light),
        displaySmall = displaySmall.copy(fontWeight = FontWeight.Light),
        headlineLarge = headlineLarge.copy(fontWeight = FontWeight.Light),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Light),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.Light),
    )
}

/** The app is always dark; there is no light theme. */
@Composable
fun BreatheInTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BreatheInColors,
        shapes = BreatheInShapes,
        typography = BreatheInTypography,
        content = content,
    )
}
