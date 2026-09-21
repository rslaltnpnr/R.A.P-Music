package com.ozin.music.core.domain

import com.ozin.music.core.data.model.Song
import kotlin.math.abs

/**
 * "Similar songs": a pure, explainable weighted-scoring heuristic - not
 * machine learning, not audio analysis. Every point of the score is
 * traceable to a real, documented rule below.
 */
object SimilaritySeeker {

    private const val SAME_ALBUM_SCORE = 50.0
    private const val SAME_ARTIST_SCORE = 30.0
    private const val SAME_GENRE_SCORE = 20.0
    private const val SHARED_PLAYLIST_SCORE_PER_PLAYLIST = 8.0
    private const val SHARED_PLAYLIST_SCORE_CAP = 24.0
    private const val SIMILAR_DURATION_SCORE = 6.0
    private const val SIMILAR_DURATION_WINDOW_MS = 20_000L
    private const val SIMILAR_RATING_SCORE = 4.0

    /**
     * Scores every candidate against [target] and returns the [limit]
     * highest-scoring ones with their raw scores, highest first. Ties break
     * by song id ascending so the ordering is deterministic. [target] itself
     * is always excluded from the results.
     *
     * [playlistCoOccurrence] maps a song id to the set of playlist ids it
     * belongs to (from [com.ozin.music.core.data.repository.PlaylistRepository]);
     * pass an empty map to skip that signal.
     */
    fun findSimilar(
        target: Song,
        candidates: List<Song>,
        playlistCoOccurrence: Map<Long, Set<Long>> = emptyMap(),
        limit: Int = 20,
    ): List<Pair<Song, Double>> {
        val targetPlaylists = playlistCoOccurrence[target.id].orEmpty()
        return candidates
            .asSequence()
            .filter { it.id != target.id }
            .map { candidate -> candidate to score(target, candidate, targetPlaylists, playlistCoOccurrence[candidate.id].orEmpty()) }
            .filter { (_, s) -> s > 0.0 }
            .sortedWith(compareByDescending<Pair<Song, Double>> { it.second }.thenBy { it.first.id })
            .take(limit)
            .toList()
    }

    private fun score(
        target: Song,
        candidate: Song,
        targetPlaylists: Set<Long>,
        candidatePlaylists: Set<Long>,
    ): Double {
        var score = 0.0

        if (target.albumId != 0L && target.albumId == candidate.albumId) {
            score += SAME_ALBUM_SCORE
        } else if (target.album.isNotBlank() && target.album.equals(candidate.album, ignoreCase = true)) {
            score += SAME_ALBUM_SCORE
        }

        if (target.artist.isNotBlank() && target.artist.equals(candidate.artist, ignoreCase = true)) {
            score += SAME_ARTIST_SCORE
        }

        if (target.genre.isNotBlank() && target.genre.equals(candidate.genre, ignoreCase = true)) {
            score += SAME_GENRE_SCORE
        }

        val sharedPlaylists = targetPlaylists.intersect(candidatePlaylists).size
        if (sharedPlaylists > 0) {
            score += (sharedPlaylists * SHARED_PLAYLIST_SCORE_PER_PLAYLIST).coerceAtMost(SHARED_PLAYLIST_SCORE_CAP)
        }

        if (abs(target.durationMs - candidate.durationMs) <= SIMILAR_DURATION_WINDOW_MS) {
            score += SIMILAR_DURATION_SCORE
        }

        if (target.rating > 0 && candidate.rating > 0 && abs(target.rating - candidate.rating) <= 1) {
            score += SIMILAR_RATING_SCORE
        }

        return score
    }
}
