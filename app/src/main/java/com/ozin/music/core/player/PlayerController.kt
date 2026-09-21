package com.ozin.music.core.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.ozin.music.core.data.model.Song
import com.ozin.music.core.data.repository.SongRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

enum class RepeatUiMode { OFF, ALL, ONE }

data class PlaybackUiState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatUiMode = RepeatUiMode.OFF,
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = -1,
    val isConnected: Boolean = false,
)

/**
 * App-process side connection to [PlaybackService] via a real Media3
 * MediaController. Exposes a single StateFlow the whole UI observes, and
 * translates UI intents (play/pause/seek/skip/reorder/favorite) into real
 * MediaController/ExoPlayer calls.
 */
@Singleton
class PlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songRepository: SongRepository,
) {
    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private var controller: MediaController? = null

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    private val songCache = LinkedHashMap<Long, Song>()

    fun connect() {
        if (controller != null) return
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        future.addListener({
            try {
                controller = future.get()
                controller?.addListener(playerListener)
                _state.value = _state.value.copy(isConnected = true)
                syncFromPlayer()
            } catch (_: Exception) {
                _state.value = _state.value.copy(isConnected = false)
            }
        }, MoreExecutors.directExecutor())
    }

    fun disconnect() {
        controller?.removeListener(playerListener)
        controller?.release()
        controller = null
    }

    fun playSongs(songs: List<Song>, startIndex: Int) {
        val ctrl = controller ?: return
        songs.forEach { songCache[it.id] = it }
        val items = songs.map { it.toMediaItem() }
        ctrl.setMediaItems(items, startIndex, 0L)
        ctrl.prepare()
        ctrl.play()
    }

    fun togglePlayPause() {
        val ctrl = controller ?: return
        if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    fun skipToNext() = controller?.seekToNextMediaItem()
    fun skipToPrevious() = controller?.seekToPreviousMediaItem()

    fun toggleShuffle() {
        val ctrl = controller ?: return
        ctrl.shuffleModeEnabled = !ctrl.shuffleModeEnabled
        syncFromPlayer()
    }

    fun cycleRepeat() {
        val ctrl = controller ?: return
        ctrl.repeatMode = when (ctrl.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        syncFromPlayer()
    }

    fun playNext(song: Song) {
        val ctrl = controller ?: return
        songCache[song.id] = song
        val insertIndex = (ctrl.currentMediaItemIndex + 1).coerceIn(0, ctrl.mediaItemCount)
        ctrl.addMediaItem(insertIndex, song.toMediaItem())
    }

    fun addToQueue(song: Song) {
        val ctrl = controller ?: return
        songCache[song.id] = song
        ctrl.addMediaItem(song.toMediaItem())
    }

    fun removeFromQueue(index: Int) {
        controller?.removeMediaItem(index)
    }

    fun moveInQueue(from: Int, to: Int) {
        controller?.moveMediaItem(from, to)
    }

    fun toggleFavorite(songId: Long) {
        scope.launch {
            val song = songRepository.getById(songId) ?: return@launch
            songRepository.setFavorite(songId, !song.isFavorite)
            val current = _state.value.currentSong
            if (current?.id == songId) {
                _state.value = _state.value.copy(currentSong = current.copy(isFavorite = !song.isFavorite))
            }
        }
    }

    fun currentAudioSessionId(): Int = controller?.let { 0 } ?: 0

    /** Exposes the underlying Player (the MediaController itself implements
     * androidx.media3.common.Player) for features that need direct Player
     * access, such as the sleep timer's volume fade. */
    fun rawPlayer(): Player? = controller

    private fun Song.toMediaItem(): MediaItem =
        MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(android.net.Uri.fromFile(java.io.File(path)))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .build()
            )
            .build()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.value = _state.value.copy(isPlaying = isPlaying)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            syncFromPlayer()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            syncFromPlayer()
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            syncFromPlayer()
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            syncFromPlayer()
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            // Defensive: skip to next item rather than getting stuck on a
            // deleted/corrupt file.
            controller?.seekToNextMediaItem()
        }
    }

    private fun syncFromPlayer() {
        val ctrl = controller ?: return
        val mediaId = ctrl.currentMediaItem?.mediaId?.toLongOrNull()
        val song = mediaId?.let { id -> songCache[id] ?: runCatching {
            kotlinx.coroutines.runBlocking { songRepository.getById(id) }
        }.getOrNull() }

        val queue = (0 until ctrl.mediaItemCount).mapNotNull { index ->
            ctrl.getMediaItemAt(index).mediaId.toLongOrNull()?.let { songCache[it] }
        }

        _state.value = _state.value.copy(
            currentSong = song,
            isPlaying = ctrl.isPlaying,
            positionMs = ctrl.currentPosition.coerceAtLeast(0),
            durationMs = ctrl.duration.coerceAtLeast(0),
            shuffleEnabled = ctrl.shuffleModeEnabled,
            repeatMode = when (ctrl.repeatMode) {
                Player.REPEAT_MODE_ALL -> RepeatUiMode.ALL
                Player.REPEAT_MODE_ONE -> RepeatUiMode.ONE
                else -> RepeatUiMode.OFF
            },
            queue = queue,
            currentIndex = ctrl.currentMediaItemIndex,
        )
    }

    fun tickPosition() {
        val ctrl = controller ?: return
        _state.value = _state.value.copy(positionMs = ctrl.currentPosition.coerceAtLeast(0))
    }
}
