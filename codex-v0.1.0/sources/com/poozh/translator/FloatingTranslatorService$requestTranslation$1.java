package com.poozh.translator;

import android.widget.TextView;
import com.poozh.translator.data.DeepSeekClient;
import com.poozh.translator.model.AnalysisResult;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.internal.Intrinsics;

/* compiled from: FloatingTranslatorService.kt */
@Metadata(d1 = {"\u0000\u001f\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000*\u0001\u0000\b\n\u0018\u00002\u00020\u0001J\u0010\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u0016J\u0010\u0010\u0006\u001a\u00020\u00032\u0006\u0010\u0007\u001a\u00020\bH\u0016¨\u0006\t"}, d2 = {"com/poozh/translator/FloatingTranslatorService$requestTranslation$1", "Lcom/poozh/translator/data/DeepSeekClient$ResultCallback;", "onSuccess", "", "result", "Lcom/poozh/translator/model/AnalysisResult;", "onFailure", "message", "", "app_debug"}, k = 1, mv = {2, 0, 0}, xi = 48)
/* loaded from: classes3.dex */
public final class FloatingTranslatorService$requestTranslation$1 implements DeepSeekClient.ResultCallback {
    final /* synthetic */ String $text;
    final /* synthetic */ FloatingTranslatorService this$0;

    FloatingTranslatorService$requestTranslation$1(FloatingTranslatorService $receiver, String $text) {
        this.this$0 = $receiver;
        this.$text = $text;
    }

    @Override // com.poozh.translator.data.DeepSeekClient.ResultCallback
    public void onSuccess(final AnalysisResult result) {
        Intrinsics.checkNotNullParameter(result, "result");
        FloatingTranslatorService floatingTranslatorService = this.this$0;
        final FloatingTranslatorService floatingTranslatorService2 = this.this$0;
        floatingTranslatorService.runOnMain(new Function0() { // from class: com.poozh.translator.FloatingTranslatorService$requestTranslation$1$$ExternalSyntheticLambda1
            @Override // kotlin.jvm.functions.Function0
            public final Object invoke() {
                Unit onSuccess$lambda$0;
                onSuccess$lambda$0 = FloatingTranslatorService$requestTranslation$1.onSuccess$lambda$0(FloatingTranslatorService.this, result);
                return onSuccess$lambda$0;
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit onSuccess$lambda$0(FloatingTranslatorService this$0, AnalysisResult $result) {
        this$0.translating = false;
        this$0.lastResult = $result;
        this$0.showReadingPage();
        this$0.showStatus("翻译完成");
        return Unit.INSTANCE;
    }

    @Override // com.poozh.translator.data.DeepSeekClient.ResultCallback
    public void onFailure(final String message) {
        Intrinsics.checkNotNullParameter(message, "message");
        FloatingTranslatorService floatingTranslatorService = this.this$0;
        final FloatingTranslatorService floatingTranslatorService2 = this.this$0;
        final String str = this.$text;
        floatingTranslatorService.runOnMain(new Function0() { // from class: com.poozh.translator.FloatingTranslatorService$requestTranslation$1$$ExternalSyntheticLambda0
            @Override // kotlin.jvm.functions.Function0
            public final Object invoke() {
                Unit onFailure$lambda$1;
                onFailure$lambda$1 = FloatingTranslatorService$requestTranslation$1.onFailure$lambda$1(FloatingTranslatorService.this, str, message);
                return onFailure$lambda$1;
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit onFailure$lambda$1(FloatingTranslatorService this$0, String $text, String $message) {
        TextView textView;
        this$0.translating = false;
        this$0.lastResult = null;
        textView = this$0.readingText;
        if (textView != null) {
            textView.setText("原文\n" + $text + "\n\n" + $message);
        }
        this$0.showStatus("翻译失败");
        return Unit.INSTANCE;
    }
}
