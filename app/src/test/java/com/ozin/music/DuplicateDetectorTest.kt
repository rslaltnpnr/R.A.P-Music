package com.ozin.music

import com.ozin.music.core.data.model.Song
import com.ozin.music.core.domain.DuplicateDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicateDetectorTest {

    private fun song(
        id: Long,
        title: String = "Song",
        artist: String = "Artist",
        path: String = "/music/$id.mp3",
        durationMs: Long = 200_000,
        sizeBytes: Long = 5_000_000,
    ) = Song(
        id = id, title = title, artist = artist, album = "Album", albumId = 1,
        durationMs = durationMs, path = path, sizeBytes = sizeBytes, year = 2020,
        trackNumber = 1, dateAdded = 0,
    )

    @Test
    fun `same normalized filename is a duplicate regardless of folder`() {
        val a = song(1, path = "/music/rock/track.mp3", durationMs = 100_000, sizeBytes = 1L)
        val b = song(2, path = "/music/pop/Track (1).mp3", durationMs = 999_000, sizeBytes = 2L)
        val groups = DuplicateDetector.findDuplicates(listOf(a, b))
        assertEquals(1, groups.size)
        assertEquals(setOf(1L, 2L), groups.first().songs.map { it.id }.toSet())
    }

    @Test
    fun `same duration and size is a duplicate even with different names`() {
        val a = song(1, path = "/music/a.mp3", durationMs = 180_000, sizeBytes = 4_000_000)
        val b = song(2, path = "/music/completely_different.mp3", durationMs = 180_500, sizeBytes = 4_000_000)
        val groups = DuplicateDetector.findDuplicates(listOf(a, b))
        assertEquals(1, groups.size)
    }

    @Test
    fun `same title and artist within duration tolerance is a duplicate`() {
        val a = song(1, title = "Yesterday", artist = "The Beatles", path = "/a.mp3", durationMs = 125_000, sizeBytes = 1)
        val b = song(2, title = "yesterday", artist = "the beatles", path = "/b.flac", durationMs = 125_800, sizeBytes = 2)
        val groups = DuplicateDetector.findDuplicates(listOf(a, b))
        assertEquals(1, groups.size)
    }

    @Test
    fun `different songs are never grouped`() {
        val a = song(1, title = "A", artist = "X", path = "/a.mp3", durationMs = 100_000, sizeBytes = 1)
        val b = song(2, title = "B", artist = "Y", path = "/b.mp3", durationMs = 400_000, sizeBytes = 2)
        assertTrue(DuplicateDetector.findDuplicates(listOf(a, b)).isEmpty())
    }

    @Test
    fun `single song list never produces groups`() {
        assertTrue(DuplicateDetector.findDuplicates(listOf(song(1))).isEmpty())
    }

    @Test
    fun `three-way duplicate forms a single group`() {
        val a = song(1, path = "/x/song.mp3")
        val b = song(2, path = "/y/song.mp3")
        val c = song(3, path = "/z/song (2).mp3")
        val groups = DuplicateDetector.findDuplicates(listOf(a, b, c))
        assertEquals(1, groups.size)
        assertEquals(3, groups.first().songs.size)
    }
}
