package com.ozin.music.core.data.repository

import com.ozin.music.core.data.local.ProblemFileDao
import com.ozin.music.core.data.model.ProblemFile
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks songs/paths that failed to scan or to play, so the user can review
 * them from a dedicated screen instead of them silently disappearing.
 */
@Singleton
class ProblemFileRepository @Inject constructor(
    private val dao: ProblemFileDao,
) {
    val problemFiles: Flow<List<ProblemFile>> = dao.observeAll()

    suspend fun report(path: String, songId: Long?, title: String, reason: String) {
        dao.upsert(
            ProblemFile(
                path = path,
                songId = songId,
                title = title.ifBlank { path.substringAfterLast('/') },
                reason = reason,
                detectedAt = System.currentTimeMillis(),
            )
        )
    }

    /** Dismiss without deleting anything on disk. */
    suspend fun ignore(path: String) = dao.deleteByPath(path)

    /** Clears the entry once a rescan/read attempt succeeds. */
    suspend fun clear(path: String) = dao.deleteByPath(path)
}
