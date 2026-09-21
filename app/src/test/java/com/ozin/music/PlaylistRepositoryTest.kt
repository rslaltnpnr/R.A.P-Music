package com.ozin.music

import com.ozin.music.core.data.model.Song
import com.ozin.music.core.data.repository.PlaylistRepository
import com.ozin.music.fakes.FakePlaylistDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@Suppress("UNCHECKED_CAST")
private fun <T> Flow<T>.snapshot(): T = (this as StateFlow<T>).value

class PlaylistRepositoryTest {

    private lateinit var dao: FakePlaylistDao
    private lateinit var repository: PlaylistRepository

    private fun song(id: Long) = Song(
        id = id, title = "Song $id", artist = "Artist", album = "Album", albumId = 1,
        durationMs = 1000, path = "/music/$id.mp3", sizeBytes = 10, year = 2020,
        trackNumber = 1, dateAdded = 0,
    )

    @Before
    fun setUp() {
        dao = FakePlaylistDao()
        repository = PlaylistRepository(dao)
        dao.seedSong(song(1))
        dao.seedSong(song(2))
        dao.seedSong(song(3))
    }

    @Test
    fun `create adds a playlist`() = runTest {
        val id = repository.create("Road trip")
        assertEquals(listOf("Road trip"), repository.playlists.snapshot().map { it.name })
        assertTrue(id > 0)
    }

    @Test
    fun `add and remove songs`() = runTest {
        val id = repository.create("Chill")
        repository.addSong(id, 1)
        repository.addSong(id, 2)
        assertEquals(listOf(1L, 2L), repository.songsIn(id).snapshot().map { it.id })

        repository.removeSong(id, 1)
        assertEquals(listOf(2L), repository.songsIn(id).snapshot().map { it.id })
    }

    @Test
    fun `reorder changes playback order`() = runTest {
        val id = repository.create("Mix")
        repository.addSong(id, 1)
        repository.addSong(id, 2)
        repository.addSong(id, 3)

        repository.reorder(id, listOf(3, 1, 2))
        assertEquals(listOf(3L, 1L, 2L), repository.songsIn(id).snapshot().map { it.id })
    }

    @Test
    fun `rename updates playlist name`() = runTest {
        val id = repository.create("Old name")
        val playlist = repository.playlists.snapshot().first { it.id == id }
        repository.rename(playlist, "New name")
        assertEquals("New name", repository.playlists.snapshot().first { it.id == id }.name)
    }

    @Test
    fun `delete removes playlist and its songs`() = runTest {
        val id = repository.create("Temp")
        repository.addSong(id, 1)
        val playlist = repository.playlists.snapshot().first { it.id == id }
        repository.delete(playlist)
        assertTrue(repository.playlists.snapshot().none { it.id == id })
    }

}
