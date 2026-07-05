package com.poozh.translator.data

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class OverlayWindowState(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

class AppSettings(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): SettingsSnapshot {
        return SettingsSnapshot(
            apiKey = decrypt(prefs.getString(KEY_API_KEY, "").orEmpty()),
            baseUrl = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL).orEmpty().ifBlank { DEFAULT_BASE_URL },
            model = prefs.getString(KEY_MODEL, DEFAULT_MODEL).orEmpty().ifBlank { DEFAULT_MODEL },
            intervalMs = prefs.getLong(KEY_INTERVAL_MS, DEFAULT_INTERVAL_MS).coerceIn(500L, 5000L),
            wifiOnly = prefs.getBoolean(KEY_WIFI_ONLY, false)
        )
    }

    fun save(baseUrl: String, model: String, intervalMs: Long, wifiOnly: Boolean) {
        prefs.edit()
            .putString(KEY_BASE_URL, baseUrl.ifBlank { DEFAULT_BASE_URL })
            .putString(KEY_MODEL, model.ifBlank { DEFAULT_MODEL })
            .putLong(KEY_INTERVAL_MS, intervalMs.coerceIn(500L, 5000L))
            .putBoolean(KEY_WIFI_ONLY, wifiOnly)
            .apply()
    }

    fun saveApiKey(apiKey: String) {
        prefs.edit().putString(KEY_API_KEY, encrypt(apiKey.trim())).apply()
    }

    fun clearApiKey() {
        prefs.edit().remove(KEY_API_KEY).apply()
    }

    fun loadBubblePosition(defaultX: Int, defaultY: Int): Pair<Int, Int> {
        return Pair(
            prefs.getInt(KEY_BUBBLE_X, defaultX),
            prefs.getInt(KEY_BUBBLE_Y, defaultY)
        )
    }

    fun saveBubblePosition(x: Int, y: Int) {
        prefs.edit()
            .putInt(KEY_BUBBLE_X, x)
            .putInt(KEY_BUBBLE_Y, y)
            .apply()
    }

    fun loadPanelState(defaultX: Int, defaultY: Int, defaultWidth: Int, defaultHeight: Int): OverlayWindowState {
        return OverlayWindowState(
            x = prefs.getInt(KEY_PANEL_X, defaultX),
            y = prefs.getInt(KEY_PANEL_Y, defaultY),
            width = prefs.getInt(KEY_PANEL_WIDTH, defaultWidth),
            height = prefs.getInt(KEY_PANEL_HEIGHT, defaultHeight)
        )
    }

    fun savePanelState(x: Int, y: Int, width: Int, height: Int) {
        prefs.edit()
            .putInt(KEY_PANEL_X, x)
            .putInt(KEY_PANEL_Y, y)
            .putInt(KEY_PANEL_WIDTH, width)
            .putInt(KEY_PANEL_HEIGHT, height)
            .apply()
    }

    private fun encrypt(value: String): String {
        if (value.isBlank()) return ""
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val cipherBytes = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
            val combined = cipher.iv + cipherBytes
            Base64.encodeToString(combined, Base64.NO_WRAP)
        }.getOrDefault("")
    }

    private fun decrypt(encoded: String): String {
        if (encoded.isBlank()) return ""
        return runCatching {
            val combined = Base64.decode(encoded, Base64.NO_WRAP)
            if (combined.size <= GCM_IV_LENGTH_BYTES) return@runCatching ""
            val iv = combined.copyOfRange(0, GCM_IV_LENGTH_BYTES)
            val cipherBytes = combined.copyOfRange(GCM_IV_LENGTH_BYTES, combined.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8)
        }.getOrDefault("")
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://api.deepseek.com/chat/completions"
        const val DEFAULT_MODEL = "deepseek-v4-flash"
        const val DEFAULT_INTERVAL_MS = 1000L

        private const val PREFS_NAME = "translator_settings"
        private const val KEY_ALIAS = "screen_translator_api_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH_BYTES = 12
        private const val GCM_TAG_LENGTH_BITS = 128

        private const val KEY_API_KEY = "api_key"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_MODEL = "model"
        private const val KEY_INTERVAL_MS = "interval_ms"
        private const val KEY_WIFI_ONLY = "wifi_only"
        private const val KEY_BUBBLE_X = "bubble_x"
        private const val KEY_BUBBLE_Y = "bubble_y"
        private const val KEY_PANEL_X = "panel_x"
        private const val KEY_PANEL_Y = "panel_y"
        private const val KEY_PANEL_WIDTH = "panel_width"
        private const val KEY_PANEL_HEIGHT = "panel_height"
    }
}
