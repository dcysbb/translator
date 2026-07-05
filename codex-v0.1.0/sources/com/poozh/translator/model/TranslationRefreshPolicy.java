package com.poozh.translator.model;

import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import kotlin.text.StringsKt;

/* compiled from: TranslationRefreshPolicy.kt */
@Metadata(d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0002\bÆ\u0002\u0018\u00002\u00020\u0001B\t\b\u0002¢\u0006\u0004\b\u0002\u0010\u0003J&\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\u00072\u0006\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\n¨\u0006\f"}, d2 = {"Lcom/poozh/translator/model/TranslationRefreshPolicy;", "", "<init>", "()V", "decide", "Lcom/poozh/translator/model/RefreshAction;", "currentText", "", "lastText", "hasCachedResult", "", "forceTranslate", "app_debug"}, k = 1, mv = {2, 0, 0}, xi = 48)
/* loaded from: classes7.dex */
public final class TranslationRefreshPolicy {
    public static final TranslationRefreshPolicy INSTANCE = new TranslationRefreshPolicy();

    private TranslationRefreshPolicy() {
    }

    public final RefreshAction decide(String currentText, String lastText, boolean hasCachedResult, boolean forceTranslate) {
        Intrinsics.checkNotNullParameter(currentText, "currentText");
        Intrinsics.checkNotNullParameter(lastText, "lastText");
        if (StringsKt.isBlank(currentText)) {
            return RefreshAction.IGNORE_EMPTY;
        }
        if (!forceTranslate && hasCachedResult && Intrinsics.areEqual(currentText, lastText)) {
            return RefreshAction.REUSE_CACHED_RESULT;
        }
        return RefreshAction.REQUEST_TRANSLATION;
    }
}
