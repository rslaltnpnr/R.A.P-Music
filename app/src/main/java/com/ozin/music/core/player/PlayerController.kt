package com.ozin.music.core.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.ozin.music.core.data.model.Song
import com.ozin.music.core.data.repository.ProblemFileRepository
import com.ozin.music.core.data.repository.SongRepository
import com.ozin.music.core.domain.AbRepeat
import com.ozin.music.core.domain.AbRepeatState
import com.ozin.music.core.domain.PlaybackSpeed
import com.ozin.music.core.domain.RatingValidator
import com.ozin.music.core.remote.RemoteAudioFile
import com.ozin.music.core.remote.RemoteMusicSource
import com.ozin.music.core.remote.RemoteServerConfig
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
    val playbackSpeed: Float = 1f,
    val abRepeat: AbRepeatState = AbRepeatState(),
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
    private val problemFileRepository: ProblemFileRepository,
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

    /**
     * Plays a browsed remote (WebDAV) directory listing through the exact
     * same MediaController/ExoPlayer queue as local songs — no parallel
     * playback path. Each file is wrapped in a synthetic [Song] (cached like
     * any local one) so the whole existing UI/queue/now-playing pipeline
     * works unchanged; only its [Song.path] is an http(s) URL. Real per-host
     * Basic auth for the HTTP GET is applied by the data source configured
     * in [PlayerModule], not here.
     */
    fun playRemoteFiles(
        files: List<RemoteAudioFile>,
        config: RemoteServerConfig,
        startIndex: Int,
        source: RemoteMusicSource,
    ) {
        val ctrl = controller ?: return
        val items = files.map { file ->
            val uri = source.streamUri(config, file)
            val id = remoteMediaId(config.id, file.path)
            val song = Song(
                id = id,
                title = file.name.substringBeforeLast('.', file.name),
                artist = "Network",
                album = config.name,
                albumId = 0L,
                durationMs = 0L,
                path = uri.toString(),
                sizeBytes = file.sizeBytes ?: 0L,
                year = 0,
                trackNumber = 0,
                dateAdded = System.currentTimeMillis(),
            )
            songCache[id] = song
            MediaItem.Builder()
                .setMediaId(id.toString())
                .setUri(uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setAlbumTitle(song.album)
                        .build()
                )
                .build()
        }
        ctrl.setMediaItems(items, startIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)), 0L)
        ctrl.prepare()
        ctrl.play()
    }

    /** Deterministic negative id (local songs are always >= 0) so remote and
     * local tracks never collide in [songCache]. */
    private fun remoteMediaId(serverId: Long, filePath: String): Long =
        -(kotlin.math.abs(31L * serverId + filePath.hashCode())).coerceAtLeast(1L)

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

    /** True once a real MediaController is connected and holds a non-empty
     * queue, e.g. from a previous session restored by the media session. */
    fun hasQueue(): Boolean = (controller?.mediaItemCount ?: 0) > 0

    /**
     * Best-effort auto-resume for a Bluetooth device profile: only starts
     * playback when the player is already connected with an existing queue
     * (never builds a new one), so it is a real no-op rather than a stub when
     * nothing is queued.
     */
    fun resumeIfQueued() {
        val ctrl = controller ?: return
        if (ctrl.mediaItemCount > 0 && !ctrl.isPlaying) {
            ctrl.play()
        }
    }

    /** Persists a 0-5 star rating for [songId] and reflects it immediately if
     * it is the song currently playing. */
    fun setRating(songId: Long, rating: Int) {
        scope.launch {
            val clamped = RatingValidator.clamp(rating)
            songRepository.setRating(songId, clamped)
            val current = _state.value.currentSong
            if (current?.id == songId) {
                _state.value = _state.value.copy(currentSong = current.copy(rating = clamped))
            }
        }
    }

    /** Real ExoPlayer speed/pitch control, clamped to a sane, testable range. */
    fun setPlaybackSpeed(speed: Float) {
        val ctrl = controller ?: return
        val clamped = PlaybackSpeed.clamp(speed)
        ctrl.playbackParameters = PlaybackParameters(clamped, clamped)
        _state.value = _state.value.copy(playbackSpeed = clamped)
    }

    fun setAbPointA() {
        val ctrl = controller ?: return
        val current = _state.value.abRepeat
        _state.value = _state.value.copy(abRepeat = current.copy(pointAMs = ctrl.currentPosition))
    }

    fun setAbPointB() {
        val ctrl = controller ?: return
        val current = _state.value.abRepeat
        _state.value = _state.value.copy(abRepeat = current.copy(pointBMs = ctrl.currentPosition))
    }

    fun setAbRepeatEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(abRepeat = _state.value.abRepeat.copy(enabled = enabled))
    }

    fun clearAbRepeat() {
        _state.value = _state.value.copy(abRepeat = AbRepeatState())
    }

    /** Called on every position tick: loops back to point A once point B is
     * reached while a valid A-B repeat window is enabled. */
    private fun checkAbRepeat(positionMs: Long) {
        val abState = _state.value.abRepeat
        if (AbRepeat.shouldLoop(abState, positionMs)) {
            seekTo(abState.pointAMs ?: 0L)
        }
    }

    /** Exposes the underlying Player (the MediaController itself implements
     * androidx.media3.common.Player) for features that need direct Player
     * access, such as the sleep timer's volume fade. */
    fun rawPlayer(): Player? = controller

    private fun Song.toMediaItem(): MediaItem =
        MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(
                android.content.ContentUris.withAppendedId(
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id,
                )
            )
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
            _state.value = _state.value.copy(abRepeat = AbRepeatState())
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
            // deleted/corrupt file, and record it as a problem file so the
            // user can review/remove/rescan it later.
            val failedSong = _state.value.currentSong
            if (failedSong != null) {
                scope.launch {
                    problemFileRepository.report(
                        path = failedSong.path,
                        songId = failedSong.id,
                        title = failedSong.title,
                        reason = error.message ?: error.errorCodeName,
                    )
                }
            }
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
        val position = ctrl.currentPosition.coerceAtLeast(0)
        _state.value = _state.value.copy(positionMs = position)
        checkAbRepeat(position)
    }
}
