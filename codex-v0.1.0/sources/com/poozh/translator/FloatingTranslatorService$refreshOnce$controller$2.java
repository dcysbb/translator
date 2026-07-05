package com.poozh.translator;

import android.graphics.Bitmap;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.FunctionReferenceImpl;
import kotlin.jvm.internal.Intrinsics;

/* compiled from: FloatingTranslatorService.kt */
@Metadata(k = 3, mv = {2, 0, 0}, xi = 48)
/* loaded from: classes3.dex */
/* synthetic */ class FloatingTranslatorService$refreshOnce$controller$2 extends FunctionReferenceImpl implements Function1<Bitmap, Unit> {
    FloatingTranslatorService$refreshOnce$controller$2(Object obj) {
        super(1, obj, FloatingTranslatorService.class, "handleCapturedFrame", "handleCapturedFrame(Landroid/graphics/Bitmap;)V", 0);
    }

    @Override // kotlin.jvm.functions.Function1
    public /* bridge */ /* synthetic */ Unit invoke(Bitmap bitmap) {
        invoke2(bitmap);
        return Unit.INSTANCE;
    }

    /* renamed from: invoke, reason: avoid collision after fix types in other method */
    public final void invoke2(Bitmap p0) {
        Intrinsics.checkNotNullParameter(p0, "p0");
        ((FloatingTranslatorService) this.receiver).handleCapturedFrame(p0);
    }
}
