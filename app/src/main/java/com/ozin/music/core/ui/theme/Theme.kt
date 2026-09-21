package com.ozin.music.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val OzinBackground = Color(0xFF0B0F19)
val OzinCard = Color(0xFF131B2E)
val OzinSurface = Color(0xFF192238)
val OzinTextPrimary = Color(0xFFFFFFFF)
val OzinTextSecondary = Color(0xFFB7C0D4)
val OzinAccentDefault = Color(0xFF7C4DFF)

// Light counterparts of the default dark palette, used when the user picks
// light or system-light mode with ThemePreset.DEFAULT_DARK.
val OzinLightBackground = Color(0xFFFAFAFE)
val OzinLightCard = Color(0xFFFFFFFF)
val OzinLightSurface = Color(0xFFF0F1F7)
val OzinLightTextPrimary = Color(0xFF1B1B1F)
val OzinLightTextSecondary = Color(0xFF49454F)

/** Dark/light mode preference, independent of [ThemePreset]. [SYSTEM] follows
 * [androidx.compose.foundation.isSystemInDarkTheme] at the call site. */
enum class ThemeMode { DARK, LIGHT, SYSTEM }

/** Selectable overall visual style for the app, beyond the single original
 * dark theme. [ThemePreset.DEFAULT_DARK] is the original Phase 1-8 theme and
 * remains the default. */
enum class ThemePreset {
    DEFAULT_DARK,
    AMOLED,
    NEON,
    RETRO,
    MINIMAL,
}

/** A small, fixed palette of user-selectable accent colors (a swatch grid in
 * Settings), threaded into whichever [ThemePreset] is active as its
 * primary/accent color. */
enum class AccentColorOption(val color: Color) {
    PURPLE(Color(0xFF7C4DFF)),
    BLUE(Color(0xFF2F80FF)),
    TEAL(Color(0xFF1DE9B6)),
    GREEN(Color(0xFF4CD964)),
    YELLOW(Color(0xFFFFD54F)),
    ORANGE(Color(0xFFFF8A50)),
    RED(Color(0xFFFF5252)),
    PINK(Color(0xFFFF4FA3)),
}

/**
 * Pure mapping from a [ThemePreset] + accent [Color] to a Material3
 * [ColorScheme]. Kept free of Compose runtime state so it is directly unit
 * testable.
 *
 * [darkMode] only affects [ThemePreset.DEFAULT_DARK]: AMOLED/NEON/RETRO/MINIMAL
 * are inherently dark-styled presets (true-black, neon-on-black, sepia,
 * near-black minimal) and stay dark regardless of the light/dark setting,
 * exactly as they always have.
 */
fun colorSchemeFor(preset: ThemePreset, accent: Color, darkMode: Boolean = true): ColorScheme = when (preset) {
    ThemePreset.DEFAULT_DARK -> if (darkMode) {
        darkColorScheme(
            primary = accent,
            secondary = accent,
            background = OzinBackground,
            surface = OzinSurface,
            surfaceVariant = OzinCard,
            onBackground = OzinTextPrimary,
            onSurface = OzinTextPrimary,
            onPrimary = OzinTextPrimary,
        )
    } else {
        lightColorScheme(
            primary = accent,
            secondary = accent,
            background = OzinLightBackground,
            surface = OzinLightSurface,
            surfaceVariant = OzinLightCard,
            onBackground = OzinLightTextPrimary,
            onSurface = OzinLightTextPrimary,
            onPrimary = Color.White,
        )
    }
    ThemePreset.AMOLED -> darkColorScheme(
        primary = accent,
        secondary = accent,
        background = Color(0xFF000000),
        surface = Color(0xFF000000),
        surfaceVariant = Color(0xFF0A0A0A),
        onBackground = OzinTextPrimary,
        onSurface = OzinTextPrimary,
        onPrimary = Color(0xFF000000),
    )
    ThemePreset.NEON -> darkColorScheme(
        primary = accent,
        secondary = accent,
        background = Color(0xFF05060F),
        surface = Color(0xFF10112A),
        surfaceVariant = Color(0xFF191B44),
        onBackground = Color(0xFFF5F5FF),
        onSurface = Color(0xFFF5F5FF),
        onPrimary = Color(0xFF05060F),
    )
    ThemePreset.RETRO -> darkColorScheme(
        primary = accent,
        secondary = accent,
        background = Color(0xFF2B1E17),
        surface = Color(0xFF3A281E),
        surfaceVariant = Color(0xFF4A3527),
        onBackground = Color(0xFFF3E5D3),
        onSurface = Color(0xFFF3E5D3),
        onPrimary = Color(0xFF2B1E17),
    )
    ThemePreset.MINIMAL -> darkColorScheme(
        primary = accent,
        secondary = accent,
        background = Color(0xFF16161A),
        surface = Color(0xFF1C1C21),
        surfaceVariant = Color(0xFF222227),
        onBackground = Color(0xFFCFCFD6),
        onSurface = Color(0xFFCFCFD6),
        onPrimary = Color(0xFF16161A),
    )
}

/**
 * OZIN Music is dark-first by design (per spec). [accent] is derived at
 * runtime from album art (Palette API) on the Now Playing screen and falls
 * back to [OzinAccentDefault] everywhere else. [preset] selects the overall
 * palette (Settings > Personalization), defaulting to the original theme.
 */
@Composable
fun OzinMusicTheme(
    preset: ThemePreset = ThemePreset.DEFAULT_DARK,
    accent: Color = OzinAccentDefault,
    darkMode: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = colorSchemeFor(preset, accent, darkMode),
        typography = MaterialTheme.typography,
        content = content,
    )
}
