package com.ozin.music.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ozin.music.core.data.model.Song
import com.ozin.music.core.data.repository.SongRepository
import com.ozin.music.core.domain.SortOrder
import com.ozin.music.core.domain.SongSorter
import com.ozin.music.core.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LibraryTab { SONGS, ALBUMS, ARTISTS, FOLDERS, GENRES, FAVORITES }
enum class ViewMode { LIST, GRID }

data class LibraryUiState(
    val songs: List<Song> = emptyList(),
    val tab: LibraryTab = LibraryTab.SONGS,
    val sortOrder: SortOrder = SortOrder.TITLE_ASC,
    val viewMode: ViewMode = ViewMode.LIST,
    val searchQuery: String = "",
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val playerController: PlayerController,
) : ViewModel() {

    private val tab = MutableStateFlow(LibraryTab.SONGS)
    private val sortOrder = MutableStateFlow(SortOrder.TITLE_ASC)
    private val viewMode = MutableStateFlow(ViewMode.LIST)
    private val searchQuery = MutableStateFlow("")

    val uiState: StateFlow<LibraryUiState> = combine(
        songRepository.songs,
        songRepository.favorites,
        tab,
        sortOrder,
        viewMode,
    ) { all, favorites, currentTab, order, mode ->
        val base = if (currentTab == LibraryTab.FAVORITES) favorites else all
        LibraryUiState(
            songs = SongSorter.sort(base, order),
            tab = currentTab,
            sortOrder = order,
            viewMode = mode,
            searchQuery = searchQuery.value,
        )
    }.combine(searchQuery) { state, query ->
        if (query.isBlank()) state else state.copy(
            songs = state.songs.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.artist.contains(query, ignoreCase = true) ||
                    it.album.contains(query, ignoreCase = true)
            },
            searchQuery = query,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun selectTab(newTab: LibraryTab) { tab.value = newTab }
    fun selectSort(order: SortOrder) { sortOrder.value = order }
    fun toggleViewMode() { viewMode.value = if (viewMode.value == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST }
    fun onSearchQueryChanged(query: String) { searchQuery.value = query }

    fun playSong(song: Song) {
        val within = uiState.value.songs
        playerController.playSongs(within, within.indexOf(song).coerceAtLeast(0))
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch { songRepository.setFavorite(song.id, !song.isFavorite) }
    }

    fun rescan() {
        viewModelScope.launch { songRepository.rescan() }
    }
}
