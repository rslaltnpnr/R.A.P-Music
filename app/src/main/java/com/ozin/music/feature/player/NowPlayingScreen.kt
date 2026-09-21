package com.ozin.music.feature.player

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.palette.graphics.Palette
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.imageLoader
import coil.request.ImageRequest
import com.ozin.music.R
import com.ozin.music.core.data.mediastore.MediaStoreScanner
import com.ozin.music.core.domain.AudioOutputDetector
import com.ozin.music.core.domain.LrcParser
import com.ozin.music.core.player.RepeatUiMode
import com.ozin.music.core.settings.NowPlayingVisualMode
import com.ozin.music.core.ui.RatingStars
import com.ozin.music.feature.debug.audioOutputLabel
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    onBack: () -> Unit,
    onOpenLyrics: () -> Unit = {},
    onOpenCarMode: () -> Unit = {},
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.playbackState.collectAsState()
    val lyrics by viewModel.lyrics.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val song = state.currentSong
    val context = LocalContext.current
    val scanner = remember(context) { MediaStoreScanner(context) }
    val audioOutputDetector = remember(context) { AudioOutputDetector(context) }
    var accentColor by remember { mutableStateOf(Color(0xFF7C4DFF)) }
    var showQueue by remember { mutableStateOf(false) }
    var showAddToPlaylist by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var audioOutput by remember { mutableStateOf(com.ozin.music.core.domain.AudioOutputKind.THIS_DEVICE) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while (true) {
            audioOutput = audioOutputDetector.currentOutput()
            delay(2_000)
        }
    }

    LaunchedEffect(song?.albumId) {
        if (song == null) return@LaunchedEffect
        scope.launch {
            try {
                val request = ImageRequest.Builder(context)
                    .data(scanner.albumArtUri(song.albumId))
                    .allowHardware(false)
                    .build()
                val result = context.imageLoader.execute(request)
                val bitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                if (bitmap != null) {
                    Palette.from(bitmap).generate { palette ->
                        val swatch = palette?.vibrantSwatch ?: palette?.dominantSwatch
                        if (swatch != null) accentColor = Color(swatch.rgb)
                    }
                }
            } catch (_: Exception) {
                // Missing/corrupt art: keep default accent.
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(accentColor.copy(alpha = 0.35f), Color(0xFF0B0F19))
                )
            )
            .padding(24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.player_collapse), tint = Color.White)
            }
            Row {
                IconButton(onClick = { showQueue = true }) {
                    Icon(Icons.Filled.QueueMusic, contentDescription = stringResource(R.string.player_queue), tint = Color.White)
                }
                Box {
                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.player_more_options), tint = Color.White)
                    }
                    DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.player_lyrics)) },
                            onClick = { showMoreMenu = false; onOpenLyrics() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.player_car_mode)) },
                            onClick = { showMoreMenu = false; onOpenCarMode() },
                        )
                        Text(
                            stringResource(R.string.player_playback_speed),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                        listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { speed ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (state.playbackSpeed == speed) {
                                            stringResource(R.string.player_speed_selected_format, speed)
                                        } else {
                                            stringResource(R.string.player_speed_format, speed)
                                        }
                                    )
                                },
                                onClick = { viewModel.setPlaybackSpeed(speed) },
                            )
                        }
                        Text(
                            stringResource(R.string.player_ab_repeat),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                        DropdownMenuItem(text = { Text(stringResource(R.string.player_set_point_a)) }, onClick = { viewModel.setAbPointA() })
                        DropdownMenuItem(text = { Text(stringResource(R.string.player_set_point_b)) }, onClick = { viewModel.setAbPointB() })
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (state.abRepeat.enabled) {
                                        stringResource(R.string.player_disable_ab_repeat)
                                    } else {
                                        stringResource(R.string.player_enable_ab_repeat)
                                    }
                                )
                            },
                            onClick = { viewModel.setAbRepeatEnabled(!state.abRepeat.enabled) },
                        )
                        DropdownMenuItem(text = { Text(stringResource(R.string.player_clear_ab_repeat)) }, onClick = { viewModel.clearAbRepeat() })
                    }
                }
            }
        }

        if (song == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.player_nothing_playing), color = Color.White)
            }
            return@Column
        }

        NowPlayingModeSwitcher(
            current = settings.nowPlayingVisualMode,
            accentColor = accentColor,
            onSelect = { viewModel.setVisualMode(it) },
        )

        val gesturesEnabled = settings.nowPlayingGesturesEnabled
        val gestureModifier = if (!gesturesEnabled) {
            Modifier
        } else {
            Modifier
                .pointerInput(song.id) {
                    detectTapGestures(
                        onDoubleTap = { viewModel.toggleFavorite() },
                        onLongPress = { showMoreMenu = true },
                    )
                }
                .pointerInput(song.id) {
                    var horizontalAccum = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { horizontalAccum = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            horizontalAccum += dragAmount
                        },
                        onDragEnd = {
                            if (abs(horizontalAccum) > SWIPE_TRIGGER_PX) {
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
                        },
                        onDragEnd = {
                            if (verticalAccum > SWIPE_TRIGGER_PX) onBack()
                        },
                    )
                }
        }

        val defaultArtRequest = ImageRequest.Builder(context)
            .data(scanner.albumArtUri(song.albumId))
            .placeholder(R.drawable.default_artwork)
            .error(R.drawable.default_artwork)
            .build()

        when (settings.nowPlayingVisualMode) {
            NowPlayingVisualMode.DEFAULT -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                        .aspectRatio(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                        .then(gestureModifier),
                ) {
                    Crossfade(targetState = song.albumId, animationSpec = tween(400), label = "artCrossfade") { _ ->
                        Image(
                            painter = rememberAsyncImagePainter(defaultArtRequest),
                            contentDescription = song.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            }
            NowPlayingVisualMode.VINYL -> {
                Box(modifier = gestureModifier) {
                    VinylVisualMode(
                        albumArtUri = scanner.albumArtUri(song.albumId),
                        isPlaying = state.isPlaying,
                        accentColor = accentColor,
                    )
                }
            }
            NowPlayingVisualMode.CASSETTE -> {
                CassetteVisualMode(
                    isPlaying = state.isPlaying,
                    accentColor = accentColor,
                )
            }
            NowPlayingVisualMode.VISUALIZER -> {
                VisualizerVisualMode(
                    songId = song.id,
                    positionMs = state.positionMs,
                    isPlaying = state.isPlaying,
                    accentColor = accentColor,
                )
            }
            NowPlayingVisualMode.FULL_ART -> {
                Box(modifier = gestureModifier) {
                    FullArtVisualMode(
                        albumArtKey = song.albumId,
                        albumArtUri = scanner.albumArtUri(song.albumId),
                    )
                }
            }
            NowPlayingVisualMode.BLUR -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .then(gestureModifier),
                ) {
                    BlurArtBackground(albumArtUri = scanner.albumArtUri(song.albumId)) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Image(
                                painter = rememberAsyncImagePainter(defaultArtRequest),
                                contentDescription = song.title,
                                modifier = Modifier
                                    .fillMaxWidth(0.6f)
                                    .aspectRatio(1f)
                                    .background(Color.Black, RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                }
            }
            NowPlayingVisualMode.MINIMAL -> {
                Box(modifier = gestureModifier) {
                    MinimalVisualMode(albumArtUri = scanner.albumArtUri(song.albumId))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                text = audioOutputLabel(audioOutput),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f),
            )
        }

        Text(song.title, style = MaterialTheme.typography.headlineSmall, color = Color.White)
        Text(
            stringResource(R.string.player_artist_album_format, song.artist, song.album),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
        )
        RatingStars(
            rating = song.rating,
            onRatingChange = { viewModel.setRating(song.id, it) },
            modifier = Modifier.padding(top = 4.dp),
            filledColor = accentColor,
            emptyColor = Color.White.copy(alpha = 0.4f),
        )

        val currentLyric = LrcParser.currentLine(lyrics, state.positionMs)
        if (lyrics.isNotEmpty()) {
            Text(
                text = currentLyric?.text.orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
                color = accentColor,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }

        Slider(
            value = state.positionMs.toFloat().coerceAtMost(state.durationMs.toFloat().coerceAtLeast(1f)),
            onValueChange = { viewModel.seekTo(it.toLong()) },
            valueRange = 0f..state.durationMs.toFloat().coerceAtLeast(1f),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatMs(state.positionMs), color = Color.White.copy(alpha = 0.6f))
            Text(formatMs(state.durationMs), color = Color.White.copy(alpha = 0.6f))
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { viewModel.toggleShuffle() }) {
                Icon(Icons.Filled.Shuffle, contentDescription = stringResource(R.string.player_shuffle), tint = if (state.shuffleEnabled) accentColor else Color.White)
            }
            IconButton(onClick = { viewModel.previous() }) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = stringResource(R.string.player_previous), tint = Color.White)
            }
            IconButton(onClick = { viewModel.togglePlayPause() }, modifier = Modifier.background(Color.White, RoundedCornerShape(50))) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.player_play_pause),
                    tint = Color.Black,
                )
            }
            IconButton(onClick = { viewModel.next() }) {
                Icon(Icons.Filled.SkipNext, contentDescription = stringResource(R.string.player_next), tint = Color.White)
            }
            IconButton(onClick = { viewModel.cycleRepeat() }) {
                Icon(
                    imageVector = if (state.repeatMode == RepeatUiMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                    contentDescription = stringResource(R.string.player_repeat),
                    tint = if (state.repeatMode != RepeatUiMode.OFF) accentColor else Color.White,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = { viewModel.toggleFavorite() }) {
                Icon(
                    imageVector = if (song.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = stringResource(R.string.library_favorite),
                    tint = if (song.isFavorite) accentColor else Color.White,
                )
            }
            IconButton(onClick = { showAddToPlaylist = true }) {
                Icon(Icons.Filled.PlaylistAdd, contentDescription = stringResource(R.string.player_add_to_playlist), tint = Color.White)
            }
        }
    }

    if (showQueue) {
        ModalBottomSheet(onDismissRequest = { showQueue = false }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.player_queue), style = MaterialTheme.typography.titleMedium)
                state.queue.forEachIndexed { index, queuedSong ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = queuedSong.title,
                            color = if (index == state.currentIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                        )
                        IconButton(
                            onClick = { viewModel.moveInQueue(index, index - 1) },
                            enabled = index > 0,
                        ) {
                            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.player_move_up))
                        }
                        IconButton(
                            onClick = { viewModel.moveInQueue(index, index + 1) },
                            enabled = index < state.queue.size - 1,
                        ) {
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.player_move_down))
                        }
                        IconButton(onClick = { viewModel.removeFromQueue(index) }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.player_remove))
                        }
                    }
                }
            }
        }
    }

    if (showAddToPlaylist) {
        ModalBottomSheet(onDismissRequest = { showAddToPlaylist = false }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.player_add_to_playlist), style = MaterialTheme.typography.titleMedium)
                if (playlists.isEmpty()) {
                    Text(stringResource(R.string.player_no_playlists_yet))
                }
                playlists.forEach { playlist ->
                    Text(
                        text = playlist.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .clickable {
                                viewModel.addCurrentToPlaylist(playlist.id)
                                showAddToPlaylist = false
                            },
                    )
                }
            }
        }
    }
}

/** Rough per-drag-event threshold (not total gesture distance) above which a
 * horizontal/vertical drag on the album art is treated as a deliberate
 * swipe (item 7). */
private const val SWIPE_TRIGGER_PX = 150f

private fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
