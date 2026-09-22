package com.ozin.music.core.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * "Dark Studio / Neon Accent" visual language: gradient brushes and a soft
 * glow backdrop built from an [AccentColorOption], used across the mini
 * player, bottom nav, brand header and Home hero to give the app a distinct,
 * branded look instead of Material3's flat defaults.
 */

/** A two-stop linear gradient from [accent] at full strength down to a
 * darker, near-black variant of the same hue - used for glow washes and
 * highlighted surfaces. */
fun neonGradientFor(accent: Color): Brush {
    val deep = lerp(accent, Color.Black, 0.65f)
    return Brush.linearGradient(
        colors = listOf(accent, deep),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
    )
}

/** A radial "bleed" of [accent] used as a low-alpha ambient background wash,
 * fading into transparency so it can sit above a solid background color. */
fun neonAmbientWash(accent: Color, maxAlpha: Float = 0.28f): Brush =
    Brush.radialGradient(
        colors = listOf(
            accent.copy(alpha = maxAlpha),
            accent.copy(alpha = maxAlpha * 0.35f),
            Color.Transparent,
        ),
        radius = 1200f,
    )

/**
 * A small glowing halo composable, meant to sit *behind* an icon or button
 * (place it as the first/bottom child of a [Box], with the real content on
 * top) to give it a neon accent glow. On API 31+ this uses a real blurred
 * colored layer via [androidx.compose.ui.draw.blur]; below that, where blur
 * has no visible effect on some OEM skins, it falls back to a plain
 * low-alpha colored halo so the accent still reads as "glowing".
 */
@Composable
fun NeonGlowBackdrop(
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier,
    alpha: Float = 0.55f,
) {
    if (Build.VERSION.SDK_INT >= 31) {
        Box(
            modifier = modifier
                .size(size)
                .blur(size / 3)
                .clip(CircleShape)
                .background(color.copy(alpha = alpha)),
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(color.copy(alpha = alpha * 0.5f)),
        )
    }
}
