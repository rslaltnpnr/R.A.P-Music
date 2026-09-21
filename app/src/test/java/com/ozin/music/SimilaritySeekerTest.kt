package com.ozin.music

import com.ozin.music.core.data.model.Song
import com.ozin.music.core.domain.SimilaritySeeker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimilaritySeekerTest {

    private fun song(
        id: Long,
        artist: String = "Artist",
        album: String = "Album",
        albumId: Long = 1,
        genre: String = "Rock",
        durationMs: Long = 200_000,
        rating: Int = 0,
    ) = Song(
        id = id, title = "Song $id", artist = artist, album = album, albumId = albumId,
        durationMs = durationMs, path = "/music/$id.mp3", sizeBytes = 10, year = 2020,
        trackNumber = 1, dateAdded = 0L, genre = genre, rating = rating,
    )

    @Test
    fun `same album ranks above same artist alone`() {
        val target = song(1, artist = "A", album = "X", albumId = 10, genre = "Rock")
        val sameAlbum = song(2, artist = "Other", album = "X", albumId = 10, genre = "Jazz")
        val sameArtistOnly = song(3, artist = "A", album = "Y", albumId = 20, genre = "Jazz")

        val result = SimilaritySeeker.findSimilar(target, listOf(sameAlbum, sameArtistOnly))
        assertEquals(listOf(2L, 3L), result.map { it.first.id })
        assertTrue(result[0].second > result[1].second)
    }

    @Test
    fun `genre and artist matches outrank an unrelated song`() {
        val target = song(1, artist = "A", album = "X", albumId = 10, genre = "Rock")
        val related = song(2, artist = "A", album = "Z", albumId = 99, genre = "Rock")
        val unrelated = song(3, artist = "B", album = "Q", albumId = 88, genre = "Country", durationMs = 999_000)

        val result = SimilaritySeeker.findSimilar(target, listOf(related, unrelated))
        assertEquals(listOf(2L), result.map { it.first.id })
    }

    @Test
    fun `shared playlist membership contributes to the score`() {
        val target = song(1, artist = "A", album = "X", albumId = 10, genre = "Rock")
        val sharesPlaylist = song(2, artist = "B", album = "Y", albumId = 20, genre = "Country")
        val coOccurrence = mapOf(1L to setOf(100L), 2L to setOf(100L))

        val result = SimilaritySeeker.findSimilar(target, listOf(sharesPlaylist), playlistCoOccurrence = coOccurrence)
        assertEquals(1, result.size)
        assertTrue(result.first().second > 0.0)
    }

    @Test
    fun `target itself is excluded from results`() {
        val target = song(1)
        val result = SimilaritySeeker.findSimilar(target, listOf(target, song(2, artist = "A", album = "X", albumId = 10)))
        assertTrue(result.none { it.first.id == 1L })
    }

    @Test
    fun `ranking is deterministic across repeated calls`() {
        val target = song(1, artist = "A", album = "X", albumId = 10, genre = "Rock")
        val candidates = listOf(
            song(2, artist = "A", album = "X", albumId = 10, genre = "Rock"),
            song(3, artist = "A", album = "Z", albumId = 20, genre = "Rock"),
            song(4, artist = "C", album = "Q", albumId = 30, genre = "Country", durationMs = 999_000),
        )
        val first = SimilaritySeeker.findSimilar(target, candidates)
        val second = SimilaritySeeker.findSimilar(target, candidates)
        assertEquals(first.map { it.first.id }, second.map { it.first.id })
    }

    @Test
    fun `unrelated candidate with zero score is excluded`() {
        val target = song(1, artist = "A", album = "X", albumId = 10, genre = "Rock", durationMs = 100_000, rating = 5)
        val unrelated = song(2, artist = "Z", album = "Y", albumId = 999, genre = "Ambient", durationMs = 999_000, rating = 0)
        val result = SimilaritySeeker.findSimilar(target, listOf(unrelated))
        assertTrue(result.isEmpty())
    }
}
