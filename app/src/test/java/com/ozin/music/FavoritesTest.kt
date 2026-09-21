package com.ozin.music

import com.ozin.music.core.data.model.Song
import com.ozin.music.fakes.FakeSongDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@Suppress("UNCHECKED_CAST")
private fun <T> Flow<T>.snapshot(): T = (this as StateFlow<T>).value

class FavoritesTest {

    private lateinit var dao: FakeSongDao

    private fun song(id: Long, favorite: Boolean = false) = Song(
        id = id, title = "Song $id", artist = "Artist", album = "Album", albumId = 1,
        durationMs = 1000, path = "/music/$id.mp3", sizeBytes = 10, year = 2020,
        trackNumber = 1, dateAdded = 0, isFavorite = favorite,
    )

    @Before
    fun setUp() {
        dao = FakeSongDao()
    }

    @Test
    fun `toggling favorite persists and shows up in favorites list`() = runTest {
        dao.upsertAll(listOf(song(1), song(2)))

        dao.setFavorite(1, true)

        assertTrue(dao.getById(1)!!.isFavorite)
        assertEquals(listOf(1L), dao.observeFavorites().snapshot().map { it.id })
    }

    @Test
    fun `toggling favorite off removes it from favorites list`() = runTest {
        dao.upsertAll(listOf(song(1, favorite = true)))

        dao.setFavorite(1, false)

        assertTrue(dao.observeFavorites().snapshot().isEmpty())
        assertEquals(false, dao.getById(1)!!.isFavorite)
    }

    @Test
    fun `recordPlay increments play count and updates last played`() = runTest {
        dao.upsertAll(listOf(song(1)))
        dao.recordPlay(1, 12345L)

        val updated = dao.getById(1)!!
        assertEquals(1, updated.playCount)
        assertEquals(12345L, updated.lastPlayedAt)
    }
}
