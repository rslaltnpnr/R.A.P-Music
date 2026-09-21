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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.ViewList
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ozin.music.core.data.mediastore.MediaStoreScanner
import com.ozin.music.core.data.model.Song
import com.ozin.music.core.domain.SongGroup
import com.ozin.music.core.domain.SortOrder

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    onSongClick: () -> Unit,
    onEditSong: (Long) -> Unit = {},
    onManageFile: (Long) -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scanner = remember(context) { MediaStoreScanner(context) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    var songForMenu by remember { mutableStateOf<Song?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = viewModel::onSearchQueryChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Search songs, artists, albums") },
            singleLine = true,
        )

        ScrollableTabRow(selectedTabIndex = LibraryTab.entries.indexOf(state.tab)) {
            LibraryTab.entries.forEach { tabValue ->
                Tab(
                    selected = state.tab == tabValue,
                    onClick = { viewModel.selectTab(tabValue) },
                    text = { Text(tabValue.name.lowercase().replaceFirstChar { it.uppercase() }) },
                )
            }
        }

        if (state.tab.isGrouped() && state.selectedGroup == null) {
            // Grouped bucket list (Albums/Artists/Folders/Genres).
            if (state.groups.isEmpty()) {
                Text(
                    text = "Nothing found.",
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
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
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
                        text = "Sort: ${state.sortOrder.name}",
                        modifier = Modifier.clickable { sortMenuOpen = true },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                        SortOrder.entries.forEach { order ->
                            DropdownMenuItem(
                                text = { Text(order.name) },
                                onClick = { viewModel.selectSort(order); sortMenuOpen = false },
                            )
                        }
                    }
                }
                IconButton(onClick = viewModel::toggleViewMode) {
                    Icon(
                        imageVector = if (state.viewMode == ViewMode.LIST) Icons.Filled.GridView else Icons.Filled.ViewList,
                        contentDescription = "Toggle view",
                    )
                }
            }
        }

        if (state.songs.isEmpty()) {
            Text(
                text = "No songs found.",
                modifier = Modifier.padding(24.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (state.viewMode == ViewMode.LIST) {
            LazyColumn {
                items(state.songs, key = { it.id }) { song ->
                    SongRow(
                        song = song,
                        scanner = scanner,
                        onClick = { viewModel.playSong(song); onSongClick() },
                        onFavorite = { viewModel.toggleFavorite(song) },
                        onLongClick = { songForMenu = song },
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
                                onClick = { viewModel.playSong(song); onSongClick() },
                                onLongClick = { songForMenu = song },
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
        )
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
        }
        IconButton(onClick = onFavorite) {
            Icon(
                imageVector = if (song.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (song.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(song.title, style = MaterialTheme.typography.titleMedium)
            Text(song.artist, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onEditInfo)
                    .padding(vertical = 12.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Icon(androidx.compose.material.icons.Icons.Filled.Edit, contentDescription = null)
                Text("Edit song info", modifier = Modifier.padding(start = 16.dp))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onManageFile)
                    .padding(vertical = 12.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Icon(androidx.compose.material.icons.Icons.Filled.Folder, contentDescription = null)
                Text("File management", modifier = Modifier.padding(start = 16.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onPlayNext)
                    .padding(vertical = 12.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.SkipNext, contentDescription = null)
                Text("Play next", modifier = Modifier.padding(start = 16.dp))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAddToQueue)
                    .padding(vertical = 12.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.QueueMusic, contentDescription = null)
                Text("Add to queue", modifier = Modifier.padding(start = 16.dp))
            }

            Text(
                "Add to playlist",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (playlists.isEmpty()) {
                Text("No playlists yet. Create one from the Lists tab.", modifier = Modifier.padding(vertical = 8.dp))
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
