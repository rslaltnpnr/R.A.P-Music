package com.ozin.music.feature.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ozin.music.core.data.mediastore.MediaStoreScanner

@Composable
fun MiniPlayerBar(onExpand: () -> Unit, viewModel: PlayerViewModel = hiltViewModel()) {
    val state by viewModel.playbackState.collectAsState()
    val song = state.currentSong ?: return
    val context = LocalContext.current
    val scanner = remember(context) { MediaStoreScanner(context) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onExpand)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = scanner.albumArtUri(song.albumId),
            contentDescription = song.title,
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp)),
        )
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(song.title, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            Text(song.artist, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        IconButton(onClick = { viewModel.togglePlayPause() }) {
            AnimatedContent(targetState = state.isPlaying, label = "play_pause") { playing ->
                Icon(
                    imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        IconButton(onClick = { viewModel.next() }) {
            Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}
