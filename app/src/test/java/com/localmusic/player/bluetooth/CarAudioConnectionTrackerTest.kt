package com.localmusic.player.bluetooth

import android.bluetooth.BluetoothProfile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CarAudioConnectionTrackerTest {
    @Test
    fun keepsCarModeEnabledUntilTheLastCarAudioDeviceDisconnects() {
        val tracker = CarAudioConnectionTracker()

        assertTrue(tracker.update("car-one", true, BluetoothProfile.STATE_CONNECTED))
        assertTrue(tracker.update("car-two", true, BluetoothProfile.STATE_CONNECTED))
        assertTrue(tracker.update("car-one", true, BluetoothProfile.STATE_DISCONNECTED))
        assertFalse(tracker.update("car-two", true, BluetoothProfile.STATE_DISCONNECTED))
    }

    @Test
    fun replacesConnectedDevicesFromProfileSnapshot() {
        val tracker = CarAudioConnectionTracker()

        assertTrue(tracker.replaceConnectedDevices(listOf("car-one", "car-two")))
        assertTrue(tracker.update("car-one", true, BluetoothProfile.STATE_DISCONNECTED))
        assertFalse(tracker.replaceConnectedDevices(emptyList()))
    }

    @Test
    fun ignoresConnectionChangesForNonCarDevices() {
        val tracker = CarAudioConnectionTracker()

        assertFalse(tracker.update("headphones", false, BluetoothProfile.STATE_CONNECTED))
    }
}
