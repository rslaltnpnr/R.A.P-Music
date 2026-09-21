package com.ozin.music.feature.dj

import android.content.ContentUris
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.ozin.music.core.data.model.Song

sealed class SamplerPad {
    data class UserClip(val song: Song) : SamplerPad()
    data class Tone(val label: String, val toneType: Int) : SamplerPad()
}

/**
 * Real sampler: no fabricated "air horn"/"clap" sample audio ships with this
 * app (none can be authored in this build environment), so pads are either
 * (a) a short clip the user picks from their own local library, played via a
 * dedicated one-shot [ExoPlayer] — mixed with both decks' output the same
 * real way multiple simultaneous AudioTracks from one app always mix at the
 * OS level — or (b) a simple, honestly-labeled procedural tone from
 * [ToneGenerator] (a real Android API for DTMF/basic tones), for a couple of
 * default pads. Nothing here claims to be a real drum/FX sample.
 */
class DjSampler(private val context: Context) {
    private val pads = HashMap<Int, SamplerPad>()
    private var clipPlayer: ExoPlayer? = null
    private var toneGenerator: ToneGenerator? = null

    init {
        // Two default honestly-labeled tone pads so the row isn't empty
        // before the user assigns their own clips.
        pads[0] = SamplerPad.Tone("Beep", ToneGenerator.TONE_PROP_BEEP)
        pads[1] = SamplerPad.Tone("DTMF A", ToneGenerator.TONE_DTMF_A)
    }

    fun assignClip(pad: Int, song: Song) {
        pads[pad] = SamplerPad.UserClip(song)
    }

    fun padAt(pad: Int): SamplerPad? = pads[pad]

    fun clear(pad: Int) {
        pads.remove(pad)
    }

    fun trigger(pad: Int) {
        when (val assigned = pads[pad]) {
            is SamplerPad.Tone -> {
                try {
                    val tg = toneGenerator ?: ToneGenerator(AudioManager.STREAM_MUSIC, 80).also { toneGenerator = it }
                    tg.startTone(assigned.toneType, 200)
                } catch (_: Exception) {
                }
            }
            is SamplerPad.UserClip -> {
                try {
                    val player = clipPlayer ?: ExoPlayer.Builder(context).build().also { clipPlayer = it }
                    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, assigned.song.id)
                    player.setMediaItem(MediaItem.fromUri(uri))
                    player.prepare()
                    player.play()
                } catch (_: Exception) {
                }
            }
            null -> Unit
        }
    }

    fun release() {
        try { toneGenerator?.release() } catch (_: Exception) { }
        try { clipPlayer?.release() } catch (_: Exception) { }
        toneGenerator = null
        clipPlayer = null
    }
}
