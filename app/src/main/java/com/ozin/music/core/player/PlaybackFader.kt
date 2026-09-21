package com.ozin.music.core.player

import androidx.media3.common.Player
import com.ozin.music.core.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A short, real volume ramp when playback starts (fade-in) or pauses
 * (fade-out), toggled from Settings. Runs on the same [Player] instance
 * ExoPlayer/MediaSession uses, driven by [Player.isPlaying] transitions.
 */
@Singleton
class PlaybackFader @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : Player.Listener {

    private var player: Player? = null
    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private var fadeJob: Job? = null

    fun attach(player: Player) {
        this.player = player
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        val p = player ?: return
        scope.launch {
            val enabled = settingsRepository.settings.first().fadeInOutEnabled
            if (!enabled) return@launch
            fadeJob?.cancel()
            fadeJob = launch {
                if (isPlaying) fade(p, from = 0f, to = 1f) else fade(p, from = p.volume, to = 0f)
            }
        }
    }

    private suspend fun fade(p: Player, from: Float, to: Float) {
        val steps = 10
        val stepDurationMs = FADE_DURATION_MS / steps
        runCatching { p.volume = from }
        for (i in 1..steps) {
            delay(stepDurationMs)
            val fraction = i / steps.toFloat()
            val value = from + (to - from) * fraction
            runCatching { p.volume = value.coerceIn(0f, 1f) }
        }
    }

    fun release() {
        fadeJob?.cancel()
        player = null
    }

    companion object {
        private const val FADE_DURATION_MS = 400L
    }
}
