package com.ozin.music

import com.ozin.music.core.domain.dj.WaveformDownsampler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WaveformDownsamplerTest {
    @Test
    fun `output size matches bucket count`() {
        val samples = ShortArray(10000) { (it % 32000).toShort() }
        val result = WaveformDownsampler.downsample(samples, 100)
        assertEquals(100, result.size)
    }

    @Test
    fun `loudest bucket normalizes to 1`() {
        val samples = ShortArray(1000) { 100 }
        samples[500] = 32000
        val result = WaveformDownsampler.downsample(samples, 10)
        assertTrue(result.max() > 0.99f)
    }

    @Test
    fun `empty input is safe`() {
        val result = WaveformDownsampler.downsample(ShortArray(0), 50)
        assertEquals(50, result.size)
        assertTrue(result.all { it == 0f })
    }

    @Test
    fun `all values in 0 to 1 range`() {
        val samples = ShortArray(5000) { ((it * 37) % 65535 - 32768).toShort() }
        val result = WaveformDownsampler.downsample(samples, 200)
        assertTrue(result.all { it in 0f..1f })
    }
}
