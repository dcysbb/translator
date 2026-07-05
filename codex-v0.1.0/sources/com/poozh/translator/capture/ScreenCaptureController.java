package com.poozh.translator.capture;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.DisplayMetrics;
import java.nio.ByteBuffer;
import kotlin.Metadata;
import kotlin.Result;
import kotlin.ResultKt;
import kotlin.Unit;
import kotlin.jdk7.AutoCloseableKt;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import kotlin.ranges.RangesKt;

/* compiled from: ScreenCaptureController.kt */
@Metadata(d1 = {"\u0000m\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\b\t\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u0004*\u0001\u001d\u0018\u0000 -2\u00020\u0001:\u0001-BO\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u000e\u0010\u0006\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\b0\u0007\u0012\u0012\u0010\t\u001a\u000e\u0012\u0004\u0012\u00020\u000b\u0012\u0004\u0012\u00020\f0\n\u0012\u0012\u0010\r\u001a\u000e\u0012\u0004\u0012\u00020\u000e\u0012\u0004\u0012\u00020\f0\n¢\u0006\u0004\b\u000f\u0010\u0010J\u0006\u0010\u001f\u001a\u00020\fJ\u0006\u0010 \u001a\u00020\fJ\u0006\u0010!\u001a\u00020\fJ\b\u0010\"\u001a\u00020\fH\u0002J\b\u0010#\u001a\u00020\u001bH\u0002J\u0010\u0010$\u001a\u00020\u000b2\u0006\u0010%\u001a\u00020&H\u0002J \u0010'\u001a\u00020\b2\u0006\u0010(\u001a\u00020\b2\u0006\u0010)\u001a\u00020*2\u0006\u0010+\u001a\u00020*H\u0002J\b\u0010,\u001a\u00020\fH\u0002R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004¢\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004¢\u0006\u0002\n\u0000R\u0016\u0010\u0006\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\b0\u0007X\u0082\u0004¢\u0006\u0002\n\u0000R\u001a\u0010\t\u001a\u000e\u0012\u0004\u0012\u00020\u000b\u0012\u0004\u0012\u00020\f0\nX\u0082\u0004¢\u0006\u0002\n\u0000R\u001a\u0010\r\u001a\u000e\u0012\u0004\u0012\u00020\u000e\u0012\u0004\u0012\u00020\f0\nX\u0082\u0004¢\u0006\u0002\n\u0000R\u000e\u0010\u0011\u001a\u00020\u0012X\u0082\u0004¢\u0006\u0002\n\u0000R\u0010\u0010\u0013\u001a\u0004\u0018\u00010\u0014X\u0082\u000e¢\u0006\u0002\n\u0000R\u0010\u0010\u0015\u001a\u0004\u0018\u00010\u0012X\u0082\u000e¢\u0006\u0002\n\u0000R\u0010\u0010\u0016\u001a\u0004\u0018\u00010\u0017X\u0082\u000e¢\u0006\u0002\n\u0000R\u0010\u0010\u0018\u001a\u0004\u0018\u00010\u0019X\u0082\u000e¢\u0006\u0002\n\u0000R\u000e\u0010\u001a\u001a\u00020\u001bX\u0082\u000e¢\u0006\u0002\n\u0000R\u0010\u0010\u001c\u001a\u00020\u001dX\u0082\u0004¢\u0006\u0004\n\u0002\u0010\u001e¨\u0006."}, d2 = {"Lcom/poozh/translator/capture/ScreenCaptureController;", "", "context", "Landroid/content/Context;", "mediaProjection", "Landroid/media/projection/MediaProjection;", "selectionProvider", "Lkotlin/Function0;", "Landroid/graphics/Rect;", "frameCallback", "Lkotlin/Function1;", "Landroid/graphics/Bitmap;", "", "errorCallback", "", "<init>", "(Landroid/content/Context;Landroid/media/projection/MediaProjection;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function1;)V", "mainHandler", "Landroid/os/Handler;", "captureThread", "Landroid/os/HandlerThread;", "captureHandler", "imageReader", "Landroid/media/ImageReader;", "virtualDisplay", "Landroid/hardware/display/VirtualDisplay;", "pendingCapture", "", "projectionCallback", "com/poozh/translator/capture/ScreenCaptureController$projectionCallback$1", "Lcom/poozh/translator/capture/ScreenCaptureController$projectionCallback$1;", "prepare", "captureOnce", "release", "ensureDisplay", "captureLatestFrame", "imageToBitmap", "image", "Landroid/media/Image;", "sanitizeRect", "rect", "maxWidth", "", "maxHeight", "releaseDisplay", "Companion", "app_debug"}, k = 1, mv = {2, 0, 0}, xi = 48)
/* loaded from: classes5.dex */
public final class ScreenCaptureController {
    private static final long INITIAL_FRAME_DELAY_MS = 120;
    private Handler captureHandler;
    private HandlerThread captureThread;
    private final Context context;
    private final Function1<String, Unit> errorCallback;
    private final Function1<Bitmap, Unit> frameCallback;
    private ImageReader imageReader;
    private final Handler mainHandler;
    private final MediaProjection mediaProjection;
    private boolean pendingCapture;
    private final ScreenCaptureController$projectionCallback$1 projectionCallback;
    private final Function0<Rect> selectionProvider;
    private VirtualDisplay virtualDisplay;

