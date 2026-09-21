package com.ozin.music.feature.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ozin.music.core.bluetooth.BluetoothProfileReceiver
import com.ozin.music.core.data.model.BluetoothDeviceProfile
import com.ozin.music.core.data.repository.BluetoothDeviceProfileRepository
import com.ozin.music.core.domain.EqPresetId
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A paired device shown in the management list, with its stored profile if any. */
data class BluetoothDeviceRow(
    val address: String,
    val name: String,
    val profile: BluetoothDeviceProfile?,
)

data class BluetoothDevicesUiState(
    val permissionGranted: Boolean = true,
    val devices: List<BluetoothDeviceRow> = emptyList(),
)

@HiltViewModel
class BluetoothDevicesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profileRepository: BluetoothDeviceProfileRepository,
) : ViewModel() {

    private val _permissionGranted = MutableStateFlow(hasBluetoothConnectPermission())
    private val _bondedDevices = MutableStateFlow<List<Pair<String, String>>>(emptyList())

    val state: StateFlow<BluetoothDevicesUiState> = combine(
        _permissionGranted,
        _bondedDevices,
        profileRepository.profiles,
    ) { granted, bonded, profiles ->
        val profilesByAddress = profiles.associateBy { it.address }
        BluetoothDevicesUiState(
            permissionGranted = granted,
            devices = bonded.map { (address, name) ->
                BluetoothDeviceRow(address = address, name = name, profile = profilesByAddress[address])
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BluetoothDevicesUiState())

    init {
        refreshBondedDevices()
    }

    fun onPermissionResult(granted: Boolean) {
        _permissionGranted.value = granted || hasBluetoothConnectPermission()
        refreshBondedDevices()
    }

    private fun hasBluetoothConnectPermission(): Boolean {
        if (Build.VERSION.SDK_INT < 31) return true
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.BLUETOOTH_CONNECT,
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** Lists bonded devices, permission-guarded: without BLUETOOTH_CONNECT on
     * API31+ this safely returns an empty list rather than crashing. */
    private fun refreshBondedDevices() {
        if (!hasBluetoothConnectPermission()) {
            _bondedDevices.value = emptyList()
            return
        }
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
            ?: BluetoothAdapter.getDefaultAdapter()
        val bonded = runCatching {
            adapter?.bondedDevices?.map { device ->
                device.address to BluetoothProfileReceiver.safeDeviceName(context, device)
            } ?: emptyList()
        }.getOrDefault(emptyList())
        _bondedDevices.value = bonded
    }

    fun saveProfile(
        address: String,
        name: String,
        eqPresetId: EqPresetId,
        crossfadeSeconds: Int,
        autoplayEnabled: Boolean,
    ) {
        viewModelScope.launch {
            profileRepository.save(address, name, eqPresetId, crossfadeSeconds, autoplayEnabled)
        }
    }

    fun deleteProfile(address: String) {
        viewModelScope.launch { profileRepository.delete(address) }
    }
}
