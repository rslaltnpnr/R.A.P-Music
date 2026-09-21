package com.ozin.music.feature.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ozin.music.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    onBack: () -> Unit,
    onSongClick: () -> Unit,
    viewModel: PlaylistDetailViewModel = hiltViewModel(),
) {
    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val selectMode by viewModel.selectMode.collectAsState()
    val selectedSongIds by viewModel.selectedSongIds.collectAsState()
    var showRenameDialog by remember { mutableStateOf(false) }
    var batchResult by remember { mutableStateOf<BatchRemoveResult?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            if (selectMode) {
                TopAppBar(
                    title = { Text(stringResource(R.string.playlist_selected_count_format, selectedSongIds.size)) },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.exitSelectMode() }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.playlist_cancel_selection))
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                coroutineScope.launch { batchResult = viewModel.removeSelected() }
                            },
                            enabled = selectedSongIds.isNotEmpty(),
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.playlist_remove_selected))
                        }
                    },
                )
            } else {
                TopAppBar(
                    title = { Text(playlist?.name ?: "Playlist") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showRenameDialog = true }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Rename")
                        }
                        IconButton(onClick = { viewModel.duplicate() }) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Duplicate")
                        }
                        IconButton(onClick = { if (songs.isNotEmpty()) { viewModel.playAll(); onSongClick() } }) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Play all")
                        }
                        IconButton(onClick = { viewModel.enterSelectMode() }) {
                            Icon(Icons.Filled.Checklist, contentDescription = stringResource(R.string.playlist_select))
                        }
                    },
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (songs.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                Text("No songs in this playlist yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(songs, key = { it.id }) { song ->
                    val index = songs.indexOf(song)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (selectMode) {
                                    viewModel.toggleSongSelected(song)
                                } else {
                                    viewModel.playAll(index); onSongClick()
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(song.title, color = MaterialTheme.colorScheme.onBackground, maxLines = 1)
                            Text(song.artist, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                        if (selectMode) {
                            Checkbox(
                                checked = song.id in selectedSongIds,
                                onCheckedChange = { viewModel.toggleSongSelected(song) },
                            )
                        } else {
                            IconButton(onClick = { viewModel.moveUp(index) }, enabled = index > 0) {
                                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move up")
                            }
                            IconButton(onClick = { viewModel.moveDown(index) }, enabled = index < songs.size - 1) {
                                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move down")
                            }
                            IconButton(onClick = { viewModel.removeSong(song) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    val result = batchResult
    if (result != null) {
        AlertDialog(
            onDismissRequest = { batchResult = null },
            title = { Text(stringResource(R.string.playlist_remove_selected)) },
            text = { Text(stringResource(R.string.playlist_batch_remove_result_format, result.succeeded, result.failed)) },
            confirmButton = {
                TextButton(onClick = { batchResult = null }) { Text(stringResource(R.string.library_ok)) }
            },
        )
    }

    if (showRenameDialog) {
        var name by remember { mutableStateOf(playlist?.name.orEmpty()) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename playlist") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = { viewModel.rename(name); showRenameDialog = false }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            },
        )
    }
}
