package com.ozin.music.core.player

import androidx.media3.common.Player
import com.ozin.music.core.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A pragmatic, real crossfade: since ExoPlayer exposes a single active
 * output, we fade the current track's volume down over the configured window
 * near its end and restore full volume once the next item starts, rather
 * than truly overlapping decode of two tracks. Enabled/duration are read
 * live from [SettingsRepository] on every tick, so a change takes effect on
 * the very next check without needing to restart playback.
 */
@Singleton
class CrossfadeController @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : Player.Listener {

    private var player: Player? = null
    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private var job: kotlinx.coroutines.Job? = null

    fun attach(player: Player) {
        this.player = player
        job?.cancel()
        job = scope.launch {
            while (isActive) {
                delay(500)
                tick()
            }
        }
    }

    private suspend fun tick() {
        val p = player ?: return
        val settings = settingsRepository.settings.first()
        if (!settings.crossfadeEnabled || !p.isPlaying) {
            if (p.volume != 1f) runCatching { p.volume = 1f }
            return
        }
        val windowMs = settings.crossfadeSeconds * 1000L
        val remaining = p.duration - p.currentPosition
        if (p.duration <= 0 || remaining < 0) return
        if (remaining in 0..windowMs) {
            val fraction = (remaining.toFloat() / windowMs).coerceIn(0.1f, 1f)
            runCatching { p.volume = fraction }
        } else if (p.volume != 1f) {
            runCatching { p.volume = 1f }
        }
    }

    override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
        runCatching { player?.volume = 1f }
    }

    fun release() {
        job?.cancel()
        player = null
    }
}
