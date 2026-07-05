package com.poozh.translator.model;

import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import kotlin.sequences.SequencesKt;
import kotlin.text.Regex;
import kotlin.text.StringsKt;

/* compiled from: TextLanguage.kt */
@Metadata(d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\bÆ\u0002\u0018\u00002\u00020\u0001B\t\b\u0002¢\u0006\u0004\b\u0002\u0010\u0003J\u000e\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u000bR\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004¢\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0005X\u0082\u0004¢\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0005X\u0082\u0004¢\u0006\u0002\n\u0000¨\u0006\f"}, d2 = {"Lcom/poozh/translator/model/LanguageDetector;", "", "<init>", "()V", "japaneseKana", "Lkotlin/text/Regex;", "cjk", "latin", "detect", "Lcom/poozh/translator/model/TextLanguage;", "text", "", "app_debug"}, k = 1, mv = {2, 0, 0}, xi = 48)
/* loaded from: classes7.dex */
public final class LanguageDetector {
    public static final LanguageDetector INSTANCE = new LanguageDetector();
    private static final Regex japaneseKana = new Regex("[\\u3040-\\u30ff\\u31f0-\\u31ff]");
    private static final Regex cjk = new Regex("[\\u3400-\\u9fff]");
    private static final Regex latin = new Regex("[A-Za-z]");

    private LanguageDetector() {
    }

    public final TextLanguage detect(String text) {
        Intrinsics.checkNotNullParameter(text, "text");
        String normalized = StringsKt.trim((CharSequence) text).toString();
        if (normalized.length() == 0) {
            return TextLanguage.UNKNOWN;
        }
        if (japaneseKana.containsMatchIn(normalized)) {
            return TextLanguage.JAPANESE;
        }
        int cjkCount = SequencesKt.count(Regex.findAll$default(cjk, normalized, 0, 2, null));
        int latinCount = SequencesKt.count(Regex.findAll$default(latin, normalized, 0, 2, null));
        return (cjkCount <= 0 || latinCount >= cjkCount) ? latinCount > 0 ? TextLanguage.ENGLISH : TextLanguage.UNKNOWN : TextLanguage.JAPANESE;
    }
}
