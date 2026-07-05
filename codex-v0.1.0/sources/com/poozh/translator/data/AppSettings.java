package com.poozh.translator.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.util.Base64;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import kotlin.Metadata;
import kotlin.Pair;
import kotlin.Result;
import kotlin.ResultKt;
import kotlin.collections.ArraysKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.ranges.RangesKt;
import kotlin.text.StringsKt;

/* compiled from: AppSettings.kt */
@Metadata(d1 = {"\u0000V\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\u0010\b\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\n\n\u0002\u0018\u0002\n\u0002\b\u0002\u0018\u0000 +2\u00020\u0001:\u0001+B\u000f\u0012\u0006\u0010\u0002\u001a\u00020\u0003¢\u0006\u0004\b\u0004\u0010\u0005J\u0006\u0010\b\u001a\u00020\tJ&\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\r2\u0006\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u0012J\u000e\u0010\u0013\u001a\u00020\u000b2\u0006\u0010\u0014\u001a\u00020\rJ\u0006\u0010\u0015\u001a\u00020\u000bJ\"\u0010\u0016\u001a\u000e\u0012\u0004\u0012\u00020\u0018\u0012\u0004\u0012\u00020\u00180\u00172\u0006\u0010\u0019\u001a\u00020\u00182\u0006\u0010\u001a\u001a\u00020\u0018J\u0016\u0010\u001b\u001a\u00020\u000b2\u0006\u0010\u001c\u001a\u00020\u00182\u0006\u0010\u001d\u001a\u00020\u0018J&\u0010\u001e\u001a\u00020\u001f2\u0006\u0010\u0019\u001a\u00020\u00182\u0006\u0010\u001a\u001a\u00020\u00182\u0006\u0010 \u001a\u00020\u00182\u0006\u0010!\u001a\u00020\u0018J&\u0010\"\u001a\u00020\u000b2\u0006\u0010\u001c\u001a\u00020\u00182\u0006\u0010\u001d\u001a\u00020\u00182\u0006\u0010#\u001a\u00020\u00182\u0006\u0010$\u001a\u00020\u0018J\u0010\u0010%\u001a\u00020\r2\u0006\u0010&\u001a\u00020\rH\u0002J\u0010\u0010'\u001a\u00020\r2\u0006\u0010(\u001a\u00020\rH\u0002J\b\u0010)\u001a\u00020*H\u0002R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004¢\u0006\u0002\n\u0000¨\u0006,"}, d2 = {"Lcom/poozh/translator/data/AppSettings;", "", "context", "Landroid/content/Context;", "<init>", "(Landroid/content/Context;)V", "prefs", "Landroid/content/SharedPreferences;", "load", "Lcom/poozh/translator/data/SettingsSnapshot;", "save", "", "baseUrl", "", AppSettings.KEY_MODEL, "intervalMs", "", "wifiOnly", "", "saveApiKey", "apiKey", "clearApiKey", "loadBubblePosition", "Lkotlin/Pair;", "", "defaultX", "defaultY", "saveBubblePosition", "x", "y", "loadPanelState", "Lcom/poozh/translator/data/OverlayWindowState;", "defaultWidth", "defaultHeight", "savePanelState", "width", "height", "encrypt", "value", "decrypt", "encoded", "getOrCreateKey", "Ljavax/crypto/SecretKey;", "Companion", "app_debug"}, k = 1, mv = {2, 0, 0}, xi = 48)
/* loaded from: classes6.dex */
public final class AppSettings {
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    public static final String DEFAULT_BASE_URL = "https://api.deepseek.com/chat/completions";
    public static final long DEFAULT_INTERVAL_MS = 1000;
    public static final String DEFAULT_MODEL = "deepseek-v4-flash";
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final String KEY_ALIAS = "screen_translator_api_key";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_BASE_URL = "base_url";
    private static final String KEY_BUBBLE_X = "bubble_x";
    private static final String KEY_BUBBLE_Y = "bubble_y";
    private static final String KEY_INTERVAL_MS = "interval_ms";
    private static final String KEY_MODEL = "model";
    private static final String KEY_PANEL_HEIGHT = "panel_height";
    private static final String KEY_PANEL_WIDTH = "panel_width";
    private static final String KEY_PANEL_X = "panel_x";
    private static final String KEY_PANEL_Y = "panel_y";
    private static final String KEY_WIFI_ONLY = "wifi_only";
    private static final String PREFS_NAME = "translator_settings";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private final SharedPreferences prefs;

