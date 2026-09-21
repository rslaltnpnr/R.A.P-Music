package com.ozin.music.feature.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ozin.music.core.data.model.Playlist
import com.ozin.music.core.data.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
) : ViewModel() {

    val playlists: StateFlow<List<Playlist>> = playlistRepository.playlists.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )

    fun create(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { playlistRepository.create(name.trim()) }
    }

    fun rename(playlist: Playlist, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch { playlistRepository.rename(playlist, newName.trim()) }
    }

    fun delete(playlist: Playlist) {
        viewModelScope.launch { playlistRepository.delete(playlist) }
    }
}
