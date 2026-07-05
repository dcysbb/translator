package com.poozh.translator.data

data class SettingsSnapshot(
    val apiKey: String,
    val baseUrl: String,
    val model: String,
    val intervalMs: Long,
    val wifiOnly: Boolean
)

