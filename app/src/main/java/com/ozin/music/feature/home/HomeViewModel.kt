package com.ozin.music.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ozin.music.core.data.model.Song
import com.ozin.music.core.data.repository.PlaylistRepository
import com.ozin.music.core.data.repository.SongRepository
import com.ozin.music.core.data.model.Playlist
import com.ozin.music.core.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class HomeUiState(
    val greeting: String = "",
    val recentlyPlayed: List<Song> = emptyList(),
    val recentlyAdded: List<Song> = emptyList(),
    val mostPlayed: List<Song> = emptyList(),
    val favorites: List<Song> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val playlistRepository: PlaylistRepository,
    private val playerController: PlayerController,
) : ViewModel() {

    init {
        viewModelScope.launch { songRepository.rescan() }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        songRepository.recentlyPlayed(10),
        songRepository.recentlyAdded(10),
        songRepository.mostPlayed(10),
        songRepository.favorites,
        playlistRepository.playlists,
    ) { recentlyPlayed, recentlyAdded, mostPlayed, favorites, playlists ->
        HomeUiState(
            greeting = greetingForNow(),
            recentlyPlayed = recentlyPlayed,
            recentlyAdded = recentlyAdded,
            mostPlayed = mostPlayed,
            favorites = favorites,
            playlists = playlists,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun playSong(song: Song, within: List<Song>) {
        val index = within.indexOf(song).coerceAtLeast(0)
        playerController.playSongs(within, index)
    }

    private fun greetingForNow(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 5 -> "Late night listening"
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            hour < 21 -> "Good evening"
            else -> "Good night"
        }
    }
}
