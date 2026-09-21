package com.ozin.music.feature.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.ozin.music.R
import com.ozin.music.core.domain.VisualizerMath
import androidx.compose.runtime.withFrameNanos
import com.ozin.music.core.settings.NowPlayingVisualMode

/**
 * Continuously advancing rotation angle (degrees), tied to actual playback:
 * it only advances while [isPlaying] is true and resets to a stopped state
 * otherwise, rather than spinning regardless of playback state.
 */
@Composable
private fun rememberPlaybackRotation(isPlaying: Boolean, degreesPerSecond: Float): Float {
    var angle by remember { mutableStateOf(0f) }
    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        var lastFrameNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                if (lastFrameNanos != 0L) {
                    val deltaSeconds = (frameNanos - lastFrameNanos) / 1_000_000_000f
                    angle = (angle + deltaSeconds * degreesPerSecond) % 360f
                }
                lastFrameNanos = frameNanos
            }
        }
    }
    return angle
}

/**
 * Deterministic animated bar visualization. This does NOT analyze real audio
 * frequency data - see [VisualizerMath] - it is a visual animation seeded by
 * the current song id and driven by actual playback position, so it moves
 * only while the song is actually playing/advancing.
 */
@Composable
fun VisualizerVisualMode(
    songId: Long,
    positionMs: Long,
    isPlaying: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val barCount = 24
    val timeSeconds = positionMs / 1000f
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(24.dp),
    ) {
        val barWidth = size.width / (barCount * 1.6f)
        val gap = barWidth * 0.6f
        val totalWidth = barCount * (barWidth + gap)
        val startX = (size.width - totalWidth) / 2f
        for (i in 0 until barCount) {
            val heightFraction = if (isPlaying) {
                VisualizerMath.barHeight(songId, timeSeconds, i, barCount)
            } else {
                // Frozen, low baseline when paused rather than a moving animation.
                0.18f
            }
            val barHeight = size.height * heightFraction
            val x = startX + i * (barWidth + gap)
            drawLine(
                color = accentColor,
                start = Offset(x, size.height - barHeight),
                end = Offset(x, size.height),
                strokeWidth = barWidth,
            )
        }
    }
}

/** Rotating vinyl record showing the album art in a circular clip, spinning
 * continuously while playing and paused (not resetting) when playback is
 * paused. */
@Composable
fun VinylVisualMode(
    albumArtUri: android.net.Uri?,
    isPlaying: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val angle = rememberPlaybackRotation(isPlaying, degreesPerSecond = 36f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .rotate(angle)
                .clip(CircleShape)
                .background(Color(0xFF111111)),
        ) {
            Image(
                painter = rememberAsyncImagePainter(albumArtUri),
                contentDescription = stringResource(R.string.player_album_art),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp)
                    .clip(CircleShape),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize(0.16f)
                    .clip(CircleShape)
                    .background(accentColor),
            )
        }
    }
}

/** Retro cassette-tape visual mode: a cassette-shaped card with two reels
 * that rotate while playing. */
@Composable
fun CassetteVisualMode(
    isPlaying: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val angle = rememberPlaybackRotation(isPlaying, degreesPerSecond = 90f)
    val cassetteBody = Color(0xFF3A2A20)
    val cassetteWindow = Color(0xFFD8C6A8)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.5f)
            .padding(24.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(cassetteBody),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(0.8f)
                .clip(RoundedCornerShape(12.dp))
                .background(cassetteWindow),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val reelRadius = size.minDimension / 6f
                val leftCenter = Offset(size.width * 0.3f, size.height * 0.5f)
                val rightCenter = Offset(size.width * 0.7f, size.height * 0.5f)
                for (center in listOf(leftCenter, rightCenter)) {
                    drawCircle(color = Color(0xFF20140E), radius = reelRadius, center = center)
                    drawCircle(
                        color = accentColor,
                        radius = reelRadius,
                        center = center,
                        style = Stroke(width = 3.dp.toPx()),
                    )
                    val spokeRadius = reelRadius * 0.7f
                    val angleRad = Math.toRadians(angle.toDouble())
                    for (spoke in 0 until 4) {
                        val spokeAngle = angleRad + spoke * (Math.PI / 2)
                        val end = Offset(
                            x = center.x + (spokeRadius * kotlin.math.cos(spokeAngle)).toFloat(),
                            y = center.y + (spokeRadius * kotlin.math.sin(spokeAngle)).toFloat(),
                        )
                        drawLine(color = Color(0xFF20140E), start = center, end = end, strokeWidth = 3.dp.toPx())
                    }
                }
                drawLine(
                    color = Color(0xFF20140E),
                    start = leftCenter,
                    end = rightCenter,
                    strokeWidth = 2.dp.toPx(),
                )
            }
        }
    }
}

/** A row of text labels to switch between Now Playing visual modes; the
 * currently active mode is highlighted with the given accent. */
@Composable
fun NowPlayingModeSwitcher(
    current: NowPlayingVisualMode,
    accentColor: Color,
    onSelect: (NowPlayingVisualMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        NowPlayingVisualMode.entries.forEach { mode ->
            val selected = mode == current
            Text(
                text = modeLabel(mode),
                color = if (selected) accentColor else Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .padding(8.dp)
                    .clickable { onSelect(mode) },
            )
        }
    }
}

@Composable
private fun modeLabel(mode: NowPlayingVisualMode): String = when (mode) {
    NowPlayingVisualMode.DEFAULT -> stringResource(R.string.player_mode_default)
    NowPlayingVisualMode.VINYL -> stringResource(R.string.player_mode_vinyl)
    NowPlayingVisualMode.CASSETTE -> stringResource(R.string.player_mode_cassette)
    NowPlayingVisualMode.VISUALIZER -> stringResource(R.string.player_mode_visualizer)
}