    public AppSettings(Context context) {
        Intrinsics.checkNotNullParameter(context, "context");
        SharedPreferences sharedPreferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
        Intrinsics.checkNotNullExpressionValue(sharedPreferences, "getSharedPreferences(...)");
        this.prefs = sharedPreferences;
    }

    public final SettingsSnapshot load() {
        String string = this.prefs.getString(KEY_API_KEY, "");
        if (string == null) {
            string = "";
        }
        String decrypt = decrypt(string);
        SharedPreferences sharedPreferences = this.prefs;
        String str = DEFAULT_BASE_URL;
        String string2 = sharedPreferences.getString(KEY_BASE_URL, DEFAULT_BASE_URL);
        if (string2 == null) {
            string2 = "";
        }
        String str2 = string2;
        if (!StringsKt.isBlank(str2)) {
            str = str2;
        }
        String str3 = str;
        SharedPreferences sharedPreferences2 = this.prefs;
        String str4 = DEFAULT_MODEL;
        String string3 = sharedPreferences2.getString(KEY_MODEL, DEFAULT_MODEL);
        String str5 = string3 != null ? string3 : "";
        if (!StringsKt.isBlank(str5)) {
            str4 = str5;
        }
        return new SettingsSnapshot(decrypt, str3, str4, RangesKt.coerceIn(this.prefs.getLong(KEY_INTERVAL_MS, 1000L), 500L, 5000L), this.prefs.getBoolean(KEY_WIFI_ONLY, false));
    }

    public final void save(String baseUrl, String model, long intervalMs, boolean wifiOnly) {
        Intrinsics.checkNotNullParameter(baseUrl, "baseUrl");
        Intrinsics.checkNotNullParameter(model, "model");
        SharedPreferences.Editor edit = this.prefs.edit();
        String str = baseUrl;
        if (StringsKt.isBlank(str)) {
            str = DEFAULT_BASE_URL;
        }
        SharedPreferences.Editor putString = edit.putString(KEY_BASE_URL, str);
        String str2 = model;
        if (StringsKt.isBlank(str2)) {
            str2 = DEFAULT_MODEL;
        }
        putString.putString(KEY_MODEL, str2).putLong(KEY_INTERVAL_MS, RangesKt.coerceIn(intervalMs, 500L, 5000L)).putBoolean(KEY_WIFI_ONLY, wifiOnly).apply();
    }

    public final void saveApiKey(String apiKey) {
        Intrinsics.checkNotNullParameter(apiKey, "apiKey");
        this.prefs.edit().putString(KEY_API_KEY, encrypt(StringsKt.trim((CharSequence) apiKey).toString())).apply();
    }

    public final void clearApiKey() {
        this.prefs.edit().remove(KEY_API_KEY).apply();
    }

    public final Pair<Integer, Integer> loadBubblePosition(int defaultX, int defaultY) {
        return new Pair<>(Integer.valueOf(this.prefs.getInt(KEY_BUBBLE_X, defaultX)), Integer.valueOf(this.prefs.getInt(KEY_BUBBLE_Y, defaultY)));
    }

    public final void saveBubblePosition(int x, int y) {
        this.prefs.edit().putInt(KEY_BUBBLE_X, x).putInt(KEY_BUBBLE_Y, y).apply();
    }

    public final OverlayWindowState loadPanelState(int defaultX, int defaultY, int defaultWidth, int defaultHeight) {
        return new OverlayWindowState(this.prefs.getInt(KEY_PANEL_X, defaultX), this.prefs.getInt(KEY_PANEL_Y, defaultY), this.prefs.getInt(KEY_PANEL_WIDTH, defaultWidth), this.prefs.getInt(KEY_PANEL_HEIGHT, defaultHeight));
    }

