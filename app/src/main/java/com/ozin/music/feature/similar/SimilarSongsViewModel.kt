package com.ozin.music.feature.similar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ozin.music.core.data.model.Song
import com.ozin.music.core.data.repository.PlaylistRepository
import com.ozin.music.core.data.repository.SongRepository
import com.ozin.music.core.domain.SimilaritySeeker
import com.ozin.music.core.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SimilarSongsUiState(
    val target: Song? = null,
    val results: List<Pair<Song, Double>> = emptyList(),
)

/**
 * Backs the "Similar songs" screen: a real, explainable ranking from
 * [SimilaritySeeker] (same genre/artist/album, shared playlists, similar
 * duration/rating) over the actual library - not a recommendation model.
 */
@HiltViewModel
class SimilarSongsViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val playlistRepository: PlaylistRepository,
    private val playerController: PlayerController,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val songId: Long = checkNotNull(savedStateHandle.get<Long>("songId"))

    private val _uiState = MutableStateFlow(SimilarSongsUiState())
    val uiState: StateFlow<SimilarSongsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val target = songRepository.getById(songId) ?: return@launch
            val allSongs = songRepository.songs.first()
            val coOccurrence = playlistRepository.songPlaylistMembership.first()
            val results = SimilaritySeeker.findSimilar(target, allSongs, coOccurrence)
            _uiState.value = SimilarSongsUiState(target = target, results = results)
        }
    }

    fun playSong(song: Song) {
        val within = _uiState.value.results.map { it.first }
        playerController.playSongs(within, within.indexOf(song).coerceAtLeast(0))
    }
}
