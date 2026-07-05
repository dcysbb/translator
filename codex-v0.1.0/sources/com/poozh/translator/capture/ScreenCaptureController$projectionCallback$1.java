package com.poozh.translator.capture;

import android.media.projection.MediaProjection;
import android.os.Handler;
import kotlin.Metadata;
import kotlin.jvm.functions.Function1;

/* compiled from: ScreenCaptureController.kt */
@Metadata(d1 = {"\u0000\u0011\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000*\u0001\u0000\b\n\u0018\u00002\u00020\u0001J\b\u0010\u0002\u001a\u00020\u0003H\u0016¨\u0006\u0004"}, d2 = {"com/poozh/translator/capture/ScreenCaptureController$projectionCallback$1", "Landroid/media/projection/MediaProjection$Callback;", "onStop", "", "app_debug"}, k = 1, mv = {2, 0, 0}, xi = 48)
/* loaded from: classes5.dex */
public final class ScreenCaptureController$projectionCallback$1 extends MediaProjection.Callback {
    final /* synthetic */ ScreenCaptureController this$0;

    ScreenCaptureController$projectionCallback$1(ScreenCaptureController $receiver) {
        this.this$0 = $receiver;
    }

    @Override // android.media.projection.MediaProjection.Callback
    public void onStop() {
        Handler handler;
        handler = this.this$0.mainHandler;
        final ScreenCaptureController screenCaptureController = this.this$0;
        handler.post(new Runnable() { // from class: com.poozh.translator.capture.ScreenCaptureController$projectionCallback$1$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                ScreenCaptureController$projectionCallback$1.onStop$lambda$0(ScreenCaptureController.this);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onStop$lambda$0(ScreenCaptureController this$0) {
        Function1 function1;
        this$0.pendingCapture = false;
        this$0.releaseDisplay();
        function1 = this$0.errorCallback;
        function1.invoke("屏幕捕获已停止");
    }
}
