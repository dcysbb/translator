package com.example.translator

/**
 * One saved translation entry: the original (OCR'd) text, the formatted
 * translation result, and when it was produced. Persisted as JSON via
 * [PreferencesManager]; the most recent [PreferencesManager.MAX_HISTORY] items
 * are kept.
 */
data class HistoryItem(
    val original: String,
    val translation: String,
    val timestamp: Long
)
