package com.ozin.music.feature.dj

import android.content.ContentUris
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.provider.MediaStore
import android.util.Log
import com.ozin.music.core.domain.dj.WaveformDownsampler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteOrder

/**
 * Decodes a track's real amplitude envelope off the main thread via
 * MediaExtractor/MediaCodec (the same codec infrastructure Media3 already
 * pulls in), downsampled to a small, bounded number of buckets so decoded
 * PCM is never retained in memory afterward — safe on low-RAM devices. On
 * any decode failure, returns null so callers can fall back to a graceful
 * flat placeholder instead of crashing.
 */
object WaveformDecoder {

    suspend fun decode(context: Context, songId: Long, bucketCount: Int = WaveformDownsampler.DEFAULT_BUCKET_COUNT): FloatArray? =
        withContext(Dispatchers.Default) {
            try {
                decodeInternal(context, songId, bucketCount)
            } catch (e: Exception) {
                Log.w(TAG, "Waveform decode failed for song $songId", e)
                null
            }
        }

    private fun decodeInternal(context: Context, songId: Long, bucketCount: Int): FloatArray? {
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

            // Cap the total raw samples we ever buffer at once, regardless of
            // track length, to bound peak memory on low-RAM devices; we fold
            // into a running downsampled accumulator instead of keeping the
            // whole track's PCM.
            val maxRawWindow = 1_000_000 // ~1M shorts (~2MB) per decode window
            val rawWindow = ShortArray(maxRawWindow)
            var rawWindowFill = 0
            val bucketAccumulator = FloatArray(bucketCount)
            var totalSamplesSeen = 0L

            val bufferInfo = MediaCodec.BufferInfo()
            var sawInputEos = false
            var sawOutputEos = false
            val timeoutUs = 10_000L

            fun flushWindow() {
                if (rawWindowFill == 0) return
                val chunk = WaveformDownsampler.downsample(rawWindow.copyOf(rawWindowFill), bucketCount)
                for (i in chunk.indices) {
                    if (chunk[i] > bucketAccumulator[i]) bucketAccumulator[i] = chunk[i]
                }
                rawWindowFill = 0
            }

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
                            if (rawWindowFill >= maxRawWindow) flushWindow()
                            rawWindow[rawWindowFill++] = shortBuffer.get(i)
                            i += 1
                            totalSamplesSeen++
                        }
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        sawOutputEos = true
                    }
                }
            }
            flushWindow()
            codec.stop()
            codec.release()

            if (totalSamplesSeen == 0L) return null
            return bucketAccumulator
        } finally {
            extractor.release()
        }
    }

    private const val TAG = "WaveformDecoder"
}
