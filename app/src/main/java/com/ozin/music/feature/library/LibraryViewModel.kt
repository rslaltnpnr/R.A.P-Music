package com.ozin.music.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ozin.music.core.data.model.Playlist
import com.ozin.music.core.data.model.Song
import com.ozin.music.core.data.repository.PlaylistRepository
import com.ozin.music.core.data.repository.SongRepository
import com.ozin.music.core.domain.LibraryGrouping
import com.ozin.music.core.domain.SongGroup
import com.ozin.music.core.domain.SortOrder
import com.ozin.music.core.domain.SongSorter
import com.ozin.music.core.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LibraryTab { SONGS, ALBUMS, ARTISTS, FOLDERS, GENRES, FAVORITES }
enum class ViewMode { LIST, GRID }

/** True for the tabs that show grouped buckets rather than a flat song list. */
fun LibraryTab.isGrouped(): Boolean =
    this == LibraryTab.ALBUMS || this == LibraryTab.ARTISTS || this == LibraryTab.FOLDERS || this == LibraryTab.GENRES

data class LibraryUiState(
    val songs: List<Song> = emptyList(),
    val groups: List<SongGroup> = emptyList(),
    val selectedGroup: SongGroup? = null,
    val tab: LibraryTab = LibraryTab.SONGS,
    val sortOrder: SortOrder = SortOrder.TITLE_ASC,
    val viewMode: ViewMode = ViewMode.LIST,
    val searchQuery: String = "",
    val playlists: List<Playlist> = emptyList(),
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val playlistRepository: PlaylistRepository,
    private val playerController: PlayerController,
) : ViewModel() {

    private val tab = MutableStateFlow(LibraryTab.SONGS)
    private val sortOrder = MutableStateFlow(SortOrder.TITLE_ASC)
    private val viewMode = MutableStateFlow(ViewMode.LIST)
    private val searchQuery = MutableStateFlow("")
    private val selectedGroupKey = MutableStateFlow<String?>(null)

    private data class Filters(
        val tab: LibraryTab,
        val order: SortOrder,
        val mode: ViewMode,
        val query: String,
        val groupKey: String?,
    )

    private val filters = combine(tab, sortOrder, viewMode, searchQuery, selectedGroupKey) { t, o, m, q, g ->
        Filters(t, o, m, q, g)
    }

    val uiState: StateFlow<LibraryUiState> = combine(
        songRepository.songs,
        songRepository.favorites,
        playlistRepository.playlists,
        filters,
    ) { all, favorites, playlists, f ->
        val filtered = if (f.query.isBlank()) all else all.filter {
            it.title.contains(f.query, ignoreCase = true) ||
                it.artist.contains(f.query, ignoreCase = true) ||
                it.album.contains(f.query, ignoreCase = true)
        }

        if (f.tab.isGrouped()) {
            val groups = when (f.tab) {
                LibraryTab.ALBUMS -> LibraryGrouping.byAlbum(filtered)
                LibraryTab.ARTISTS -> LibraryGrouping.byArtist(filtered)
                LibraryTab.FOLDERS -> LibraryGrouping.byFolder(filtered)
                LibraryTab.GENRES -> LibraryGrouping.byGenre(filtered)
                else -> emptyList()
            }
            val selected = groups.firstOrNull { it.key == f.groupKey }
            LibraryUiState(
                songs = selected?.songs.orEmpty(),
                groups = groups,
                selectedGroup = selected,
                tab = f.tab,
                sortOrder = f.order,
                viewMode = f.mode,
                searchQuery = f.query,
                playlists = playlists,
            )
        } else {
            val base = if (f.tab == LibraryTab.FAVORITES) filtered.filter { it.isFavorite } else filtered
            LibraryUiState(
                songs = SongSorter.sort(base, f.order),
                groups = emptyList(),
                selectedGroup = null,
                tab = f.tab,
                sortOrder = f.order,
                viewMode = f.mode,
                searchQuery = f.query,
                playlists = playlists,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun selectTab(newTab: LibraryTab) {
        tab.value = newTab
        selectedGroupKey.value = null
    }

    fun selectGroup(group: SongGroup) { selectedGroupKey.value = group.key }
    fun clearGroupSelection() { selectedGroupKey.value = null }

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

    fun setRating(song: Song, rating: Int) {
        viewModelScope.launch { songRepository.setRating(song.id, rating) }
    }

    fun playNext(song: Song) = playerController.playNext(song)
    fun addToQueue(song: Song) = playerController.addToQueue(song)

    fun addToPlaylist(playlistId: Long, song: Song) {
        viewModelScope.launch { playlistRepository.addSong(playlistId, song.id) }
    }

    fun rescan() {
        viewModelScope.launch { songRepository.rescan() }
    }
}
