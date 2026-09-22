package com.ozin.music.feature.player

import android.media.AudioManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ozin.music.R
import com.ozin.music.core.data.mediastore.MediaStoreScanner
import com.ozin.music.core.ui.components.WaveformMotif
import com.ozin.music.core.ui.theme.NeonGlowBackdrop
import kotlin.math.abs

/** Per-drag-event horizontal distance (not total gesture length) above which
 * a swipe on the mini player is treated as deliberate next/previous, mirroring
 * [com.ozin.music.feature.player.NowPlayingScreen]'s own swipe threshold. */
private const val MINI_PLAYER_SWIPE_TRIGGER_PX = 120f

/** Accumulated vertical drag distance that corresponds to one volume step,
 * so dragging up/down smoothly raises/lowers the media stream volume instead
 * of only reacting once per whole gesture. */
private const val VOLUME_DRAG_STEP_PX = 40f

@Composable
fun MiniPlayerBar(onExpand: () -> Unit, viewModel: PlayerViewModel = hiltViewModel()) {
    val state by viewModel.playbackState.collectAsState()
    val song = state.currentSong ?: return
    val context = LocalContext.current
    val scanner = remember(context) { MediaStoreScanner(context) }
    val accent = MaterialTheme.colorScheme.primary
    val audioManager = remember(context) { context.getSystemService(AudioManager::class.java) }

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
                .pointerInput(song.id) {
                    var horizontalAccum = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { horizontalAccum = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            horizontalAccum += dragAmount
                        },
                        onDragEnd = {
                            if (abs(horizontalAccum) > MINI_PLAYER_SWIPE_TRIGGER_PX) {
                                if (horizontalAccum < 0) viewModel.next() else viewModel.previous()
                            }
                        },
                    )
                }
                .pointerInput(song.id) {
                    var verticalAccum = 0f
                    detectVerticalDragGestures(
                        onDragStart = { verticalAccum = 0f },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            verticalAccum += dragAmount
                            while (verticalAccum <= -VOLUME_DRAG_STEP_PX) {
                                audioManager?.adjustStreamVolume(
                                    AudioManager.STREAM_MUSIC,
                                    AudioManager.ADJUST_RAISE,
                                    AudioManager.FLAG_SHOW_UI,
                                )
                                verticalAccum += VOLUME_DRAG_STEP_PX
                            }
                            while (verticalAccum >= VOLUME_DRAG_STEP_PX) {
                                audioManager?.adjustStreamVolume(
                                    AudioManager.STREAM_MUSIC,
                                    AudioManager.ADJUST_LOWER,
                                    AudioManager.FLAG_SHOW_UI,
                                )
                                verticalAccum -= VOLUME_DRAG_STEP_PX
                            }
                        },
                        onDragEnd = { verticalAccum = 0f },
                    )
                }
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
