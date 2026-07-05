package com.poozh.translator.ocr;

import android.graphics.Bitmap;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.io.Closeable;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import kotlin.ranges.RangesKt;
import kotlin.text.StringsKt;

/* compiled from: ScreenTextRecognizer.kt */
@Metadata(d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0006\u0018\u0000 \u00142\u00020\u0001:\u0002\u0013\u0014B\u0007¢\u0006\u0004\b\u0002\u0010\u0003J\u0016\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\fJ \u0010\r\u001a\u00020\b2\u0006\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\fH\u0002J\f\u0010\u0010\u001a\u00020\n*\u00020\nH\u0002J\f\u0010\u0011\u001a\u00020\b*\u00020\nH\u0002J\b\u0010\u0012\u001a\u00020\bH\u0016R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004¢\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0005X\u0082\u0004¢\u0006\u0002\n\u0000¨\u0006\u0015"}, d2 = {"Lcom/poozh/translator/ocr/ScreenTextRecognizer;", "Ljava/io/Closeable;", "<init>", "()V", "japaneseRecognizer", "Lcom/google/mlkit/vision/text/TextRecognizer;", "latinRecognizer", "recognize", "", "bitmap", "Landroid/graphics/Bitmap;", "callback", "Lcom/poozh/translator/ocr/ScreenTextRecognizer$Callback;", "recognizeLatin", "image", "Lcom/google/mlkit/vision/common/InputImage;", "scaleForRecognition", "safeRecycle", "close", "Callback", "Companion", "app_debug"}, k = 1, mv = {2, 0, 0}, xi = 48)
/* loaded from: classes4.dex */
public final class ScreenTextRecognizer implements Closeable {
    private static final int MAX_RECOGNITION_SIDE_PX = 1600;
    private final TextRecognizer japaneseRecognizer;
    private final TextRecognizer latinRecognizer;

    /* compiled from: ScreenTextRecognizer.kt */
    @Metadata(d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\bf\u0018\u00002\u00020\u0001J\u0010\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H&J\u0010\u0010\u0006\u001a\u00020\u00032\u0006\u0010\u0007\u001a\u00020\u0005H&¨\u0006\b"}, d2 = {"Lcom/poozh/translator/ocr/ScreenTextRecognizer$Callback;", "", "onSuccess", "", "text", "", "onFailure", "message", "app_debug"}, k = 1, mv = {2, 0, 0}, xi = 48)
    public interface Callback {
        void onFailure(String message);

        void onSuccess(String text);
    }

    public ScreenTextRecognizer() {
        TextRecognizer client = TextRecognition.getClient(new JapaneseTextRecognizerOptions.Builder().build());
        Intrinsics.checkNotNullExpressionValue(client, "getClient(...)");
        this.japaneseRecognizer = client;
        TextRecognizer client2 = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        Intrinsics.checkNotNullExpressionValue(client2, "getClient(...)");
        this.latinRecognizer = client2;
    }

    public final void recognize(Bitmap bitmap, final Callback callback) {
        Intrinsics.checkNotNullParameter(bitmap, "bitmap");
        Intrinsics.checkNotNullParameter(callback, "callback");
        final Bitmap workingBitmap = scaleForRecognition(bitmap);
        if (workingBitmap != bitmap) {
            bitmap.recycle();
        }
        final InputImage image = InputImage.fromBitmap(workingBitmap, 0);
        Intrinsics.checkNotNullExpressionValue(image, "fromBitmap(...)");
        Task<Text> process = this.japaneseRecognizer.process(image);
        final Function1 function1 = new Function1() { // from class: com.poozh.translator.ocr.ScreenTextRecognizer$$ExternalSyntheticLambda3
            @Override // kotlin.jvm.functions.Function1
            public final Object invoke(Object obj) {
                Unit recognize$lambda$0;
                recognize$lambda$0 = ScreenTextRecognizer.recognize$lambda$0(ScreenTextRecognizer.this, workingBitmap, callback, image, (Text) obj);
                return recognize$lambda$0;
            }
        };
        process.addOnSuccessListener(new OnSuccessListener() { // from class: com.poozh.translator.ocr.ScreenTextRecognizer$$ExternalSyntheticLambda4
            @Override // com.google.android.gms.tasks.OnSuccessListener
            public final void onSuccess(Object obj) {
                Function1.this.invoke(obj);
            }
        }).addOnFailureListener(new OnFailureListener() { // from class: com.poozh.translator.ocr.ScreenTextRecognizer$$ExternalSyntheticLambda5
            @Override // com.google.android.gms.tasks.OnFailureListener
            public final void onFailure(Exception exc) {
                ScreenTextRecognizer.recognize$lambda$2(ScreenTextRecognizer.this, image, workingBitmap, callback, exc);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit recognize$lambda$0(ScreenTextRecognizer this$0, Bitmap $workingBitmap, Callback $callback, InputImage $image, Text japaneseText) {
        String text = japaneseText.getText();
        Intrinsics.checkNotNullExpressionValue(text, "getText(...)");
        String text2 = StringsKt.trim((CharSequence) text).toString();
        if (!StringsKt.isBlank(text2)) {
            this$0.safeRecycle($workingBitmap);
            $callback.onSuccess(text2);
        } else {
            this$0.recognizeLatin($image, $workingBitmap, $callback);
        }
        return Unit.INSTANCE;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void recognize$lambda$2(ScreenTextRecognizer this$0, InputImage $image, Bitmap $workingBitmap, Callback $callback, Exception it) {
        Intrinsics.checkNotNullParameter(it, "it");
        this$0.recognizeLatin($image, $workingBitmap, $callback);
    }

    private final void recognizeLatin(InputImage image, final Bitmap bitmap, final Callback callback) {
        Task<Text> process = this.latinRecognizer.process(image);
        final Function1 function1 = new Function1() { // from class: com.poozh.translator.ocr.ScreenTextRecognizer$$ExternalSyntheticLambda0
            @Override // kotlin.jvm.functions.Function1
            public final Object invoke(Object obj) {
                Unit recognizeLatin$lambda$3;
                recognizeLatin$lambda$3 = ScreenTextRecognizer.recognizeLatin$lambda$3(ScreenTextRecognizer.this, bitmap, callback, (Text) obj);
                return recognizeLatin$lambda$3;
            }
        };
        process.addOnSuccessListener(new OnSuccessListener() { // from class: com.poozh.translator.ocr.ScreenTextRecognizer$$ExternalSyntheticLambda1
            @Override // com.google.android.gms.tasks.OnSuccessListener
            public final void onSuccess(Object obj) {
                Function1.this.invoke(obj);
            }
        }).addOnFailureListener(new OnFailureListener() { // from class: com.poozh.translator.ocr.ScreenTextRecognizer$$ExternalSyntheticLambda2
            @Override // com.google.android.gms.tasks.OnFailureListener
            public final void onFailure(Exception exc) {
                ScreenTextRecognizer.recognizeLatin$lambda$5(ScreenTextRecognizer.this, bitmap, callback, exc);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit recognizeLatin$lambda$3(ScreenTextRecognizer this$0, Bitmap $bitmap, Callback $callback, Text text) {
        Intrinsics.checkNotNullParameter(text, "text");
        this$0.safeRecycle($bitmap);
        String text2 = text.getText();
        Intrinsics.checkNotNullExpressionValue(text2, "getText(...)");
        $callback.onSuccess(StringsKt.trim((CharSequence) text2).toString());
        return Unit.INSTANCE;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void recognizeLatin$lambda$5(ScreenTextRecognizer this$0, Bitmap $bitmap, Callback $callback, Exception error) {
        Intrinsics.checkNotNullParameter(error, "error");
        this$0.safeRecycle($bitmap);
        String message = error.getMessage();
        if (message == null) {
            message = "OCR 识别失败";
        }
        $callback.onFailure(message);
    }

    private final Bitmap scaleForRecognition(Bitmap $this$scaleForRecognition) {
        int largestSide = Math.max($this$scaleForRecognition.getWidth(), $this$scaleForRecognition.getHeight());
        if (largestSide <= MAX_RECOGNITION_SIDE_PX) {
            return $this$scaleForRecognition;
        }
        float ratio = 1600.0f / largestSide;
        int scaledWidth = RangesKt.coerceAtLeast((int) ($this$scaleForRecognition.getWidth() * ratio), 1);
        int scaledHeight = RangesKt.coerceAtLeast((int) ($this$scaleForRecognition.getHeight() * ratio), 1);
        Bitmap createScaledBitmap = Bitmap.createScaledBitmap($this$scaleForRecognition, scaledWidth, scaledHeight, true);
        Intrinsics.checkNotNullExpressionValue(createScaledBitmap, "createScaledBitmap(...)");
        return createScaledBitmap;
    }

    private final void safeRecycle(Bitmap $this$safeRecycle) {
        if (!$this$safeRecycle.isRecycled()) {
            $this$safeRecycle.recycle();
        }
    }

    @Override // java.io.Closeable, java.lang.AutoCloseable
    public void close() {
        this.japaneseRecognizer.close();
        this.latinRecognizer.close();
    }
}
