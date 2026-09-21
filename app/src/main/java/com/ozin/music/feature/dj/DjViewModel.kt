package com.ozin.music.feature.dj

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ozin.music.core.data.model.Song
import com.ozin.music.core.data.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Thin, rotation-surviving wrapper around the Hilt-singleton
 * [DjPlaybackEngine]. Since the engine itself is a singleton, deck state
 * (loaded tracks, hot cues, loops, crossfader position) is never lost on
 * screen rotation or ViewModel recreation — only [DjPlaybackEngine.release]
 * (called when DJ Mode is actually navigated away from) tears the decks
 * down.
 */
@HiltViewModel
class DjViewModel @Inject constructor(
    val engine: DjPlaybackEngine,
    songRepository: SongRepository,
) : ViewModel() {

    val library: StateFlow<List<Song>> = songRepository.songs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deckAState get() = engine.deckA.state
    val deckBState get() = engine.deckB.state
    val engineState get() = engine.engineState

    init {
        engine.start()
    }

    fun loadSong(deck: DeckId, song: Song) = engine.loadSong(deck, song)
    fun togglePlayPause(deck: DeckId) = engine.deckOf(deck).togglePlayPause()
    fun cue(deck: DeckId) = engine.deckOf(deck).cue()
    fun sync(deck: DeckId) = engine.sync(deck)
    fun setCrossfader(position: Float) = engine.setCrossfader(position)
    fun setSpeed(deck: DeckId, speed: Float) = engine.deckOf(deck).setSpeed(speed)
    fun setGain(deck: DeckId, gain: Float) = engine.setGain(deck, gain)
    fun setLow(deck: DeckId, mb: Int) = engine.deckOf(deck).effects.setLow(mb)
    fun setMid(deck: DeckId, mb: Int) = engine.deckOf(deck).effects.setMid(mb)
    fun setHigh(deck: DeckId, mb: Int) = engine.deckOf(deck).effects.setHigh(mb)
    fun setFilter(deck: DeckId, amount: Float) = engine.deckOf(deck).effects.setFilter(amount)
    fun setReverbPreset(deck: DeckId, preset: Int) = engine.deckOf(deck).effects.setReverbPreset(preset)
    fun onHotCuePress(deck: DeckId, pad: Int) = engine.deckOf(deck).onHotCuePress(pad)
    fun onHotCueLongPress(deck: DeckId, pad: Int) = engine.deckOf(deck).onHotCueLongPress(pad)
    fun setLoopIn(deck: DeckId) = engine.deckOf(deck).setLoopIn()
    fun setLoopOut(deck: DeckId) = engine.deckOf(deck).setLoopOut()
    fun setAutoLoop(deck: DeckId, durationMs: Long) = engine.deckOf(deck).setAutoLoop(durationMs)
    fun toggleLoopActive(deck: DeckId) = engine.deckOf(deck).toggleLoopActive()
    fun exitLoop(deck: DeckId) = engine.deckOf(deck).exitLoop()
    fun nudgeSeek(deck: DeckId, deltaMs: Long) = engine.deckOf(deck).nudgeSeek(deltaMs)
    fun seekTo(deck: DeckId, positionMs: Long) = engine.deckOf(deck).seekTo(positionMs)
    fun triggerSamplerPad(pad: Int) = engine.sampler.trigger(pad)
    fun assignSamplerClip(pad: Int, song: Song) = engine.sampler.assignClip(pad, song)
    fun setAutomixSettings(settings: AutomixSettings) = engine.setAutomixSettings(settings)
    fun clearSyncMessage() = engine.clearSyncMessage()

    fun search(query: String, onResult: (List<Song>) -> Unit) {
        viewModelScope.launch {
            val all = library.value
            onResult(
                if (query.isBlank()) all
                else all.filter {
                    it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true)
                },
            )
        }
    }

    /** Called when DJ Mode is actually closed (not on rotation). */
    fun closeDjMode() {
        engine.release()
    }
}
