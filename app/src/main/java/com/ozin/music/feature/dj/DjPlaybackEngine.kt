package com.ozin.music.feature.dj

import android.content.Context
import com.ozin.music.core.data.model.Song
import com.ozin.music.core.data.repository.SongRepository
import com.ozin.music.core.domain.dj.AutomixSelector
import com.ozin.music.core.domain.dj.CrossfaderCurve
import com.ozin.music.core.domain.dj.DjSync
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

data class AutomixSettings(
    val enabled: Boolean = false,
    val preferFavorites: Boolean = false,
    val transitionSeconds: Int = 8,
    val triggerBeforeEndMs: Long = 15_000L,
)

data class DjEngineState(
    val crossfaderPosition: Float = 0f, // -1 (A) .. 1 (B)
    val gainA: Float = 1f,
    val gainB: Float = 1f,
    val automix: AutomixSettings = AutomixSettings(),
    val isRecording: Boolean = false,
    val recordingSupported: Boolean = DjRecorder.isSupported(),
    val recordedFilePath: String? = null,
    val syncMessage: String? = null,
)

/**
 * Owns DJ Mode's fully separate dual-deck audio engine. Two independent
 * ExoPlayer instances (via [DjDeck]) play simultaneously — Android mixes
 * multiple concurrent AudioTracks from the same app at the OS level, which
 * is real, standard behavior, not a hack. This is entirely separate from
 * [com.ozin.music.core.player.PlayerController]'s single MediaSession path.
 *
 * Held as a Hilt singleton so a ViewModel can survive rotation without
 * losing deck state, while still being explicitly torn down via [release]
 * when DJ Mode is actually closed (not on every configuration change).
 */
@Singleton
class DjPlaybackEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songRepository: SongRepository,
) {
    val deckA = DjDeck(context, DeckId.A)
    val deckB = DjDeck(context, DeckId.B)
    val sampler = DjSampler(context)
    val recorder = DjRecorder(context)

    private val _engineState = MutableStateFlow(DjEngineState())
    val engineState: StateFlow<DjEngineState> = _engineState.asStateFlow()

    private val recentlyPlayed = ArrayDeque<Song>(8)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tickJob: Job? = null
    private var automixJob: Job? = null

    fun start() {
        if (tickJob?.isActive == true) return
        applyCrossfader(_engineState.value.crossfaderPosition)
        tickJob = scope.launch {
            while (isActive) {
                deckA.tick()
                deckB.tick()
                checkAutomixTrigger()
                kotlinx.coroutines.delay(200L)
            }
        }
    }

    fun loadSong(deck: DeckId, song: Song) {
        val target = deckOf(deck)
        target.loadSong(song)
        scope.launch {
            val amplitudes = WaveformDecoder.decode(context, song.id)
            if (amplitudes != null) target.setWaveform(amplitudes)
        }
    }

    fun rememberPlayed(song: Song) {
        recentlyPlayed.addFirst(song)
        while (recentlyPlayed.size > 8) recentlyPlayed.removeLast()
    }

    fun deckOf(id: DeckId): DjDeck = if (id == DeckId.A) deckA else deckB

    fun setCrossfader(position: Float) {
        _engineState.value = _engineState.value.copy(crossfaderPosition = position.coerceIn(-1f, 1f))
        applyCrossfader(_engineState.value.crossfaderPosition)
    }

    private fun applyCrossfader(position: Float) {
        val volumes = CrossfaderCurve.volumesFor(position)
        deckA.setCrossfaderVolume(volumes.deckA)
        deckB.setCrossfaderVolume(volumes.deckB)
    }

    fun setGain(deck: DeckId, gain: Float) {
        deckOf(deck).setGain(gain)
        _engineState.value = when (deck) {
            DeckId.A -> _engineState.value.copy(gainA = gain)
            DeckId.B -> _engineState.value.copy(gainB = gain)
        }
    }

    /** Real Sync: adjusts [target]'s speed to match [reference]'s BPM when
     * both are known; otherwise a real no-op with a clear UI message. */
    fun sync(target: DeckId) {
        val targetDeck = deckOf(target)
        val referenceDeck = deckOf(if (target == DeckId.A) DeckId.B else DeckId.A)
        val result = DjSync.computeSyncSpeed(
            thisDeckOriginalBpm = targetDeck.state.value.originalBpm,
            otherDeckBpm = referenceDeck.state.value.originalBpm,
        )
        when (result) {
            is DjSync.Result.NewSpeed -> {
                targetDeck.setSpeed(result.speed)
                _engineState.value = _engineState.value.copy(syncMessage = null)
            }
            DjSync.Result.BpmUnknown -> {
                _engineState.value = _engineState.value.copy(syncMessage = "bpm_unknown")
            }
        }
    }

    fun clearSyncMessage() {
        _engineState.value = _engineState.value.copy(syncMessage = null)
    }

    // --- Automix ---
    fun setAutomixSettings(settings: AutomixSettings) {
        _engineState.value = _engineState.value.copy(automix = settings)
    }

    private fun checkAutomixTrigger() {
        val settings = _engineState.value.automix
        if (!settings.enabled) return
        if (automixJob?.isActive == true) return

        val playingDeckId = when {
            deckA.state.value.isPlaying && !deckB.state.value.isPlaying -> DeckId.A
            deckB.state.value.isPlaying && !deckA.state.value.isPlaying -> DeckId.B
            else -> return // both or neither playing: nothing to automix into
        }
        val playing = deckOf(playingDeckId)
        val idleId = if (playingDeckId == DeckId.A) DeckId.B else DeckId.A
        val idle = deckOf(idleId)
        val playingState = playing.state.value
        val remaining = playingState.durationMs - playingState.positionMs
        if (playingState.durationMs <= 0 || remaining > settings.triggerBeforeEndMs) return

        automixJob = scope.launch {
            val currentSong = playingState.song
            val library = songRepository.songs.first().filter {
                it.id != deckA.state.value.song?.id && it.id != deckB.state.value.song?.id
            }
            val next = AutomixSelector.pickNext(
                library = library,
                currentSong = currentSong,
                recentlyPlayed = recentlyPlayed.toList(),
                preferFavorites = settings.preferFavorites,
                // No per-library BPM cache exists outside of a loaded deck's
                // own ID3 read, so BPM-closeness scoring is honestly skipped
                // here rather than faked; genre/artist/favorite rules still
                // apply for real.
            ) ?: return@launch

            loadSong(idleId, next)
            // Wait briefly for prepare, then start the idle deck and crossfade.
            kotlinx.coroutines.delay(400)
            idle.play()
            val steps = max(1, settings.transitionSeconds * 5)
            val start = _engineState.value.crossfaderPosition
            val end = if (idleId == DeckId.B) 1f else -1f
            for (i in 1..steps) {
                val t = i / steps.toFloat()
                setCrossfader(start + (end - start) * t)
                kotlinx.coroutines.delay((settings.transitionSeconds * 1000L) / steps)
            }
            playing.pause()
            currentSong?.let { rememberPlayed(it) }
        }
    }

    fun release() {
        tickJob?.cancel()
        automixJob?.cancel()
        deckA.release()
        deckB.release()
        sampler.release()
        recorder.stop()
    }
}
