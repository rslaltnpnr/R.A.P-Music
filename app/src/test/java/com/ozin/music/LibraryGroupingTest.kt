package com.ozin.music

import com.ozin.music.core.data.model.Song
import com.ozin.music.core.domain.LibraryGrouping
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryGroupingTest {

    private fun song(
        id: Long,
        title: String,
        artist: String,
        album: String,
        albumId: Long,
        path: String,
        genre: String = "Unknown",
        trackNumber: Int = 0,
    ) = Song(
        id = id, title = title, artist = artist, album = album, albumId = albumId,
        durationMs = 1000, path = path, sizeBytes = 10, year = 2020,
        trackNumber = trackNumber, dateAdded = 0, genre = genre,
    )

    private val songs = listOf(
        song(1, "Song A", "Artist X", "Album One", 1, "/music/rock/a.mp3", genre = "Rock", trackNumber = 2),
        song(2, "Song B", "Artist X", "Album One", 1, "/music/rock/b.mp3", genre = "Rock", trackNumber = 1),
        song(3, "Song C", "Artist Y", "Album Two", 2, "/music/pop/c.mp3", genre = "Pop"),
    )

    @Test
    fun `groups by album and sorts songs by track number`() {
        val groups = LibraryGrouping.byAlbum(songs)
        assertEquals(listOf("Album One", "Album Two"), groups.map { it.title })
        val albumOne = groups.first { it.title == "Album One" }
        assertEquals(listOf(2L, 1L), albumOne.songs.map { it.id })
    }

    @Test
    fun `groups by artist`() {
        val groups = LibraryGrouping.byArtist(songs)
        assertEquals(setOf("Artist X", "Artist Y"), groups.map { it.title }.toSet())
        val artistX = groups.first { it.title == "Artist X" }
        assertEquals(2, artistX.songs.size)
    }

    @Test
    fun `groups by folder using parent directory`() {
        val groups = LibraryGrouping.byFolder(songs)
        assertEquals(setOf("/music/rock", "/music/pop"), groups.map { it.subtitle }.toSet())
        val rock = groups.first { it.subtitle == "/music/rock" }
        assertEquals(2, rock.songs.size)
    }

    @Test
    fun `groups by genre`() {
        val groups = LibraryGrouping.byGenre(songs)
        assertEquals(setOf("Rock", "Pop"), groups.map { it.title }.toSet())
        val rock = groups.first { it.title == "Rock" }
        assertEquals(2, rock.songs.size)
    }

    @Test
    fun `unknown values fall back to placeholder titles`() {
        val blank = listOf(song(4, "D", "", "", 0, "/x/d.mp3", genre = ""))
        assertEquals("Unknown Artist", LibraryGrouping.byArtist(blank).first().title)
        assertEquals("Unknown Album", LibraryGrouping.byAlbum(blank).first().title)
        assertEquals("Unknown", LibraryGrouping.byGenre(blank).first().title)
    }
}
