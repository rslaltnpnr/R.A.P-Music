package com.ozin.music.feature.player

import android.media.audiofx.Visualizer
import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.ozin.music.R
import com.ozin.music.core.domain.VisualizerMath
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
import com.ozin.music.core.player.AudioSessionIdBridge
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
 * Bar visualization driven by a real `android.media.audiofx.Visualizer`
 * attached to the currently playing ExoPlayer's audio session id (published
 * by [com.ozin.music.core.player.PlaybackService] via
 * [AudioSessionIdBridge], the same attach-on-session-id-change pattern
 * `EffectsChain` already uses for the other audiofx classes). It captures
 * real FFT frames with [Visualizer.setDataCaptureListener] and turns them
 * into per-bar magnitudes - this is genuine audio analysis, not a canned
 * animation.
 *
 * `Visualizer` is not guaranteed to be available: some OEMs restrict it, it
 * can require a capture-audio-adjacent permission on some API levels, and
 * `Visualizer(sessionId)`'s constructor or `setEnabled(true)` can throw at
 * runtime on real devices in ways that can't be fully predicted here. If
 * attachment or capture fails for any reason, this falls back to the
 * original deterministic, harmless [VisualizerMath] animation (documented
 * there as NOT real audio data) rather than showing a blank or crashing
 * screen - the fallback path is only ever used when real capture could not
 * be established, never presented as if it were real.
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
    val audioSessionId by AudioSessionIdBridge.audioSessionId.collectAsState()
    val fftBars = remember { mutableStateListOf(*FloatArray(barCount) { 0f }.toTypedArray()) }
    var realCaptureActive by remember { mutableStateOf(false) }

    DisposableEffect(audioSessionId) {
        var visualizer: Visualizer? = null
        if (audioSessionId != 0) {
            try {
                visualizer = Visualizer(audioSessionId).apply {
                    val captureSize = Visualizer.getCaptureSizeRange()[1]
                    setCaptureSize(captureSize)
                    setDataCaptureListener(
                        object : Visualizer.OnDataCaptureListener {
                            override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, samplingRate: Int) = Unit

                            override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                                if (fft == null || fft.size < 4) return
                                // Android's FFT packing: fft[0] = DC real, fft[1] =
                                // Nyquist real, then (re, im) pairs for bins
                                // 1..n/2-1. Compute a magnitude per bin, then fold
                                // those magnitudes into `barCount` buckets by
                                // averaging, and normalize with a fixed ceiling
                                // (typical speech/music FFT magnitudes from this API
                                // rarely exceed a few hundred) so bars stay in 0..1.
                                val binCount = fft.size / 2
                                val magnitudes = FloatArray(binCount)
                                magnitudes[0] = kotlin.math.abs(fft[0].toInt()).toFloat()
                                for (bin in 1 until binCount) {
                                    val re = fft[2 * bin].toFloat()
                                    val im = if (2 * bin + 1 < fft.size) fft[2 * bin + 1].toFloat() else 0f
                                    magnitudes[bin] = kotlin.math.sqrt(re * re + im * im)
                                }
                                val bucketSize = (binCount / barCount).coerceAtLeast(1)
                                for (i in 0 until barCount) {
                                    val from = i * bucketSize
                                    val to = ((i + 1) * bucketSize).coerceAtMost(binCount)
                                    if (from >= to) continue
                                    var sum = 0f
                                    for (b in from until to) sum += magnitudes[b]
                                    val avg = sum / (to - from)
                                    fftBars[i] = (avg / 180f).coerceIn(0.05f, 1f)
                                }
                            }
                        },
                        Visualizer.getMaxCaptureRate() / 2,
                        false,
                        true,
                    )
                    enabled = true
                }
                realCaptureActive = true
            } catch (_: Exception) {
                // RuntimeException (native init failure), SecurityException
                // (capture not permitted on this OEM/build) or
                // UnsupportedOperationException - fall back below.
                realCaptureActive = false
                visualizer?.release()
                visualizer = null
            }
        } else {
            realCaptureActive = false
        }
        onDispose {
            try {
                visualizer?.enabled = false
                visualizer?.release()
            } catch (_: Exception) {
                // Already released/invalid - nothing to clean up.
            }
        }
    }

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
            val heightFraction = if (!isPlaying) {
                // Frozen, low baseline when paused rather than a moving animation.
                0.18f
            } else if (realCaptureActive) {
                fftBars[i]
            } else {
                // Fallback path: real Visualizer capture is unavailable on this
                // device/build, so this shows the deterministic, clearly
                // documented non-audio animation instead of a blank canvas.
                VisualizerMath.barHeight(songId, timeSeconds, i, barCount)
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

/** Art-forward mode: the album art fills most of the screen width, crossfading
 * between songs (item 6). Minimal chrome is added by the caller in
 * [NowPlayingScreen] (title/artist/controls only, no extra visual gimmick). */
@Composable
fun FullArtVisualMode(
    albumArtKey: Any?,
    albumArtUri: android.net.Uri?,
    modifier: Modifier = Modifier,
) {
    Crossfade(
        targetState = albumArtKey to albumArtUri,
        animationSpec = tween(durationMillis = 400),
        label = "fullArtCrossfade",
    ) { (_, uri) ->
        Image(
            painter = rememberAsyncImagePainter(uri),
            contentDescription = stringResource(R.string.player_album_art),
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(20.dp)),
            contentScale = ContentScale.Crop,
        )
    }
}

/** Minimal mode: a smaller, simple square art tile — the caller keeps the
 * rest of the screen down to title/artist/core transport controls only. */
@Composable
fun MinimalVisualMode(
    albumArtUri: android.net.Uri?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp)),
    ) {
        Image(
            painter = rememberAsyncImagePainter(albumArtUri),
            contentDescription = stringResource(R.string.player_album_art),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}

/**
 * Full-screen blurred/darkened album-art backdrop behind [content]. Uses
 * Compose's `Modifier.blur()` (API 31+); below API 31 it falls back to a
 * darker overlay over the unblurred image rather than pulling in a
 * RenderScript dependency, per this feature's constraints.
 */
@Composable
fun BlurArtBackground(
    albumArtUri: android.net.Uri?,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        val painter = rememberAsyncImagePainter(albumArtUri)
        val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .let { if (canBlur) it.blur(48.dp) else it },
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (canBlur) 0.35f else 0.68f)),
        )
        content()
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
    NowPlayingVisualMode.FULL_ART -> stringResource(R.string.player_mode_full_art)
    NowPlayingVisualMode.BLUR -> stringResource(R.string.player_mode_blur)
    NowPlayingVisualMode.MINIMAL -> stringResource(R.string.player_mode_minimal)
}