    public final void savePanelState(int x, int y, int width, int height) {
        this.prefs.edit().putInt(KEY_PANEL_X, x).putInt(KEY_PANEL_Y, y).putInt(KEY_PANEL_WIDTH, width).putInt(KEY_PANEL_HEIGHT, height).apply();
    }

    private final String encrypt(String value) {
        Object obj;
        if (StringsKt.isBlank(value)) {
            return "";
        }
        try {
            Result.Companion companion = Result.INSTANCE;
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(1, getOrCreateKey());
            Charset UTF_8 = StandardCharsets.UTF_8;
            Intrinsics.checkNotNullExpressionValue(UTF_8, "UTF_8");
            byte[] bytes = value.getBytes(UTF_8);
            Intrinsics.checkNotNullExpressionValue(bytes, "getBytes(...)");
            byte[] doFinal = cipher.doFinal(bytes);
            byte[] iv = cipher.getIV();
            Intrinsics.checkNotNullExpressionValue(iv, "getIV(...)");
            Intrinsics.checkNotNull(doFinal);
            obj = Result.m17constructorimpl(Base64.encodeToString(ArraysKt.plus(iv, doFinal), 2));
        } catch (Throwable th) {
            Result.Companion companion2 = Result.INSTANCE;
            obj = Result.m17constructorimpl(ResultKt.createFailure(th));
        }
        return (String) (Result.m23isFailureimpl(obj) ? "" : obj);
    }

    private final String decrypt(String encoded) {
        Object obj;
        String str;
        if (StringsKt.isBlank(encoded)) {
            return "";
        }
        try {
            Result.Companion companion = Result.INSTANCE;
            AppSettings appSettings = this;
            byte[] decode = Base64.decode(encoded, 2);
            if (decode.length <= 12) {
                str = "";
            } else {
                Intrinsics.checkNotNull(decode);
                byte[] copyOfRange = ArraysKt.copyOfRange(decode, 0, 12);
                byte[] copyOfRange2 = ArraysKt.copyOfRange(decode, 12, decode.length);
                Cipher cipher = Cipher.getInstance(TRANSFORMATION);
                cipher.init(2, appSettings.getOrCreateKey(), new GCMParameterSpec(128, copyOfRange));
                byte[] doFinal = cipher.doFinal(copyOfRange2);
                Intrinsics.checkNotNullExpressionValue(doFinal, "doFinal(...)");
                Charset UTF_8 = StandardCharsets.UTF_8;
                Intrinsics.checkNotNullExpressionValue(UTF_8, "UTF_8");
                str = new String(doFinal, UTF_8);
            }
            obj = Result.m17constructorimpl(str);
        } catch (Throwable th) {
            Result.Companion companion2 = Result.INSTANCE;
            obj = Result.m17constructorimpl(ResultKt.createFailure(th));
        }
        return (String) (Result.m23isFailureimpl(obj) ? "" : obj);
    }

    private final SecretKey getOrCreateKey() {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);
        KeyStore.Entry entry = keyStore.getEntry(KEY_ALIAS, null);
        KeyStore.SecretKeyEntry secretKeyEntry = entry instanceof KeyStore.SecretKeyEntry ? (KeyStore.SecretKeyEntry) entry : null;
        if (secretKeyEntry == null) {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES", ANDROID_KEYSTORE);
            KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(KEY_ALIAS, 3).setBlockModes("GCM").setEncryptionPaddings("NoPadding").setRandomizedEncryptionRequired(true).build();
            Intrinsics.checkNotNullExpressionValue(spec, "build(...)");
            keyGenerator.init(spec);
            SecretKey generateKey = keyGenerator.generateKey();
            Intrinsics.checkNotNullExpressionValue(generateKey, "generateKey(...)");
            return generateKey;
        }
        SecretKey secretKey = secretKeyEntry.getSecretKey();
        Intrinsics.checkNotNullExpressionValue(secretKey, "getSecretKey(...)");
        return secretKey;
    }
}
