package com.ozin.music.core.domain.dj

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

/**
 * Pure, testable equal-power crossfade math. Position is in `[-1, 1]`:
 * -1 = full Deck A, 0 = center (both at ~0.707), 1 = full Deck B.
 *
 * Equal-power (constant-power) crossfading keeps perceived loudness roughly
 * constant through the sweep, unlike a naive linear fade which dips in the
 * middle — the standard real-DJ-mixer crossfade curve.
 */
object CrossfaderCurve {
    data class Volumes(val deckA: Float, val deckB: Float)

    /** Maps [-1, 1] to a quarter sine/cosine sweep: at x=-1, angle=0 (A=1,B=0);
     * at x=1, angle=pi/2 (A=0,B=1). */
    fun volumesFor(position: Float): Volumes {
        val clamped = position.coerceIn(-1f, 1f)
        val t = (clamped + 1f) / 2f // 0..1
        val angle = (t * PI / 2.0)
        val a = cos(angle).toFloat()
        val b = sin(angle).toFloat()
        return Volumes(deckA = a.coerceIn(0f, 1f), deckB = b.coerceIn(0f, 1f))
    }
}
