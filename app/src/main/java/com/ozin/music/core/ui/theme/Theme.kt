package com.ozin.music.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val OzinBackground = Color(0xFF0B0F19)
val OzinCard = Color(0xFF131B2E)
val OzinSurface = Color(0xFF192238)
val OzinTextPrimary = Color(0xFFFFFFFF)
val OzinTextSecondary = Color(0xFFB7C0D4)
val OzinAccentDefault = Color(0xFF7C4DFF)

/**
 * OZIN Music is dark-first by design (per spec). [accent] is derived at
 * runtime from album art (Palette API) on the Now Playing screen and falls
 * back to [OzinAccentDefault] everywhere else.
 */
@Composable
fun OzinMusicTheme(
    accent: Color = OzinAccentDefault,
    content: @Composable () -> Unit,
) {
    val colorScheme = darkColorScheme(
        primary = accent,
        secondary = accent,
        background = OzinBackground,
        surface = OzinSurface,
        surfaceVariant = OzinCard,
        onBackground = OzinTextPrimary,
        onSurface = OzinTextPrimary,
        onPrimary = OzinTextPrimary,
    )
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}
