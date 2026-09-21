package com.ozin.music.core.data.repository

import com.ozin.music.core.data.local.BluetoothDeviceProfileDao
import com.ozin.music.core.data.model.BluetoothDeviceProfile
import com.ozin.music.core.domain.EqPresetId
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CRUD access to per-Bluetooth-device profiles (EQ preset, crossfade seconds,
 * autoplay), keyed by MAC address.
 */
@Singleton
class BluetoothDeviceProfileRepository @Inject constructor(
    private val dao: BluetoothDeviceProfileDao,
) {
    val profiles: Flow<List<BluetoothDeviceProfile>> = dao.observeAll()

    suspend fun getByAddress(address: String): BluetoothDeviceProfile? = dao.getByAddress(address)

    suspend fun save(
        address: String,
        name: String,
        eqPresetId: EqPresetId,
        crossfadeSeconds: Int,
        autoplayEnabled: Boolean,
    ) {
        dao.upsert(
            BluetoothDeviceProfile(
                address = address,
                name = name,
                eqPresetId = eqPresetId.name,
                crossfadeSeconds = crossfadeSeconds,
                autoplayEnabled = autoplayEnabled,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun delete(address: String) = dao.deleteByAddress(address)
}
