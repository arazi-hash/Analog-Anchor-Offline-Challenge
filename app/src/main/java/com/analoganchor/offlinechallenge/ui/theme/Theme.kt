package com.analoganchor.offlinechallenge.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val DarkColorScheme = darkColorScheme(
    background = Obsidian,
    surface = DeepSurface,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    primary = CyanGlow,
    secondary = CyanGlowVariant,
    error = AmberWarning,
    onPrimary = Obsidian
)

@Composable
fun OfflineChallengeTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = Typography,
            content = content
        )
    }
}
