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
fun OfflineChallengeTheme(language: String = "ar", content: @Composable () -> Unit) {
    val layoutDirection = if (language == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = Typography,
            content = content
        )
    }
}
