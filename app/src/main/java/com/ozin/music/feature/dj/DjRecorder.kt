package com.ozin.music.feature.dj

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.nio.ByteBuffer

/**
 * Real recording of this app's own DJ Mode output via
 * MediaProjection + AudioPlaybackCaptureConfiguration (the standard Android
 * API since API 29 for capturing an app's own USAGE_MEDIA playback),
 * encoded to a real .m4a (AAC/MP4) file via MediaCodec + MediaMuxer.
 *
 * Honest risk note: this pipeline is written to the documented API contract
 * but has not been exercised on a real device/emulator in this sandbox (no
 * Android runtime is available here). If CI or a real device surfaces an
 * issue, look first at: the encoder input buffer timing/looping logic below,
 * and whether AudioPlaybackCaptureConfiguration correctly captures this
 * app's own concurrently-playing dual ExoPlayer output (it is documented to
 * capture by usage/uid, which should include both decks since they're both
 * this app's own AudioTracks).
 *
 * Requires API 29+; gracefully unavailable below that (checked by the
 * caller via [isSupported]) rather than crashing.
 */
class DjRecorder(private val context: Context) {
    var isRecording: Boolean = false
        private set

    private var mediaProjection: MediaProjection? = null
    private var audioRecord: AudioRecord? = null
    private var encoder: MediaCodec? = null
    private var muxer: MediaMuxer? = null
    private var muxerTrackIndex = -1
    private var muxerStarted = false
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    var lastOutputFile: File? = null
        private set

    companion object {
        private const val TAG = "DjRecorder"
        private const val SAMPLE_RATE = 44100
        private const val BIT_RATE = 128_000

        fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

        /** Real consent-flow intent; the caller launches this for a result
         * and passes the result back into [start]. The system's
         * "share your screen" style dialog for an audio-only capture is a
         * known, unavoidable quirk of this API, not a bug introduced here. */
        fun createCaptureIntent(activity: Activity): Intent {
            val manager = activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            return manager.createScreenCaptureIntent()
        }
    }

    @SuppressLint("MissingPermission")
    fun start(resultCode: Int, data: Intent, outputFile: File): Boolean {
        if (!isSupported() || isRecording) return false
        try {
            val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val projection = manager.getMediaProjection(resultCode, data) ?: return false
            mediaProjection = projection

            val captureConfig = AudioPlaybackCaptureConfiguration.Builder(projection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .build()

            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                .build()

            val minBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
            ).coerceAtLeast(4096)

            val record = AudioRecord.Builder()
                .setAudioFormat(format)
                .setBufferSizeInBytes(minBufferSize * 2)
                .setAudioPlaybackCaptureConfig(captureConfig)
                .build()
            audioRecord = record

            val mediaFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, SAMPLE_RATE, 2).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
            }
            val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            codec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder = codec

            val mux = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            muxer = mux
            muxerTrackIndex = -1
            muxerStarted = false
            lastOutputFile = outputFile

            codec.start()
            record.startRecording()
            isRecording = true

            job = scope.launch { recordLoop(record, codec, mux) }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start DJ recording", e)
            stop()
            return false
        }
    }

    private fun recordLoop(record: AudioRecord, codec: MediaCodec, mux: MediaMuxer) {
        val pcmBuffer = ByteArray(4096)
        val bufferInfo = MediaCodec.BufferInfo()
        try {
            while (isRecording) {
                val read = record.read(pcmBuffer, 0, pcmBuffer.size)
                if (read > 0) {
                    val inIndex = codec.dequeueInputBuffer(10_000)
                    if (inIndex >= 0) {
                        val inputBuffer: ByteBuffer? = codec.getInputBuffer(inIndex)
                        inputBuffer?.clear()
                        inputBuffer?.put(pcmBuffer, 0, read)
                        codec.queueInputBuffer(inIndex, 0, read, System.nanoTime() / 1000, 0)
                    }
                }
                var outIndex = codec.dequeueOutputBuffer(bufferInfo, 0)
                while (outIndex >= 0) {
                    if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        muxerTrackIndex = mux.addTrack(codec.outputFormat)
                        mux.start()
                        muxerStarted = true
                    } else {
                        val outBuffer = codec.getOutputBuffer(outIndex)
                        if (outBuffer != null && bufferInfo.size > 0 && muxerStarted) {
                            mux.writeSampleData(muxerTrackIndex, outBuffer, bufferInfo)
                        }
                        codec.releaseOutputBuffer(outIndex, false)
                    }
                    outIndex = codec.dequeueOutputBuffer(bufferInfo, 0)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "DJ recording loop failed", e)
        }
    }

    fun stop() {
        isRecording = false
        job?.cancel()
        try { audioRecord?.stop() } catch (_: Exception) { }
        try { audioRecord?.release() } catch (_: Exception) { }
        try { if (muxerStarted) muxer?.stop() } catch (_: Exception) { }
        try { muxer?.release() } catch (_: Exception) { }
        try { encoder?.stop() } catch (_: Exception) { }
        try { encoder?.release() } catch (_: Exception) { }
        try { mediaProjection?.stop() } catch (_: Exception) { }
        audioRecord = null
        muxer = null
        encoder = null
        mediaProjection = null
        muxerStarted = false
    }
}
