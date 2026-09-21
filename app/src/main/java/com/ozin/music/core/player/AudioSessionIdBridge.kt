package com.ozin.music.core.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Process-wide holder for the ExoPlayer audio session id, published by
 * [PlaybackService] (same lifecycle it already tracks for [EffectsChain])
 * and read by the real FFT visualizer ([com.ozin.music.feature.player.VisualizerVisualMode])
 * to attach `android.media.audiofx.Visualizer` to the actual audio output.
 *
 * A plain singleton object (rather than a Hilt-injected class) is
 * deliberate here: both the publisher (service) and the only consumer
 * (a `@Composable` that has no Hilt entry point of its own convenient to
 * reach) live in the same process, and this is a single, read-mostly int -
 * exactly the case this app already uses simple singletons for elsewhere
 * (e.g. `VisualizerMath`).
 */
object AudioSessionIdBridge {
    private val _audioSessionId = MutableStateFlow(0)
    val audioSessionId: StateFlow<Int> = _audioSessionId

    fun update(sessionId: Int) {
        _audioSessionId.value = sessionId
    }
}