    /* JADX WARN: Multi-variable type inference failed */
    public ScreenCaptureController(Context context, MediaProjection mediaProjection, Function0<Rect> selectionProvider, Function1<? super Bitmap, Unit> frameCallback, Function1<? super String, Unit> errorCallback) {
        Intrinsics.checkNotNullParameter(context, "context");
        Intrinsics.checkNotNullParameter(mediaProjection, "mediaProjection");
        Intrinsics.checkNotNullParameter(selectionProvider, "selectionProvider");
        Intrinsics.checkNotNullParameter(frameCallback, "frameCallback");
        Intrinsics.checkNotNullParameter(errorCallback, "errorCallback");
        this.context = context;
        this.mediaProjection = mediaProjection;
        this.selectionProvider = selectionProvider;
        this.frameCallback = frameCallback;
        this.errorCallback = errorCallback;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.projectionCallback = new ScreenCaptureController$projectionCallback$1(this);
    }

    public final void prepare() {
        ensureDisplay();
    }

    public final void captureOnce() {
        if (this.pendingCapture) {
            this.mainHandler.post(new Runnable() { // from class: com.poozh.translator.capture.ScreenCaptureController$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    ScreenCaptureController.captureOnce$lambda$0(ScreenCaptureController.this);
                }
            });
            return;
        }
        ensureDisplay();
        this.pendingCapture = true;
        Handler handler = this.captureHandler;
        if (handler != null) {
            handler.postDelayed(new Runnable() { // from class: com.poozh.translator.capture.ScreenCaptureController$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    ScreenCaptureController.captureOnce$lambda$2(ScreenCaptureController.this);
                }
            }, INITIAL_FRAME_DELAY_MS);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void captureOnce$lambda$0(ScreenCaptureController this$0) {
        this$0.errorCallback.invoke("正在截取屏幕，请稍候");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void captureOnce$lambda$2(final ScreenCaptureController this$0) {
        boolean delivered = this$0.captureLatestFrame();
        this$0.pendingCapture = false;
        if (!delivered) {
            this$0.mainHandler.post(new Runnable() { // from class: com.poozh.translator.capture.ScreenCaptureController$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    ScreenCaptureController.captureOnce$lambda$2$lambda$1(ScreenCaptureController.this);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void captureOnce$lambda$2$lambda$1(ScreenCaptureController this$0) {
        this$0.errorCallback.invoke("暂时没有可用画面，请再点一次刷新");
    }

    public final void release() {
        this.pendingCapture = false;
        releaseDisplay();
        try {
            Result.Companion companion = Result.INSTANCE;
            ScreenCaptureController screenCaptureController = this;
            screenCaptureController.mediaProjection.unregisterCallback(screenCaptureController.projectionCallback);
            Result.m17constructorimpl(Unit.INSTANCE);
        } catch (Throwable th) {
            Result.Companion companion2 = Result.INSTANCE;
            Result.m17constructorimpl(ResultKt.createFailure(th));
        }
        try {
            Result.Companion companion3 = Result.INSTANCE;
            this.mediaProjection.stop();
            Result.m17constructorimpl(Unit.INSTANCE);
        } catch (Throwable th2) {
            Result.Companion companion4 = Result.INSTANCE;
            Result.m17constructorimpl(ResultKt.createFailure(th2));
        }
    }

    private final void ensureDisplay() {
        if (this.virtualDisplay != null) {
            return;
        }
        DisplayMetrics metrics = this.context.getResources().getDisplayMetrics();
        int width = Math.max(metrics.widthPixels, 1);
        int height = Math.max(metrics.heightPixels, 1);
        int density = metrics.densityDpi;
        HandlerThread thread = new HandlerThread("screen-translator-capture");
        thread.start();
        this.captureThread = thread;
        this.captureHandler = new Handler(thread.getLooper());
        ImageReader reader = ImageReader.newInstance(width, height, 1, 2);
        Intrinsics.checkNotNullExpressionValue(reader, "newInstance(...)");
        this.imageReader = reader;
        this.mediaProjection.registerCallback(this.projectionCallback, this.mainHandler);
        this.virtualDisplay = this.mediaProjection.createVirtualDisplay("ScreenTranslator", width, height, density, 16, reader.getSurface(), null, this.captureHandler);
    }

    private final boolean captureLatestFrame() {
        Object m17constructorimpl;
        Rect rect;
        ImageReader reader = this.imageReader;
        if (reader == null) {
            return false;
        }
        try {
            Result.Companion companion = Result.INSTANCE;
            ScreenCaptureController screenCaptureController = this;
            m17constructorimpl = Result.m17constructorimpl(reader.acquireLatestImage());
        } catch (Throwable th) {
            Result.Companion companion2 = Result.INSTANCE;
            m17constructorimpl = Result.m17constructorimpl(ResultKt.createFailure(th));
        }
        if (Result.m23isFailureimpl(m17constructorimpl)) {
            m17constructorimpl = null;
        }
        Image image = (Image) m17constructorimpl;
        if (image == null) {
            return false;
        }
        Image image2 = image;
        try {
            Bitmap imageToBitmap = imageToBitmap(image2);
            Rect invoke = this.selectionProvider.invoke();
            if (invoke == null || (rect = sanitizeRect(invoke, imageToBitmap.getWidth(), imageToBitmap.getHeight())) == null) {
                rect = new Rect(0, 0, imageToBitmap.getWidth(), imageToBitmap.getHeight());
            }
            if (rect.width() >= 8 && rect.height() >= 8) {
                final Bitmap createBitmap = Bitmap.createBitmap(imageToBitmap, rect.left, rect.top, rect.width(), rect.height());
                Intrinsics.checkNotNullExpressionValue(createBitmap, "createBitmap(...)");
                imageToBitmap.recycle();
                this.mainHandler.post(new Runnable() { // from class: com.poozh.translator.capture.ScreenCaptureController$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        ScreenCaptureController.captureLatestFrame$lambda$9$lambda$8(ScreenCaptureController.this, createBitmap);
                    }
                });
                AutoCloseableKt.closeFinally(image2, null);
                return true;
            }
            imageToBitmap.recycle();
            AutoCloseableKt.closeFinally(image2, null);
            return false;
        } finally {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void captureLatestFrame$lambda$9$lambda$8(ScreenCaptureController this$0, Bitmap $cropped) {
        this$0.frameCallback.invoke($cropped);
    }

    private final Bitmap imageToBitmap(Image image) {
        Image.Plane plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer();
        buffer.rewind();
        int pixelStride = plane.getPixelStride();
        int rowStride = plane.getRowStride();
        int rowPadding = rowStride - (image.getWidth() * pixelStride);
        int paddedWidth = image.getWidth() + (rowPadding / pixelStride);
        Bitmap paddedBitmap = Bitmap.createBitmap(paddedWidth, image.getHeight(), Bitmap.Config.ARGB_8888);
        Intrinsics.checkNotNullExpressionValue(paddedBitmap, "createBitmap(...)");
        paddedBitmap.copyPixelsFromBuffer(buffer);
        Bitmap bitmap = Bitmap.createBitmap(paddedBitmap, 0, 0, image.getWidth(), image.getHeight());
        Intrinsics.checkNotNullExpressionValue(bitmap, "createBitmap(...)");
        paddedBitmap.recycle();
        return bitmap;
    }

    private final Rect sanitizeRect(Rect rect, int maxWidth, int maxHeight) {
        int left = RangesKt.coerceIn(rect.left, 0, maxWidth - 1);
        int top = RangesKt.coerceIn(rect.top, 0, maxHeight - 1);
        int right = RangesKt.coerceIn(rect.right, left + 1, maxWidth);
        int bottom = RangesKt.coerceIn(rect.bottom, top + 1, maxHeight);
        return new Rect(left, top, right, bottom);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void releaseDisplay() {
        Unit unit;
        Unit unit2;
        try {
            Result.Companion companion = Result.INSTANCE;
            VirtualDisplay virtualDisplay = this.virtualDisplay;
            if (virtualDisplay != null) {
                virtualDisplay.release();
                unit2 = Unit.INSTANCE;
            } else {
                unit2 = null;
            }
            Result.m17constructorimpl(unit2);
        } catch (Throwable th) {
            Result.Companion companion2 = Result.INSTANCE;
            Result.m17constructorimpl(ResultKt.createFailure(th));
        }
        this.virtualDisplay = null;
        try {
            Result.Companion companion3 = Result.INSTANCE;
            ImageReader imageReader = this.imageReader;
            if (imageReader != null) {
                imageReader.close();
                unit = Unit.INSTANCE;
            } else {
                unit = null;
            }
            Result.m17constructorimpl(unit);
        } catch (Throwable th2) {
            Result.Companion companion4 = Result.INSTANCE;
            Result.m17constructorimpl(ResultKt.createFailure(th2));
        }
        this.imageReader = null;
        Handler handler = this.captureHandler;
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        HandlerThread handlerThread = this.captureThread;
        if (handlerThread != null) {
            handlerThread.quitSafely();
        }
        this.captureHandler = null;
        this.captureThread = null;
    }
}
