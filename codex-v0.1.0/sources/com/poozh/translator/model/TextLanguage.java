package com.poozh.translator.model;

import androidx.core.os.EnvironmentCompat;
import kotlin.Metadata;
import kotlin.enums.EnumEntries;
import kotlin.enums.EnumEntriesKt;

/* compiled from: TextLanguage.kt */
@Metadata(d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0000\n\u0002\u0010\u000e\n\u0002\b\b\b\u0086\u0081\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0011\b\u0002\u0012\u0006\u0010\u0002\u001a\u00020\u0003¢\u0006\u0004\b\u0004\u0010\u0005R\u0011\u0010\u0002\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0007j\u0002\b\bj\u0002\b\tj\u0002\b\n¨\u0006\u000b"}, d2 = {"Lcom/poozh/translator/model/TextLanguage;", "", "apiName", "", "<init>", "(Ljava/lang/String;ILjava/lang/String;)V", "getApiName", "()Ljava/lang/String;", "JAPANESE", "ENGLISH", "UNKNOWN", "app_debug"}, k = 1, mv = {2, 0, 0}, xi = 48)
/* loaded from: classes7.dex */
public enum TextLanguage {
    JAPANESE("ja"),
    ENGLISH("en"),
    UNKNOWN(EnvironmentCompat.MEDIA_UNKNOWN);

    private final String apiName;
    private static final /* synthetic */ EnumEntries $ENTRIES = EnumEntriesKt.enumEntries($VALUES);

    TextLanguage(String apiName) {
        this.apiName = apiName;
    }

    public final String getApiName() {
        return this.apiName;
    }

    public static EnumEntries<TextLanguage> getEntries() {
        return $ENTRIES;
    }
}
