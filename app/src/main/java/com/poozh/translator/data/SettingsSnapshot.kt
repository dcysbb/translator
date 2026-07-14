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
    val thinkingEnabled: Boolean = false,
    /** Custom system prompt. When non-blank, replaces the built-in per-language
     *  prompt entirely. Use {text} as a placeholder for the OCR text, or omit
     *  it to have the text appended automatically as the user message. */
    val customSystemPrompt: String = ""
)

