package com.localmusic.player.bluetooth

import android.content.SharedPreferences

/** Persistent user override for the simplified, driving-safe player controls. */
class CarModePreferences(private val sharedPreferences: SharedPreferences) {
    fun isManuallyEnabled(): Boolean = sharedPreferences.getBoolean(KEY_MANUALLY_ENABLED, false)

    fun setManuallyEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_MANUALLY_ENABLED, enabled).apply()
    }

    fun markedCarDeviceIds(): Set<String> =
        sharedPreferences.getStringSet(KEY_MARKED_CAR_DEVICE_IDS, emptySet()).orEmpty()

    fun setCarDeviceMarked(deviceId: String, marked: Boolean) {
        val updatedIds = markedCarDeviceIds().toMutableSet()
        if (marked) updatedIds += deviceId else updatedIds -= deviceId
        sharedPreferences.edit().putStringSet(KEY_MARKED_CAR_DEVICE_IDS, updatedIds).apply()
    }

    private companion object {
        const val KEY_MANUALLY_ENABLED = "car_mode_manually_enabled"
        const val KEY_MARKED_CAR_DEVICE_IDS = "car_mode_marked_car_device_ids"
    }
}
