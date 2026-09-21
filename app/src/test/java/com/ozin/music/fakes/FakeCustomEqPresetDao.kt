package com.ozin.music.fakes

import com.ozin.music.core.data.local.CustomEqPresetDao
import com.ozin.music.core.data.model.CustomEqPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory fake standing in for Room in JVM unit tests. */
class FakeCustomEqPresetDao : CustomEqPresetDao {

    private val presets = mutableMapOf<Long, CustomEqPreset>()
    private var nextId = 1L
    private val _all = MutableStateFlow<List<CustomEqPreset>>(emptyList())

    private fun emit() {
        _all.value = presets.values.sortedByDescending { it.createdAt }
    }

    override fun observeAll(): Flow<List<CustomEqPreset>> = _all.asStateFlow()

    override suspend fun getById(id: Long): CustomEqPreset? = presets[id]

    override suspend fun insert(preset: CustomEqPreset): Long {
        val id = if (preset.id != 0L) preset.id else nextId++
        presets[id] = preset.copy(id = id)
        emit()
        return id
    }

    override suspend fun update(preset: CustomEqPreset) {
        presets[preset.id] = preset
        emit()
    }

    override suspend fun delete(preset: CustomEqPreset) {
        presets.remove(preset.id)
        emit()
    }

    override suspend fun deleteById(id: Long) {
        presets.remove(id)
        emit()
    }
}
