package com.ozin.music.core.domain

import kotlin.math.sin

/**
 * Pure, deterministic math behind the Now Playing "Visualizer" visual mode.
 *
 * This does NOT analyze real audio frequency data. It produces a bar height
 * in [0, 1] purely as a function of (songId, timeSeconds, barIndex,
 * barCount): the same inputs always give the same output. The song id seeds
 * a per-song pseudo-random pattern (so different songs "look" different but
 * consistently so), and timeSeconds - which the caller derives from actual
 * playback position - drives a smooth animated wave, so the visualization
 * only appears to move while the song is actually playing/advancing.
 */
object VisualizerMath {

    /**
     * Returns a bar height in the range [0f, 1f] for [barIndex] (of
     * [barCount] total bars) of [songId] at [timeSeconds] into an
     * animation clock derived from playback position.
     */
    fun barHeight(songId: Long, timeSeconds: Float, barIndex: Int, barCount: Int): Float {
        val safeBarCount = barCount.coerceAtLeast(1)
        val safeIndex = barIndex.coerceIn(0, safeBarCount - 1)
        val seed = songId * 1_000_003L + safeIndex * 97L
        val speed = 0.55f + (safeIndex % 5) * 0.12f
        val phaseOffset = pseudoRandom(seed) * (2 * Math.PI).toFloat()
        val wave = (sin(timeSeconds * speed + phaseOffset).toFloat() + 1f) / 2f
        val variance = 0.35f + 0.65f * pseudoRandom(seed + 7L)
        val height = 0.12f + 0.88f * wave * variance
        return height.coerceIn(0f, 1f)
    }

    /** Deterministic hash of [seed] into [0, 1). Same seed -> same output. */
    private fun pseudoRandom(seed: Long): Float {
        var x = seed
        x = (x xor (x ushr 33)) * -0x61c8864680b583ebL
        x = (x xor (x ushr 33)) * -0x61c8864680b583ebL
        x = x xor (x ushr 33)
        val nonNegative = x and 0x7FFFFFFFFFFFFFFFL
        return (nonNegative.toDouble() / Long.MAX_VALUE.toDouble()).toFloat().coerceIn(0f, 1f)
    }
}
