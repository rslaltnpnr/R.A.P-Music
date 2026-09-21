package com.ozin.music.core.domain.dj

/** Pure BPM-sync math. Real BPM detection is out of scope for this app (no
 * audio-analysis pipeline exists anywhere in the codebase); this only uses
 * BPM when it is genuinely known, e.g. from an ID3 tag
 * (MediaMetadataRetriever.METADATA_KEY_BEATS_PER_MINUTE). When either deck's
 * BPM is unknown, sync is a real, honest no-op rather than a faked result. */
object DjSync {
    sealed class Result {
        data class NewSpeed(val speed: Float) : Result()
        object BpmUnknown : Result()
    }

    /**
     * Computes the playback speed multiplier that the *target* deck must run
     * at so its effective BPM matches [otherDeckBpm], given the target
     * deck's own original (unsped-up) BPM [thisDeckOriginalBpm].
     */
    fun computeSyncSpeed(thisDeckOriginalBpm: Float?, otherDeckBpm: Float?): Result {
        if (thisDeckOriginalBpm == null || otherDeckBpm == null) return Result.BpmUnknown
        if (thisDeckOriginalBpm <= 0f || otherDeckBpm <= 0f) return Result.BpmUnknown
        val ratio = otherDeckBpm / thisDeckOriginalBpm
        return Result.NewSpeed(ratio.coerceIn(0.5f, 2.0f))
    }
}
