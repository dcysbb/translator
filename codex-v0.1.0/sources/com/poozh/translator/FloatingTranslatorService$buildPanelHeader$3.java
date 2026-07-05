package com.poozh.translator;

import android.view.WindowManager;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.FunctionReferenceImpl;
import kotlin.jvm.internal.Intrinsics;

/* compiled from: FloatingTranslatorService.kt */
@Metadata(k = 3, mv = {2, 0, 0}, xi = 48)
/* loaded from: classes3.dex */
/* synthetic */ class FloatingTranslatorService$buildPanelHeader$3 extends FunctionReferenceImpl implements Function1<WindowManager.LayoutParams, Unit> {
    FloatingTranslatorService$buildPanelHeader$3(Object obj) {
        super(1, obj, FloatingTranslatorService.class, "savePanelWindowState", "savePanelWindowState(Landroid/view/WindowManager$LayoutParams;)V", 0);
    }

    @Override // kotlin.jvm.functions.Function1
    public /* bridge */ /* synthetic */ Unit invoke(WindowManager.LayoutParams layoutParams) {
        invoke2(layoutParams);
        return Unit.INSTANCE;
    }

    /* renamed from: invoke, reason: avoid collision after fix types in other method */
    public final void invoke2(WindowManager.LayoutParams p0) {
        Intrinsics.checkNotNullParameter(p0, "p0");
        ((FloatingTranslatorService) this.receiver).savePanelWindowState(p0);
    }
}
