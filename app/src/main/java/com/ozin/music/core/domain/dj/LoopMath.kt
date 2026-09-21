package com.ozin.music.core.domain.dj

/** Pure loop-length math. Beat-length presets (1/4 to 32 beats) degrade
 * gracefully to a fixed real-time duration (assuming 120bpm, i.e. 500ms per
 * beat) when a track's BPM is unknown, and compute a real duration from BPM
 * when it is known. */
object LoopMath {
    private const val DEFAULT_BEAT_MS = 500.0 // 120bpm fallback

    /** Beat-length presets exposed in the UI, in units of beats. */
    val BEAT_PRESETS = listOf(0.25, 0.5, 1.0, 2.0, 4.0, 8.0, 16.0, 32.0)

    /** Duration in ms of [beats] beats, using [bpm] when known (> 0), else a
     * fixed 120bpm assumption. */
    fun loopDurationMs(beats: Double, bpm: Float?): Long {
        val msPerBeat = if (bpm != null && bpm > 0f) 60000.0 / bpm else DEFAULT_BEAT_MS
        return (beats * msPerBeat).toLong().coerceAtLeast(50L)
    }
}
