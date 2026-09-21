package com.ozin.music.feature.remote

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ozin.music.core.remote.RemoteAudioFile
import com.ozin.music.core.remote.RemoteEntry
import com.ozin.music.core.remote.RemoteFolder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteBrowseScreen(
    onBack: () -> Unit,
    onSongClick: () -> Unit,
    viewModel: RemoteBrowseViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.serverName.ifBlank { "Browse" }) },
                navigationIcon = {
                    IconButton(onClick = { if (state.canGoUp) viewModel.navigateUp() else onBack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                state.currentPath,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val errorMessage = state.error
            when {
                state.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
                errorMessage != null -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                    androidx.compose.material3.Button(onClick = viewModel::retry) { Text("Retry") }
                }
                state.entries.isEmpty() -> Text(
                    "No folders or supported audio files here.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    items(state.entries, key = { it.path }) { entry ->
                        RemoteEntryRow(
                            entry = entry,
                            onClick = {
                                when (entry) {
                                    is RemoteFolder -> viewModel.openFolder(entry)
                                    is RemoteAudioFile -> {
                                        viewModel.playAudioFile(entry)
                                        onSongClick()
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RemoteEntryRow(entry: RemoteEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = if (entry is RemoteFolder) Icons.Filled.Folder else Icons.Filled.AudioFile,
            contentDescription = null,
        )
        Text(entry.name, color = MaterialTheme.colorScheme.onBackground)
    }
}
