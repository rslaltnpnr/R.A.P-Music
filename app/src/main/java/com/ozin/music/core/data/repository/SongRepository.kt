package com.ozin.music.core.data.repository

import com.ozin.music.core.data.local.SongDao
import com.ozin.music.core.data.mediastore.MediaStoreScanner
import com.ozin.music.core.data.model.Song
import com.ozin.music.core.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongRepository @Inject constructor(
    private val songDao: SongDao,
    private val scanner: MediaStoreScanner,
    private val settingsRepository: SettingsRepository,
    private val problemFileRepository: ProblemFileRepository,
) {
    val songs: Flow<List<Song>> = songDao.observeAll()
    val favorites: Flow<List<Song>> = songDao.observeFavorites()
    fun recentlyAdded(limit: Int = 25): Flow<List<Song>> = songDao.observeRecentlyAdded(limit)
    fun recentlyPlayed(limit: Int = 25): Flow<List<Song>> = songDao.observeRecentlyPlayed(limit)
    fun mostPlayed(limit: Int = 25): Flow<List<Song>> = songDao.observeMostPlayed(limit)
    fun search(query: String): Flow<List<Song>> = songDao.search(query)

    suspend fun getById(id: Long): Song? = songDao.getById(id)

    /** Re-scans MediaStore, merging in existing favorite/play-count state and
     * dropping rows for files that no longer exist. */
    suspend fun rescan() {
        val excluded = settingsRepository.settings.first().excludedFolders
        val scanned = scanner.scan(excluded) { path, error ->
            if (path != null) {
                kotlinx.coroutines.runBlocking {
                    problemFileRepository.report(
                        path = path,
                        songId = null,
                        title = path.substringAfterLast('/'),
                        reason = error.message ?: error.javaClass.simpleName,
                    )
                }
            }
        }
        val existingIds = songDao.getAllIds().toSet()
        val scannedIds = scanned.map { it.id }.toSet()

        val merged = scanned.map { fresh ->
            val existing = songDao.getById(fresh.id)
            if (existing != null) {
                fresh.copy(
                    isFavorite = existing.isFavorite,
                    playCount = existing.playCount,
                    lastPlayedAt = existing.lastPlayedAt,
                )
            } else {
                fresh
            }
        }
        songDao.upsertAll(merged)

        val staleIds = (existingIds - scannedIds).toList()
        if (staleIds.isNotEmpty()) {
            songDao.deleteByIds(staleIds)
        }
    }

    suspend fun setFavorite(songId: Long, favorite: Boolean) = songDao.setFavorite(songId, favorite)

    /** Writes a metadata edit straight into Room so the UI refreshes without a full rescan. */
    suspend fun applyMetadataEdit(song: Song) = songDao.update(song)

    suspend fun recordPlay(songId: Long) = songDao.recordPlay(songId, System.currentTimeMillis())
}
