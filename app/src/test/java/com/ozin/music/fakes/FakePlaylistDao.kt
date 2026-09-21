package com.ozin.music.fakes

import com.ozin.music.core.data.local.PlaylistDao
import com.ozin.music.core.data.model.Playlist
import com.ozin.music.core.data.model.PlaylistSongCrossRef
import com.ozin.music.core.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory fake standing in for Room in JVM unit tests (no Robolectric needed). */
class FakePlaylistDao : PlaylistDao {

    private var nextId = 1L
    private val playlists = mutableListOf<Playlist>()
    private val crossRefs = mutableListOf<PlaylistSongCrossRef>()
    private val songsById = mutableMapOf<Long, Song>()

    private val _playlistsFlow = MutableStateFlow<List<Playlist>>(emptyList())

    fun seedSong(song: Song) {
        songsById[song.id] = song
    }

    override fun observePlaylists(): StateFlow<List<Playlist>> = _playlistsFlow.asStateFlow()

    override suspend fun insert(playlist: Playlist): Long {
        val id = if (playlist.id != 0L) playlist.id else nextId++
        playlists += playlist.copy(id = id)
        emit()
        return id
    }

    override suspend fun update(playlist: Playlist) {
        val index = playlists.indexOfFirst { it.id == playlist.id }
        if (index >= 0) playlists[index] = playlist
        emit()
    }

    override suspend fun delete(playlist: Playlist) {
        playlists.removeAll { it.id == playlist.id }
        crossRefs.removeAll { it.playlistId == playlist.id }
        emit()
    }

    override suspend fun clearSongs(playlistId: Long) {
        crossRefs.removeAll { it.playlistId == playlistId }
    }

    override suspend fun addSongCrossRef(crossRef: PlaylistSongCrossRef) {
        crossRefs.removeAll { it.playlistId == crossRef.playlistId && it.songId == crossRef.songId }
        crossRefs += crossRef
    }

    override suspend fun removeSong(playlistId: Long, songId: Long) {
        crossRefs.removeAll { it.playlistId == playlistId && it.songId == songId }
    }

    override suspend fun maxPosition(playlistId: Long): Int? =
        crossRefs.filter { it.playlistId == playlistId }.maxOfOrNull { it.position }

    override fun observeSongsForPlaylist(playlistId: Long) =
        MutableStateFlow(
            crossRefs.filter { it.playlistId == playlistId }
                .sortedBy { it.position }
                .mapNotNull { songsById[it.songId] }
        ).asStateFlow()

    private fun emit() {
        _playlistsFlow.value = playlists.toList()
    }
}
