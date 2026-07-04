package com.example.translator

/**
 * De-duplicates OCR output so we don't keep sending near-identical text to
 * DeepSeek. OCR is noisy: the same on-screen text can flicker slightly between
 * frames, so we normalize before comparing.
 *
 * - [shouldTranslate] returns true only when the normalized text is new (or
 *   sufficiently different from the last request).
 * - [remember] records the text we just sent so subsequent identical frames
 *   are skipped.
 */
class OcrTextCache(
    /** 0..1: how similar two normalized strings must be to be considered the same. */
    private val similarityThreshold: Double = 0.92
) {
    private var lastSent: String = ""

    /** Normalize whitespace and drop common OCR punctuation noise. */
    fun normalize(text: String): String =
        text
            .replace('\r', ' ')
            .replace('\n', ' ')
            .trim()
            .replace(Regex("\\s+"), " ")
            .lowercase()

    /**
     * True if [text] differs enough from the last sent text to warrant a new
     * translation request. The very first call always returns true.
     */
    fun shouldTranslate(text: String): Boolean {
        val normalized = normalize(text)
        if (normalized.isBlank()) return false
        if (lastSent.isEmpty()) return true
        return similarity(lastSent, normalized) < similarityThreshold
    }

    /** Record [text] as the most recently dispatched request. */
    fun remember(text: String) {
        lastSent = normalize(text)
    }

    /** Clear cached state (e.g. when the user picks a new selection). */
    fun reset() {
        lastSent = ""
    }

    /**
     * Token-sortbag similarity in 0..1. Cheap and robust to small OCR noise and
     * word reorder. 1.0 means identical after normalization.
     */
    fun similarity(a: String, b: String): Double {
        if (a == b) return 1.0
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val ta = multiset(a)
        val tb = multiset(b)
        var intersection = 0L
        for ((k, v) in ta) {
            intersection += minOf(v.toLong(), (tb[k] ?: 0).toLong())
        }
        val union = ta.values.sum().toLong() + tb.values.sum().toLong() - intersection
        return if (union == 0L) 1.0 else intersection.toDouble() / union
    }

    private fun multiset(s: String): Map<String, Int> {
        val counts = HashMap<String, Int>()
        for (tok in s.split(' ')) {
            if (tok.isEmpty()) continue
            counts[tok] = (counts[tok] ?: 0) + 1
        }
        return counts
    }
}
