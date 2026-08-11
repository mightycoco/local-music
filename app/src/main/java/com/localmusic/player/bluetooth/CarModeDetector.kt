package com.localmusic.player.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice

/** Detects likely car audio devices from Bluetooth class metadata. */
class CarModeDetector {
    @SuppressLint("MissingPermission")
    fun isLikelyCarDevice(device: BluetoothDevice?): Boolean {
        val deviceClass =
                runCatching { device?.bluetoothClass?.deviceClass }.getOrNull() ?: return false
        return deviceClass == BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO
    }
}
