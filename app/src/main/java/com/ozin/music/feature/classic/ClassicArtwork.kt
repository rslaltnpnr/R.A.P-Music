package com.ozin.music.feature.classic

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import kotlin.math.roundToInt

internal val ClassicBlack = Color(0xFF030305)
internal val ClassicGreen = Color(0xFF00FF14)
internal val ClassicWhite = Color(0xFFE8EAF3)
private val ClassicGold = Color(0xFFE2C57B)
private val DarkBevel = Color(0xFF101019)
private val LightBevel = Color(0xFF77778B)
private val TextPaint = Paint().apply {
    isAntiAlias = false
    typeface = Typeface.create("monospace", Typeface.BOLD)
}

internal fun DrawScope.pxRect(x: Float, y: Float, w: Float, h: Float, color: Color) {
    drawRect(color, Offset(x, y), Size(w, h))
}

internal fun DrawScope.pixelText(text: String, x: Float, y: Float, size: Float, color: Color, center: Boolean = false) {
    drawIntoCanvas {
        TextPaint.color = color.toArgb()
        TextPaint.textSize = size
        TextPaint.textAlign = if (center) Paint.Align.CENTER else Paint.Align.LEFT
        it.nativeCanvas.drawText(text, x, y, TextPaint)
    }
}

internal fun DrawScope.shell(height: Float) {
    drawRect(Brush.horizontalGradient(listOf(Color(0xFF181824), Color(0xFF383852), Color(0xFF222233))),
        topLeft = Offset.Zero, size = Size(275f, height))
    pxRect(1f, 1f, 273f, 1f, LightBevel)
    pxRect(1f, 1f, 1f, height - 2, Color(0xFF5C5C70))
    pxRect(1f, height - 2, 273f, 1f, DarkBevel)
    pxRect(273f, 2f, 1f, height - 3, DarkBevel)
    if (height > 20f) {
        pxRect(5f, 13f, 265f, 1f, DarkBevel)
        pxRect(6f, 14f, 263f, 1f, LightBevel)
        pxRect(6f, 15f, 1f, height - 21, LightBevel)
        pxRect(269f, 14f, 1f, height - 20, DarkBevel)
        pxRect(6f, height - 5, 264f, 1f, LightBevel)
    }
}

internal fun DrawScope.titleRail(title: String, menu: Boolean = false, close: Boolean = true) {
    pxRect(2f, 2f, 271f, 10f, Color(0xFF252536))
    pxRect(2f, 11f, 271f, 1f, Color(0xFF646477))
    val halfText = title.length * 2.55f + 4
    val leftStart = if (menu) 20f else 8f
    for (i in 0..2) {
        val c = listOf(Color(0xFF7B6945), Color(0xFFFFE6A5), Color(0xFFBA9E5E))[i]
        pxRect(leftStart, 4f + i, 137.5f - halfText - leftStart, 1f, c)
        val right = 137.5f + halfText
        pxRect(right, 4f + i, 246f - right, 1f, c)
    }
    pixelText(title, 137.5f, 9.7f, 8.2f, ClassicWhite, true)
    if (menu) {
        bolt(7f, 3f)
        chromeGlyph(246f, "−")
    }
    chromeGlyph(255f, "▣")
    if (close) chromeGlyph(264f, "×")
}

private fun DrawScope.chromeGlyph(x: Float, symbol: String) {
    pxRect(x, 3f, 7f, 7f, Color(0xFF454653))
    pxRect(x, 3f, 7f, 1f, Color(0xFF6E6D7C))
    pixelText(symbol, x + 3.5f, 9f, 7f, ClassicGold, true)
}

internal fun DrawScope.bolt(x: Float, y: Float, zoom: Float = 1f) {
    val mark = Path().apply {
        moveTo(x + 5 * zoom, y)
        lineTo(x, y + 5 * zoom)
        lineTo(x + 3 * zoom, y + 5 * zoom)
        lineTo(x + zoom, y + 9 * zoom)
        lineTo(x + 8 * zoom, y + 3 * zoom)
        lineTo(x + 5 * zoom, y + 3 * zoom)
        close()
    }
    drawPath(mark, ClassicGold)
}

internal fun DrawScope.well(x: Float, y: Float, w: Float, h: Float, color: Color = ClassicBlack) {
    pxRect(x, y, w, h, color)
    pxRect(x, y, w, 1f, Color(0xFF141422))
    pxRect(x, y, 1f, h, Color(0xFF181825))
    pxRect(x, y + h - 1, w, 1f, LightBevel)
    pxRect(x + w - 1, y + 1, 1f, h - 1, LightBevel)
}

internal fun DrawScope.buttonFace(x: Float, y: Float, w: Float, h: Float, active: Boolean = false) {
    drawRect(Brush.verticalGradient(
        if (active) listOf(Color(0xFF798998), Color(0xFFABB8C2))
        else listOf(Color(0xFFDAE6EA), Color(0xFFADBBC8), Color(0xFF8393A6)),
        startY = y, endY = y + h,
    ), Offset(x, y), Size(w, h))
    pxRect(x, y, w, 1f, if (active) Color(0xFF535C68) else Color(0xFFECF1F2))
    pxRect(x, y, 1f, h, if (active) Color(0xFF414758) else Color(0xFFC6D2DB))
    pxRect(x, y + h - 1, w, 1f, Color(0xFF161C29))
    pxRect(x + w - 1, y + 1, 1f, h - 1, Color(0xFF485567))
}

internal fun DrawScope.labeledButton(label: String, x: Float, y: Float, w: Float, h: Float, active: Boolean = false) {
    buttonFace(x, y, w, h, active)
    pixelText(label, x + w / 2, y + h / 2 + 2.3f, if (label.length > 5) 5.5f else 6.6f, Color(0xFF1C2635), true)
}

