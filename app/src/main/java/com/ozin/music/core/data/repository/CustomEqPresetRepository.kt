package com.ozin.music.core.data.repository

import com.ozin.music.core.data.local.CustomEqPresetDao
import com.ozin.music.core.data.model.CustomEqPreset
import com.ozin.music.core.data.model.encodeBands
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** CRUD access to the user's saved custom equalizer presets. */
@Singleton
class CustomEqPresetRepository @Inject constructor(
    private val dao: CustomEqPresetDao,
) {
    val presets: Flow<List<CustomEqPreset>> = dao.observeAll()

    suspend fun getById(id: Long): CustomEqPreset? = dao.getById(id)

    suspend fun save(
        name: String,
        bands: List<Int>,
        preampMb: Int,
        bassBoostStrength: Int,
        virtualizerStrength: Int,
        loudnessGainMb: Int,
    ): Long = dao.insert(
        CustomEqPreset(
            name = name,
            bands = encodeBands(bands),
            preampMb = preampMb,
            bassBoostStrength = bassBoostStrength,
            virtualizerStrength = virtualizerStrength,
            loudnessGainMb = loudnessGainMb,
            createdAt = System.currentTimeMillis(),
        )
    )

    suspend fun rename(preset: CustomEqPreset, newName: String) {
        if (newName.isBlank()) return
        dao.update(preset.copy(name = newName))
    }

    suspend fun delete(preset: CustomEqPreset) = dao.delete(preset)
}
