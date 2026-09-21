package com.ozin.music.core.data.repository

import com.ozin.music.core.data.local.PlaylistDao
import com.ozin.music.core.data.model.Playlist
import com.ozin.music.core.data.model.PlaylistSongCrossRef
import com.ozin.music.core.data.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao,
) {
    val playlists: Flow<List<Playlist>> = playlistDao.observePlaylists()

    /** songId -> the set of playlist ids it belongs to, used by
     * [com.ozin.music.core.domain.SimilaritySeeker] as a co-occurrence
     * signal ("these two songs are often kept together"). */
    val songPlaylistMembership: Flow<Map<Long, Set<Long>>> = playlistDao.observeAllCrossRefs().map { crossRefs ->
        crossRefs.groupBy({ it.songId }, { it.playlistId }).mapValues { it.value.toSet() }
    }

    suspend fun create(name: String): Long = playlistDao.insert(Playlist(name = name))

    suspend fun rename(playlist: Playlist, newName: String) =
        playlistDao.update(playlist.copy(name = newName))

    suspend fun delete(playlist: Playlist) = playlistDao.delete(playlist)

    fun songsIn(playlistId: Long): Flow<List<Song>> = playlistDao.observeSongsForPlaylist(playlistId)

    suspend fun addSong(playlistId: Long, songId: Long) {
        val nextPosition = (playlistDao.maxPosition(playlistId) ?: -1) + 1
        playlistDao.addSongCrossRef(PlaylistSongCrossRef(playlistId, songId, nextPosition))
    }

    suspend fun removeSong(playlistId: Long, songId: Long) = playlistDao.removeSong(playlistId, songId)

    suspend fun reorder(playlistId: Long, orderedSongIds: List<Long>) =
        playlistDao.reorder(playlistId, orderedSongIds)

    suspend fun moveSong(playlistId: Long, fromIndex: Int, toIndex: Int) {
        val current = playlistDao.observeSongsForPlaylist(playlistId).first()
        if (fromIndex !in current.indices || toIndex !in current.indices) return
        val mutable = current.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        playlistDao.reorder(playlistId, mutable.map { it.id })
    }

    suspend fun duplicate(playlist: Playlist): Long {
        val newId = playlistDao.insert(Playlist(name = "${playlist.name} (copy)", coverSongId = playlist.coverSongId))
        val songs = playlistDao.observeSongsForPlaylist(playlist.id).first()
        songs.forEachIndexed { index, song ->
            playlistDao.addSongCrossRef(PlaylistSongCrossRef(newId, song.id, index))
        }
        return newId
    }
}
