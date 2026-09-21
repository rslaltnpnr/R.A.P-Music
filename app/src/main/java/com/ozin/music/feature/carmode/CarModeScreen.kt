package com.ozin.music.feature.carmode

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberAsyncImagePainter
import com.ozin.music.core.data.mediastore.MediaStoreScanner
import com.ozin.music.feature.player.PlayerViewModel

/**
 * Simplified, large-button, low-clutter Now Playing variant for car head
 * units / distracted-driving use: big touch targets, no dense overflow menu,
 * larger text. Reachable from Now Playing's overflow menu ("Car mode"); the
 * regular Now Playing screen is untouched. The manifest does not lock the
 * app to portrait, so this composable lays out fine when the OS/head unit is
 * in landscape - no manifest change was needed or made.
 */
@Composable
fun CarModeScreen(onExit: () -> Unit, viewModel: PlayerViewModel = hiltViewModel()) {
    val state by viewModel.playbackState.collectAsState()
    val song = state.currentSong
    val context = LocalContext.current
    val scanner = remember(context) { MediaStoreScanner(context) }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F19))
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth(0.4f)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp)),
        ) {
            if (song != null) {
                Image(
                    painter = rememberAsyncImagePainter(scanner.albumArtUri(song.albumId)),
                    contentDescription = song.title,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onExit, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Exit car mode", tint = Color.White)
                }
            }

            Text(
                text = song?.title ?: "Nothing playing",
                color = Color.White,
                fontSize = 36.sp,
                maxLines = 1,
            )
            Text(
                text = song?.artist.orEmpty(),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 24.sp,
                maxLines = 1,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { viewModel.previous() }, modifier = Modifier.size(88.dp)) {
                    Icon(
                        Icons.Filled.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(64.dp),
                    )
                }
                IconButton(
                    onClick = { viewModel.togglePlayPause() },
                    modifier = Modifier
                        .size(120.dp)
                        .background(Color.White, RoundedCornerShape(50)),
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.Black,
                        modifier = Modifier.size(72.dp),
                    )
                }
                IconButton(onClick = { viewModel.next() }, modifier = Modifier.size(88.dp)) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(64.dp),
                    )
                }
            }
        }
    }
}
