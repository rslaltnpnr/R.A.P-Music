package com.ozin.music.core.player

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.ozin.music.core.data.repository.ListeningStatsRepository
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
 * played", and additionally logs a per-event [com.ozin.music.core.data.model.ListeningEvent]
 * (song, timestamp, actual listened duration) that the statistics dashboard
 * derives all of its time-range aggregates from.
 */
@Singleton
class ListeningStatsRecorder @Inject constructor(
    private val songRepository: SongRepository,
    private val listeningStatsRepository: ListeningStatsRepository,
) : Player.Listener {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var lastRecordedSongId: Long? = null

    private var currentSongId: Long? = null
    private var currentStartedAtMs: Long = 0L

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        flushCurrentEvent()
        val songId = mediaItem?.mediaId?.toLongOrNull()
        if (songId != null) {
            record(songId)
            currentSongId = songId
            currentStartedAtMs = System.currentTimeMillis()
        } else {
            currentSongId = null
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_ENDED) {
            flushCurrentEvent()
        }
    }

    private fun record(songId: Long) {
        if (lastRecordedSongId == songId) return
        lastRecordedSongId = songId
        scope.launch {
            runCatching { songRepository.recordPlay(songId) }
        }
    }

    /** Logs a real listening event for whatever song was playing before this
     * transition/end, using how long it was actually listened to. */
    private fun flushCurrentEvent() {
        val songId = currentSongId ?: return
        val elapsed = System.currentTimeMillis() - currentStartedAtMs
        currentSongId = null
        if (elapsed > 0) {
            scope.launch {
                runCatching { listeningStatsRepository.record(songId, elapsed) }
            }
        }
    }
}
