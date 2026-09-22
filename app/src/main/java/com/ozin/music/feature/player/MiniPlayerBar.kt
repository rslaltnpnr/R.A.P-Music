package com.ozin.music.feature.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ozin.music.R
import com.ozin.music.core.data.mediastore.MediaStoreScanner
import com.ozin.music.core.ui.components.WaveformMotif
import com.ozin.music.core.ui.theme.NeonGlowBackdrop

@Composable
fun MiniPlayerBar(onExpand: () -> Unit, viewModel: PlayerViewModel = hiltViewModel()) {
    val state by viewModel.playbackState.collectAsState()
    val song = state.currentSong ?: return
    val context = LocalContext.current
    val scanner = remember(context) { MediaStoreScanner(context) }
    val accent = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .border(1.dp, Color.White.copy(alpha = 0.08f)),
    ) {
        // Very low-opacity waveform texture tied to the current accent,
        // purely decorative chrome behind the glass mini-player surface.
        WaveformMotif(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .align(Alignment.Center),
            barCount = 60,
            color = accent.copy(alpha = 0.08f),
            animated = true,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
            Box(contentAlignment = Alignment.Center) {
                NeonGlowBackdrop(color = accent, size = 40.dp)
                IconButton(onClick = { viewModel.togglePlayPause() }) {
                    AnimatedContent(targetState = state.isPlaying, label = "play_pause") { playing ->
                        Icon(
                            imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = stringResource(R.string.player_play_pause),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            IconButton(onClick = { viewModel.next() }) {
                Icon(Icons.Filled.SkipNext, contentDescription = stringResource(R.string.player_next), tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
