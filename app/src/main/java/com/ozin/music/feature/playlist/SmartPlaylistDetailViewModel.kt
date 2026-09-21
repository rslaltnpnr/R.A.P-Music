package com.ozin.music.feature.playlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ozin.music.core.data.model.SmartPlaylist
import com.ozin.music.core.data.model.Song
import com.ozin.music.core.data.repository.SmartPlaylistRepository
import com.ozin.music.core.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class SmartPlaylistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val smartPlaylistRepository: SmartPlaylistRepository,
    private val playerController: PlayerController,
) : ViewModel() {

    val smartPlaylistId: Long = checkNotNull(savedStateHandle.get<Long>("smartPlaylistId"))

    val playlist: StateFlow<SmartPlaylist?> = smartPlaylistRepository.playlists
        .map { list -> list.firstOrNull { it.id == smartPlaylistId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val songs: StateFlow<List<Song>> = playlist
        .flatMapLatest { p -> if (p == null) MutableStateFlow(emptyList()) else smartPlaylistRepository.evaluate(p) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun playAll(startIndex: Int = 0) {
        val current = songs.value
        if (current.isEmpty()) return
        playerController.playSongs(current, startIndex.coerceIn(0, current.size - 1))
    }
}
