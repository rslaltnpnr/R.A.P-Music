package com.ozin.music.core.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(UnstableApi::class)
class ClassicAudioProcessorTest {
    private fun processor(channels: Int) = ClassicAudioProcessor().apply {
        configure(AudioFormat(48_000, channels, C.ENCODING_PCM_16BIT))
        flush()
    }

    private fun pcm(vararg samples: Int): ByteBuffer = ByteBuffer.allocate(samples.size * 2)
        .order(ByteOrder.LITTLE_ENDIAN).apply { samples.forEach { putShort(it.toShort()) }; flip() }

    private fun output(processor: ClassicAudioProcessor): IntArray {
        val buffer = processor.output.order(ByteOrder.LITTLE_ENDIAN)
        return IntArray(buffer.remaining() / 2) { buffer.short.toInt() }
    }

    @Test fun centerPreservesPcmAndPanAttenuatesOnlyOppositeChannel() {
        val player = processor(2)
        player.queueInput(pcm(20000, -15000, 1000, 2000))
        assertArrayEquals(intArrayOf(20000, -15000, 1000, 2000), output(player))
        player.balance = 1f
        player.queueInput(pcm(20000, -15000))
        assertArrayEquals(intArrayOf(0, -15000), output(player))
        player.balance = -0.5f
        player.queueInput(pcm(20000, -15000))
        assertArrayEquals(intArrayOf(20000, -7500), output(player))
    }

    @Test fun monoIsNotMutedByPan() {
        val player = processor(1)
        player.balance = 1f
        player.queueInput(pcm(20000, -15000))
        assertArrayEquals(intArrayOf(20000, -15000), output(player))
    }

    @Test fun meterUsesRealPcmAndClearsWhenDisabled() {
        val player = processor(2)
        player.capture = true
        val wave = IntArray(2048) { index ->
            (sin(2 * PI * 375 * (index / 2) / 48_000) * 20_000).toInt()
        }
        player.queueInput(pcm(*wave))
        assertArrayEquals(wave, output(player))
        val frame = player.frame.value
        assertEquals(19, frame.spectrum.size)
        assertEquals(75, frame.waveform.size)
        assertTrue(frame.spectrum.max() > 0.7f)
        assertTrue(frame.waveform.any { it > 0.3f } && frame.waveform.any { it < -0.3f })
        player.capture = false
        player.queueInput(pcm(0, 0))
        assertTrue(player.frame.value.spectrum.isEmpty())
    }
}
