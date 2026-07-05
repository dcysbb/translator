package com.example.translator

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

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
        migrateLegacyIfNeeded()
    }

    // ───────────────────────── Provider selection ──────────────────────────

    /** The currently selected provider id (see [ModelProviders]). */
    var currentProviderId: String
        get() = sharedPreferences.getString(KEY_PROVIDER, ModelProviders.DEFAULT_ID)
            ?: ModelProviders.DEFAULT_ID
        set(value) = sharedPreferences.edit().putString(KEY_PROVIDER, value).apply()

    /** The active [ModelProvider] preset, derived from [currentProviderId]. */
    val currentProvider: ModelProvider
        get() {
            val preset = ModelProviders.byId(currentProviderId)
            // For the "Custom" provider, the user supplies the base URL.
            return if (preset.id == ModelProviders.CUSTOM_ID) {
                preset.copy(baseUrl = customBaseUrl)
            } else {
                preset
            }
        }

    /** Base URL for the "Custom" provider (ignored for built-in presets). */
    var customBaseUrl: String
        get() = sharedPreferences.getString(KEY_CUSTOM_BASE_URL, "") ?: ""
        set(value) = sharedPreferences.edit().putString(KEY_CUSTOM_BASE_URL, value).apply()

    // ───────────────────────── Per-provider credentials ────────────────────
    //
    // API key and model are stored per-provider (key = "<type>_<providerId>")
    // so switching providers never loses a configured key/model.

    fun getApiKey(providerId: String): String =
        sharedPreferences.getString(apiKeyFor(providerId), "") ?: ""

    fun setApiKey(providerId: String, value: String) =
        sharedPreferences.edit().putString(apiKeyFor(providerId), value).apply()

    fun getModel(providerId: String): String =
        sharedPreferences.getString(modelKeyFor(providerId), "") ?: ""

    fun setModel(providerId: String, value: String) =
        sharedPreferences.edit().putString(modelKeyFor(providerId), value).apply()

    private fun apiKeyFor(providerId: String) = "api_key_$providerId"
    private fun modelKeyFor(providerId: String) = "model_$providerId"

    // ───────────── Convenience accessors for the active provider ────────────

    /** API key for the currently selected provider. */
    val apiKey: String
        get() = getApiKey(currentProviderId)

    /** Model for the currently selected provider (falls back to the preset default). */
    val modelName: String
        get() {
            val stored = getModel(currentProviderId)
            return stored.ifBlank { currentProvider.defaultModel }
        }

    /** Base URL for the currently selected provider. */
    val baseUrl: String
        get() = currentProvider.baseUrl

    // ───────────────────────── Legacy compatibility ────────────────────────
    //
    // Older code wrote flat "api_key"/"model_name" keys. On first run after
    // upgrade, migrate them into the DeepSeek provider so existing users keep
    // their configured key. Idempotent (guarded by a migration flag).
    private fun migrateLegacyIfNeeded() {
        if (sharedPreferences.getBoolean(KEY_MIGRATED, false)) return
        val legacyKey = sharedPreferences.getString(LEGACY_KEY_API_KEY, null)
        val legacyModel = sharedPreferences.getString(LEGACY_KEY_MODEL, null)
        if (legacyKey != null && getApiKey("deepseek").isEmpty()) {
            setApiKey("deepseek", legacyKey)
        }
        if (legacyModel != null && getModel("deepseek").isEmpty()) {
            setModel("deepseek", legacyModel)
        }
        sharedPreferences.edit().putBoolean(KEY_MIGRATED, true).apply()
    }

    /** When true, only request translations over Wi-Fi. */
    var wifiOnly: Boolean
        get() = sharedPreferences.getBoolean(KEY_WIFI_ONLY, false)
        set(value) = sharedPreferences.edit().putBoolean(KEY_WIFI_ONLY, value).apply()

    // ───────────────────────── Translation history ──────────────────────────
    //
    // Persisted as a JSON array under [KEY_HISTORY]. Only the most recent
    // [MAX_HISTORY] entries are kept; older ones are dropped on save.

    private val gson = Gson()

    fun loadHistory(): List<HistoryItem> {
        val json = sharedPreferences.getString(KEY_HISTORY, null) ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<HistoryItem>>() {}.type
            gson.fromJson<List<HistoryItem>>(json, type) ?: emptyList()
        }.getOrDefault(emptyList())
    }

    fun saveHistory(items: List<HistoryItem>) {
        sharedPreferences.edit()
            .putString(KEY_HISTORY, gson.toJson(items))
            .apply()
    }

    companion object {
        private const val KEY_PROVIDER = "current_provider_id"
        private const val KEY_CUSTOM_BASE_URL = "custom_base_url"
        private const val KEY_WIFI_ONLY = "wifi_only"
        private const val KEY_HISTORY = "translation_history"
        private const val KEY_MIGRATED = "providers_migrated"

        // Legacy flat keys (pre-multi-provider).
        private const val LEGACY_KEY_API_KEY = "api_key"
        private const val LEGACY_KEY_MODEL = "model_name"

        const val MAX_HISTORY = 50
    }
}
