package com.ozin.music

import com.ozin.music.core.data.model.Song
import com.ozin.music.core.domain.RatingValidator
import com.ozin.music.fakes.FakeSongDao
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RatingValidatorTest {

    @Test
    fun `values within range pass through unchanged`() {
        for (rating in 0..5) {
            assertEquals(rating, RatingValidator.clamp(rating))
        }
    }

    @Test
    fun `negative values clamp to 0`() {
        assertEquals(0, RatingValidator.clamp(-1))
        assertEquals(0, RatingValidator.clamp(-100))
    }

    @Test
    fun `values above 5 clamp to 5`() {
        assertEquals(5, RatingValidator.clamp(6))
        assertEquals(5, RatingValidator.clamp(999))
    }

    @Test
    fun `rating persists via the dao and survives a re-read`() = runTest {
        val dao = FakeSongDao()
        val song = Song(
            id = 1, title = "Song", artist = "Artist", album = "Album", albumId = 1,
            durationMs = 1000, path = "/music/1.mp3", sizeBytes = 10, year = 2020,
            trackNumber = 1, dateAdded = 0,
        )
        dao.upsertAll(listOf(song))

        dao.setRating(1, RatingValidator.clamp(4))

        assertEquals(4, dao.getById(1)!!.rating)
    }
}
