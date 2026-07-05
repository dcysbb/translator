package com.poozh.translator.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import androidx.core.app.NotificationCompat;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import kotlin.ranges.RangesKt;

/* compiled from: SelectionOverlayView.kt */
@Metadata(d1 = {"\u0000N\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u0007\n\u0002\b\u0004\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\u0018\u00002\u00020\u0001B1\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0012\u0010\u0004\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00070\u0005\u0012\f\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00070\t¢\u0006\u0004\b\n\u0010\u000bJ\u0010\u0010\u0018\u001a\u00020\u00072\u0006\u0010\u0019\u001a\u00020\u001aH\u0014J\u0010\u0010\u001b\u001a\u00020\u00172\u0006\u0010\u001c\u001a\u00020\u001dH\u0016J\b\u0010\u001e\u001a\u00020\u0006H\u0002J\u0010\u0010\u001f\u001a\u00020\u00122\u0006\u0010 \u001a\u00020\u0012H\u0002R\u001a\u0010\u0004\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00070\u0005X\u0082\u0004¢\u0006\u0002\n\u0000R\u0014\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00070\tX\u0082\u0004¢\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\rX\u0082\u0004¢\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\rX\u0082\u0004¢\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\rX\u0082\u0004¢\u0006\u0002\n\u0000R\u000e\u0010\u0010\u001a\u00020\rX\u0082\u0004¢\u0006\u0002\n\u0000R\u000e\u0010\u0011\u001a\u00020\u0012X\u0082\u000e¢\u0006\u0002\n\u0000R\u000e\u0010\u0013\u001a\u00020\u0012X\u0082\u000e¢\u0006\u0002\n\u0000R\u000e\u0010\u0014\u001a\u00020\u0012X\u0082\u000e¢\u0006\u0002\n\u0000R\u000e\u0010\u0015\u001a\u00020\u0012X\u0082\u000e¢\u0006\u0002\n\u0000R\u000e\u0010\u0016\u001a\u00020\u0017X\u0082\u000e¢\u0006\u0002\n\u0000¨\u0006!"}, d2 = {"Lcom/poozh/translator/ui/SelectionOverlayView;", "Landroid/view/View;", "context", "Landroid/content/Context;", "onSelected", "Lkotlin/Function1;", "Landroid/graphics/Rect;", "", "onCanceled", "Lkotlin/Function0;", "<init>", "(Landroid/content/Context;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function0;)V", "maskPaint", "Landroid/graphics/Paint;", "clearPaint", "borderPaint", "textPaint", "startX", "", "startY", "currentX", "currentY", "dragging", "", "onDraw", "canvas", "Landroid/graphics/Canvas;", "onTouchEvent", NotificationCompat.CATEGORY_EVENT, "Landroid/view/MotionEvent;", "currentRect", "dp", "value", "app_debug"}, k = 1, mv = {2, 0, 0}, xi = 48)
/* loaded from: classes8.dex */
public final class SelectionOverlayView extends View {
    private final Paint borderPaint;
    private final Paint clearPaint;
    private float currentX;
    private float currentY;
    private boolean dragging;
    private final Paint maskPaint;
    private final Function0<Unit> onCanceled;
    private final Function1<Rect, Unit> onSelected;
    private float startX;
    private float startY;
    private final Paint textPaint;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    /* JADX WARN: Multi-variable type inference failed */
    public SelectionOverlayView(Context context, Function1<? super Rect, Unit> onSelected, Function0<Unit> onCanceled) {
        super(context);
        Intrinsics.checkNotNullParameter(context, "context");
        Intrinsics.checkNotNullParameter(onSelected, "onSelected");
        Intrinsics.checkNotNullParameter(onCanceled, "onCanceled");
        this.onSelected = onSelected;
        this.onCanceled = onCanceled;
        Paint paint = new Paint(1);
        paint.setColor(Color.argb(135, 0, 0, 0));
        this.maskPaint = paint;
        Paint paint2 = new Paint(1);
        paint2.setColor(Color.argb(70, 15, 118, 110));
        this.clearPaint = paint2;
        Paint paint3 = new Paint(1);
        paint3.setColor(Color.rgb(45, 212, 191));
        paint3.setStyle(Paint.Style.STROKE);
        paint3.setStrokeWidth(dp(2.0f));
        this.borderPaint = paint3;
        Paint paint4 = new Paint(1);
        paint4.setColor(-1);
        paint4.setTextSize(dp(15.0f));
        this.textPaint = paint4;
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        Intrinsics.checkNotNullParameter(canvas, "canvas");
        canvas.drawRect(0.0f, 0.0f, getWidth(), getHeight(), this.maskPaint);
        canvas.drawText("拖动选择要识别的屏幕区域，点按空白取消", dp(18.0f), dp(34.0f), this.textPaint);
        if (this.dragging) {
            Rect rect = currentRect();
            canvas.drawRect(rect, this.clearPaint);
            canvas.drawRect(rect, this.borderPaint);
        }
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent event) {
        Intrinsics.checkNotNullParameter(event, "event");
        switch (event.getActionMasked()) {
            case 0:
                this.startX = event.getX();
                this.startY = event.getY();
                this.currentX = event.getX();
                this.currentY = event.getY();
                this.dragging = true;
                invalidate();
                break;
            case 1:
                this.currentX = event.getX();
                this.currentY = event.getY();
                Rect rect = currentRect();
                this.dragging = false;
                invalidate();
                if (Math.abs(event.getX() - this.startX) < dp(10.0f) && Math.abs(event.getY() - this.startY) < dp(10.0f)) {
                    this.onCanceled.invoke();
                    break;
                } else if (rect.width() >= dp(48.0f) && rect.height() >= dp(24.0f)) {
                    this.onSelected.invoke(rect);
                    break;
                } else {
                    this.onCanceled.invoke();
                    break;
                }
                break;
            case 2:
                this.currentX = event.getX();
                this.currentY = event.getY();
                invalidate();
                break;
        }
        return true;
    }

    private final Rect currentRect() {
        int left = RangesKt.coerceIn((int) Math.min(this.startX, this.currentX), 0, Math.max(getWidth() - 1, 0));
        int top = RangesKt.coerceIn((int) Math.min(this.startY, this.currentY), 0, Math.max(getHeight() - 1, 0));
        int right = RangesKt.coerceIn((int) Math.max(this.startX, this.currentX), left + 1, Math.max(getWidth(), 1));
        int bottom = RangesKt.coerceIn((int) Math.max(this.startY, this.currentY), top + 1, Math.max(getHeight(), 1));
        return new Rect(left, top, right, bottom);
    }

    private final float dp(float value) {
        return getResources().getDisplayMetrics().density * value;
    }
}
