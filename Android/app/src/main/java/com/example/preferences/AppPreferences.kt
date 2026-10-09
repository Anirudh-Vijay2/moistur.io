package com.example.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.model.ThemeMode
import com.example.model.UpdateFrequency

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("moistur_io_prefs", Context.MODE_PRIVATE)

    var esp32Ip: String
        get() = prefs.getString(KEY_ESP32_IP, DEFAULT_IP) ?: DEFAULT_IP
        set(value) = prefs.edit().putString(KEY_ESP32_IP, value.trim()).apply()

    var themeMode: ThemeMode
        get() {
            val name = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
            return try {
                ThemeMode.valueOf(name ?: ThemeMode.SYSTEM.name)
            } catch (e: Exception) {
                ThemeMode.SYSTEM
            }
        }
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value.name).apply()

    var updateFrequency: UpdateFrequency
        get() {
            val interval = prefs.getLong(KEY_UPDATE_FREQ, UpdateFrequency.TWO_SECONDS.intervalMs)
            return UpdateFrequency.fromInterval(interval)
        }
        set(value) = prefs.edit().putLong(KEY_UPDATE_FREQ, value.intervalMs).apply()

    var lastMoisture: Int
        get() = prefs.getInt(KEY_LAST_MOISTURE, 50)
        set(value) = prefs.edit().putInt(KEY_LAST_MOISTURE, value).apply()

    var lastPumpState: Boolean
        get() = prefs.getBoolean(KEY_LAST_PUMP, false)
        set(value) = prefs.edit().putBoolean(KEY_LAST_PUMP, value).apply()

    companion object {
        private const val KEY_ESP32_IP = "key_esp32_ip"
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_UPDATE_FREQ = "key_update_freq"
        private const val KEY_LAST_MOISTURE = "key_last_moisture"
        private const val KEY_LAST_PUMP = "key_last_pump"
        const val DEFAULT_IP = "172.25.89.50"
    }
}
