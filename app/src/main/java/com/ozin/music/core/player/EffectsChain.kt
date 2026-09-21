package com.ozin.music.core.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.util.Log
import androidx.media3.common.Player
import javax.inject.Inject
import javax.inject.Singleton

enum class EqPreset { FLAT, BASS_BOOST, VOCAL, TREBLE_BOOST, ROCK }

/**
 * Real android.media.audiofx effect chain attached to ExoPlayer's audio
 * session id. Every effect is created and used defensively: unsupported
 * hardware/effects throw on some devices, and a single failure must never
 * take down playback.
 */
@Singleton
class EffectsChain @Inject constructor() : Player.Listener {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var sessionId: Int = 0

    var enabled: Boolean = false
        set(value) {
            field = value
            equalizer?.enabled = value
            bassBoost?.enabled = value
            virtualizer?.enabled = value
            loudnessEnhancer?.enabled = value
        }

    override fun onAudioSessionIdChanged(audioSessionId: Int) {
        attach(audioSessionId)
    }

    private fun attach(newSessionId: Int) {
        if (newSessionId == sessionId && equalizer != null) return
        release()
        sessionId = newSessionId
        try {
            equalizer = Equalizer(0, newSessionId).also { it.enabled = enabled }
        } catch (e: Exception) {
            Log.w(TAG, "Equalizer unsupported", e)
        }
        try {
            bassBoost = BassBoost(0, newSessionId).also { it.enabled = enabled }
        } catch (e: Exception) {
            Log.w(TAG, "BassBoost unsupported", e)
        }
        try {
            virtualizer = Virtualizer(0, newSessionId).also { it.enabled = enabled }
        } catch (e: Exception) {
            Log.w(TAG, "Virtualizer unsupported", e)
        }
        try {
            loudnessEnhancer = LoudnessEnhancer(newSessionId).also { it.enabled = enabled }
        } catch (e: Exception) {
            Log.w(TAG, "LoudnessEnhancer unsupported", e)
        }
    }

    fun setNormalization(enabled: Boolean) {
        try {
            loudnessEnhancer?.setTargetGain(if (enabled) 500 else 0)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set loudness gain", e)
        }
    }

    fun applyPreset(preset: EqPreset) {
        val eq = equalizer ?: return
        try {
            val bands = eq.numberOfBands.toInt()
            val levelRange = eq.bandLevelRange
            val min = levelRange[0]
            val max = levelRange[1]
            fun level(fraction: Float): Short = (min + (max - min) * fraction).toInt().toShort()
            for (bandIndex in 0 until bands) {
                val fraction = when (preset) {
                    EqPreset.FLAT -> 0.5f
                    EqPreset.BASS_BOOST -> if (bandIndex < bands / 3) 0.9f else 0.5f
                    EqPreset.VOCAL -> if (bandIndex in bands / 3 until 2 * bands / 3) 0.75f else 0.45f
                    EqPreset.TREBLE_BOOST -> if (bandIndex >= 2 * bands / 3) 0.9f else 0.5f
                    EqPreset.ROCK -> if (bandIndex < bands / 3 || bandIndex >= 2 * bands / 3) 0.8f else 0.4f
                }
                eq.setBandLevel(bandIndex.toShort(), level(fraction))
            }
            if (preset == EqPreset.BASS_BOOST) {
                try { bassBoost?.setStrength(800) } catch (_: Exception) { }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to apply EQ preset", e)
        }
    }

    fun bandCount(): Int = try { equalizer?.numberOfBands?.toInt() ?: 0 } catch (_: Exception) { 0 }

    fun setBandLevel(band: Int, level: Short) {
        try {
            equalizer?.setBandLevel(band.toShort(), level)
        } catch (_: Exception) {
        }
    }

    fun bandLevelRange(): ShortArray = try {
        equalizer?.bandLevelRange ?: shortArrayOf(-1500, 1500)
    } catch (_: Exception) {
        shortArrayOf(-1500, 1500)
    }

    fun release() {
        try { equalizer?.release() } catch (_: Exception) { }
        try { bassBoost?.release() } catch (_: Exception) { }
        try { virtualizer?.release() } catch (_: Exception) { }
        try { loudnessEnhancer?.release() } catch (_: Exception) { }
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
    }

    companion object {
        private const val TAG = "EffectsChain"
    }
}
