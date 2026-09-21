package com.ozin.music

import com.ozin.music.core.data.model.Song
import com.ozin.music.core.domain.SortOrder
import com.ozin.music.core.domain.SongSorter
import org.junit.Assert.assertEquals
import org.junit.Test

class SongSorterTest {

    private fun song(id: Long, title: String, artist: String, dateAdded: Long, playCount: Int, durationMs: Long) =
        Song(
            id = id,
            title = title,
            artist = artist,
            album = "Album",
            albumId = 1,
            durationMs = durationMs,
            path = "/music/$title.mp3",
            sizeBytes = 1000,
            year = 2020,
            trackNumber = 1,
            dateAdded = dateAdded,
            playCount = playCount,
        )

    private val songs = listOf(
        song(1, "Banana", "Bob", dateAdded = 100, playCount = 2, durationMs = 200_000),
        song(2, "Apple", "Alice", dateAdded = 300, playCount = 5, durationMs = 100_000),
        song(3, "Cherry", "Carl", dateAdded = 200, playCount = 1, durationMs = 300_000),
    )

    @Test
    fun `sorts title ascending case-insensitively`() {
        val sorted = SongSorter.sort(songs, SortOrder.TITLE_ASC)
        assertEquals(listOf("Apple", "Banana", "Cherry"), sorted.map { it.title })
    }

    @Test
    fun `sorts title descending`() {
        val sorted = SongSorter.sort(songs, SortOrder.TITLE_DESC)
        assertEquals(listOf("Cherry", "Banana", "Apple"), sorted.map { it.title })
    }

    @Test
    fun `sorts by date added descending`() {
        val sorted = SongSorter.sort(songs, SortOrder.DATE_ADDED)
        assertEquals(listOf(2L, 3L, 1L), sorted.map { it.id })
    }

    @Test
    fun `sorts by most played descending`() {
        val sorted = SongSorter.sort(songs, SortOrder.MOST_PLAYED)
        assertEquals(listOf(2L, 1L, 3L), sorted.map { it.id })
    }

    @Test
    fun `sorts by duration descending`() {
        val sorted = SongSorter.sort(songs, SortOrder.DURATION)
        assertEquals(listOf(3L, 1L, 2L), sorted.map { it.id })
    }

    @Test
    fun `sorts by artist ascending`() {
        val sorted = SongSorter.sort(songs, SortOrder.ARTIST)
        assertEquals(listOf("Alice", "Bob", "Carl"), sorted.map { it.artist })
    }
}
