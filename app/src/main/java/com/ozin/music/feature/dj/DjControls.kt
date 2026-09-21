package com.ozin.music.feature.dj

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * A Canvas-drawn rotary knob, dragged vertically (a standard mixer-knob
 * interaction: drag up increases, drag down decreases). [value] is 0..1;
 * [onChange] fires continuously during drag.
 */
@Composable
fun RotaryKnob(
    value: Float,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    accent: Color = DjColors.deckA,
) {
    var internalValue by remember(value) { mutableFloatStateOf(value) }
    Canvas(
        modifier = modifier
            .size(size)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val delta = -dragAmount.y / 300f
                    internalValue = (internalValue + delta).coerceIn(0f, 1f)
                    onChange(internalValue)
                }
            },
    ) {
        val radius = min(this.size.width, this.size.height) / 2f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        drawCircle(color = DjColors.knobTrack, radius = radius, center = center)
        // 270-degree sweep starting at 135deg (bottom-left) like a real mixer knob.
        val startAngleDeg = 135f
        val sweepDeg = 270f
        val angleDeg = startAngleDeg + sweepDeg * internalValue
        val angleRad = Math.toRadians(angleDeg.toDouble())
        val indicatorLength = radius * 0.85f
        val end = Offset(
            x = center.x + (indicatorLength * cos(angleRad)).toFloat(),
            y = center.y + (indicatorLength * sin(angleRad)).toFloat(),
        )
        drawArc(
            color = accent,
            startAngle = startAngleDeg,
            sweepAngle = sweepDeg * internalValue,
            useCenter = false,
            style = Stroke(width = radius * 0.18f),
            topLeft = Offset(center.x - radius * 0.8f, center.y - radius * 0.8f),
            size = androidx.compose.ui.geometry.Size(radius * 1.6f, radius * 1.6f),
        )
        drawLine(color = Color.White, start = center, end = end, strokeWidth = radius * 0.12f)
    }
}

/**
 * A large horizontal draggable crossfader. [position] in [-1, 1].
 */
@Composable
fun Crossfader(position: Float, onChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(DjColors.panelSurface, RoundedCornerShape(8.dp))
            .border(1.dp, DjColors.knobTrack, RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val width = this.size.width.toFloat().coerceAtLeast(1f)
                    val delta = (dragAmount.x / width) * 2f
                    onChange((position + delta).coerceIn(-1f, 1f))
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(40.dp)) {
            val width = this.size.width
            val height = this.size.height
            drawLine(
                color = DjColors.knobTrack,
                start = Offset(0f, height / 2f),
                end = Offset(width, height / 2f),
                strokeWidth = 4f,
            )
            val t = (position.coerceIn(-1f, 1f) + 1f) / 2f
            val x = t * width
            drawCircle(color = Color.White, radius = height * 0.42f, center = Offset(x, height / 2f))
            drawCircle(color = DjColors.ledCyan, radius = height * 0.3f, center = Offset(x, height / 2f))
        }
    }
}

/**
 * A simplified, honest jog wheel: rotates continuously while playing (a slow
 * animation, matching the app's existing vinyl visual language) and, on
 * drag, seeks the deck — a fine nudge while playing, proportional scrub
 * while paused. Does not attempt real audio scratch/pitch-bending, which is
 * out of scope for a safe, blind implementation.
 */
@Composable
fun JogWheel(
    isPlaying: Boolean,
    rotationDegrees: Float,
    onSeekDelta: (Long) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = DjColors.deckA,
    size: Dp = 120.dp,
) {
    Canvas(
        modifier = modifier
            .size(size)
            .pointerInput(isPlaying) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    // Angle-independent simplification: horizontal+vertical
                    // drag distance maps to a seek delta. Real scratch-audio
                    // pitch bending is intentionally out of scope.
                    val distance = dragAmount.x + dragAmount.y
                    val msPerPixel = if (isPlaying) 15L else 60L
                    onSeekDelta((distance * msPerPixel).toLong())
                }
            },
    ) {
        val radius = min(this.size.width, this.size.height) / 2f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        drawCircle(color = DjColors.panelSurfaceLight, radius = radius, center = center)
        drawCircle(color = DjColors.knobTrack, radius = radius * 0.92f, center = center)
        val angleRad = Math.toRadians(rotationDegrees.toDouble())
        val markerEnd = Offset(
            x = center.x + (radius * 0.75f * cos(angleRad)).toFloat(),
            y = center.y + (radius * 0.75f * sin(angleRad)).toFloat(),
        )
        drawLine(color = accent, start = center, end = markerEnd, strokeWidth = radius * 0.08f)
        drawCircle(color = accent, radius = radius * 0.1f, center = center)
    }
}

