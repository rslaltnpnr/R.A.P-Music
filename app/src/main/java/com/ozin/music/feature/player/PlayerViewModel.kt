package com.ozin.music.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ozin.music.core.data.repository.PlaylistRepository
import com.ozin.music.core.domain.EqAutoSuggest
import com.ozin.music.core.domain.LrcParser
import com.ozin.music.core.domain.LyricLine
import com.ozin.music.core.domain.Mood
import com.ozin.music.core.domain.MoodTagCodec
import com.ozin.music.core.domain.EqPresetId
import com.ozin.music.core.player.PlaybackUiState
import com.ozin.music.core.player.PlayerController
import com.ozin.music.core.settings.AppSettings
import com.ozin.music.core.settings.NowPlayingVisualMode
import com.ozin.music.core.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A pending, dismissible "try this EQ preset?" suggestion for the song
 * currently playing (item 2). Never applied without an explicit user tap on
 * Apply - see [PlayerViewModel.applyEqSuggestion]. */
data class EqSuggestion(val mood: Mood, val presetId: EqPresetId)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    val playerController: PlayerController,
    private val playlistRepository: PlaylistRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val playbackState: StateFlow<PlaybackUiState> = playerController.state

    private val _lyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val lyrics: StateFlow<List<LyricLine>> = _lyrics.asStateFlow()

    val playlists = playlistRepository.playlists.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )

    val settings: StateFlow<AppSettings> = settingsRepository.settings.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings()
    )

    private val _eqSuggestion = MutableStateFlow<EqSuggestion?>(null)
    /** Non-null when Now Playing should show the one-time EQ preset banner
     * for the currently-playing song (item 2). */
    val eqSuggestion: StateFlow<EqSuggestion?> = _eqSuggestion.asStateFlow()

    /** Song id the suggestion was last computed (and shown or intentionally
     * skipped) for, so it is offered at most once per song per app session -
     * an in-memory, not persisted, "one-time" scope (see item 2 in the
     * commit message for exactly what this does and doesn't cover). */
    private var lastEqSuggestionSongId: Long? = null

    fun setVisualMode(mode: NowPlayingVisualMode) {
        viewModelScope.launch { settingsRepository.setNowPlayingVisualMode(mode) }
    }

    init {
        viewModelScope.launch {
            while (true) {
                playerController.tickPosition()
                val currentPath = playbackState.value.currentSong?.path
                if (currentPath != null) {
                    val parsed = LrcParser.findAndParseFor(currentPath) ?: emptyList()
                    if (parsed != _lyrics.value) _lyrics.value = parsed
                }
                delay(500)
            }
        }
        // Item 2: whenever the current song changes, check once whether its
        // real mood tags (MoodClassifier/MoodTagCodec - already computed on
        // Song.moodTags, not invented here) map to an EQ preset different
        // from the one currently active, and if so surface it as a pending
        // suggestion. Runs at most once per song id per session.
        viewModelScope.launch {
            playbackState.collect { state ->
                val song = state.currentSong
                if (song == null) {
                    _eqSuggestion.value = null
                    return@collect
                }
                if (song.id == lastEqSuggestionSongId) return@collect
                lastEqSuggestionSongId = song.id
                val currentSettings = settings.value
                if (!currentSettings.eqSuggestionEnabled) {
                    _eqSuggestion.value = null
                    return@collect
                }
                val moods = MoodTagCodec.decode(song.moodTags)
                val suggestion = EqAutoSuggest.suggestFor(moods)
                _eqSuggestion.value = if (suggestion != null && suggestion.second != currentSettings.eqPreset) {
                    EqSuggestion(suggestion.first, suggestion.second)
                } else {
                    null
                }
            }
        }
    }

    /** Applies the pending EQ suggestion's preset (and turns the EQ on if it
     * was off) - the only path that ever changes the active EQ preset from
     * this banner; it always requires this explicit call, never fires on its
     * own. */
    fun applyEqSuggestion() {
        val suggestion = _eqSuggestion.value ?: return
        viewModelScope.launch {
            settingsRepository.setEqPreset(suggestion.presetId)
            settingsRepository.setEqEnabled(true)
        }
        _eqSuggestion.value = null
    }

    fun dismissEqSuggestion() {
        _eqSuggestion.value = null
    }

    fun togglePlayPause() = playerController.togglePlayPause()
    fun next() = playerController.skipToNext()
    fun previous() = playerController.skipToPrevious()
    fun seekTo(positionMs: Long) = playerController.seekTo(positionMs)
    fun toggleShuffle() = playerController.toggleShuffle()
    fun cycleRepeat() = playerController.cycleRepeat()
    fun toggleFavorite() {
        playbackState.value.currentSong?.let { playerController.toggleFavorite(it.id) }
    }

    fun setRating(songId: Long, rating: Int) = playerController.setRating(songId, rating)

    fun addCurrentToPlaylist(playlistId: Long) {
        val songId = playbackState.value.currentSong?.id ?: return
        viewModelScope.launch { playlistRepository.addSong(playlistId, songId) }
    }

    fun removeFromQueue(index: Int) = playerController.removeFromQueue(index)
    fun moveInQueue(from: Int, to: Int) = playerController.moveInQueue(from, to)

    fun setPlaybackSpeed(speed: Float) = playerController.setPlaybackSpeed(speed)
    fun setAbPointA() = playerController.setAbPointA()
    fun setAbPointB() = playerController.setAbPointB()
    fun setAbRepeatEnabled(enabled: Boolean) = playerController.setAbRepeatEnabled(enabled)
    fun clearAbRepeat() = playerController.clearAbRepeat()
}
