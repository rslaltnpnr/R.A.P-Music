package com.ozin.music.feature.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Recommend
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ozin.music.R
import com.ozin.music.core.data.mediastore.MediaStoreScanner
import com.ozin.music.core.data.model.Playlist
import com.ozin.music.core.data.model.Song
import com.ozin.music.core.domain.Mood
import com.ozin.music.core.domain.MoodTagCodec
import com.ozin.music.core.domain.SongGroup
import com.ozin.music.core.domain.SortOrder
import com.ozin.music.core.ui.RatingStars
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    onSongClick: () -> Unit,
    onEditSong: (Long) -> Unit = {},
    onManageFile: (Long) -> Unit = {},
    onSimilarSongs: (Long) -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scanner = remember(context) { MediaStoreScanner(context) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    var songForMenu by remember { mutableStateOf<Song?>(null) }
    var showAddToPlaylistSheet by remember { mutableStateOf(false) }
    var batchResult by remember { mutableStateOf<BatchAddResult?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (state.selectMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.library_selected_count_format, state.selectedSongIds.size),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Row {
                    IconButton(
                        onClick = { showAddToPlaylistSheet = true },
                        enabled = state.selectedSongIds.isNotEmpty(),
                    ) {
                        Icon(Icons.Filled.PlaylistAdd, contentDescription = stringResource(R.string.library_add_selected_to_playlist))
                    }
                    IconButton(onClick = viewModel::exitSelectMode) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.library_cancel_selection))
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp, top = 16.dp, bottom = 16.dp),
                    placeholder = { Text(stringResource(R.string.library_search_placeholder)) },
                    singleLine = true,
                )
                IconButton(onClick = { viewModel.enterSelectMode() }) {
                    Icon(Icons.Filled.Checklist, contentDescription = stringResource(R.string.library_select))
                }
            }
        }

        ScrollableTabRow(selectedTabIndex = LibraryTab.entries.indexOf(state.tab)) {
            LibraryTab.entries.forEach { tabValue ->
                Tab(
                    selected = state.tab == tabValue,
                    onClick = { viewModel.selectTab(tabValue) },
                    text = { Text(libraryTabLabel(tabValue)) },
                )
            }
        }

        if (state.tab.isGrouped() && state.selectedGroup == null) {
            // Grouped bucket list (Albums/Artists/Folders/Genres).
            if (state.groups.isEmpty()) {
                Text(
                    text = stringResource(R.string.library_nothing_found),
                    modifier = Modifier.padding(24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn {
                    items(state.groups, key = { it.key }) { group ->
                        GroupRow(group, scanner, onClick = { viewModel.selectGroup(group) })
                    }
                }
            }
            return@Column
        }

        if (state.tab.isGrouped() && state.selectedGroup != null) {
            val group = state.selectedGroup!!
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                IconButton(onClick = { viewModel.clearGroupSelection() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.library_back))
                }
                Column {
                    Text(group.title, color = MaterialTheme.colorScheme.onBackground)
                    Text(group.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box {
                    Text(
                        text = stringResource(R.string.library_sort_format, sortOrderLabel(state.sortOrder)),
                        modifier = Modifier.clickable { sortMenuOpen = true },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                        SortOrder.entries.forEach { order ->
                            DropdownMenuItem(
                                text = { Text(sortOrderLabel(order)) },
                                onClick = { viewModel.selectSort(order); sortMenuOpen = false },
                            )
                        }
                    }
                }
                IconButton(onClick = viewModel::toggleViewMode) {
                    Icon(
                        imageVector = if (state.viewMode == ViewMode.LIST) Icons.Filled.GridView else Icons.Filled.ViewList,
                        contentDescription = stringResource(R.string.library_toggle_view),
                    )
                }
            }
        }

        if (state.songs.isEmpty()) {
            Text(
                text = stringResource(R.string.library_no_songs_found),
                modifier = Modifier.padding(24.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (state.viewMode == ViewMode.LIST) {
            LazyColumn {
                items(state.songs, key = { it.id }) { song ->
                    SongRow(
                        song = song,
                        scanner = scanner,
                        selectMode = state.selectMode,
                        selected = song.id in state.selectedSongIds,
                        onClick = {
                            if (state.selectMode) {
                                viewModel.toggleSongSelected(song)
                            } else {
                                viewModel.playSong(song); onSongClick()
                            }
                        },
                        onFavorite = { viewModel.toggleFavorite(song) },
                        onLongClick = { if (!state.selectMode) songForMenu = song },
                    )
                }
            }
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize()) {
                gridItems(state.songs, key = { it.id }) { song ->
                    Column(
                        modifier = Modifier
                            .padding(8.dp)
                            .combinedClickable(
                                onClick = {
                                    if (state.selectMode) {
                                        viewModel.toggleSongSelected(song)
                                    } else {
                                        viewModel.playSong(song); onSongClick()
                                    }
                                },
                                onLongClick = { if (!state.selectMode) songForMenu = song },
                            ),
                    ) {
                        AsyncImage(
                            model = scanner.albumArtUri(song.albumId),
                            contentDescription = song.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .size(160.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                        )
                        Text(song.title, color = MaterialTheme.colorScheme.onBackground, maxLines = 1)
                        Text(song.artist, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
            }
        }
    }

    val menuSong = songForMenu
    if (menuSong != null) {
        SongActionSheet(
            song = menuSong,
            playlists = state.playlists,
            onDismiss = { songForMenu = null },
            onPlayNext = { viewModel.playNext(menuSong); songForMenu = null },
            onAddToQueue = { viewModel.addToQueue(menuSong); songForMenu = null },
            onAddToPlaylist = { playlistId -> viewModel.addToPlaylist(playlistId, menuSong); songForMenu = null },
            onEditInfo = { onEditSong(menuSong.id); songForMenu = null },
            onManageFile = { onManageFile(menuSong.id); songForMenu = null },
            onSimilarSongs = { onSimilarSongs(menuSong.id); songForMenu = null },
            onRatingChange = { rating -> viewModel.setRating(menuSong, rating) },
        )
    }

    if (showAddToPlaylistSheet) {
        AddToPlaylistSheet(
            playlists = state.playlists,
            onDismiss = { showAddToPlaylistSheet = false },
            onPlaylistChosen = { playlistId ->
                showAddToPlaylistSheet = false
                coroutineScope.launch {
                    batchResult = viewModel.addSelectedToPlaylist(playlistId)
                }
            },
        )
    }

    val result = batchResult
    if (result != null) {
        AlertDialog(
            onDismissRequest = { batchResult = null },
            title = { Text(stringResource(R.string.library_add_to_playlist_title)) },
            text = { Text(stringResource(R.string.library_batch_result_format, result.succeeded, result.failed)) },
            confirmButton = {
                TextButton(onClick = { batchResult = null }) { Text(stringResource(R.string.library_ok)) }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddToPlaylistSheet(
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onPlaylistChosen: (Long) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.library_add_to_playlist_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (playlists.isEmpty()) {
                Text(stringResource(R.string.library_no_playlists_yet), modifier = Modifier.padding(vertical = 8.dp))
            }
            playlists.forEach { playlist ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlaylistChosen(playlist.id) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.PlaylistAdd, contentDescription = null)
                    Text(playlist.name, modifier = Modifier.padding(start = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun GroupRow(group: SongGroup, scanner: MediaStoreScanner, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = scanner.albumArtUri(group.albumId),
            contentDescription = group.title,
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(group.title, color = MaterialTheme.colorScheme.onBackground, maxLines = 1)
            Text(group.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SongRow(
    song: Song,
    scanner: MediaStoreScanner,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onLongClick: () -> Unit,
    selectMode: Boolean = false,
    selected: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = scanner.albumArtUri(song.albumId),
            contentDescription = song.title,
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
        )
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f),
        ) {
            Text(song.title, color = MaterialTheme.colorScheme.onBackground, maxLines = 1)
            Text(song.artist, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            MoodChipRow(song.moodTags)
        }
        if (selectMode) {
            Checkbox(checked = selected, onCheckedChange = { onClick() })
        } else {
            IconButton(onClick = onFavorite) {
                Icon(
                    imageVector = if (song.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = stringResource(R.string.library_favorite),
                    tint = if (song.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SongActionSheet(
    song: Song,
    playlists: List<com.ozin.music.core.data.model.Playlist>,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: (Long) -> Unit,
    onEditInfo: () -> Unit = {},
    onManageFile: () -> Unit = {},
    onSimilarSongs: () -> Unit = {},
    onRatingChange: (Int) -> Unit = {},
) {
    var rating by remember(song.id) { mutableStateOf(song.rating) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(song.title, style = MaterialTheme.typography.titleMedium)
            Text(song.artist, color = MaterialTheme.colorScheme.onSurfaceVariant)
            RatingStars(
                rating = rating,
                onRatingChange = { rating = it; onRatingChange(it) },
                modifier = Modifier.padding(vertical = 8.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onEditInfo)
                    .padding(vertical = 12.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null)
                Text(stringResource(R.string.library_edit_song_info), modifier = Modifier.padding(start = 16.dp))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onManageFile)
                    .padding(vertical = 12.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Folder, contentDescription = null)
                Text(stringResource(R.string.library_file_management), modifier = Modifier.padding(start = 16.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onPlayNext)
                    .padding(vertical = 12.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.SkipNext, contentDescription = null)
                Text(stringResource(R.string.library_play_next), modifier = Modifier.padding(start = 16.dp))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAddToQueue)
                    .padding(vertical = 12.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.QueueMusic, contentDescription = null)
                Text(stringResource(R.string.library_add_to_queue), modifier = Modifier.padding(start = 16.dp))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSimilarSongs)
                    .padding(vertical = 12.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Recommend, contentDescription = null)
                Text(stringResource(R.string.library_similar_songs), modifier = Modifier.padding(start = 16.dp))
            }

            Text(
                stringResource(R.string.library_add_to_playlist),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (playlists.isEmpty()) {
                Text(stringResource(R.string.library_no_playlists_yet), modifier = Modifier.padding(vertical = 8.dp))
            }
            playlists.forEach { playlist ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAddToPlaylist(playlist.id) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.PlaylistAdd, contentDescription = null)
                    Text(playlist.name, modifier = Modifier.padding(start = 16.dp))
                }
            }
        }
    }
}

/**
 * Small chip row showing a song's heuristic mood tags (see MoodClassifier),
 * matching the plain "rounded box + text" tag style used elsewhere for
 * short labels. Renders nothing when no mood tags have been computed yet.
 */
@Composable
private fun MoodChipRow(moodTagsRaw: String) {
    val moods = MoodTagCodec.decode(moodTagsRaw)
    if (moods.isEmpty()) return
    Row(modifier = Modifier.padding(top = 2.dp)) {
        moods.forEach { mood ->
            Box(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = moodLabel(mood),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun moodLabel(mood: Mood): String = when (mood) {
    Mood.ENERGETIC -> stringResource(R.string.library_mood_energetic)
    Mood.CALM -> stringResource(R.string.library_mood_calm)
    Mood.SAD -> stringResource(R.string.library_mood_sad)
    Mood.HAPPY -> stringResource(R.string.library_mood_happy)
    Mood.DARK -> stringResource(R.string.library_mood_dark)
    Mood.WORKOUT -> stringResource(R.string.library_mood_workout)
    Mood.NIGHT -> stringResource(R.string.library_mood_night)
}

@Composable
private fun sortOrderLabel(order: SortOrder): String = when (order) {
    SortOrder.TITLE_ASC -> stringResource(R.string.library_sort_title_asc)
    SortOrder.TITLE_DESC -> stringResource(R.string.library_sort_title_desc)
    SortOrder.DATE_ADDED -> stringResource(R.string.library_sort_date_added)
    SortOrder.MOST_PLAYED -> stringResource(R.string.library_sort_most_played)
    SortOrder.DURATION -> stringResource(R.string.library_sort_duration)
    SortOrder.ARTIST -> stringResource(R.string.library_sort_artist)
    SortOrder.ALBUM -> stringResource(R.string.library_sort_album)
}

@Composable
private fun libraryTabLabel(tab: LibraryTab): String = when (tab) {
    LibraryTab.SONGS -> stringResource(R.string.library_tab_songs)
    LibraryTab.ALBUMS -> stringResource(R.string.library_tab_albums)
    LibraryTab.ARTISTS -> stringResource(R.string.library_tab_artists)
    LibraryTab.FOLDERS -> stringResource(R.string.library_tab_folders)
    LibraryTab.GENRES -> stringResource(R.string.library_tab_genres)
    LibraryTab.FAVORITES -> stringResource(R.string.library_tab_favorites)
}
