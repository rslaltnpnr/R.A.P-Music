package com.ozin.music.core.domain.dj

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Pure downsampling of raw PCM (16-bit) sample magnitudes into a bounded
 * number of amplitude buckets for waveform rendering. Kept separate from the
 * MediaExtractor/MediaCodec decode I/O so it is directly unit-testable, and
 * bounds memory: callers never need to hold more than [bucketCount] floats
 * per track after calling this, regardless of source length. */
object WaveformDownsampler {
    const val DEFAULT_BUCKET_COUNT = 300

    /** [samples] are raw signed 16-bit PCM values (mono, or already
     * channel-mixed by the caller). Returns [bucketCount] peak-normalized
     * (0..1) amplitude values, one per bucket, spanning the whole array. */
    fun downsample(samples: ShortArray, bucketCount: Int = DEFAULT_BUCKET_COUNT): FloatArray {
        if (samples.isEmpty() || bucketCount <= 0) return FloatArray(max(bucketCount, 0))
        val buckets = FloatArray(bucketCount)
        val samplesPerBucket = max(1, samples.size / bucketCount)
        for (i in 0 until bucketCount) {
            val start = i * samplesPerBucket
            if (start >= samples.size) break
            val end = min(samples.size, start + samplesPerBucket)
            var peak = 0
            for (j in start until end) {
                val v = abs(samples[j].toInt())
                if (v > peak) peak = v
            }
            buckets[i] = (peak / 32768f).coerceIn(0f, 1f)
        }
        // Normalize so the loudest bucket hits 1.0, for a visually useful waveform.
        val max = buckets.maxOrNull() ?: 0f
        if (max > 0.001f) {
            for (i in buckets.indices) buckets[i] = (buckets[i] / max).coerceIn(0f, 1f)
        }
        return buckets
    }
}
