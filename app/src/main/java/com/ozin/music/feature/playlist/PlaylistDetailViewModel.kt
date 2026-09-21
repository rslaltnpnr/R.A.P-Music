package com.ozin.music.feature.playlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ozin.music.core.data.model.Playlist
import com.ozin.music.core.data.model.Song
import com.ozin.music.core.data.repository.PlaylistRepository
import com.ozin.music.core.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Outcome of a batch "remove selected songs from this playlist" action. */
data class BatchRemoveResult(val succeeded: Int, val failed: Int)

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playlistRepository: PlaylistRepository,
    private val playerController: PlayerController,
) : ViewModel() {

    val playlistId: Long = checkNotNull(savedStateHandle.get<Long>("playlistId"))

    val playlist: StateFlow<Playlist?> = playlistRepository.playlists
        .map { list -> list.firstOrNull { it.id == playlistId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val songs: StateFlow<List<Song>> = playlistRepository.songsIn(playlistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectMode = MutableStateFlow(false)
    val selectMode: StateFlow<Boolean> = _selectMode

    private val _selectedSongIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedSongIds: StateFlow<Set<Long>> = _selectedSongIds

    fun playAll(startIndex: Int = 0) {
        val current = songs.value
        if (current.isEmpty()) return
        playerController.playSongs(current, startIndex.coerceIn(0, current.size - 1))
    }

    fun removeSong(song: Song) {
        viewModelScope.launch { playlistRepository.removeSong(playlistId, song.id) }
    }

    fun moveUp(index: Int) {
        if (index <= 0) return
        viewModelScope.launch { playlistRepository.moveSong(playlistId, index, index - 1) }
    }

    fun moveDown(index: Int) {
        if (index >= songs.value.size - 1) return
        viewModelScope.launch { playlistRepository.moveSong(playlistId, index, index + 1) }
    }

    fun rename(newName: String) {
        if (newName.isBlank()) return
        val current = playlist.value ?: return
        viewModelScope.launch { playlistRepository.rename(current, newName.trim()) }
    }

    fun duplicate() {
        val current = playlist.value ?: return
        viewModelScope.launch { playlistRepository.duplicate(current) }
    }

    fun enterSelectMode() {
        _selectMode.value = true
    }

    fun exitSelectMode() {
        _selectMode.value = false
        _selectedSongIds.value = emptySet()
    }

    fun toggleSongSelected(song: Song) {
        val current = _selectedSongIds.value
        _selectedSongIds.value = if (song.id in current) current - song.id else current + song.id
    }

    /** Removes every currently selected song from this playlist, one at a
     * time so a single failure never aborts the rest of the batch. */
    suspend fun removeSelected(): BatchRemoveResult {
        val ids = _selectedSongIds.value
        var succeeded = 0
        var failed = 0
        for (songId in ids) {
            try {
                playlistRepository.removeSong(playlistId, songId)
                succeeded++
            } catch (_: Exception) {
                failed++
            }
        }
        exitSelectMode()
        return BatchRemoveResult(succeeded, failed)
    }
}
