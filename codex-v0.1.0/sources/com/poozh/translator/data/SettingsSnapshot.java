package com.poozh.translator.data;

import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* compiled from: SettingsSnapshot.kt */
@Metadata(d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0013\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B/\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\u0006\u0010\u0005\u001a\u00020\u0003\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t¢\u0006\u0004\b\n\u0010\u000bJ\t\u0010\u0014\u001a\u00020\u0003HÆ\u0003J\t\u0010\u0015\u001a\u00020\u0003HÆ\u0003J\t\u0010\u0016\u001a\u00020\u0003HÆ\u0003J\t\u0010\u0017\u001a\u00020\u0007HÆ\u0003J\t\u0010\u0018\u001a\u00020\tHÆ\u0003J;\u0010\u0019\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00032\b\b\u0002\u0010\u0006\u001a\u00020\u00072\b\b\u0002\u0010\b\u001a\u00020\tHÆ\u0001J\u0013\u0010\u001a\u001a\u00020\t2\b\u0010\u001b\u001a\u0004\u0018\u00010\u0001HÖ\u0003J\t\u0010\u001c\u001a\u00020\u001dHÖ\u0001J\t\u0010\u001e\u001a\u00020\u0003HÖ\u0001R\u0011\u0010\u0002\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rR\u0011\u0010\u0004\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\rR\u0011\u0010\u0005\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\rR\u0011\u0010\u0006\u001a\u00020\u0007¢\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u0011R\u0011\u0010\b\u001a\u00020\t¢\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0013¨\u0006\u001f"}, d2 = {"Lcom/poozh/translator/data/SettingsSnapshot;", "", "apiKey", "", "baseUrl", "model", "intervalMs", "", "wifiOnly", "", "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JZ)V", "getApiKey", "()Ljava/lang/String;", "getBaseUrl", "getModel", "getIntervalMs", "()J", "getWifiOnly", "()Z", "component1", "component2", "component3", "component4", "component5", "copy", "equals", "other", "hashCode", "", "toString", "app_debug"}, k = 1, mv = {2, 0, 0}, xi = 48)
/* loaded from: classes6.dex */
public final /* data */ class SettingsSnapshot {
    private final String apiKey;
    private final String baseUrl;
    private final long intervalMs;
    private final String model;
    private final boolean wifiOnly;

    public static /* synthetic */ SettingsSnapshot copy$default(SettingsSnapshot settingsSnapshot, String str, String str2, String str3, long j, boolean z, int i, Object obj) {
        if ((i & 1) != 0) {
            str = settingsSnapshot.apiKey;
        }
        if ((i & 2) != 0) {
            str2 = settingsSnapshot.baseUrl;
        }
        if ((i & 4) != 0) {
            str3 = settingsSnapshot.model;
        }
        if ((i & 8) != 0) {
            j = settingsSnapshot.intervalMs;
        }
        if ((i & 16) != 0) {
            z = settingsSnapshot.wifiOnly;
        }
        boolean z2 = z;
        String str4 = str3;
        return settingsSnapshot.copy(str, str2, str4, j, z2);
    }

    /* renamed from: component1, reason: from getter */
    public final String getApiKey() {
        return this.apiKey;
    }

    /* renamed from: component2, reason: from getter */
    public final String getBaseUrl() {
        return this.baseUrl;
    }

    /* renamed from: component3, reason: from getter */
    public final String getModel() {
        return this.model;
    }

    /* renamed from: component4, reason: from getter */
    public final long getIntervalMs() {
        return this.intervalMs;
    }

    /* renamed from: component5, reason: from getter */
    public final boolean getWifiOnly() {
        return this.wifiOnly;
    }

    public final SettingsSnapshot copy(String apiKey, String baseUrl, String model, long intervalMs, boolean wifiOnly) {
        Intrinsics.checkNotNullParameter(apiKey, "apiKey");
        Intrinsics.checkNotNullParameter(baseUrl, "baseUrl");
        Intrinsics.checkNotNullParameter(model, "model");
        return new SettingsSnapshot(apiKey, baseUrl, model, intervalMs, wifiOnly);
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SettingsSnapshot)) {
            return false;
        }
        SettingsSnapshot settingsSnapshot = (SettingsSnapshot) other;
        return Intrinsics.areEqual(this.apiKey, settingsSnapshot.apiKey) && Intrinsics.areEqual(this.baseUrl, settingsSnapshot.baseUrl) && Intrinsics.areEqual(this.model, settingsSnapshot.model) && this.intervalMs == settingsSnapshot.intervalMs && this.wifiOnly == settingsSnapshot.wifiOnly;
    }

    public int hashCode() {
        return (((((((this.apiKey.hashCode() * 31) + this.baseUrl.hashCode()) * 31) + this.model.hashCode()) * 31) + Long.hashCode(this.intervalMs)) * 31) + Boolean.hashCode(this.wifiOnly);
    }

    public String toString() {
        return "SettingsSnapshot(apiKey=" + this.apiKey + ", baseUrl=" + this.baseUrl + ", model=" + this.model + ", intervalMs=" + this.intervalMs + ", wifiOnly=" + this.wifiOnly + ")";
    }

    public SettingsSnapshot(String apiKey, String baseUrl, String model, long intervalMs, boolean wifiOnly) {
        Intrinsics.checkNotNullParameter(apiKey, "apiKey");
        Intrinsics.checkNotNullParameter(baseUrl, "baseUrl");
        Intrinsics.checkNotNullParameter(model, "model");
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.intervalMs = intervalMs;
        this.wifiOnly = wifiOnly;
    }

    public final String getApiKey() {
        return this.apiKey;
    }

    public final String getBaseUrl() {
        return this.baseUrl;
    }

    public final String getModel() {
        return this.model;
    }

    public final long getIntervalMs() {
        return this.intervalMs;
    }

    public final boolean getWifiOnly() {
        return this.wifiOnly;
    }
}
