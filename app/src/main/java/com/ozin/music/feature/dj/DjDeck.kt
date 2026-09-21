package com.ozin.music.feature.dj

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.ozin.music.core.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class DeckId { A, B }

data class LoopState(
    val inMs: Long? = null,
    val outMs: Long? = null,
    val active: Boolean = false,
) {
    val isValid: Boolean get() = inMs != null && outMs != null && outMs > inMs
}

data class DeckUiState(
    val song: Song? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: Float = 1f,
    val originalBpm: Float? = null,
    val hotCues: Map<Int, Long> = emptyMap(),
    val loop: LoopState = LoopState(),
    val waveform: FloatArray? = null,
    val cuePointMs: Long = 0L,
) {
    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = System.identityHashCode(this)
}

/**
 * One independent DJ deck: owns exactly one [ExoPlayer] instance for its
 * entire lifetime (never recreated on track load, so rapid track switching
 * never leaks players), its own [DjChannelEffects] chain attached to its own
 * audio session id, in-memory hot cues, and A/B loop state. Fully separate
 * from the main app's single-session [com.ozin.music.core.player.PlayerController]
 * — no MediaController/MediaSession involved, by design (DJ Mode is an
 * in-app performance tool, not a lock-screen-integrated playback path).
 */
class DjDeck(
    private val context: Context,
    val id: DeckId,
) {
    val player: ExoPlayer = ExoPlayer.Builder(context).build()
    val effects = DjChannelEffects()

    private val _state = MutableStateFlow(DeckUiState())
    val state: StateFlow<DeckUiState> = _state.asStateFlow()

    private var baseVolume: Float = 1f
    private var crossfaderVolume: Float = 1f
    private var gainMultiplier: Float = 1f

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.value = _state.value.copy(isPlaying = isPlaying)
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                effects.attach(audioSessionId)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _state.value = _state.value.copy(durationMs = player.duration.coerceAtLeast(0))
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                // Defensive: a broken file on one deck must never take down
                // the other deck or crash DJ Mode. Just stop this deck.
                player.stop()
            }
        })
    }

    /** Loads a new local track. Safe to call while this deck (or the other
     * deck) is currently playing — the same ExoPlayer instance is reused, so
     * no resources leak and the other deck is entirely unaffected. */
    fun loadSong(song: Song) {
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id)
        val bpm = readBpmTag(song.path)
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        _state.value = DeckUiState(
            song = song,
            originalBpm = bpm,
            waveform = null,
        )
    }

    private fun readBpmTag(path: String): Float? = try {
        MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(path)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BEATS_PER_MINUTE)?.toFloatOrNull()
        }
    } catch (_: Exception) {
        null
    }

    private inline fun MediaMetadataRetriever.use(block: (MediaMetadataRetriever) -> Float?): Float? {
        return try {
            block(this)
        } finally {
            release()
        }
    }

    fun setWaveform(amplitudes: FloatArray) {
        _state.value = _state.value.copy(waveform = amplitudes)
    }

    fun play() = player.play()
    fun pause() = player.pause()
    fun togglePlayPause() {
        if (player.isPlaying) pause() else play()
    }

    fun cue() {
        // Standard DJ "cue" behavior: while paused, jump to the stored cue
        // point; pressing while playing sets/returns to the cue point.
        val cuePoint = _state.value.cuePointMs
        player.pause()
        player.seekTo(cuePoint)
    }

    fun setCuePointHere() {
        _state.value = _state.value.copy(cuePointMs = player.currentPosition.coerceAtLeast(0))
    }

    fun seekTo(positionMs: Long) {
        val duration = player.duration
        val clamped = if (duration > 0) positionMs.coerceIn(0, duration) else positionMs.coerceAtLeast(0)
        player.seekTo(clamped)
    }

    fun nudgeSeek(deltaMs: Long) {
        seekTo((player.currentPosition + deltaMs).coerceAtLeast(0))
    }

    fun setSpeed(speed: Float) {
        val clamped = com.ozin.music.core.domain.PlaybackSpeed.clamp(speed)
        player.playbackParameters = PlaybackParameters(clamped, clamped)
        _state.value = _state.value.copy(speed = clamped)
    }

    // --- Hot cues: minimum 8 pads, in-memory. Spam-safe: pure map ops. ---
    fun onHotCuePress(pad: Int) {
        val cues = _state.value.hotCues
        val existing = cues[pad]
        if (existing == null) {
            _state.value = _state.value.copy(hotCues = cues + (pad to player.currentPosition.coerceAtLeast(0)))
        } else {
            seekTo(existing)
        }
    }

    fun onHotCueLongPress(pad: Int) {
        _state.value = _state.value.copy(hotCues = _state.value.hotCues - pad)
    }

    // --- Loop ---
    fun setLoopIn() {
        _state.value = _state.value.copy(loop = _state.value.loop.copy(inMs = player.currentPosition))
    }

    fun setLoopOut() {
        _state.value = _state.value.copy(loop = _state.value.loop.copy(outMs = player.currentPosition))
    }

    /** Sets an auto-loop of [durationMs] starting at the current position. */
    fun setAutoLoop(durationMs: Long) {
        val start = player.currentPosition
        _state.value = _state.value.copy(
            loop = LoopState(inMs = start, outMs = start + durationMs, active = true),
        )
    }

    fun toggleLoopActive() {
        val loop = _state.value.loop
        if (loop.isValid) {
            _state.value = _state.value.copy(loop = loop.copy(active = !loop.active))
        }
    }

    fun exitLoop() {
        _state.value = _state.value.copy(loop = _state.value.loop.copy(active = false))
    }

    fun clearLoop() {
        _state.value = _state.value.copy(loop = LoopState())
    }

    /** Called from the shared engine tick loop; performs the real reloop
     * seek (same tick-and-check pattern as the main app's A-B repeat). */
    fun tick() {
        val position = player.currentPosition.coerceAtLeast(0)
        val loop = _state.value.loop
        if (loop.active && loop.isValid && position >= (loop.outMs ?: Long.MAX_VALUE)) {
            player.seekTo(loop.inMs ?: 0L)
        }
        _state.value = _state.value.copy(positionMs = position)
    }

    // --- Volume: real per-deck output volume, driven by crossfader position
    // (via [setCrossfaderVolume]) multiplied by channel gain (via [setGain]),
    // so both apply together to ExoPlayer's actual `volume`. ---
    fun setCrossfaderVolume(volume: Float) {
        crossfaderVolume = volume.coerceIn(0f, 1f)
        applyVolume()
    }

    fun setGain(multiplier: Float) {
        gainMultiplier = multiplier.coerceIn(0f, 2f)
        applyVolume()
    }

    private fun applyVolume() {
        player.volume = (baseVolume * crossfaderVolume * gainMultiplier).coerceIn(0f, 1f)
    }

    fun release() {
        effects.release()
        player.release()
    }
}
