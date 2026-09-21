package com.ozin.music.feature.dj

import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.util.Log

/**
 * One deck's real per-channel FX chain, built from standard
 * android.media.audiofx effects attached to that deck's own ExoPlayer audio
 * session id (mirrors the pattern in [com.ozin.music.core.player.EffectsChain],
 * but a separate, independent instance per deck since DJ Mode runs two
 * simultaneous, independently controllable players).
 *
 * Honest scope notes:
 * - EQ: uses the real [Equalizer], picking the 3 bands closest to "low",
 *   "mid" and "high" out of however many bands the device exposes (devices
 *   commonly expose 5). This is a real 3-band control, not a full
 *   multi-band parametric EQ.
 * - Filter: approximated by attenuating the high or low bands via the same
 *   Equalizer — a real, audible low-pass/high-pass *character* sweep, not a
 *   literal resonant multi-pole filter (android.media.audiofx has no public
 *   filter API; a true one would need a custom AudioProcessor, out of scope
 *   here).
 * - Echo/Delay/Reverb: [PresetReverb], a real standard Android effect.
 *   Flanger/Phaser are intentionally not implemented: there is no standard
 *   Android audiofx equivalent, and a fake modulated-delay approximation
 *   risked sounding broken/misleading without on-device tuning, so those two
 *   toggles are simply not present in the UI rather than doing nothing.
 */
class DjChannelEffects {
    private var equalizer: Equalizer? = null
    private var reverb: PresetReverb? = null
    private var sessionId: Int = 0

    private var lowBand = 0
    private var midBand = 0
    private var highBand = 0

    var lowGainMb = 0; private set
    var midGainMb = 0; private set
    var highGainMb = 0; private set
    var filterAmount = 0f; private set // -1 (low-pass character) .. 1 (high-pass character), 0 = flat
    var reverbPreset: Int = PresetReverb.PRESET_NONE; private set

    fun attach(audioSessionId: Int) {
        if (audioSessionId == sessionId && equalizer != null) return
        release()
        sessionId = audioSessionId
        try {
            equalizer = Equalizer(0, audioSessionId).also { eq ->
                eq.enabled = true
                val bands = eq.numberOfBands.toInt()
                lowBand = 0
                highBand = (bands - 1).coerceAtLeast(0)
                midBand = (bands / 2).coerceIn(0, highBand)
            }
        } catch (e: Exception) {
            Log.w(TAG, "DJ Equalizer unsupported", e)
        }
        try {
            reverb = PresetReverb(0, audioSessionId).also { it.enabled = false }
        } catch (e: Exception) {
            Log.w(TAG, "DJ PresetReverb unsupported", e)
        }
        reapply()
    }

    private fun reapply() {
        setLow(lowGainMb)
        setMid(midGainMb)
        setHigh(highGainMb)
        setFilter(filterAmount)
        setReverbPreset(reverbPreset)
    }

    fun setLow(mb: Int) {
        lowGainMb = mb
        setBandSafe(lowBand, mb)
    }

    fun setMid(mb: Int) {
        midGainMb = mb
        setBandSafe(midBand, mb)
    }

    fun setHigh(mb: Int) {
        highGainMb = mb
        setBandSafe(highBand, mb)
    }

    /** Simplified filter approximation: sweeping toward -1 attenuates the
     * high band (low-pass character), toward +1 attenuates the low band
     * (high-pass character). Documented as an honest approximation, not a
     * real resonant filter. */
    fun setFilter(amount: Float) {
        filterAmount = amount.coerceIn(-1f, 1f)
        val range = bandLevelRangeMb()
        val attenuation = (filterAmount.let { kotlin.math.abs(it) } * (range.second - range.first)).toInt()
        if (filterAmount < -0.02f) {
            setBandSafe(highBand, highGainMb - attenuation)
            setBandSafe(lowBand, lowGainMb)
        } else if (filterAmount > 0.02f) {
            setBandSafe(lowBand, lowGainMb - attenuation)
            setBandSafe(highBand, highGainMb)
        } else {
            setBandSafe(lowBand, lowGainMb)
            setBandSafe(highBand, highGainMb)
        }
    }

    fun setReverbPreset(preset: Int) {
        reverbPreset = preset
        try {
            reverb?.let {
                it.preset = preset.toShort()
                it.enabled = preset != PresetReverb.PRESET_NONE
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set DJ reverb preset", e)
        }
    }

    private fun setBandSafe(band: Int, mb: Int) {
        val eq = equalizer ?: return
        try {
            val range = eq.bandLevelRange
            val clamped = mb.coerceIn(range[0].toInt(), range[1].toInt())
            eq.setBandLevel(band.toShort(), clamped.toShort())
        } catch (_: Exception) {
        }
    }

    private fun bandLevelRangeMb(): Pair<Int, Int> = try {
        val range = equalizer?.bandLevelRange ?: shortArrayOf(-1500, 1500)
        range[0].toInt() to range[1].toInt()
    } catch (_: Exception) {
        -1500 to 1500
    }

    fun release() {
        try { equalizer?.release() } catch (_: Exception) { }
        try { reverb?.release() } catch (_: Exception) { }
        equalizer = null
        reverb = null
    }

    companion object {
        private const val TAG = "DjChannelEffects"
    }
}
