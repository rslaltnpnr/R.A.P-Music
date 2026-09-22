package com.ozin.music.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.remember
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.clip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ozin.music.R
import com.ozin.music.core.data.mediastore.MediaStoreScanner
import com.ozin.music.core.data.model.Song
import com.ozin.music.core.ui.components.SectionHeader
import com.ozin.music.core.ui.components.WaveformMotif
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.ozin.music.core.ui.theme.Spacing
import com.ozin.music.core.ui.theme.neonAmbientWash

@Composable
fun HomeScreen(onSongClick: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val accent = MaterialTheme.colorScheme.primary

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .background(neonAmbientWash(accent))
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.xl),
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Subtle decorative waveform texture behind the greeting,
                // purely ambient chrome (not tied to real audio playback).
                WaveformMotif(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .align(Alignment.CenterEnd),
                    barCount = 28,
                    color = accent.copy(alpha = 0.18f),
                    animated = true,
                )
                Text(
                    text = state.greeting,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.align(Alignment.CenterStart),
                )
            }
        }
        if (state.recentlyPlayed.isNotEmpty()) {
            item { HomeSection(title = stringResource(R.string.home_recently_played), songs = state.recentlyPlayed) { song -> viewModel.playSong(song, state.recentlyPlayed); onSongClick() } }
        }
        if (state.recentlyAdded.isNotEmpty()) {
            item { HomeSection(title = stringResource(R.string.home_recently_added), songs = state.recentlyAdded) { song -> viewModel.playSong(song, state.recentlyAdded); onSongClick() } }
        }
        if (state.mostPlayed.isNotEmpty()) {
            item { HomeSection(title = stringResource(R.string.home_most_played), songs = state.mostPlayed) { song -> viewModel.playSong(song, state.mostPlayed); onSongClick() } }
        }
        if (state.favorites.isNotEmpty()) {
            item { HomeSection(title = stringResource(R.string.home_favorites), songs = state.favorites) { song -> viewModel.playSong(song, state.favorites); onSongClick() } }
        }
        if (state.recentlyPlayed.isEmpty() && state.recentlyAdded.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.home_empty_library),
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
        SectionHeader(title = title)
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(Spacing.xs))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            items(songs, key = { it.id }) { song ->
                Column(
                    modifier = Modifier
                        .width(120.dp)
                        .clickable { onClick(song) },
                ) {
                    AsyncImage(
                        model = scanner.albumArtUri(song.albumId),
                        contentDescription = song.title,
                        placeholder = painterResource(R.drawable.default_artwork),
                        error = painterResource(R.drawable.default_artwork),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(120.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
                            .clip(MaterialTheme.shapes.medium),
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
