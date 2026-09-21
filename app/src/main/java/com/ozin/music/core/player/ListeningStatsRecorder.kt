package com.ozin.music.core.player

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.ozin.music.core.data.repository.SongRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Player.Listener that records a "play" the moment a track meaningfully
 * starts (transition into it while actually playing), updating Room's
 * play-count / last-played columns which back "recently played" and "most
 * played".
 */
@Singleton
class ListeningStatsRecorder @Inject constructor(
    private val songRepository: SongRepository,
) : Player.Listener {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var lastRecordedSongId: Long? = null

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        val songId = mediaItem?.mediaId?.toLongOrNull() ?: return
        record(songId)
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_READY) {
            // Covers the very first item, which never fires a transition.
        }
    }

    private fun record(songId: Long) {
        if (lastRecordedSongId == songId) return
        lastRecordedSongId = songId
        scope.launch {
            runCatching { songRepository.recordPlay(songId) }
        }
    }
}
