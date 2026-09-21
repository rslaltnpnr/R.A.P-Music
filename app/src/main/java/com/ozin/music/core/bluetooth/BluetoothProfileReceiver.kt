package com.ozin.music.core.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.ozin.music.core.data.repository.BluetoothDeviceProfileRepository
import com.ozin.music.core.domain.EqPresetId
import com.ozin.music.core.player.PlayerController
import com.ozin.music.core.settings.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Manifest-registered receiver for Bluetooth connect/disconnect: system
 * broadcasts, so this reaches the app even if its process was not running.
 * On connect, looks up a stored profile for the device's MAC address and
 * applies its EQ preset/crossfade settings (read live by [PlayerController]'s
 * playback pipeline, so writing them is enough to apply them) and, if
 * enabled, resumes playback when a queue already exists.
 */
@AndroidEntryPoint
class BluetoothProfileReceiver : BroadcastReceiver() {

    @Inject lateinit var profileRepository: BluetoothDeviceProfileRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var playerController: PlayerController

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != BluetoothDevice.ACTION_ACL_CONNECTED) return

        val device: BluetoothDevice = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        } ?: return

        val address = device.address ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val profile = profileRepository.getByAddress(address)
                if (profile != null) {
                    val eqPreset = runCatching { EqPresetId.valueOf(profile.eqPresetId) }
                        .getOrDefault(EqPresetId.NORMAL)
                    settingsRepository.setEqEnabled(true)
                    settingsRepository.setEqPreset(eqPreset)
                    settingsRepository.setCrossfadeEnabled(profile.crossfadeSeconds > 0)
                    settingsRepository.setCrossfadeSeconds(profile.crossfadeSeconds)
                    if (profile.autoplayEnabled) {
                        kotlinx.coroutines.withContext(Dispatchers.Main.immediate) {
                            playerController.connect()
                            playerController.resumeIfQueued()
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        /**
         * Reads a connected/bonded device's display name, guarded behind the
         * runtime BLUETOOTH_CONNECT permission required on API 31+. Falls
         * back to the MAC address rather than crashing when not granted.
         */
        fun safeDeviceName(context: Context, device: BluetoothDevice): String {
            if (Build.VERSION.SDK_INT >= 31) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.BLUETOOTH_CONNECT,
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) return device.address
            }
            return runCatching { device.name }.getOrNull() ?: device.address
        }
    }
}
