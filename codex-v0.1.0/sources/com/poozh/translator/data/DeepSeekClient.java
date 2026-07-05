package com.poozh.translator.data;

import com.poozh.translator.data.DeepSeekClient;
import com.poozh.translator.model.AnalysisResult;
import com.poozh.translator.model.TextLanguage;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import kotlin.Metadata;
import kotlin.Result;
import kotlin.ResultKt;
import kotlin.io.CloseableKt;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import kotlin.text.StringsKt;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONArray;
import org.json.JSONObject;

/* compiled from: DeepSeekClient.kt */
@Metadata(d1 = {"\u00008\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\u0018\u0000 \u00142\u00020\u0001:\u0002\u0013\u0014B\u0011\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003¢\u0006\u0004\b\u0004\u0010\u0005J(\u0010\u0006\u001a\u0004\u0018\u00010\u00072\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000fJ \u0010\u0010\u001a\u00020\u00112\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\u0012\u001a\u00020\tH\u0002R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004¢\u0006\u0002\n\u0000¨\u0006\u0015"}, d2 = {"Lcom/poozh/translator/data/DeepSeekClient;", "", "httpClient", "Lokhttp3/OkHttpClient;", "<init>", "(Lokhttp3/OkHttpClient;)V", "analyze", "Lokhttp3/Call;", "text", "", "language", "Lcom/poozh/translator/model/TextLanguage;", "settings", "Lcom/poozh/translator/data/SettingsSnapshot;", "callback", "Lcom/poozh/translator/data/DeepSeekClient$ResultCallback;", "buildRequestJson", "Lorg/json/JSONObject;", "model", "ResultCallback", "Companion", "app_debug"}, k = 1, mv = {2, 0, 0}, xi = 48)
/* loaded from: classes6.dex */
public final class DeepSeekClient {
    private static final MediaType JSON_MEDIA_TYPE = MediaType.INSTANCE.get("application/json; charset=utf-8");
    private final OkHttpClient httpClient;

    /* compiled from: DeepSeekClient.kt */
    @Metadata(d1 = {"\u0000\u001e\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\bf\u0018\u00002\u00020\u0001J\u0010\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H&J\u0010\u0010\u0006\u001a\u00020\u00032\u0006\u0010\u0007\u001a\u00020\bH&¨\u0006\t"}, d2 = {"Lcom/poozh/translator/data/DeepSeekClient$ResultCallback;", "", "onSuccess", "", "result", "Lcom/poozh/translator/model/AnalysisResult;", "onFailure", "message", "", "app_debug"}, k = 1, mv = {2, 0, 0}, xi = 48)
    public interface ResultCallback {
        void onFailure(String message);

        void onSuccess(AnalysisResult result);
    }

    /* JADX WARN: Multi-variable type inference failed */
    public DeepSeekClient() {
        this(null, 1, 0 == true ? 1 : 0);
    }

    public DeepSeekClient(OkHttpClient httpClient) {
        Intrinsics.checkNotNullParameter(httpClient, "httpClient");
        this.httpClient = httpClient;
    }

    public /* synthetic */ DeepSeekClient(OkHttpClient okHttpClient, int i, DefaultConstructorMarker defaultConstructorMarker) {
        this((i & 1) != 0 ? new OkHttpClient.Builder().connectTimeout(20L, TimeUnit.SECONDS).readTimeout(60L, TimeUnit.SECONDS).writeTimeout(20L, TimeUnit.SECONDS).build() : okHttpClient);
    }

