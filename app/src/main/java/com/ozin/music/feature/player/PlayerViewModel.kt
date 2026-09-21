package com.ozin.music.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ozin.music.core.data.repository.PlaylistRepository
import com.ozin.music.core.domain.LrcParser
import com.ozin.music.core.domain.LyricLine
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
