package com.ozin.music

import com.ozin.music.core.data.model.Song
import com.ozin.music.core.domain.BuiltInSmartPlaylists
import com.ozin.music.core.domain.SmartPlaylistEngine
import com.ozin.music.core.domain.SmartPlaylistRule
import com.ozin.music.core.domain.SmartPlaylistRuleCodec
import com.ozin.music.core.domain.SmartRuleField
import com.ozin.music.core.domain.SmartRuleOperator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val DAY_MS = 24L * 60L * 60L * 1000L

class SmartPlaylistEngineTest {

    private val now = 1_700_000_000_000L

    private fun song(
        id: Long,
        genre: String = "Rock",
        artist: String = "Artist",
        album: String = "Album",
        year: Int = 2020,
        durationMs: Long = 200_000,
        rating: Int = 0,
        favorite: Boolean = false,
        playCount: Int = 0,
        dateAdded: Long = now,
        lastPlayedAt: Long = 0L,
    ) = Song(
        id = id, title = "Song $id", artist = artist, album = album, albumId = 1,
        durationMs = durationMs, path = "/music/$id.mp3", sizeBytes = 10, year = year,
        trackNumber = 1, dateAdded = dateAdded, genre = genre, isFavorite = favorite,
        playCount = playCount, lastPlayedAt = lastPlayedAt, rating = rating,
    )

    @Test
    fun `string field supports equals and contains`() {
        val songs = listOf(song(1, genre = "Jazz"), song(2, genre = "Hard Rock"), song(3, genre = "Pop"))

        val equalsResult = SmartPlaylistEngine.evaluate(
            songs, listOf(SmartPlaylistRule(SmartRuleField.GENRE, SmartRuleOperator.EQUALS, "jazz")), now,
        )
        assertEquals(listOf(1L), equalsResult.map { it.id })

        val containsResult = SmartPlaylistEngine.evaluate(
            songs, listOf(SmartPlaylistRule(SmartRuleField.GENRE, SmartRuleOperator.CONTAINS, "rock")), now,
        )
        assertEquals(listOf(2L), containsResult.map { it.id })
    }

    @Test
    fun `numeric field supports greater-or-equal and less-or-equal`() {
        val songs = listOf(song(1, year = 2000), song(2, year = 2010), song(3, year = 2020))

        val recent = SmartPlaylistEngine.evaluate(
            songs, listOf(SmartPlaylistRule(SmartRuleField.YEAR, SmartRuleOperator.GREATER_OR_EQUAL, "2010")), now,
        )
        assertEquals(listOf(2L, 3L), recent.map { it.id })

        val old = SmartPlaylistEngine.evaluate(
            songs, listOf(SmartPlaylistRule(SmartRuleField.YEAR, SmartRuleOperator.LESS_OR_EQUAL, "2000")), now,
        )
        assertEquals(listOf(1L), old.map { it.id })
    }

    @Test
    fun `rating field matches`() {
        val songs = listOf(song(1, rating = 5), song(2, rating = 2), song(3, rating = 0))
        val fiveStar = SmartPlaylistEngine.evaluate(
            songs, listOf(SmartPlaylistRule(SmartRuleField.RATING, SmartRuleOperator.EQUALS, "5")), now,
        )
        assertEquals(listOf(1L), fiveStar.map { it.id })
    }

    @Test
    fun `favorite field is a boolean equality check`() {
        val songs = listOf(song(1, favorite = true), song(2, favorite = false))
        val favorites = SmartPlaylistEngine.evaluate(
            songs, listOf(SmartPlaylistRule(SmartRuleField.FAVORITE, SmartRuleOperator.EQUALS, "true")), now,
        )
        assertEquals(listOf(1L), favorites.map { it.id })
    }

    @Test
    fun `play count field matches`() {
        val songs = listOf(song(1, playCount = 10), song(2, playCount = 1))
        val popular = SmartPlaylistEngine.evaluate(
            songs, listOf(SmartPlaylistRule(SmartRuleField.PLAY_COUNT, SmartRuleOperator.GREATER_OR_EQUAL, "5")), now,
        )
        assertEquals(listOf(1L), popular.map { it.id })
    }

