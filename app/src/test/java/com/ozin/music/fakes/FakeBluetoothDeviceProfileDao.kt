package com.ozin.music.fakes

import com.ozin.music.core.data.local.BluetoothDeviceProfileDao
import com.ozin.music.core.data.model.BluetoothDeviceProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory fake standing in for Room in JVM unit tests. */
class FakeBluetoothDeviceProfileDao : BluetoothDeviceProfileDao {

    private val profiles = mutableMapOf<String, BluetoothDeviceProfile>()
    private val _all = MutableStateFlow<List<BluetoothDeviceProfile>>(emptyList())

    private fun emit() {
        _all.value = profiles.values.sortedByDescending { it.updatedAt }
    }

    override fun observeAll(): StateFlow<List<BluetoothDeviceProfile>> = _all.asStateFlow()

    override suspend fun getByAddress(address: String): BluetoothDeviceProfile? = profiles[address]

    override suspend fun upsert(profile: BluetoothDeviceProfile) {
        profiles[profile.address] = profile
        emit()
    }

    override suspend fun delete(profile: BluetoothDeviceProfile) {
        profiles.remove(profile.address)
        emit()
    }

    override suspend fun deleteByAddress(address: String) {
        profiles.remove(address)
        emit()
    }
}
