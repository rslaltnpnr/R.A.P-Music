package com.ozin.music.core.domain.dj

import com.ozin.music.core.data.model.Song

/** Pure, honest next-track selection heuristic for Automix. No "AI" claim —
 * it is a small set of explainable rules: prefer the same genre as the
 * currently playing track, avoid repeating the same artist as recently
 * played when an alternative exists, optionally prefer favorites, and only
 * factor in BPM closeness when BPM is actually known for both tracks (via
 * [bpmOf]) since this app has no real BPM-detection pipeline. */
object AutomixSelector {

    /**
     * @param library candidate pool (already excludes the currently loaded
     *   tracks on both decks, which the caller filters out beforehand).
     * @param currentSong the track that is about to end.
     * @param recentlyPlayed most-recent-first history, used to avoid an
     *   immediate artist repeat.
     * @param preferFavorites when true, favorites get a scoring bonus.
     * @param bpmOf best-effort BPM lookup (null when unknown); only used as
     *   a tie-breaker, never a hard requirement.
     */
    fun pickNext(
        library: List<Song>,
        currentSong: Song?,
        recentlyPlayed: List<Song>,
        preferFavorites: Boolean,
        bpmOf: (Song) -> Float? = { null },
    ): Song? {
        if (library.isEmpty()) return null
        val recentArtists = recentlyPlayed.take(3).map { it.artist }.toSet()
        val currentBpm = currentSong?.let(bpmOf)

        fun score(candidate: Song): Double {
            var s = 0.0
            if (currentSong != null && candidate.genre.isNotBlank() &&
                candidate.genre.equals(currentSong.genre, ignoreCase = true)
            ) {
                s += 10.0
            }
            if (candidate.artist in recentArtists) {
                s -= 8.0
            }
            if (preferFavorites && candidate.isFavorite) {
                s += 3.0
            }
            val candidateBpm = bpmOf(candidate)
            if (currentBpm != null && candidateBpm != null && currentBpm > 0f) {
                val diff = kotlin.math.abs(candidateBpm - currentBpm)
                // Closer BPM scores higher; only ever applied when both are known.
                s += (10.0 - diff.coerceAtMost(10.0))
            }
            return s
        }

        // Prefer a candidate that isn't an immediate artist repeat when one exists.
        val nonRepeat = library.filter { it.artist !in recentArtists }
        val pool = if (nonRepeat.isNotEmpty()) nonRepeat else library
        return pool.maxByOrNull { score(it) }
    }
}
