package com.ozin.music.core.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.util.Log
import androidx.media3.common.Player
import com.ozin.music.core.domain.EqPresetId
import com.ozin.music.core.domain.EqPresets
import javax.inject.Inject
import javax.inject.Singleton

/** Kept for source compatibility with any old call sites; the real preset
 * table now lives in [EqPresetId]/[EqPresets]. */
typealias EqPreset = EqPresetId

/**
 * Real android.media.audiofx effect chain attached to ExoPlayer's audio
 * session id. Every effect is created and used defensively: unsupported
 * hardware/effects throw on some devices, and a single failure must never
 * take down playback or any other effect. Desired state (enabled, preset,
 * custom bands, preamp, bass/virtualizer/loudness strength) is kept here in
 * memory and re-applied whenever the audio session id changes (e.g. a new
 * track starts a new ExoPlayer internal session), so settings survive across
 * track changes and are restored from DataStore once by the caller.
 */
@Singleton
class EffectsChain @Inject constructor() : Player.Listener {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var sessionId: Int = 0

    // Desired state, applied to whichever effect instances currently exist.
    private var desiredEnabled: Boolean = false
    private var desiredPreset: EqPresetId = EqPresetId.NORMAL
    private var desiredCustomBands: IntArray = IntArray(0)
    private var desiredPreampMb: Int = 0
    private var desiredBassStrength: Int = 0
    private var desiredVirtualizerStrength: Int = 0
    private var desiredLoudnessGainMb: Int = 0

    var enabled: Boolean
        get() = desiredEnabled
        set(value) {
            desiredEnabled = value
            try { equalizer?.enabled = value } catch (_: Exception) { }
            try { bassBoost?.enabled = value } catch (_: Exception) { }
            try { virtualizer?.enabled = value } catch (_: Exception) { }
            try { loudnessEnhancer?.enabled = value } catch (_: Exception) { }
        }

    override fun onAudioSessionIdChanged(audioSessionId: Int) {
        attach(audioSessionId)
    }

    private fun attach(newSessionId: Int) {
        if (newSessionId == sessionId && equalizer != null) return
        release()
        sessionId = newSessionId
        try {
            equalizer = Equalizer(0, newSessionId).also { it.enabled = desiredEnabled }
        } catch (e: Exception) {
            Log.w(TAG, "Equalizer unsupported", e)
        }
        try {
            bassBoost = BassBoost(0, newSessionId).also { it.enabled = desiredEnabled }
        } catch (e: Exception) {
            Log.w(TAG, "BassBoost unsupported", e)
        }
        try {
            virtualizer = Virtualizer(0, newSessionId).also { it.enabled = desiredEnabled }
        } catch (e: Exception) {
            Log.w(TAG, "Virtualizer unsupported", e)
        }
        try {
            loudnessEnhancer = LoudnessEnhancer(newSessionId).also { it.enabled = desiredEnabled }
        } catch (e: Exception) {
            Log.w(TAG, "LoudnessEnhancer unsupported", e)
        }
        // Re-apply whatever the caller last asked for onto the fresh effect instances.
        reapplyAll()
    }

    private fun reapplyAll() {
        if (desiredPreset == EqPresetId.CUSTOM && desiredCustomBands.isNotEmpty()) {
            applyCustomBands(desiredCustomBands)
        } else {
            applyPresetInternal(desiredPreset)
        }
        setPreampMb(desiredPreampMb)
        setBassBoostStrength(desiredBassStrength)
        setVirtualizerStrength(desiredVirtualizerStrength)
        setLoudnessGainMb(desiredLoudnessGainMb)
    }

    fun setNormalization(enabled: Boolean) {
        // Kept for backward compatibility: maps the simple on/off toggle onto
        // a real loudness gain, the same effect setLoudnessGainMb drives.
        setLoudnessGainMb(if (enabled) 500 else 0)
    }

    fun setLoudnessGainMb(mb: Int) {
        desiredLoudnessGainMb = mb
        try {
            loudnessEnhancer?.setTargetGain(mb)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set loudness gain", e)
        }
    }

