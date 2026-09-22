package com.ozin.music.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.sin

/**
 * Purely decorative waveform chrome (not tied to real audio capture - that
 * remains the job of DJ Mode / Now Playing's visualizer). Draws [barCount]
 * vertical bars with a fixed, deterministic pseudo-random-looking height
 * pattern, optionally animated with a slow, low-amplitude pulse so it reads
 * as ambient studio texture rather than a static decoration.
 */
@Composable
fun WaveformMotif(
    modifier: Modifier = Modifier,
    barCount: Int = 40,
    color: Color = Color.White,
    animated: Boolean = true,
) {
    val phase = if (animated) {
        val transition = rememberInfiniteTransition(label = "waveform_motif")
        val animatedPhase by transition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 6000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "waveform_phase",
        )
        animatedPhase
    } else {
        0f
    }

    Canvas(modifier = modifier) {
        drawWaveformBars(barCount = barCount, color = color, phase = phase)
    }
}

private fun DrawScope.drawWaveformBars(barCount: Int, color: Color, phase: Float) {
    if (barCount <= 0) return
    val width = size.width
    val height = size.height
    val barWidth = width / (barCount * 1.6f)
    val gap = barWidth * 0.6f
    val minHeightFraction = 0.18f

    for (i in 0 until barCount) {
        // Deterministic pseudo-random pattern: a mix of two sine waves at
        // different frequencies, seeded by the bar index, so the shape looks
        // organic rather than a single uniform sine.
        val seed = i.toFloat()
        val base = (sin(seed * 0.9f) * 0.5f + 0.5f)
        val detail = (sin(seed * 2.7f + 1.3f) * 0.5f + 0.5f)
        var heightFraction = (base * 0.65f + detail * 0.35f).coerceIn(0f, 1f)

        if (phase != 0f) {
            // Gentle low-amplitude animated pulse layered on top of the
            // static pattern; amplitude is small so it stays subtle chrome.
            val pulse = sin(phase + seed * 0.5f) * 0.08f
            heightFraction = (heightFraction + pulse).coerceIn(minHeightFraction, 1f)
        } else {
            heightFraction = heightFraction.coerceAtLeast(minHeightFraction)
        }

        val barHeight = height * heightFraction
        val x = i * (barWidth + gap) + barWidth / 2
        if (x > width) break

        drawLine(
            color = color,
            start = Offset(x, height / 2 - barHeight / 2),
            end = Offset(x, height / 2 + barHeight / 2),
            strokeWidth = barWidth.coerceAtLeast(1f),
            cap = StrokeCap.Round,
        )
    }
}