internal enum class Transport { PREVIOUS, PLAY, PAUSE, STOP, NEXT, EJECT }

internal fun DrawScope.transportIcon(action: Transport, x: Float, y: Float, color: Color) {
    fun triangle(left: Float, top: Float, width: Float, flip: Boolean = false) {
        val from = if (flip) left + width else left
        val to = if (flip) left else left + width
        val path = Path().apply {
            moveTo(from, top)
            lineTo(to, top + 3f)
            lineTo(from, top + 6f)
            close()
        }
        drawPath(path, color)
    }
    when (action) {
        Transport.PREVIOUS -> {
            pxRect(x, y, 1f, 6f, color)
            triangle(x + 2f, y, 6f, true)
        }
        Transport.PLAY -> triangle(x + 1f, y, 6f)
        Transport.PAUSE -> {
            pxRect(x + 1f, y, 2f, 6f, color)
            pxRect(x + 5f, y, 2f, 6f, color)
        }
        Transport.STOP -> pxRect(x + 1f, y, 6f, 6f, color)
        Transport.NEXT -> {
            triangle(x, y, 6f)
            pxRect(x + 7f, y, 1f, 6f, color)
        }
        Transport.EJECT -> {
            val path = Path().apply {
                moveTo(x, y + 4)
                lineTo(x + 4, y)
                lineTo(x + 8, y + 4)
                close()
            }
            drawPath(path, color)
            pxRect(x, y + 5, 8f, 1.5f, color)
        }
    }
}

private val DigitMasks = intArrayOf(0x3f, 0x06, 0x5b, 0x4f, 0x66, 0x6d, 0x7d, 0x07, 0x7f, 0x6f)

internal fun DrawScope.digitalTime(text: String, x: Float, y: Float) {
    var at = x
    text.forEach { ch ->
        when (ch) {
            ':' -> {
                pxRect(at + 1, y + 4, 1f, 1f, ClassicGreen)
                pxRect(at + 1, y + 8, 1f, 1f, ClassicGreen)
                at += 5
            }
            '-' -> {
                pxRect(at, y + 6, 4f, 1f, ClassicGreen)
                at += 5
            }
            in '0'..'9' -> {
                val mask = DigitMasks[ch - '0']
                val segments = listOf(
                    floatArrayOf(1f, 0f, 6f, 2f), floatArrayOf(7f, 1f, 2f, 5f),
                    floatArrayOf(7f, 7f, 2f, 5f), floatArrayOf(1f, 11f, 6f, 2f),
                    floatArrayOf(0f, 7f, 2f, 5f), floatArrayOf(0f, 1f, 2f, 5f),
                    floatArrayOf(1f, 5.5f, 6f, 2f),
                )
                segments.forEachIndexed { i, s ->
                    pxRect(at + s[0], y + s[1], s[2], s[3],
                        if (mask and (1 shl i) != 0) ClassicGreen else Color(0xFF092414))
                }
                at += 10
            }
        }
    }
}

internal fun DrawScope.meterGrid() {
    well(11f, 22f, 93f, 43f, Color(0xFF030609))
    for (x in 13..101 step 2) for (y in 24..62 step 2) {
        pxRect(x.toFloat(), y.toFloat(), 1f, 1f, Color(0xFF10172A))
    }
    pxRect(23f, 61f, 76f, 1f, Color(0xFF092A39))
}

internal fun DrawScope.horizontalControl(x: Float, y: Float, w: Float, h: Float, value: Float, balance: Boolean = false) {
    val f = value.coerceIn(0f, 1f)
    val intensity = if (balance) kotlin.math.abs(f - 0.5f) * 2f else f
    well(x, y, w, h, Color(0xFF13172A))
    val end = if (balance) x + w / 2 else x + 1
    val fillX = if (balance) minOf(end, x + w * f) else end
    val fillW = if (balance) kotlin.math.abs(w * (f - 0.5f)) else (w - 2) * f
    if (fillW >= 1f) {
        val hue = 108f - intensity * 106f
        pxRect(fillX, y + 3, fillW, h - 6, Color.hsv(hue, 0.91f, 0.75f))
        pxRect(fillX, y + 3, fillW, 1f, Color.hsv(hue, 0.70f, 1f))
    }
    val thumb = (x + (w - 13f) * f).roundToInt().toFloat()
    buttonFace(thumb, y, 13f, h)
    for (i in 0..2) pxRect(thumb + 4 + i * 2, y + 3, 1f, h - 6, Color(0xFF5E6A79))
}

internal fun DrawScope.positionControl(value: Float, enabled: Boolean) {
    well(17f, 73f, 248f, 9f, Color(0xFF1A1A29))
    if (!enabled) return
    val x = 18f + 219 * value.coerceIn(0f, 1f)
    buttonFace(x, 73f, 27f, 9f)
    for (i in 0..5) pxRect(x + 7 + i * 2, 76f, 1f, 3f, Color(0xFF596678))
}

internal fun DrawScope.eqTrack(x: Float, value: Int) {
    val f = ((value + 1200) / 2400f).coerceIn(0f, 1f)
    well(x, 36f, 8f, 65f, Color(0xFF0F1520))
    val hue = 107f * (1f - f)
    pxRect(x + 2, 38f, 4f, 60f, Color.hsv(hue, 0.85f, 0.68f))
    pxRect(x + 3, 38f, 1f, 60f, Color.hsv(hue, 0.73f, 0.95f))
    val y = 38 + (1f - f) * 54f
    buttonFace(x - 1, y, 10f, 7f)
    pxRect(x + 1, y + 3, 6f, 1f, Color(0xFF4E5F70))
}
