package com.poozh.translator.data

/**
 * Incrementally extracts the value of the JSON string field `"translation"`
 * from a streaming model response, one chunk of text at a time.
 *
 * The model is prompted to emit `"translation"` as the first field, so this
 * extractor can surface a displayable translation to the user well before the
 * full JSON object has arrived. It is deliberately tolerant:
 *   - raw chunks may split a key / value / escape sequence across calls;
 *   - `\"`, `\\`, `\/`, `\b`, `\f`, `\n`, `\r`, `\t` and `\uXXXX` are un-escaped;
 *   - anything before `"translation"` is skipped.
 *
 * Feed every received `delta.content` chunk via [append]. After each append,
 * [currentTranslation] holds the longest displayable translation seen so far.
 * [isComplete] flips true once the translation field's closing quote is read.
 */
class StreamingTranslationExtractor {

    private val raw = StringBuilder()
    private val published = StringBuilder()
    private var state = State.BEFORE_KEY
    // True if the key just read in IN_KEY was "translation".
    private var wantValue = false
    // Position cursor into `raw` that has already been processed.
    private var cursor = 0
    // When reading a \uXXXX, accumulate hex digits here.
    private var unicodeDigits: StringBuilder? = null

    private enum class State {
        BEFORE_KEY,   // scanning for the opening quote of a key
        IN_KEY,       // inside a quoted key, accumulating until closing quote
        AFTER_KEY,    // saw a key, waiting for `:`; if key != translation, also skip its value
        IN_VALUE,     // inside the translation string value
        VALUE_CLOSED, // closing quote of the value consumed
        SKIPPING_VALUE // skipping the value of a non-translation string field
    }

    /** Feed one chunk of model content (a `delta.content` fragment). */
    fun append(chunk: String) {
        raw.append(chunk)
        consume()
    }

    /** The longest displayable translation extracted so far (un-escaped). */
    val currentTranslation: String get() = published.toString()

    /** True once the translation field's closing quote has been processed. */
    val isComplete: Boolean get() = state == State.VALUE_CLOSED

    /**
     * Advance the state machine over the unprocessed portion of [raw]. Stops
     * when the buffer is exhausted or an in-progress token (escape / `\u`) is
     * incomplete and needs more input.
     */
    private fun consume() {
        while (cursor < raw.length) {
            when (state) {
                State.BEFORE_KEY -> {
                    val c = raw[cursor]
                    if (c == '"') {
                        cursor++
                        state = State.IN_KEY
                    } else {
                        cursor++
                    }
                }
                State.IN_KEY -> {
                    // Find the closing quote of the key (honoring escapes).
                    val end = indexUnescapedQuote(raw, cursor)
                    if (end < 0) {
                        // Key spans beyond current buffer; wait for more.
                        return
                    }
                    val key = raw.substring(cursor, end)
                    cursor = end + 1
                    state = State.AFTER_KEY
                    // Remember whether this key is the one we want.
                    wantValue = key == "translation"
                }
                State.AFTER_KEY -> {
                    val c = raw[cursor]
                    when {
                        c == '"' -> {
                            // Opening quote of this field's string value.
                            cursor++
                            if (wantValue) {
                                state = State.IN_VALUE
                                published.setLength(0)
                            } else {
                                // Non-translation string field — consume its closing
                                // quote so inner `"` aren't mistaken for keys.
                                state = State.SKIPPING_VALUE
                            }
                        }
                        c == ':' || c.isWhitespace() || c == ',' -> {
                            cursor++
                        }
                        else -> {
                            // A non-string value (number/bool/array/object) for a
                            // field we don't care about. We can't reliably skip it
                            // character-by-character, so just go back to BEFORE_KEY
                            // and let it rescan; the value's contents will be treated
                            // as noise until the next `"key"` pattern. This is safe
                            // because we only ever publish from a "translation" value.
                            cursor++
                            state = State.BEFORE_KEY
                            wantValue = false
                        }
                    }
                }
                State.SKIPPING_VALUE -> {
                    // Skip over a quoted string value (honoring escapes) until
                    // its closing quote, then go back to scanning for keys.
                    val c = raw[cursor]
                    if (c == '\\') {
                        if (cursor + 1 >= raw.length) return
                        cursor += 2
                    } else if (c == '"') {
                        cursor++
                        state = State.BEFORE_KEY
                    } else {
                        cursor++
                    }
                }
                State.IN_VALUE -> {
                    // Mid-\uXXXX: keep accumulating hex digits.
                    val ud = unicodeDigits
                    if (ud != null) {
                        while (cursor < raw.length && ud.length < 4) {
                            ud.append(raw[cursor])
                            cursor++
                        }
                        if (ud.length >= 4) {
                            val code = ud.toString().toIntOrNull(16) ?: 0xFFFD
                            published.appendCodePoint(code)
                            unicodeDigits = null
                        } else {
                            return // need more digits
                        }
                        continue
                    }
                    val c = raw[cursor]
                    if (c == '\\') {
                        if (cursor + 1 >= raw.length) return // incomplete escape
                        val e = raw[cursor + 1]
                        when (e) {
                            '"' -> published.append('"')
                            '\\' -> published.append('\\')
                            '/' -> published.append('/')
                            'b' -> published.append('\b')
                            'f' -> published.append('\u000C')
                            'n' -> published.append('\n')
                            'r' -> published.append('\r')
                            't' -> published.append('\t')
                            'u' -> {
                                // Start collecting 4 hex digits.
                                unicodeDigits = StringBuilder()
                                cursor += 2
                                continue
                            }
                            else -> published.append(e) // unknown escape: keep raw
                        }
                        cursor += 2
                    } else if (c == '"') {
                        cursor++
                        state = State.VALUE_CLOSED
                    } else {
                        published.append(c)
                        cursor++
                    }
                }
                State.VALUE_CLOSED -> return
            }
        }
    }

    /** Index of the next unescaped `"` in [s] at or after [from], or -1. */
    private fun indexUnescapedQuote(s: StringBuilder, from: Int): Int {
        var i = from
        while (i < s.length) {
            val c = s[i]
            if (c == '\\') { i += 2; continue }
            if (c == '"') return i
            i++
        }
        return -1
    }

    fun reset() {
        raw.setLength(0)
        published.setLength(0)
        cursor = 0
        state = State.BEFORE_KEY
        wantValue = false
        unicodeDigits = null
    }
}