    fun setBassBoostStrength(strength: Int) {
        desiredBassStrength = strength.coerceIn(0, 1000)
        try {
            bassBoost?.setStrength(desiredBassStrength.toShort())
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set bass boost strength", e)
        }
    }

    fun setVirtualizerStrength(strength: Int) {
        desiredVirtualizerStrength = strength.coerceIn(0, 1000)
        try {
            virtualizer?.setStrength(desiredVirtualizerStrength.toShort())
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set virtualizer strength", e)
        }
    }

    /** Applies a named preset, scaled from the 10-band reference table to
     * however many bands this device's Equalizer actually exposes. */
    fun applyPreset(preset: EqPresetId) {
        desiredPreset = preset
        applyPresetInternal(preset)
        setPreampMb(desiredPreampMb)
    }

    private fun applyPresetInternal(preset: EqPresetId) {
        val eq = equalizer ?: return
        try {
            val bands = eq.numberOfBands.toInt()
            val range = bandLevelRange()
            val reference = EqPresets.referenceTables[preset] ?: return
            val mapped = EqPresets.mapToDeviceBands(reference, bands, range[0].toInt()..range[1].toInt())
            for (bandIndex in 0 until bands) {
                eq.setBandLevel(bandIndex.toShort(), mapped[bandIndex].toShort())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to apply EQ preset", e)
        }
    }

    /** Applies explicit per-band gains (millibel), e.g. from a user-tuned
     * custom preset saved in Settings. */
    fun applyCustomBands(bands: IntArray) {
        desiredPreset = EqPresetId.CUSTOM
        desiredCustomBands = bands
        val eq = equalizer ?: return
        try {
            val range = eq.bandLevelRange
            for (i in bands.indices) {
                if (i >= eq.numberOfBands) break
                val level = (bands[i] + desiredPreampMb).coerceIn(range[0].toInt(), range[1].toInt())
                eq.setBandLevel(i.toShort(), level.toShort())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to apply custom EQ bands", e)
        }
    }

    /** Sets a single band's level directly (millibel), used by the custom
     * band sliders in the Equalizer screen. Also records it as the user's
     * custom setup. */
    fun setBandLevel(band: Int, level: Short) {
        try {
            equalizer?.setBandLevel(band.toShort(), level)
            val updated = desiredCustomBands.copyOf(bandCount()).also {
                if (band < it.size) it[band] = level.toInt()
            }
            desiredCustomBands = updated
            desiredPreset = EqPresetId.CUSTOM
        } catch (_: Exception) {
        }
    }

    /** Shifts every band by [mb] millibel relative to the currently applied
     * preset/custom bands (a simple, real preamp control). */
    fun setPreampMb(mb: Int) {
        desiredPreampMb = mb
        val eq = equalizer ?: return
        try {
            val range = eq.bandLevelRange
            val base = if (desiredPreset == EqPresetId.CUSTOM && desiredCustomBands.isNotEmpty()) {
                desiredCustomBands
            } else {
                EqPresets.mapToDeviceBands(
                    EqPresets.referenceTables[desiredPreset] ?: return,
                    eq.numberOfBands.toInt(),
                    range[0].toInt()..range[1].toInt(),
                )
            }
            val withPreamp = EqPresets.applyPreamp(base, mb, range[0].toInt()..range[1].toInt())
            for (i in withPreamp.indices) {
                if (i >= eq.numberOfBands) break
                eq.setBandLevel(i.toShort(), withPreamp[i].toShort())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to apply preamp", e)
        }
    }

    fun bandCount(): Int = try { equalizer?.numberOfBands?.toInt() ?: 0 } catch (_: Exception) { 0 }

    fun centerFrequencyHz(band: Int): Int = try {
        (equalizer?.getCenterFreq(band.toShort()) ?: 0) / 1000
    } catch (_: Exception) { 0 }

    fun bandLevelRange(): ShortArray = try {
        equalizer?.bandLevelRange ?: shortArrayOf(-1500, 1500)
    } catch (_: Exception) {
        shortArrayOf(-1500, 1500)
    }

    fun currentBandLevel(band: Int): Short = try {
        equalizer?.getBandLevel(band.toShort()) ?: 0
    } catch (_: Exception) { 0 }

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
