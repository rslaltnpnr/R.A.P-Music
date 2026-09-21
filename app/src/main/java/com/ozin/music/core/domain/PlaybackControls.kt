package com.ozin.music.core.domain

/** Pure playback-speed/pitch and A-B repeat/crossfade logic, kept free of any
 * Media3/Android dependency so it can be unit tested directly. */
object PlaybackSpeed {
    const val MIN_SPEED = 0.5f
    const val MAX_SPEED = 2.0f

    /** Clamps a requested playback speed into the supported [MIN_SPEED, MAX_SPEED] range. */
    fun clamp(speed: Float): Float = speed.coerceIn(MIN_SPEED, MAX_SPEED)
}

data class AbRepeatState(
    val pointAMs: Long? = null,
    val pointBMs: Long? = null,
    val enabled: Boolean = false,
) {
    /** True only when both points are set in the right order. */
    val isValid: Boolean get() = pointAMs != null && pointBMs != null && pointBMs > pointAMs
}

object AbRepeat {
    /**
     * Returns true when playback has reached/passed point B while a valid
     * A-B loop is enabled, meaning the caller should seek back to point A.
     */
    fun shouldLoop(state: AbRepeatState, positionMs: Long): Boolean {
        if (!state.enabled || !state.isValid) return false
        return positionMs >= (state.pointBMs ?: return false)
    }
}

object SmartCrossfade {
    /**
     * Real, simple heuristic: when "smart crossfade" is on, skip crossfading
     * between two consecutive tracks that share the same (non-blank) album,
     * so a continuous album/live recording isn't chopped by a fade. Falls
     * back to the ordinary crossfade decision otherwise.
     */
    fun shouldCrossfade(
        crossfadeEnabled: Boolean,
        smartCrossfadeEnabled: Boolean,
        currentAlbum: String?,
        nextAlbum: String?,
    ): Boolean {
        if (!crossfadeEnabled) return false
        if (!smartCrossfadeEnabled) return true
        if (currentAlbum.isNullOrBlank() || nextAlbum.isNullOrBlank()) return true
        return currentAlbum != nextAlbum
    }
}
