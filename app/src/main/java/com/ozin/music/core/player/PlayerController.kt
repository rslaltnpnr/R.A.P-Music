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
import com.ozin.music.core.domain.ArtworkResolver
import com.ozin.music.core.domain.ArtworkSource
import com.ozin.music.core.domain.PlaybackSpeed
import com.ozin.music.core.domain.RatingValidator
import com.ozin.music.core.remote.RemoteAudioFile
import com.ozin.music.core.remote.RemoteMusicSource
import com.ozin.music.core.remote.RemoteServerConfig
import com.ozin.music.core.settings.AppSettings
import com.ozin.music.core.settings.SettingsRepository
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
    /** Which tier resolved artwork for the current song, surfaced to the
     * Debug screen (item 9) straight from the real resolver path. */
    val currentArtworkSource: ArtworkSource? = null,
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
    private val artworkResolver: ArtworkResolver,
    private val settingsRepository: SettingsRepository,
) {
    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private var controller: MediaController? = null

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    private val songCache = LinkedHashMap<Long, Song>()

    /** Latest settings snapshot, kept current by a background collector so
     * artwork/privacy resolution never has to block on a suspend read from
     * the hot media-item-building path. */
    @Volatile private var latestSettings = AppSettings()

    init {
        scope.launch {
            settingsRepository.settings.collect { latestSettings = it }
        }
    }

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
        val safeIndex = startIndex.coerceIn(0, (songs.size - 1).coerceAtLeast(0))
        scope.launch {
            // Fast path: get playback started immediately with lightweight,
            // synchronous artwork (a direct MediaStore URI, which Media3/Coil
            // load lazily — no I/O here) so a big playlist never blocks
            // playback while every song's embedded art is being extracted.
            val fastItems = songs.map { it.toFastMediaItem() }
            ctrl.setMediaItems(fastItems, safeIndex, 0L)
            ctrl.prepare()
            ctrl.play()
            // Then resolve full artwork (embedded ID3 art, quality tiers,
            // privacy enforcement) per song and patch each MediaItem in
            // place, starting with the one actually playing.
            val order = (songs.indices).sortedBy { index -> kotlin.math.abs(index - safeIndex) }
            for (index in order) {
                val ctrlNow = controller ?: return@launch
                if (index >= ctrlNow.mediaItemCount) continue
                val resolved = songs[index].toMediaItem()
                if (index < ctrlNow.mediaItemCount) {
                    ctrlNow.replaceMediaItem(index, resolved)
                }
                if (index == ctrlNow.currentMediaItemIndex) {
                    _state.value = _state.value.copy(currentArtworkSource = lastResolvedSource)
                }
            }
        }
    }

    /** Set by [Song.toMediaItem] right after resolving artwork for the
     * currently-playing song, so [playSongs] can surface it in [state]
     * without a second, parallel resolution pass. */
    @Volatile private var lastResolvedSource: ArtworkSource? = null

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
        ctrl.addMediaItem(insertIndex, song.toFastMediaItem())
        scope.launch {
            val resolved = song.toMediaItem()
            val ctrlNow = controller ?: return@launch
            if (insertIndex < ctrlNow.mediaItemCount) ctrlNow.replaceMediaItem(insertIndex, resolved)
        }
    }

    fun addToQueue(song: Song) {
        val ctrl = controller ?: return
        songCache[song.id] = song
        val insertIndex = ctrl.mediaItemCount
        ctrl.addMediaItem(song.toFastMediaItem())
        scope.launch {
            val resolved = song.toMediaItem()
            val ctrlNow = controller ?: return@launch
            if (insertIndex < ctrlNow.mediaItemCount) ctrlNow.replaceMediaItem(insertIndex, resolved)
        }
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

    /** Fully resolves real artwork (embedded ID3 art, MediaStore album art or
     * the default artwork), quality tier and lock-screen privacy for this
     * song's [MediaItem]. Suspends on I/O — see [ArtworkResolver]. */
    private suspend fun Song.toMediaItem(): MediaItem {
        val settings = latestSettings
        val item = MediaItemFactory.buildMediaItem(
            context = context,
            song = this,
            artworkResolver = artworkResolver,
            settings = settings,
        )
        lastResolvedSource = artworkResolver.resolve(
            song = this,
            quality = settings.artworkQuality,
            privacy = settings.lockScreenPrivacy,
            showArtwork = settings.lockScreenShowArtwork,
        ).source
        return item
    }

    /** Synchronous, no-I/O fallback used to start playback immediately: a
     * plain MediaStore album-art URI (loaded lazily by Media3, same as
     * before this change) rather than the fully resolved artwork. Replaced
     * in place by [toMediaItem] shortly after via [MediaController.replaceMediaItem]. */
    private fun Song.toFastMediaItem(): MediaItem =
        MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(MediaItemFactory.contentUriFor(id))
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
            refreshArtworkSourceForCurrent()
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

    /** Re-resolves (read-only, no MediaItem patching) which artwork tier
     * would apply to the song now playing, so the Debug screen reflects
     * reality after a skip/previous even though [playSongs]'s own patch loop
     * only runs once per queue build. */
    private fun refreshArtworkSourceForCurrent() {
        val song = _state.value.currentSong ?: return
        scope.launch {
            val settings = latestSettings
            val source = artworkResolver.resolve(
                song = song,
                quality = settings.artworkQuality,
                privacy = settings.lockScreenPrivacy,
                showArtwork = settings.lockScreenShowArtwork,
            ).source
            _state.value = _state.value.copy(currentArtworkSource = source)
        }
    }

    fun tickPosition() {
        val ctrl = controller ?: return
        val position = ctrl.currentPosition.coerceAtLeast(0)
        _state.value = _state.value.copy(positionMs = position)
        checkAbRepeat(position)
    }
}
