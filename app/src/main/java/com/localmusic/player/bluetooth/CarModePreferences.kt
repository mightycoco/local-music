package com.localmusic.player.bluetooth

import android.content.SharedPreferences

/** Persistent user override for the simplified, driving-safe player controls. */
class CarModePreferences(private val sharedPreferences: SharedPreferences) {
    fun isManuallyEnabled(): Boolean = sharedPreferences.getBoolean(KEY_MANUALLY_ENABLED, false)

    fun setManuallyEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_MANUALLY_ENABLED, enabled).apply()
    }

    private companion object {
        const val KEY_MANUALLY_ENABLED = "car_mode_manually_enabled"
    }
}
