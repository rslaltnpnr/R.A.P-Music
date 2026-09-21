package com.ozin.music.core.domain

import com.ozin.music.core.data.model.ListeningEvent
import com.ozin.music.core.data.model.Song
import java.util.Calendar
import java.util.TimeZone

/**
 * A mood/category tag a song can be assigned. These are simple, honest
 * heuristic labels derived from real fields on [Song] (mainly genre) and,
 * where available, real listening-time patterns from [ListeningEvent] rows.
 *
 * This is NOT machine learning and NOT audio analysis (there is no BPM,
 * spectral or waveform data anywhere in this app) - it is a documented
 * keyword/rule table plus a simple listening-time heuristic. It is
 * intentionally simple and explainable, and should never be presented to
 * the user as "AI" or "smart" beyond that plain description.
 */
enum class Mood { ENERGETIC, CALM, SAD, HAPPY, DARK, WORKOUT, NIGHT }

object MoodClassifier {

    /** Genre keyword -> moods it leans towards. Matching is case-insensitive
     * substring matching against [Song.genre], so e.g. "Hard Rock" matches
     * the "rock" rule below. Multiple rules can match and their moods are
     * unioned. This table is a deliberately small, documented starting
     * point, not an exhaustive genre taxonomy. */
    private val genreRules: List<Pair<String, Set<Mood>>> = listOf(
        "rap" to setOf(Mood.ENERGETIC, Mood.WORKOUT),
        "hip hop" to setOf(Mood.ENERGETIC, Mood.WORKOUT),
        "electronic" to setOf(Mood.ENERGETIC, Mood.WORKOUT),
        "dance" to setOf(Mood.ENERGETIC, Mood.HAPPY, Mood.WORKOUT),
        "edm" to setOf(Mood.ENERGETIC, Mood.WORKOUT),
        "techno" to setOf(Mood.ENERGETIC, Mood.WORKOUT, Mood.DARK),
        "metal" to setOf(Mood.ENERGETIC, Mood.DARK, Mood.WORKOUT),
        "punk" to setOf(Mood.ENERGETIC, Mood.DARK),
        "rock" to setOf(Mood.ENERGETIC),
        "pop" to setOf(Mood.HAPPY, Mood.ENERGETIC),
        "classical" to setOf(Mood.CALM),
        "ambient" to setOf(Mood.CALM, Mood.NIGHT),
        "acoustic" to setOf(Mood.CALM),
        "jazz" to setOf(Mood.CALM, Mood.NIGHT),
        "lo-fi" to setOf(Mood.CALM, Mood.NIGHT),
        "lofi" to setOf(Mood.CALM, Mood.NIGHT),
        "sad" to setOf(Mood.SAD),
        "blues" to setOf(Mood.SAD, Mood.CALM),
        "ballad" to setOf(Mood.SAD, Mood.CALM),
        "soul" to setOf(Mood.CALM, Mood.HAPPY),
        "reggae" to setOf(Mood.HAPPY, Mood.CALM),
        "folk" to setOf(Mood.CALM),
    )

    /** A song counts as leaning [Mood.NIGHT] when at least this fraction of
     * its recorded listens started at night (22:00-05:59, device-local time
     * of each play). This is a real signal derived from
     * [ListeningEvent.timestampMs], not a guess. */
    private const val NIGHT_LISTEN_RATIO_THRESHOLD = 0.6
    private const val MIN_EVENTS_FOR_NIGHT_SIGNAL = 3

    /**
     * Computes the mood tags for [song]. [songListeningEvents] should be the
     * subset of listening events for this exact song (pass an empty list to
     * skip the listening-pattern signal and rely on genre alone).
     */
    fun classify(song: Song, songListeningEvents: List<ListeningEvent> = emptyList()): Set<Mood> {
        val tags = mutableSetOf<Mood>()

        val genre = song.genre.lowercase()
        genreRules.forEach { (keyword, moods) ->
            if (genre.contains(keyword)) tags += moods
        }

        if (isNightListener(songListeningEvents)) {
            tags += Mood.NIGHT
        }

        return tags
    }

    private fun isNightListener(events: List<ListeningEvent>): Boolean {
        if (events.size < MIN_EVENTS_FOR_NIGHT_SIGNAL) return false
        val calendar = Calendar.getInstance(TimeZone.getDefault())
        val nightCount = events.count { event ->
            calendar.timeInMillis = event.timestampMs
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            hour >= 22 || hour < 6
        }
        return nightCount.toDouble() / events.size >= NIGHT_LISTEN_RATIO_THRESHOLD
    }
}

/**
 * Hand-rolled, dependency-free encoding of a [Mood] set into the single
 * String column [Song.moodTags] (comma-separated mood names), matching the
 * plain-string storage precedent already used for [Song.genre].
 */
object MoodTagCodec {
    private const val SEPARATOR = ","

    fun encode(moods: Set<Mood>): String = moods.joinToString(SEPARATOR) { it.name }

    fun decode(raw: String): Set<Mood> {
        if (raw.isBlank()) return emptySet()
        return raw.split(SEPARATOR)
            .mapNotNull { runCatching { Mood.valueOf(it.trim()) }.getOrNull() }
            .toSet()
    }
}
