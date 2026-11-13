package com.ozin.music.core.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class ClassicAudioFrame(
    val spectrum: List<Float> = emptyList(),
    val waveform: List<Float> = emptyList(),
)

/** Main music player PCM tap, not a microphone input. DJ decks keep their own pipeline. */
@Singleton
@OptIn(UnstableApi::class)
class ClassicAudioProcessor @Inject constructor() : BaseAudioProcessor() {
    @Volatile var balance = 0f
    @Volatile var capture = false

    private val _frame = MutableStateFlow(ClassicAudioFrame())
    val frame = _frame.asStateFlow()
    private val samples = FloatArray(256)
    private val real = FloatArray(samples.size)
    private val imag = FloatArray(samples.size)
    private val hann = FloatArray(samples.size) {
        (0.5 - 0.5 * cos(2 * PI * it / (samples.size - 1))).toFloat()
    }
    private var filled = 0
    private var sampleCounter = 0
    private var framesUntilPublish = 0

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat =
        if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT && inputAudioFormat.channelCount > 0) {
            inputAudioFormat
        } else AudioFormat.NOT_SET

    override fun queueInput(inputBuffer: ByteBuffer) {
        val output = replaceOutputBuffer(inputBuffer.remaining())
        val pan = balance.coerceIn(-1f, 1f)
        val channels = inputAudioFormat.channelCount
        val sampling = capture
        if (!sampling && (filled != 0 || _frame.value.spectrum.isNotEmpty())) clearCapture()
        if (!sampling && (pan == 0f || channels == 1)) {
            output.put(inputBuffer)
        } else {
            inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
            output.order(ByteOrder.LITTLE_ENDIAN)
            val leftGain = 1f - pan.coerceAtLeast(0f)
            val rightGain = 1f + pan.coerceAtMost(0f)
            val stride = (inputAudioFormat.sampleRate / 12_000).coerceAtLeast(1)
            while (inputBuffer.remaining() >= channels * 2) {
                var sum = 0f
                repeat(channels) { channel ->
                    val input = inputBuffer.short.toInt()
                    val gain = when {
                        channels == 1 -> 1f
                        channel == 0 -> leftGain
                        channel == 1 -> rightGain
                        else -> 1f
                    }
                    val sample = if (gain == 1f) input else (input * gain).roundToInt()
                    output.putShort(sample.toShort())
                    sum += sample
                }
                if (sampling && ++sampleCounter >= stride) {
                    sampleCounter = 0
                    if (framesUntilPublish > 0) {
                        framesUntilPublish--
                    } else {
                        samples[filled++] = sum / (channels * 32768f)
                        if (filled == samples.size) {
                            publishFrame()
                            filled = 0
                            framesUntilPublish = (inputAudioFormat.sampleRate / stride / 30 - samples.size)
                                .coerceAtLeast(0)
                        }
                    }
                }
            }
            output.put(inputBuffer)
        }
        output.flip()
    }

    private fun publishFrame() {
        val n = samples.size
        val bits = Integer.numberOfTrailingZeros(n)
        for (i in samples.indices) {
            val reversed = Integer.reverse(i) ushr (32 - bits)
            real[reversed] = samples[i] * hann[i]
            imag[reversed] = 0f
        }
        var span = 2
        while (span <= n) {
            val half = span / 2
            for (j in 0 until half) {
                val angle = -2 * PI * j / span
                val c = cos(angle).toFloat()
                val s = sin(angle).toFloat()
                for (i in j until n step span) {
                    val k = i + half
                    val r = real[k] * c - imag[k] * s
                    val im = real[k] * s + imag[k] * c
                    real[k] = real[i] - r
                    imag[k] = imag[i] - im
                    real[i] += r
                    imag[i] += im
                }
            }
            span *= 2
        }
        val edges = intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 10, 13, 16, 20, 25, 32, 40, 51, 64, 81, 102, 128)
        val spectrum = List(edges.size - 1) { band ->
            var peak = 0f
            for (i in edges[band] until edges[band + 1]) {
                peak = maxOf(peak, sqrt(real[i] * real[i] + imag[i] * imag[i]) * 4f / n)
            }
            ((20f * log10(peak.coerceAtLeast(0.0001f)) + 60f) / 60f).coerceIn(0f, 1f)
        }
        _frame.value = ClassicAudioFrame(spectrum, List(75) { samples[it * n / 75] })
    }

    override fun onFlush() = clearCapture()

    private fun clearCapture() {
        filled = 0
        sampleCounter = 0
        framesUntilPublish = 0
        _frame.value = ClassicAudioFrame()
    }
}
