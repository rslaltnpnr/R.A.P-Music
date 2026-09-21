package com.ozin.music.core.player

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.ozin.music.core.data.model.Song
import com.ozin.music.core.data.repository.PlaylistRepository
import com.ozin.music.core.data.repository.SongRepository
import com.ozin.music.core.domain.AutoMediaTree
import com.ozin.music.core.domain.AutoNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Every connecting controller (system media UI, Bluetooth headset, Android
 * Auto, lockscreen, notification) gets full playback command access; kept
 * accept-all like before. Also implements the [MediaLibrarySession.Callback]
 * browse methods that expose the app's library to Android Auto (and any
 * other `MediaBrowser` client), routing every browse/play request through
 * the same real [SongRepository]/[PlaylistRepository] the rest of the app
 * uses rather than a parallel data path. Kept on the SAME session/player
 * [PlaybackService] already uses for phone playback (Media3 supports only
 * one active session per player), so Auto simply drives the existing queue.
 */
class OzinLibrarySessionCallback(
    private val songRepository: SongRepository,
    private val playlistRepository: PlaylistRepository,
) : MediaLibrarySession.Callback {

    private val callbackScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private fun <T> ioFuture(block: suspend () -> T): ListenableFuture<T> {
        val future = SettableFuture.create<T>()
        callbackScope.launch(Dispatchers.IO) {
            runCatching { block() }
                .onSuccess { future.set(it) }
                .onFailure { future.setException(it) }
        }
        return future
    }

    override fun onGetLibraryRoot(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: LibraryParams?,
    ): ListenableFuture<LibraryResult<MediaItem>> {
        val rootItem = MediaItem.Builder()
            .setMediaId(AutoMediaTree.ROOT_ID)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setTitle("OZIN Music")
                    .build()
            )
            .build()
        return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
    }

    override fun onGetChildren(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: LibraryParams?,
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = ioFuture {
        val nodes = when (parentId) {
            AutoMediaTree.ROOT_ID -> AutoMediaTree.rootChildren()
            AutoMediaTree.CATEGORY_SONGS -> AutoMediaTree.songNodes(songRepository.songs.first())
            AutoMediaTree.CATEGORY_FAVORITES -> AutoMediaTree.songNodes(songRepository.favorites.first())
            AutoMediaTree.CATEGORY_RECENT -> AutoMediaTree.songNodes(songRepository.recentlyPlayed().first())
            AutoMediaTree.CATEGORY_PLAYLISTS -> AutoMediaTree.playlistNodes(playlistRepository.playlists.first())
            else -> {
                val playlistId = AutoMediaTree.playlistIdOrNull(parentId)
                if (playlistId != null) {
                    AutoMediaTree.songNodes(playlistRepository.songsIn(playlistId).first())
                } else {
                    emptyList()
                }
            }
        }
        LibraryResult.ofItemList(ImmutableList.copyOf(nodes.map { it.toMediaItem() }), params)
    }

    override fun onGetItem(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        mediaId: String,
    ): ListenableFuture<LibraryResult<MediaItem>> = ioFuture {
        val songId = AutoMediaTree.songIdOrNull(mediaId)
        val song = songId?.let { songRepository.getById(it) }
        if (song != null) {
            LibraryResult.ofItem(song.toPlayableMediaItem(), null)
        } else {
            LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
        }
    }

    /**
     * Resolves browse-tree [MediaItem]s (which carry only a media id) into
     * fully playable ones with a real file [android.net.Uri] so tapping a
     * song/queueing a category in Android Auto starts real playback on the
     * app's real player, exactly like a phone tap does.
     */
    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>,
    ): ListenableFuture<MutableList<MediaItem>> = ioFuture {
        mediaItems.mapNotNull { requested ->
            val songId = AutoMediaTree.songIdOrNull(requested.mediaId) ?: return@mapNotNull null
            songRepository.getById(songId)?.toPlayableMediaItem()
        }.toMutableList()
    }

    private fun AutoNode.toMediaItem(): MediaItem =
        MediaItem.Builder()
            .setMediaId(mediaId)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(subtitle)
                    .setIsBrowsable(browsable)
                    .setIsPlayable(playable)
                    .build()
            )
            .build()

    private fun Song.toPlayableMediaItem(): MediaItem =
        MediaItem.Builder()
            .setMediaId(AutoMediaTree.songMediaId(id))
            .setUri(android.net.Uri.fromFile(java.io.File(path)))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build()
            )
            .build()
}
