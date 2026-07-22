package com.localmusic.player.bluetooth

import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice

/** Detects likely car audio devices from Bluetooth class metadata. */
class CarModeDetector {
    fun isLikelyCarDevice(device: BluetoothDevice?): Boolean {
        val deviceClass = device?.bluetoothClass?.deviceClass ?: return false
        return deviceClass == BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO
    }
}