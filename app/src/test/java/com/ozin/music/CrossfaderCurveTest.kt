package com.ozin.music

import com.ozin.music.core.domain.dj.CrossfaderCurve
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CrossfaderCurveTest {
    @Test
    fun `full left endpoint is deck A only`() {
        val v = CrossfaderCurve.volumesFor(-1f)
        assertEquals(1f, v.deckA, 0.001f)
        assertEquals(0f, v.deckB, 0.001f)
    }

    @Test
    fun `full right endpoint is deck B only`() {
        val v = CrossfaderCurve.volumesFor(1f)
        assertEquals(0f, v.deckA, 0.001f)
        assertEquals(1f, v.deckB, 0.001f)
    }

    @Test
    fun `equal power holds across the sweep`() {
        for (x in -10..10) {
            val position = x / 10f
            val v = CrossfaderCurve.volumesFor(position)
            val power = v.deckA * v.deckA + v.deckB * v.deckB
            assertTrue("power was $power at $position", kotlin.math.abs(power - 1f) < 0.01f)
        }
    }

    @Test
    fun `center is equal and roughly 0-707`() {
        val v = CrossfaderCurve.volumesFor(0f)
        assertEquals(v.deckA, v.deckB, 0.01f)
        assertEquals(0.707f, v.deckA, 0.01f)
    }

    @Test
    fun `out of range values are clamped`() {
        val low = CrossfaderCurve.volumesFor(-5f)
        val high = CrossfaderCurve.volumesFor(5f)
        assertEquals(1f, low.deckA, 0.001f)
        assertEquals(1f, high.deckB, 0.001f)
    }
}
