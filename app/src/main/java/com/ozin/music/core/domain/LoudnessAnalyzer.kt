package com.ozin.music.core.domain

import android.content.ContentUris
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteOrder
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Decodes a track's real PCM audio off the main thread via
 * MediaExtractor/MediaCodec (mirrors [com.ozin.music.feature.dj.WaveformDecoder]'s
 * decode-loop exactly: same EOS handling, same defensive try/catch, same
 * bounded-memory running accumulator so decoded PCM is never fully retained)
 * and computes a **simplified RMS-based loudness estimate**.
 *
 * This is explicitly NOT a full EBU R128 / ITU-R BS.1770 implementation:
 * there is no K-weighting filter and no gating of silent/quiet blocks, so the
 * result is only a relative dB figure derived from mean-square sample energy,
 * not a true LUFS value. It is honest and useful for *relative* per-track
 * comparison within this library (quieter recordings get a bigger boost, and
 * vice versa) but should never be described as "EBU R128" or "ReplayGain" in
 * UI copy - the [com.ozin.music.core.data.model.Song.loudnessLufs] field name
 * is kept only for source/DB compatibility with that eventual, more rigorous
 * algorithm.
 */
object LoudnessAnalyzer {

    /** Returns a relative loudness estimate in dB (typically in the roughly
     * -60..0 range for real music), or null on any decode failure. */
    suspend fun analyze(context: Context, songId: Long): Float? =
        withContext(Dispatchers.Default) {
            try {
                analyzeInternal(context, songId)
            } catch (e: Exception) {
                Log.w(TAG, "Loudness analysis failed for song $songId", e)
                null
            }
        }

    private fun analyzeInternal(context: Context, songId: Long): Float? {
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId)
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            var audioTrackIndex = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    format = f
                    break
                }
            }
            if (audioTrackIndex < 0 || format == null) return null
            extractor.selectTrack(audioTrackIndex)

            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            // Running sum-of-squares accumulator instead of buffering the
            // whole track's PCM, so peak memory stays bounded regardless of
            // track length (same bounded-memory approach WaveformDecoder uses).
            var sumOfSquares = 0.0
            var sampleCount = 0L

            val bufferInfo = MediaCodec.BufferInfo()
            var sawInputEos = false
            var sawOutputEos = false
            val timeoutUs = 10_000L

            var guard = 0
            while (!sawOutputEos && guard < 200_000) {
                guard++
                if (!sawInputEos) {
                    val inIndex = codec.dequeueInputBuffer(timeoutUs)
                    if (inIndex >= 0) {
                        val buffer = codec.getInputBuffer(inIndex)
                        val sampleSize = if (buffer != null) extractor.readSampleData(buffer, 0) else -1
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            sawInputEos = true
                        } else {
                            codec.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val outIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
                if (outIndex >= 0) {
                    val outBuffer = codec.getOutputBuffer(outIndex)
                    if (outBuffer != null && bufferInfo.size > 0) {
                        outBuffer.order(ByteOrder.LITTLE_ENDIAN)
                        outBuffer.position(bufferInfo.offset)
                        outBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        val shortBuffer = outBuffer.asShortBuffer()
                        var i = 0
                        while (i < shortBuffer.remaining()) {
                            val sample = shortBuffer.get(i).toDouble()
                            sumOfSquares += sample * sample
                            sampleCount++
                            i += 1
                        }
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        sawOutputEos = true
                    }
                }
            }
            codec.stop()
            codec.release()

            if (sampleCount == 0L) return null
            val meanSquare = sumOfSquares / sampleCount
            val rms = sqrt(meanSquare)
            // Full-scale 16-bit reference (32768) so a maximal-amplitude
            // constant signal would land at 0dB; real music sits well below.
            val fullScale = 32768.0
            val dbRelativeToFullScale = 20.0 * log10((rms / fullScale).coerceAtLeast(1e-9))
            return dbRelativeToFullScale.toFloat()
        } finally {
            extractor.release()
        }
    }

    private const val TAG = "LoudnessAnalyzer"
}
