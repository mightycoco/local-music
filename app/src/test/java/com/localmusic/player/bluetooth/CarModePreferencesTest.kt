package com.localmusic.player.bluetooth

import com.localmusic.player.playlist.FakeSharedPreferences
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CarModePreferencesTest {
    @Test
    fun manualCarModeOverrideDefaultsToDisabledAndPersists() {
        val preferences = CarModePreferences(FakeSharedPreferences())

        assertFalse(preferences.isManuallyEnabled())

        preferences.setManuallyEnabled(true)

        assertTrue(preferences.isManuallyEnabled())
    }

    @Test
    fun markedCarDevicesPersistAndCanBeRemoved() {
        val preferences = CarModePreferences(FakeSharedPreferences())

        preferences.setCarDeviceMarked("car-stereo", true)

        assertTrue("car-stereo" in preferences.markedCarDeviceIds())

        preferences.setCarDeviceMarked("car-stereo", false)

        assertFalse("car-stereo" in preferences.markedCarDeviceIds())
    }
}
