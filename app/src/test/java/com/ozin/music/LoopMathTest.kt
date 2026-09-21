package com.ozin.music

import com.ozin.music.core.domain.dj.LoopMath
import org.junit.Assert.assertEquals
import org.junit.Test

class LoopMathTest {
    @Test
    fun `known bpm computes real duration`() {
        // 120bpm -> 500ms/beat -> 4 beats = 2000ms
        assertEquals(2000L, LoopMath.loopDurationMs(4.0, 120f))
    }

    @Test
    fun `unknown bpm falls back to fixed default`() {
        assertEquals(2000L, LoopMath.loopDurationMs(4.0, null))
        assertEquals(500L, LoopMath.loopDurationMs(1.0, null))
    }

    @Test
    fun `fractional beat presets work`() {
        assertEquals(125L, LoopMath.loopDurationMs(0.25, null))
    }
}
