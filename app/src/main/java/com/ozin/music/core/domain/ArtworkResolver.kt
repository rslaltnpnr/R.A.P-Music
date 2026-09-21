package com.ozin.music.core.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.net.toUri
import com.ozin.music.R
import com.ozin.music.core.data.mediastore.MediaStoreScanner
import com.ozin.music.core.data.model.Song
import com.ozin.music.core.settings.ArtworkQuality
import com.ozin.music.core.settings.LockScreenPrivacy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Where a resolved artwork ultimately came from — surfaced to the Debug
 * screen (item 9) so it reflects the real resolver path rather than a
 * parallel diagnostic guess. */
enum class ArtworkSource { EMBEDDED, MEDIA_STORE, DEFAULT }

/** Either a directly-usable [uri] (preferred — Media3/Coil load it lazily)
 * or raw [data] bytes (used only when the source has no stable URI, i.e.
 * embedded ID3 art extracted in-memory). Exactly one of the two is non-null
 * unless [source] is [ArtworkSource.DEFAULT], in which case [uri] points at
 * the bundled default artwork drawable. */
data class ArtworkResult(
    val uri: Uri?,
    val data: ByteArray?,
    val source: ArtworkSource,
)

/**
 * Resolves the real artwork to show for a [Song], in priority order:
 * embedded ID3/format picture -> MediaStore album-art content URI -> the
 * bundled default artwork drawable. Also enforces the user's artwork-quality
 * tier (downsampling for BALANCED/AUTO) and lock-screen privacy settings.
 * All work here is off the main thread.
 */
@Singleton
class ArtworkResolver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaStoreScanner: MediaStoreScanner,
) {
    private val defaultArtworkUri: Uri by lazy {
        "android.resource://${context.packageName}/${R.drawable.default_artwork}".toUri()
    }

    /**
     * @param showArtwork item 3's `lockScreenShowArtwork`; when false the
     * default artwork is always used, same as [LockScreenPrivacy.HIDE_ARTWORK].
     */
    suspend fun resolve(
        song: Song,
        quality: ArtworkQuality,
        privacy: LockScreenPrivacy,
        showArtwork: Boolean,
    ): ArtworkResult = withContext(Dispatchers.IO) {
        val artworkHidden = !showArtwork ||
            privacy == LockScreenPrivacy.HIDE_ARTWORK ||
            privacy == LockScreenPrivacy.PRIVATE
        if (artworkHidden) {
            return@withContext ArtworkResult(defaultArtworkUri, null, ArtworkSource.DEFAULT)
        }

        val embedded = runCatching { extractEmbeddedPicture(song.path) }.getOrNull()
        if (embedded != null && embedded.isNotEmpty()) {
            val bytes = when (quality) {
                ArtworkQuality.HIGH -> embedded.takeIf { it.size <= MAX_EMBEDDED_BYTES }
                    ?: downsampleBytes(embedded, HIGH_MAX_DIMENSION, HIGH_JPEG_QUALITY)
                ArtworkQuality.BALANCED -> downsampleBytes(embedded, BALANCED_MAX_DIMENSION, BALANCED_JPEG_QUALITY)
                ArtworkQuality.AUTO -> downsampleBytes(embedded, AUTO_MAX_DIMENSION, AUTO_JPEG_QUALITY)
            }
            if (bytes != null) {
                return@withContext ArtworkResult(null, bytes, ArtworkSource.EMBEDDED)
            }
        }

        val mediaStoreUri = mediaStoreScanner.albumArtUri(song.albumId)
        val mediaStoreExists = runCatching {
            context.contentResolver.openInputStream(mediaStoreUri)?.use { true } ?: false
        }.getOrDefault(false)
        if (mediaStoreExists) {
            return@withContext when (quality) {
                ArtworkQuality.HIGH -> ArtworkResult(mediaStoreUri, null, ArtworkSource.MEDIA_STORE)
                ArtworkQuality.BALANCED -> downsampleUriToCacheFile(
                    song.id, mediaStoreUri, BALANCED_MAX_DIMENSION, BALANCED_JPEG_QUALITY, "balanced",
                )?.let { ArtworkResult(it, null, ArtworkSource.MEDIA_STORE) }
                    ?: ArtworkResult(mediaStoreUri, null, ArtworkSource.MEDIA_STORE)
                ArtworkQuality.AUTO -> downsampleUriToCacheFile(
                    song.id, mediaStoreUri, AUTO_MAX_DIMENSION, AUTO_JPEG_QUALITY, "auto",
                )?.let { ArtworkResult(it, null, ArtworkSource.MEDIA_STORE) }
                    ?: ArtworkResult(mediaStoreUri, null, ArtworkSource.MEDIA_STORE)
            }
        }

        ArtworkResult(defaultArtworkUri, null, ArtworkSource.DEFAULT)
    }

    /** Reads the audio file's own embedded cover art (ID3 APIC / equivalent
     * format-level picture), if present. Returns null for files with none. */
    private fun extractEmbeddedPicture(path: String): ByteArray? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            retriever.embeddedPicture
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun downsampleBytes(source: ByteArray, maxDimension: Int, jpegQuality: Int): ByteArray? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(source, 0, source.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxDimension)
        }
        val bitmap = BitmapFactory.decodeByteArray(source, 0, source.size, options) ?: return null
        return compressToJpeg(bitmap, jpegQuality)
    }

    private fun downsampleUriToCacheFile(
        songId: Long,
        uri: Uri,
        maxDimension: Int,
        jpegQuality: Int,
        tag: String,
    ): Uri? {
        val cacheDir = File(context.cacheDir, "artwork").apply { mkdirs() }
        val outFile = File(cacheDir, "${songId}_$tag.jpg")
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            val sampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxDimension)
            val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, BitmapFactory.Options().apply { inSampleSize = sampleSize })
            } ?: return null
            bitmap.use { bmp ->
                outFile.outputStream().use { out -> bmp.compress(Bitmap.CompressFormat.JPEG, jpegQuality, out) }
            }
            return Uri.fromFile(outFile)
        } catch (_: Exception) {
            return null
        }
    }

    private fun compressToJpeg(bitmap: Bitmap, quality: Int): ByteArray = bitmap.use { bmp ->
        ByteArrayOutputStream().use { out ->
            bmp.compress(Bitmap.CompressFormat.JPEG, quality, out)
            out.toByteArray()
        }
    }

    private inline fun <R> Bitmap.use(block: (Bitmap) -> R): R {
        try {
            return block(this)
        } finally {
            if (!isRecycled) recycle()
        }
    }

    private fun sampleSizeFor(width: Int, height: Int, maxDimension: Int): Int {
        var sampleSize = 1
        var longestSide = maxOf(width, height)
        while (longestSide / (sampleSize * 2) >= maxDimension) {
            sampleSize *= 2
        }
        return sampleSize
    }

    companion object {
        private const val MAX_EMBEDDED_BYTES = 1_500_000
        private const val HIGH_MAX_DIMENSION = 1024
        private const val HIGH_JPEG_QUALITY = 92
        private const val BALANCED_MAX_DIMENSION = 512
        private const val BALANCED_JPEG_QUALITY = 85
        private const val AUTO_MAX_DIMENSION = 300
        private const val AUTO_JPEG_QUALITY = 80
    }
}
