package com.ozin.music.core.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ozin.music.core.data.model.Playlist
import com.ozin.music.core.data.model.PlaylistSongCrossRef
import com.ozin.music.core.data.model.PlaylistWithSongs
import com.ozin.music.core.data.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun observePlaylists(): Flow<List<Playlist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: Playlist): Long

    @Update
    suspend fun update(playlist: Playlist)

    @Delete
    suspend fun delete(playlist: Playlist)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun clearSongs(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongCrossRef(crossRef: PlaylistSongCrossRef)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSong(playlistId: Long, songId: Long)

    @Query(
        "SELECT MAX(position) FROM playlist_songs WHERE playlistId = :playlistId"
    )
    suspend fun maxPosition(playlistId: Long): Int?

    @Query(
        "SELECT songs.* FROM songs INNER JOIN playlist_songs " +
            "ON songs.id = playlist_songs.songId " +
            "WHERE playlist_songs.playlistId = :playlistId " +
            "ORDER BY playlist_songs.position ASC"
    )
    fun observeSongsForPlaylist(playlistId: Long): Flow<List<Song>>

    @Transaction
    suspend fun reorder(playlistId: Long, orderedSongIds: List<Long>) {
        clearSongs(playlistId)
        orderedSongIds.forEachIndexed { index, songId ->
            addSongCrossRef(PlaylistSongCrossRef(playlistId, songId, index))
        }
    }

    fun observePlaylistWithSongs(playlist: Playlist): Flow<PlaylistWithSongs> =
        observeSongsForPlaylist(playlist.id).map { PlaylistWithSongs(playlist, it) }
}