    public final Call analyze(final String text, TextLanguage language, SettingsSnapshot settings, final ResultCallback callback) {
        Intrinsics.checkNotNullParameter(text, "text");
        Intrinsics.checkNotNullParameter(language, "language");
        Intrinsics.checkNotNullParameter(settings, "settings");
        Intrinsics.checkNotNullParameter(callback, "callback");
        if (StringsKt.isBlank(settings.getApiKey())) {
            callback.onFailure("请先在主界面填写 DeepSeek API Key");
            return null;
        }
        if (StringsKt.isBlank(text)) {
            callback.onFailure("没有可翻译的文本");
            return null;
        }
        RequestBody.Companion companion = RequestBody.INSTANCE;
        String jSONObject = buildRequestJson(text, language, settings.getModel()).toString();
        Intrinsics.checkNotNullExpressionValue(jSONObject, "toString(...)");
        RequestBody body = companion.create(jSONObject, JSON_MEDIA_TYPE);
        Request request = new Request.Builder().url(settings.getBaseUrl()).header("Authorization", "Bearer " + settings.getApiKey()).header("Content-Type", "application/json").post(body).build();
        Call call = this.httpClient.newCall(request);
        call.enqueue(new Callback() { // from class: com.poozh.translator.data.DeepSeekClient$analyze$1
            @Override // okhttp3.Callback
            public void onFailure(Call call2, IOException e) {
                Intrinsics.checkNotNullParameter(call2, "call");
                Intrinsics.checkNotNullParameter(e, "e");
                DeepSeekClient.ResultCallback resultCallback = DeepSeekClient.ResultCallback.this;
                String message = e.getMessage();
                if (message == null) {
                    message = "DeepSeek 请求失败";
                }
                resultCallback.onFailure(message);
            }

            @Override // okhttp3.Callback
            public void onResponse(Call call2, Response response) {
                Object obj;
                Intrinsics.checkNotNullParameter(call2, "call");
                Intrinsics.checkNotNullParameter(response, "response");
                Response response2 = response;
                DeepSeekClient.ResultCallback resultCallback = DeepSeekClient.ResultCallback.this;
                String str = text;
                try {
                    Response response3 = response2;
                    ResponseBody body2 = response3.body();
                    String string = body2 != null ? body2.string() : null;
                    if (string == null) {
                        string = "";
                    }
                    if (!response3.isSuccessful()) {
                        resultCallback.onFailure("DeepSeek HTTP " + response3.code() + ": " + StringsKt.take(string, 240));
                        CloseableKt.closeFinally(response2, null);
                        return;
                    }
                    try {
                        Result.Companion companion2 = Result.INSTANCE;
                        DeepSeekClient$analyze$1 deepSeekClient$analyze$1 = this;
                        String string2 = new JSONObject(string).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
                        AnalysisJsonParser analysisJsonParser = AnalysisJsonParser.INSTANCE;
                        Intrinsics.checkNotNull(string2);
                        obj = Result.m17constructorimpl(analysisJsonParser.parse(string2, str));
                    } catch (Throwable th) {
                        Result.Companion companion3 = Result.INSTANCE;
                        obj = Result.m17constructorimpl(ResultKt.createFailure(th));
                    }
                    if (Result.m24isSuccessimpl(obj)) {
                        resultCallback.onSuccess((AnalysisResult) obj);
                    }
                    Throwable m20exceptionOrNullimpl = Result.m20exceptionOrNullimpl(obj);
                    if (m20exceptionOrNullimpl != null) {
                        String message = m20exceptionOrNullimpl.getMessage();
                        if (message == null) {
                            message = "DeepSeek 返回解析失败";
                        }
                        resultCallback.onFailure(message);
                    }
                    CloseableKt.closeFinally(response2, null);
                } finally {
                }
            }
        });
        return call;
    }

    private final JSONObject buildRequestJson(String text, TextLanguage language, String model) {
        JSONArray messages = new JSONArray().put(new JSONObject().put("role", "system").put("content", DeepSeekPrompt.INSTANCE.systemPrompt(language))).put(new JSONObject().put("role", "user").put("content", DeepSeekPrompt.INSTANCE.userPrompt(text, language)));
        JSONObject put = new JSONObject().put("model", model).put("messages", messages).put("temperature", 0.2d).put("stream", false).put("thinking", new JSONObject().put("type", "disabled")).put("response_format", new JSONObject().put("type", "json_object"));
        Intrinsics.checkNotNullExpressionValue(put, "put(...)");
        return put;
    }
}
