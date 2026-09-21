package com.ozin.music.core.data.model

import androidx.room.Entity

/**
 * A stored per-Bluetooth-device playback profile: which EQ preset and
 * crossfade duration to apply, and whether to auto-resume playback, when
 * this device connects. Keyed by MAC address, which is available from the
 * connect broadcast even without runtime permission to read the device name.
 */
@Entity(tableName = "bluetooth_device_profiles", primaryKeys = ["address"])
data class BluetoothDeviceProfile(
    val address: String,
    val name: String,
    val eqPresetId: String,
    val crossfadeSeconds: Int,
    val autoplayEnabled: Boolean,
    val updatedAt: Long = System.currentTimeMillis(),
)
