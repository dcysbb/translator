package com.poozh.translator.data;

import com.poozh.translator.model.TextLanguage;
import kotlin.Metadata;
import kotlin.NoWhenBranchMatchedException;
import kotlin.jvm.internal.Intrinsics;
import kotlin.text.StringsKt;

/* compiled from: DeepSeekPrompt.kt */
@Metadata(d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\bÆ\u0002\u0018\u00002\u00020\u0001B\t\b\u0002¢\u0006\u0004\b\u0002\u0010\u0003J\u000e\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u0007J\u0016\u0010\b\u001a\u00020\u00052\u0006\u0010\t\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u0007¨\u0006\n"}, d2 = {"Lcom/poozh/translator/data/DeepSeekPrompt;", "", "<init>", "()V", "systemPrompt", "", "language", "Lcom/poozh/translator/model/TextLanguage;", "userPrompt", "text", "app_debug"}, k = 1, mv = {2, 0, 0}, xi = 48)
/* loaded from: classes6.dex */
public final class DeepSeekPrompt {
    public static final DeepSeekPrompt INSTANCE = new DeepSeekPrompt();

    /* compiled from: DeepSeekPrompt.kt */
    @Metadata(k = 3, mv = {2, 0, 0}, xi = 48)
    public /* synthetic */ class WhenMappings {
        public static final /* synthetic */ int[] $EnumSwitchMapping$0;

        static {
            int[] iArr = new int[TextLanguage.values().length];
            try {
                iArr[TextLanguage.JAPANESE.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                iArr[TextLanguage.ENGLISH.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                iArr[TextLanguage.UNKNOWN.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            $EnumSwitchMapping$0 = iArr;
        }
    }

    private DeepSeekPrompt() {
    }

    public final String systemPrompt(TextLanguage language) {
        Intrinsics.checkNotNullParameter(language, "language");
        switch (WhenMappings.$EnumSwitchMapping$0[language.ordinal()]) {
            case 1:
                return "你是日语阅读助手。请把用户提供的屏幕 OCR 文本翻译为自然中文，并解析日语语法。\n必须只输出合法 JSON，不要 Markdown，不要解释 JSON 外的内容。\nJSON 字段：sourceText, translation, language, summary, vocabulary, particles, conjugations, fixedExpressions, tone, grammar。\nvocabulary/particles/conjugations/fixedExpressions 是数组，每项包含 surface, reading, meaning, note。\ngrammar 是字符串数组。若某字段无内容，使用空字符串或空数组。";
            case 2:
                return "你是英语阅读助手。请把用户提供的屏幕 OCR 文本翻译为自然中文，并给出简短词句说明。\n必须只输出合法 JSON，不要 Markdown，不要解释 JSON 外的内容。\nJSON 字段：sourceText, translation, language, summary, vocabulary, particles, conjugations, fixedExpressions, tone, grammar。\n英语不需要助词和活用解析，相关数组可为空。";
            case 3:
                return "你是屏幕文本翻译助手。请识别文本主要语言，翻译为自然中文，并在可能时给出简短解析。\n必须只输出合法 JSON，不要 Markdown，不要解释 JSON 外的内容。\nJSON 字段：sourceText, translation, language, summary, vocabulary, particles, conjugations, fixedExpressions, tone, grammar。";
            default:
                throw new NoWhenBranchMatchedException();
        }
    }

    public final String userPrompt(String text, TextLanguage language) {
        Intrinsics.checkNotNullParameter(text, "text");
        Intrinsics.checkNotNullParameter(language, "language");
        return StringsKt.trimIndent("\n            语言提示：" + language.getApiName() + "\n            OCR 文本：\n            " + text + "\n        ");
    }
}
