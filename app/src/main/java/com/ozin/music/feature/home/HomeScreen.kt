package com.ozin.music.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.remember
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ozin.music.core.data.mediastore.MediaStoreScanner
import com.ozin.music.core.data.model.Song
import androidx.compose.ui.platform.LocalContext

@Composable
fun HomeScreen(onSongClick: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Text(
                text = state.greeting,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        if (state.recentlyPlayed.isNotEmpty()) {
            item { HomeSection(title = "Recently played", songs = state.recentlyPlayed) { song -> viewModel.playSong(song, state.recentlyPlayed); onSongClick() } }
        }
        if (state.recentlyAdded.isNotEmpty()) {
            item { HomeSection(title = "Recently added", songs = state.recentlyAdded) { song -> viewModel.playSong(song, state.recentlyAdded); onSongClick() } }
        }
        if (state.mostPlayed.isNotEmpty()) {
            item { HomeSection(title = "Most played", songs = state.mostPlayed) { song -> viewModel.playSong(song, state.mostPlayed); onSongClick() } }
        }
        if (state.favorites.isNotEmpty()) {
            item { HomeSection(title = "Favorites", songs = state.favorites) { song -> viewModel.playSong(song, state.favorites); onSongClick() } }
        }
        if (state.recentlyPlayed.isEmpty() && state.recentlyAdded.isEmpty()) {
            item {
                Text(
                    text = "Your library is empty. Add some music to your device to get started.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HomeSection(title: String, songs: List<Song>, onClick: (Song) -> Unit) {
    val context = LocalContext.current
    val scanner = remember(context) { MediaStoreScanner(context) }
    Column {
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(songs, key = { it.id }) { song ->
                Column(
                    modifier = Modifier
                        .width(120.dp)
                        .clickable { onClick(song) },
                ) {
                    AsyncImage(
                        model = scanner.albumArtUri(song.albumId),
                        contentDescription = song.title,
                        modifier = Modifier
                            .size(120.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                    )
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
