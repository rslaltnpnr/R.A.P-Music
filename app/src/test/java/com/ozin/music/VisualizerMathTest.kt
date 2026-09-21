package com.ozin.music

import com.ozin.music.core.domain.VisualizerMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualizerMathTest {

    @Test
    fun `same inputs produce the same bar height`() {
        val a = VisualizerMath.barHeight(songId = 42L, timeSeconds = 3.25f, barIndex = 5, barCount = 24)
        val b = VisualizerMath.barHeight(songId = 42L, timeSeconds = 3.25f, barIndex = 5, barCount = 24)
        assertEquals(a, b, 0f)
    }

    @Test
    fun `bar height stays within the expected 0 to 1 range`() {
        for (songId in listOf(1L, 2L, 999L, 123456789L)) {
            for (barIndex in 0 until 24) {
                for (t in 0..20) {
                    val height = VisualizerMath.barHeight(songId, t * 0.37f, barIndex, 24)
                    assertTrue("height=$height out of range", height in 0f..1f)
                }
            }
        }
    }

    @Test
    fun `different songs produce different patterns`() {
        val songA = (0 until 24).map { VisualizerMath.barHeight(1L, 1.5f, it, 24) }
        val songB = (0 until 24).map { VisualizerMath.barHeight(2L, 1.5f, it, 24) }
        assertTrue(songA != songB)
    }

    @Test
    fun `time advancing changes the animation`() {
        val t0 = VisualizerMath.barHeight(7L, 0f, 3, 24)
        val t1 = VisualizerMath.barHeight(7L, 5f, 3, 24)
        assertTrue(t0 != t1)
    }
}