/** LED-style transport button. [active] controls the glow color, matching
 * the spec: Play=green, Cue=orange, Sync=cyan. */
@Composable
fun LedButton(
    label: String,
    active: Boolean,
    ledColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(width = 64.dp, height = 44.dp)
            .background(
                if (active) ledColor.copy(alpha = 0.25f) else DjColors.panelSurface,
                RoundedCornerShape(8.dp),
            )
            .border(1.dp, if (active) ledColor else DjColors.knobTrack, RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectTapGestures { onClick() }
            },
    ) {
        androidx.compose.material3.Text(
            text = label,
            color = if (active) ledColor else DjColors.textSecondary,
            modifier = Modifier.align(androidx.compose.ui.Alignment.Center),
            fontSize = 12.sp,
        )
    }
}

/** One hot-cue pad: glows [accent] when set, tap seeks/sets, long-press clears. */
@Composable
fun HotCuePad(
    label: String,
    isSet: Boolean,
    accent: Color,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .background(if (isSet) accent.copy(alpha = 0.35f) else DjColors.panelSurface, RoundedCornerShape(6.dp))
            .border(1.dp, if (isSet) accent else DjColors.knobTrack, RoundedCornerShape(6.dp))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onTap() }, onLongPress = { onLongPress() })
            },
    ) {
        androidx.compose.material3.Text(
            text = label,
            color = if (isSet) Color.White else DjColors.textSecondary,
            modifier = Modifier.align(androidx.compose.ui.Alignment.Center),
            fontSize = 11.sp,
        )
    }
}

/** Real waveform render from downsampled amplitude data, scrolling/highlighting
 * up to the current playhead. Cue points and the loop region are drawn as an
 * overlay. Gracefully renders a flat placeholder line when [amplitudes] is
 * null (e.g. decode failed or hasn't finished), never crashing. */
@Composable
fun WaveformView(
    amplitudes: FloatArray?,
    progress: Float,
    hotCues: List<Float>,
    loopRange: ClosedFloatingPointRange<Float>?,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxWidth().height(64.dp).background(DjColors.panelBg)) {
        val width = this.size.width
        val height = this.size.height
        val mid = height / 2f
        if (amplitudes != null && amplitudes.isNotEmpty()) {
            val barWidth = width / amplitudes.size
            for (i in amplitudes.indices) {
                val amp = amplitudes[i].coerceIn(0f, 1f)
                val barHeight = amp * height * 0.9f
                val x = i * barWidth
                val played = (i / amplitudes.size.toFloat()) <= progress
                drawLine(
                    color = if (played) accent else DjColors.knobTrack,
                    start = Offset(x, mid - barHeight / 2f),
                    end = Offset(x, mid + barHeight / 2f),
                    strokeWidth = barWidth.coerceAtLeast(1f),
                )
            }
        } else {
            drawLine(color = DjColors.knobTrack, start = Offset(0f, mid), end = Offset(width, mid), strokeWidth = 2f)
        }
        loopRange?.let { range ->
            drawRect(
                color = DjColors.ledCyan.copy(alpha = 0.25f),
                topLeft = Offset(range.start * width, 0f),
                size = androidx.compose.ui.geometry.Size((range.endInclusive - range.start) * width, height),
            )
        }
        hotCues.forEach { fraction ->
            val x = fraction.coerceIn(0f, 1f) * width
            drawLine(color = DjColors.ledOrange, start = Offset(x, 0f), end = Offset(x, height), strokeWidth = 2f)
        }
        val playheadX = progress.coerceIn(0f, 1f) * width
        drawLine(color = Color.White, start = Offset(playheadX, 0f), end = Offset(playheadX, height), strokeWidth = 2f)
    }
}
