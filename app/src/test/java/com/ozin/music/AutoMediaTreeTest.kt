package com.ozin.music

import com.ozin.music.core.data.model.Playlist
import com.ozin.music.core.data.model.Song
import com.ozin.music.core.domain.AutoMediaTree
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoMediaTreeTest {

    private fun song(id: Long, title: String = "Song $id") = Song(
        id = id, title = title, artist = "Artist $id", album = "Album", albumId = 1,
        durationMs = 1000, path = "/music/$id.mp3", sizeBytes = 10, year = 2020,
        trackNumber = 1, dateAdded = 0,
    )

    @Test
    fun `root exposes the four fixed categories`() {
        val root = AutoMediaTree.rootChildren()
        assertEquals(
            listOf(
                AutoMediaTree.CATEGORY_SONGS,
                AutoMediaTree.CATEGORY_FAVORITES,
                AutoMediaTree.CATEGORY_RECENT,
                AutoMediaTree.CATEGORY_PLAYLISTS,
            ),
            root.map { it.mediaId },
        )
        assertTrue(root.all { it.browsable && !it.playable })
    }

    @Test
    fun `song nodes are playable with a stable media id`() {
        val nodes = AutoMediaTree.songNodes(listOf(song(1), song(2)))

        assertEquals(listOf("song_1", "song_2"), nodes.map { it.mediaId })
        assertTrue(nodes.all { it.playable && !it.browsable })
        assertEquals("Artist 1", nodes[0].subtitle)
    }

    @Test
    fun `playlist nodes are browsable with a stable media id`() {
        val nodes = AutoMediaTree.playlistNodes(listOf(Playlist(id = 7, name = "Road trip")))

        assertEquals("playlist_7", nodes.single().mediaId)
        assertTrue(nodes.single().browsable && !nodes.single().playable)
    }

    @Test
    fun `songIdOrNull round-trips through songMediaId`() {
        assertEquals(42L, AutoMediaTree.songIdOrNull(AutoMediaTree.songMediaId(42L)))
        assertNull(AutoMediaTree.songIdOrNull("playlist_1"))
        assertNull(AutoMediaTree.songIdOrNull(AutoMediaTree.ROOT_ID))
    }

    @Test
    fun `playlistIdOrNull round-trips through playlistMediaId`() {
        assertEquals(9L, AutoMediaTree.playlistIdOrNull(AutoMediaTree.playlistMediaId(9L)))
        assertNull(AutoMediaTree.playlistIdOrNull("song_1"))
    }

    @Test
    fun `song list is capped at MAX_ITEMS_PER_CATEGORY`() {
        val many = (1..(AutoMediaTree.MAX_ITEMS_PER_CATEGORY + 50)).map { song(it.toLong()) }
        val nodes = AutoMediaTree.songNodes(many)
        assertEquals(AutoMediaTree.MAX_ITEMS_PER_CATEGORY, nodes.size)
    }
}
