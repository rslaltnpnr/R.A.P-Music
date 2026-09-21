package com.ozin.music.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ozin.music.core.data.model.Song
import com.ozin.music.core.data.repository.SongRepository
import com.ozin.music.core.domain.QueryIntentParser
import com.ozin.music.core.domain.SmartPlaylistEngine
import com.ozin.music.core.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SmartSearchUiState(
    val query: String = "",
    val results: List<Song> = emptyList(),
    val hasSearched: Boolean = false,
)

/**
 * Backs the "Smart search" screen: a local, offline keyword search over the
 * real library, built on [QueryIntentParser] + the existing
 * [SmartPlaylistEngine]. This is a simple keyword mapper, not natural
 * language understanding - see [QueryIntentParser]'s own doc comment.
 */
@HiltViewModel
class SmartSearchViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val playerController: PlayerController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SmartSearchUiState())
    val uiState: StateFlow<SmartSearchUiState> = _uiState.asStateFlow()

    fun onQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
    }

    fun search() {
        val query = _uiState.value.query
        viewModelScope.launch {
            val allSongs = songRepository.songs.first()
            val intent = QueryIntentParser.parse(query)

            var results = if (intent.rules.isEmpty()) allSongs else SmartPlaylistEngine.evaluate(allSongs, intent.rules)

            if (intent.remainderText.isNotEmpty()) {
                results = results.filter {
                    it.title.contains(intent.remainderText, ignoreCase = true) ||
                        it.artist.contains(intent.remainderText, ignoreCase = true) ||
                        it.album.contains(intent.remainderText, ignoreCase = true)
                }
            }

            if (intent.sortByPlayCountDescending) {
                results = results.sortedByDescending { it.playCount }
            }

            _uiState.value = _uiState.value.copy(results = results, hasSearched = true)
        }
    }

    fun playSong(song: Song) {
        val within = _uiState.value.results
        playerController.playSongs(within, within.indexOf(song).coerceAtLeast(0))
    }
}
