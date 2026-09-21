package com.ozin.music.core.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-saved custom equalizer preset: a named snapshot of the exact tunable
 * fields the "Custom" EQ slot already tracks in [com.ozin.music.core.settings.SettingsRepository]
 * / [com.ozin.music.core.player.EffectsChain] (band gains, preamp, bass boost,
 * virtualizer, loudness gain). [bands] mirrors the comma-separated encoding
 * [com.ozin.music.core.settings.SettingsRepository] already uses for
 * `eqCustomBands`, for consistency.
 */
@Entity(tableName = "custom_eq_presets")
data class CustomEqPreset(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val bands: String,
    val preampMb: Int,
    val bassBoostStrength: Int,
    val virtualizerStrength: Int,
    val loudnessGainMb: Int,
    val createdAt: Long,
)

/** Decodes [CustomEqPreset.bands] the same way `SettingsRepository` decodes
 * `eqCustomBands`: a comma-separated list of millibel band gains. */
fun CustomEqPreset.decodedBands(): List<Int> =
    bands.takeIf { it.isNotBlank() }?.split(",")?.mapNotNull { it.toIntOrNull() } ?: emptyList()

/** Encodes a list of band gains the same way `SettingsRepository` encodes
 * `eqCustomBands`. */
fun encodeBands(bands: List<Int>): String = bands.joinToString(",")
