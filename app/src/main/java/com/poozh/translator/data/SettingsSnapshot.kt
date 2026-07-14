package com.poozh.translator.data

data class SettingsSnapshot(
    val providerId: String,
    val apiKey: String,
    val baseUrl: String,
    val model: String,
    val intervalMs: Long,
    val wifiOnly: Boolean,
    /** Whether to enable the reasoning model's thinking phase. When false,
     *  `thinking:{type:"disabled"}` is sent so the model answers directly
     *  without a chain-of-thought (much faster). */
    val thinkingEnabled: Boolean = false
)

