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

    // ───────────── Provider selection ─────────────
    //
    // The active provider id drives which key/model/baseUrl are returned by
    // [load]. API key and model are stored per-provider so switching never
    // loses a configured key/model.

    /** The currently selected provider id (see [ModelProviders]). */
    var currentProviderId: String
        get() = prefs.getString(KEY_PROVIDER, ModelProviders.DEFAULT_PROVIDER_ID)
            ?: ModelProviders.DEFAULT_PROVIDER_ID
        private set(value) = prefs.edit().putString(KEY_PROVIDER, value).apply()

    fun getApiKey(providerId: String): String =
        decrypt(prefs.getString(apiKeyFor(providerId), "").orEmpty())

    fun saveApiKey(providerId: String, apiKey: String) =
        prefs.edit().putString(apiKeyFor(providerId), encrypt(apiKey.trim())).apply()

    fun getModel(providerId: String): String =
        prefs.getString(modelKeyFor(providerId), "").orEmpty()

    fun saveModel(providerId: String, model: String) =
        prefs.edit().putString(modelKeyFor(providerId), model).apply()

    private fun apiKeyFor(providerId: String) = "api_key_$providerId"
    private fun modelKeyFor(providerId: String) = "model_$providerId"

    /** Switch the active provider and persist it. */
    fun selectProvider(providerId: String) {
        currentProviderId = providerId
    }

    fun load(): SettingsSnapshot {
        migrateLegacyIfNeeded()
        // Always repair a known-bad Paratera model id (case-sensitive on the
        // provider). Cheap and idempotent.
        fixParateraModelName()
        val provider = ModelProviders.byId(currentProviderId)
        val model = getModel(provider.id).ifBlank { provider.defaultModel }
        // baseUrl falls back to the preset; users can override per-provider via
        // [saveBaseUrl] (the UI normally just uses the preset value).
        val baseUrl = prefs.getString(baseUrlFor(provider.id), null) ?: provider.baseUrl
        return SettingsSnapshot(
            providerId = provider.id,
            apiKey = getApiKey(provider.id),
            baseUrl = baseUrl,
            model = model,
            intervalMs = prefs.getLong(KEY_INTERVAL_MS, DEFAULT_INTERVAL_MS).coerceIn(500L, 5000L),
            wifiOnly = prefs.getBoolean(KEY_WIFI_ONLY, false),
            thinkingEnabled = prefs.getBoolean(KEY_THINKING_ENABLED, true)
        )
    }

    fun save(
        providerId: String,
        baseUrl: String,
        model: String,
        intervalMs: Long,
        wifiOnly: Boolean,
        thinkingEnabled: Boolean = true
    ) {
        selectProvider(providerId)
        prefs.edit()
            .putString(baseUrlFor(providerId), baseUrl.ifBlank { ModelProviders.byId(providerId).baseUrl })
            .putString(modelKeyFor(providerId), model.ifBlank { ModelProviders.byId(providerId).defaultModel })
            .putLong(KEY_INTERVAL_MS, intervalMs.coerceIn(500L, 5000L))
            .putBoolean(KEY_WIFI_ONLY, wifiOnly)
            .putBoolean(KEY_THINKING_ENABLED, thinkingEnabled)
            .apply()
    }

    private fun baseUrlFor(providerId: String) = "base_url_$providerId"

    /** Save the API key for the *current* provider (convenience for the UI). */
    fun saveApiKeyForCurrent(apiKey: String) {
        saveApiKey(currentProviderId, apiKey)
    }

    fun clearApiKey() {
        prefs.edit().remove(apiKeyFor(currentProviderId)).apply()
    }

    /**
     * One-time migrations:
     * 1. Legacy flat `api_key`/`base_url`/`model` (pre-multi-provider) → the
     *    current provider's slots, so existing users keep their config.
     * 2. Seed bundled default API keys (e.g. the Paratera demo key) into their
     *    providers, but only when the user hasn't already set/cleared one.
     */
    private fun migrateLegacyIfNeeded() {
        if (prefs.getBoolean(KEY_MIGRATED, false)) return
        // Legacy single-provider values → current provider.
        val legacyKey = decrypt(prefs.getString(KEY_API_KEY_LEGACY, "").orEmpty())
        if (legacyKey.isNotBlank() && getApiKey(currentProviderId).isEmpty()) {
            saveApiKey(currentProviderId, legacyKey)
        }
        // Seed bundled keys.
        for ((providerId, key) in DEFAULT_API_KEYS) {
            if (getApiKey(providerId).isEmpty()) {
                saveApiKey(providerId, key)
            }
        }
        prefs.edit().putBoolean(KEY_MIGRATED, true).apply()
        fixParateraModelName()
    }

    /**
     * Earlier builds seeded Paratera with the wrong model id
     * (`deepseek-v4-flash` / `deepseek-r1`, lowercase). Paratera's model ids
     * are case-sensitive and the lowercase form is rejected with HTTP 400, so
     * translations silently failed. Rewrite any known-bad value to the correct
     * `DeepSeek-V4-Flash`. Idempotent — runs every launch but only writes when
     * the stored value is one of the known-bad strings.
     */
    private fun fixParateraModelName() {
        val current = getModel("paratera")
        if (current in BAD_PARATERA_MODELS) {
            saveModel("paratera", "DeepSeek-V4-Flash")
        }
    }

    /** Overlay window opacity (bubble + panel). 0.3..1.0, default 1.0. */
    var overlayOpacity: Float
        get() = prefs.getFloat(KEY_OVERLAY_OPACITY, 1.0f).coerceIn(MIN_OVERLAY_OPACITY, 1.0f)
        set(value) = prefs.edit()
            .putFloat(KEY_OVERLAY_OPACITY, value.coerceIn(MIN_OVERLAY_OPACITY, 1.0f))
            .apply()

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

        private const val KEY_PROVIDER = "current_provider_id"
        private const val KEY_MIGRATED = "providers_migrated"
        // Legacy flat keys (pre-multi-provider) — read once for migration only.
        private const val KEY_API_KEY_LEGACY = "api_key"
        private const val KEY_INTERVAL_MS = "interval_ms"
        private const val KEY_WIFI_ONLY = "wifi_only"
        private const val KEY_THINKING_ENABLED = "thinking_enabled"
        private const val KEY_OVERLAY_OPACITY = "overlay_opacity"
        /** Lowest allowed overlay opacity — below this the overlay becomes hard
         *  to see/tap. */
        private const val MIN_OVERLAY_OPACITY = 0.3f
        private const val KEY_BUBBLE_X = "bubble_x"
        private const val KEY_BUBBLE_Y = "bubble_y"
        private const val KEY_PANEL_X = "panel_x"
        private const val KEY_PANEL_Y = "panel_y"
        private const val KEY_PANEL_WIDTH = "panel_width"

        /**
         * API keys bundled with the APK so the listed providers work without the
         * user having to sign up first. Seeded into prefs once (see
         * [migrateLegacyIfNeeded]) only when the user hasn't already set their
         * own key for that provider.
         *
         * ⚠️ Bundled keys ship in the APK — anyone with the APK has access.
         * Replace with user-supplied keys before any public release.
         */
        private val DEFAULT_API_KEYS: Map<String, String> = mapOf(
            "paratera" to "sk-kjO3KDWZ2XuUJG87sM8d6Q"
        )

        /** Paratera model ids that earlier builds seeded incorrectly. They are
         *  rejected by the provider (HTTP 400 "no healthy deployments") because
         *  Paratera ids are case-sensitive. Mapped to the correct value in
         *  [fixParateraModelName]. */
        private val BAD_PARATERA_MODELS: Set<String> = setOf(
            "deepseek-v4-flash", "deepseek-r1", "deepseek-v3", "qwen2.5-72b-instruct"
        )
        private const val KEY_PANEL_HEIGHT = "panel_height"
    }
}
