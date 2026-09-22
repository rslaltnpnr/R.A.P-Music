package com.ozin.music.core.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import androidx.core.graphics.ColorUtils
import java.io.File
import java.io.FileOutputStream

/**
 * Renders a shareable "Now Playing" card entirely locally: no network call,
 * no third-party SDK. The card is a plain [android.graphics.Canvas] drawing
 * (same drawing-API style as the rest of the app's local image work, e.g.
 * artwork resolution) rather than a Compose capture, so it has no dependency
 * on a particular Compose/graphicsLayer version behaving a certain way.
 *
 * Visual language mirrors the Now Playing screen itself: a dark base with a
 * vertical gradient toward the song's accent color (the same accent already
 * derived from album art via Palette on that screen) and a rounded card for
 * the artwork.
 */
object NowPlayingCardRenderer {

    private const val WIDTH = 1080
    private const val HEIGHT = 1080
    private const val DARK_BASE = 0xFF0B0F19.toInt()

    /**
     * @param albumArt decoded album art bitmap, or null to fall back to a
     *   plain accent-colored tile (still a real, locally-drawn card, not a
     *   placeholder claiming to have art).
     * @param accentColorArgb the accent color already computed for the
     *   current song on the Now Playing screen.
     */
    fun render(
        title: String,
        artist: String,
        albumArt: Bitmap?,
        accentColorArgb: Int,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val backgroundPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, HEIGHT.toFloat(),
                ColorUtils.setAlphaComponent(accentColorArgb, 140),
                DARK_BASE,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), backgroundPaint)

        val artSize = 720f
        val artLeft = (WIDTH - artSize) / 2f
        val artTop = 120f
        val artRect = RectF(artLeft, artTop, artLeft + artSize, artTop + artSize)
        val cornerRadius = 36f

        val artPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        if (albumArt != null) {
            val shader = android.graphics.BitmapShader(albumArt, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            val scale = maxOf(artSize / albumArt.width, artSize / albumArt.height)
            val matrix = android.graphics.Matrix()
            matrix.setScale(scale, scale)
            matrix.postTranslate(artLeft, artTop)
            shader.setLocalMatrix(matrix)
            artPaint.shader = shader
        } else {
            artPaint.color = accentColorArgb
        }
        canvas.drawRoundRect(artRect, cornerRadius, cornerRadius, artPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 56f
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val artistPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(200, 255, 255, 255)
            textSize = 40f
            textAlign = Paint.Align.CENTER
        }

        val centerX = WIDTH / 2f
        val titleY = artTop + artSize + 100f
        canvas.drawText(ellipsize(titlePaint, title, WIDTH - 120f), centerX, titleY, titlePaint)
        canvas.drawText(ellipsize(artistPaint, artist, WIDTH - 120f), centerX, titleY + 64f, artistPaint)

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(150, 255, 255, 255)
            textSize = 30f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("R.A.P Music", centerX, HEIGHT - 60f, brandPaint)

        return bitmap
    }

    private fun ellipsize(paint: Paint, text: String, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var truncated = text
        while (truncated.isNotEmpty() && paint.measureText("$truncated…") > maxWidth) {
            truncated = truncated.dropLast(1)
        }
        return "$truncated…"
    }

    /** Writes the card to a narrowly-scoped cache subdirectory and returns a
     * `content://` URI via the app's existing FileProvider, ready to hand to
     * [android.content.Intent.ACTION_SEND]. */
    fun saveToShareCache(context: Context, bitmap: Bitmap): android.net.Uri {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, "now_playing_card.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
