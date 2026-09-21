package com.ozin.music

import com.ozin.music.core.data.model.Song
import com.ozin.music.core.domain.dj.AutomixSelector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AutomixSelectorTest {
    private fun song(
        id: Long,
        artist: String,
        genre: String,
        favorite: Boolean = false,
    ) = Song(
        id = id,
        title = "Track $id",
        artist = artist,
        album = "Album",
        albumId = 1L,
        durationMs = 200_000L,
        path = "/song$id.mp3",
        sizeBytes = 1000L,
        year = 2020,
        trackNumber = 1,
        dateAdded = 0L,
        genre = genre,
        isFavorite = favorite,
    )

    @Test
    fun `prefers same genre as current track`() {
        val current = song(1, "Artist A", "House")
        val candidates = listOf(
            song(2, "Artist B", "Jazz"),
            song(3, "Artist C", "House"),
        )
        val picked = AutomixSelector.pickNext(candidates, current, emptyList(), preferFavorites = false)
        assertEquals(3L, picked?.id)
    }

    @Test
    fun `avoids repeating the artist just played when an alternative exists`() {
        val current = song(1, "Artist A", "House")
        val recentlyPlayed = listOf(song(1, "Artist A", "House"))
        val candidates = listOf(
            song(2, "Artist A", "House"),
            song(3, "Artist B", "House"),
        )
        val picked = AutomixSelector.pickNext(candidates, current, recentlyPlayed, preferFavorites = false)
        assertEquals(3L, picked?.id)
    }

    @Test
    fun `falls back to repeat artist if it is the only candidate`() {
        val current = song(1, "Artist A", "House")
        val recentlyPlayed = listOf(song(1, "Artist A", "House"))
        val candidates = listOf(song(2, "Artist A", "House"))
        val picked = AutomixSelector.pickNext(candidates, current, recentlyPlayed, preferFavorites = false)
        assertEquals(2L, picked?.id)
    }

    @Test
    fun `prefers favorites when the setting is on`() {
        val current = song(1, "Artist A", "House")
        val candidates = listOf(
            song(2, "Artist B", "House", favorite = false),
            song(3, "Artist C", "House", favorite = true),
        )
        val picked = AutomixSelector.pickNext(candidates, current, emptyList(), preferFavorites = true)
        assertEquals(3L, picked?.id)
    }

    @Test
    fun `bpm closeness only applied when both known`() {
        val current = song(1, "Artist A", "House")
        val candidates = listOf(
            song(2, "Artist B", "House"),
            song(3, "Artist C", "House"),
        )
        val bpm = mapOf(1L to 120f, 2L to 200f, 3L to 122f)
        val picked = AutomixSelector.pickNext(candidates, current, emptyList(), preferFavorites = false) { bpm[it.id] }
        assertEquals(3L, picked?.id)
    }

    @Test
    fun `empty library returns null`() {
        assertNull(AutomixSelector.pickNext(emptyList(), null, emptyList(), preferFavorites = false))
    }
}
