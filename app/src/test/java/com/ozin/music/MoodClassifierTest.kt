package com.ozin.music

import com.ozin.music.core.data.model.ListeningEvent
import com.ozin.music.core.data.model.Song
import com.ozin.music.core.domain.Mood
import com.ozin.music.core.domain.MoodClassifier
import com.ozin.music.core.domain.MoodTagCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class MoodClassifierTest {

    private fun song(id: Long = 1, genre: String, durationMs: Long = 200_000) = Song(
        id = id, title = "Song $id", artist = "Artist", album = "Album", albumId = 1,
        durationMs = durationMs, path = "/music/$id.mp3", sizeBytes = 10, year = 2020,
        trackNumber = 1, dateAdded = 0L, genre = genre,
    )

    @Test
    fun `rap genre leans energetic and workout`() {
        val moods = MoodClassifier.classify(song(genre = "Rap"))
        assertTrue(moods.contains(Mood.ENERGETIC))
        assertTrue(moods.contains(Mood.WORKOUT))
    }

    @Test
    fun `classical genre leans calm only`() {
        val moods = MoodClassifier.classify(song(genre = "Classical"))
        assertEquals(setOf(Mood.CALM), moods)
    }

    @Test
    fun `blues genre leans sad and calm`() {
        val moods = MoodClassifier.classify(song(genre = "Delta Blues"))
        assertTrue(moods.contains(Mood.SAD))
        assertTrue(moods.contains(Mood.CALM))
    }

    @Test
    fun `unknown genre yields no genre-based tags`() {
        val moods = MoodClassifier.classify(song(genre = "Unknown"))
        assertTrue(moods.isEmpty())
    }

    @Test
    fun `frequent late-night plays add the night tag`() {
        val calendar = Calendar.getInstance(TimeZone.getDefault())
        calendar.set(2024, Calendar.JANUARY, 1, 23, 0, 0)
        val nightTimestamp = calendar.timeInMillis
        val events = List(5) { ListeningEvent(songId = 1, timestampMs = nightTimestamp, durationMs = 60_000) }

        val moods = MoodClassifier.classify(song(genre = "Unknown"), events)
        assertTrue(moods.contains(Mood.NIGHT))
    }

    @Test
    fun `too few listening events do not trigger the night signal`() {
        val calendar = Calendar.getInstance(TimeZone.getDefault())
        calendar.set(2024, Calendar.JANUARY, 1, 23, 0, 0)
        val nightTimestamp = calendar.timeInMillis
        val events = listOf(ListeningEvent(songId = 1, timestampMs = nightTimestamp, durationMs = 60_000))

        val moods = MoodClassifier.classify(song(genre = "Unknown"), events)
        assertTrue(moods.isEmpty())
    }

    @Test
    fun `mood tag codec round-trips`() {
        val moods = setOf(Mood.ENERGETIC, Mood.NIGHT)
        val decoded = MoodTagCodec.decode(MoodTagCodec.encode(moods))
        assertEquals(moods, decoded)
    }

    @Test
    fun `mood tag codec decodes blank as empty set`() {
        assertTrue(MoodTagCodec.decode("").isEmpty())
    }
}
