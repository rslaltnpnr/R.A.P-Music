package com.ozin.music.core.data.repository

import com.ozin.music.core.data.local.ListeningEventDao
import com.ozin.music.core.data.model.ListeningEvent
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListeningStatsRepository @Inject constructor(
    private val listeningEventDao: ListeningEventDao,
) {
    val events: Flow<List<ListeningEvent>> = listeningEventDao.observeAll()

    /** Real DB-level date-range query (see [com.ozin.music.core.data.local.ListeningEventDao.observeInRange])
     * used by the listening recap screen instead of filtering the full
     * event log client-side. */
    fun eventsInRange(startMs: Long, endMs: Long): Flow<List<ListeningEvent>> =
        listeningEventDao.observeInRange(startMs, endMs)

    /** Records one real listening session. Sub-second blips (a skip a moment
     * after transitioning) are not counted as a listen. */
    suspend fun record(songId: Long, durationMs: Long, timestampMs: Long = System.currentTimeMillis()) {
        if (durationMs < 1000L) return
        listeningEventDao.insert(ListeningEvent(songId = songId, timestampMs = timestampMs, durationMs = durationMs))
    }
}
