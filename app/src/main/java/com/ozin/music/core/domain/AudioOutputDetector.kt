package com.ozin.music.core.domain

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** The current real audio output route, as best determinable from public
 * Android APIs. */
enum class AudioOutputKind { THIS_DEVICE, BLUETOOTH, WIRED_HEADPHONES, CAR }

/**
 * Detects the active audio output device for display (item 8's Now Playing
 * indicator and item 9's Debug screen).
 *
 * Uses [AudioManager.getDevices] (API 23+, fine for this app's minSdk 26)
 * over the older `isBluetoothA2dpOn()`/`isWiredHeadsetOn()` pair: those two
 * are deprecated, only cover two of the four states this app wants to show,
 * and `isBluetoothA2dpOn()` in particular is documented as unreliable for
 * anything except "is A2DP audio routing currently active", not "which
 * device". `getDevices(GET_DEVICES_OUTPUTS)` gives a real device list with a
 * `type` for each, which is what actually lets THIS_DEVICE/BLUETOOTH/WIRED/
 * CAR be told apart.
 *
 * Car detection is a heuristic: [AudioDeviceInfo.TYPE_BLUETOOTH_A2DP] whose
 * product name contains a common car-audio keyword, or the dedicated
 * [AudioDeviceInfo.TYPE_BUS] (Android Automotive/car audio bus) type where
 * available. This is best-effort only — a Bluetooth car kit with a generic
 * product name will show as plain Bluetooth, not Car.
 */
@Singleton
class AudioOutputDetector @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun currentOutput(): AudioOutputKind {
        val audioManager = context.getSystemService<AudioManager>() ?: return AudioOutputKind.THIS_DEVICE
        val devices = try {
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        } catch (_: Exception) {
            return AudioOutputKind.THIS_DEVICE
        }

        // Prefer the most specific/likely-active external route. A device
        // can report several outputs; there is no public "which one is the
        // audio currently flowing to" query, so we pick by priority: a
        // car-like route first, then any Bluetooth route, then wired, else
        // the device's own speaker/earpiece.
        var sawBluetooth = false
        var sawWired = false
        for (device in devices) {
            if (isCarLike(device)) return AudioOutputKind.CAR
            if (isBluetooth(device)) sawBluetooth = true
            if (isWired(device)) sawWired = true
        }
        return when {
            sawBluetooth -> AudioOutputKind.BLUETOOTH
            sawWired -> AudioOutputKind.WIRED_HEADPHONES
            else -> AudioOutputKind.THIS_DEVICE
        }
    }

    private fun isBluetooth(device: AudioDeviceInfo): Boolean =
        device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO

    private fun isWired(device: AudioDeviceInfo): Boolean =
        device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && device.type == AudioDeviceInfo.TYPE_USB_HEADSET)

    private fun isCarLike(device: AudioDeviceInfo): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 && device.type == AudioDeviceInfo.TYPE_BUS) {
            return true
        }
        if (device.type != AudioDeviceInfo.TYPE_BLUETOOTH_A2DP) return false
        val name = device.productName?.toString()?.lowercase() ?: return false
        return CAR_NAME_HINTS.any { hint -> name.contains(hint) }
    }

    companion object {
        private val CAR_NAME_HINTS = listOf("car", "auto", "carplay", "android auto", "audi", "bmw", "toyota", "honda")
    }
}