    @Test
    fun `date added relative day window`() {
        val songs = listOf(
            song(1, dateAdded = now - 5 * DAY_MS),
            song(2, dateAdded = now - 60 * DAY_MS),
        )
        val recent = SmartPlaylistEngine.evaluate(
            songs, listOf(SmartPlaylistRule(SmartRuleField.DATE_ADDED, SmartRuleOperator.GREATER_OR_EQUAL, "30")), now,
        )
        assertEquals(listOf(1L), recent.map { it.id })
    }

    @Test
    fun `date played relative day window, never-played counts as long ago`() {
        val songs = listOf(
            song(1, lastPlayedAt = now - 1 * DAY_MS),
            song(2, lastPlayedAt = 0L),
        )
        val notPlayedRecently = SmartPlaylistEngine.evaluate(
            songs, listOf(SmartPlaylistRule(SmartRuleField.DATE_PLAYED, SmartRuleOperator.LESS_OR_EQUAL, "30")), now,
        )
        assertEquals(listOf(2L), notPlayedRecently.map { it.id })
    }

    @Test
    fun `rules within one smart playlist are AND-combined`() {
        val songs = listOf(
            song(1, favorite = true, playCount = 20),
            song(2, favorite = true, playCount = 1),
            song(3, favorite = false, playCount = 20),
        )
        val result = SmartPlaylistEngine.evaluate(
            songs,
            listOf(
                SmartPlaylistRule(SmartRuleField.FAVORITE, SmartRuleOperator.EQUALS, "true"),
                SmartPlaylistRule(SmartRuleField.PLAY_COUNT, SmartRuleOperator.GREATER_OR_EQUAL, "5"),
            ),
            now,
        )
        assertEquals(listOf(1L), result.map { it.id })
    }

    @Test
    fun `codec round-trips a rule list including escaped separators`() {
        val rules = listOf(
            SmartPlaylistRule(SmartRuleField.ARTIST, SmartRuleOperator.CONTAINS, "AC::DC;; Band"),
            SmartPlaylistRule(SmartRuleField.RATING, SmartRuleOperator.GREATER_OR_EQUAL, "3"),
        )
        val decoded = SmartPlaylistRuleCodec.decode(SmartPlaylistRuleCodec.encode(rules))
        assertEquals(rules, decoded)
    }

    @Test
    fun `built-in recently added returns songs added within 30 days`() {
        val songs = listOf(song(1, dateAdded = now - 10 * DAY_MS), song(2, dateAdded = now - 90 * DAY_MS))
        val result = SmartPlaylistEngine.evaluate(songs, BuiltInSmartPlaylists.recentlyAdded(), now)
        assertEquals(listOf(1L), result.map { it.id })
    }

    @Test
    fun `built-in popular this week requires recent plays and a play count floor`() {
        val songs = listOf(
            song(1, lastPlayedAt = now - 2 * DAY_MS, playCount = 8),
            song(2, lastPlayedAt = now - 2 * DAY_MS, playCount = 1),
            song(3, lastPlayedAt = now - 20 * DAY_MS, playCount = 8),
        )
        val result = SmartPlaylistEngine.evaluate(songs, BuiltInSmartPlaylists.popularThisWeek(), now)
        assertEquals(listOf(1L), result.map { it.id })
    }

    @Test
    fun `built-in neglected favorites requires favorite and stale last-played`() {
        val songs = listOf(
            song(1, favorite = true, lastPlayedAt = now - 90 * DAY_MS),
            song(2, favorite = true, lastPlayedAt = now - 1 * DAY_MS),
            song(3, favorite = false, lastPlayedAt = now - 90 * DAY_MS),
            song(4, favorite = true, lastPlayedAt = 0L),
        )
        val result = SmartPlaylistEngine.evaluate(songs, BuiltInSmartPlaylists.neglectedFavorites(), now)
        assertTrue(result.map { it.id }.containsAll(listOf(1L, 4L)))
        assertEquals(2, result.size)
    }
}
