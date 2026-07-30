package com.localmusic.player.bluetooth

import android.bluetooth.BluetoothProfile

/** Tracks the currently connected Bluetooth devices that are classified as car audio. */
class CarAudioConnectionTracker {
    private val connectedCarDeviceIds = mutableSetOf<String>()

    fun replaceConnectedDevices(deviceIds: Collection<String>): Boolean {
        connectedCarDeviceIds.clear()
        connectedCarDeviceIds.addAll(deviceIds)
        return isCarAudioConnected()
    }

    fun update(deviceId: String, isCarDevice: Boolean, connectionState: Int): Boolean {
        if (!isCarDevice) return isCarAudioConnected()

        when (connectionState) {
            BluetoothProfile.STATE_CONNECTED -> connectedCarDeviceIds += deviceId
            BluetoothProfile.STATE_DISCONNECTED -> connectedCarDeviceIds -= deviceId
        }
        return isCarAudioConnected()
    }

    private fun isCarAudioConnected(): Boolean = connectedCarDeviceIds.isNotEmpty()
}
