package com.example.translator

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class PreferencesManager(context: Context) {
    private val sharedPreferences: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        sharedPreferences = EncryptedSharedPreferences.create(
            context,
            "secret_shared_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    var apiKey: String
        get() = sharedPreferences.getString(KEY_API_KEY, "") ?: ""
        set(value) = sharedPreferences.edit().putString(KEY_API_KEY, value).apply()

    var modelName: String
        get() = sharedPreferences.getString(KEY_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
        set(value) = sharedPreferences.edit().putString(KEY_MODEL, value).apply()

    var baseUrl: String
        get() = sharedPreferences.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
        set(value) = sharedPreferences.edit().putString(KEY_BASE_URL, value).apply()

    /** OCR/translation interval in milliseconds. */
    var recognitionIntervalMs: Long
        get() {
            val v = sharedPreferences.getString(KEY_INTERVAL, DEFAULT_INTERVAL.toString())
                ?: DEFAULT_INTERVAL.toString()
            return v.toLongOrNull() ?: DEFAULT_INTERVAL
        }
        set(value) = sharedPreferences.edit()
            .putString(KEY_INTERVAL, value.toString()).apply()

    /** When true, only request translations over Wi-Fi. */
    var wifiOnly: Boolean
        get() = sharedPreferences.getBoolean(KEY_WIFI_ONLY, false)
        set(value) = sharedPreferences.edit().putBoolean(KEY_WIFI_ONLY, value).apply()

    companion object {
        private const val KEY_API_KEY = "api_key"
        private const val KEY_MODEL = "model_name"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_INTERVAL = "recognition_interval_ms"
        private const val KEY_WIFI_ONLY = "wifi_only"

        const val DEFAULT_MODEL = "deepseek-chat"
        const val DEFAULT_BASE_URL = "https://api.deepseek.com/"
        const val DEFAULT_INTERVAL = 1000L
    }
}
