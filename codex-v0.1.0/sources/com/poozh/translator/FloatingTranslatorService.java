package com.poozh.translator;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.mlkit.common.MlKitException;
import com.poozh.translator.capture.ScreenCaptureController;
import com.poozh.translator.data.AppSettings;
import com.poozh.translator.data.DeepSeekClient;
import com.poozh.translator.data.OverlayWindowState;
import com.poozh.translator.data.SettingsSnapshot;
import com.poozh.translator.model.AnalysisResult;
import com.poozh.translator.model.LanguageDetector;
import com.poozh.translator.model.RefreshAction;
import com.poozh.translator.model.TranslationRefreshPolicy;
import com.poozh.translator.ocr.ScreenTextRecognizer;
import com.poozh.translator.ui.SelectionOverlayView;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import kotlin.Metadata;
import kotlin.NoWhenBranchMatchedException;
import kotlin.Pair;
import kotlin.Result;
import kotlin.ResultKt;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.Ref;
import kotlin.ranges.RangesKt;
import kotlin.text.StringsKt;
import okhttp3.Call;

/* compiled from: FloatingTranslatorService.kt */
@Metadata(d1 = {"\u0000À\u0001\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0018\u0002\n\u0002\b\u000b\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b\n\n\u0002\u0018\u0002\n\u0002\b\u0015\u0018\u0000 \u007f2\u00020\u0001:\u0002~\u007fB\u0007¢\u0006\u0004\b\u0002\u0010\u0003J\b\u0010'\u001a\u00020(H\u0016J\"\u0010)\u001a\u00020*2\b\u0010+\u001a\u0004\u0018\u00010,2\u0006\u0010-\u001a\u00020*2\u0006\u0010.\u001a\u00020*H\u0016J\u0014\u0010/\u001a\u0004\u0018\u0001002\b\u0010+\u001a\u0004\u0018\u00010,H\u0016J\b\u00101\u001a\u00020(H\u0016J\u0010\u00102\u001a\u00020(2\u0006\u0010+\u001a\u00020,H\u0002J\b\u00103\u001a\u00020(H\u0002J\b\u00104\u001a\u00020(H\u0002J\b\u00105\u001a\u00020(H\u0002J\b\u00106\u001a\u00020(H\u0002J\u0010\u00107\u001a\u00020\u000f2\u0006\u00108\u001a\u000209H\u0002J\u0010\u0010:\u001a\u00020\u000f2\u0006\u00108\u001a\u000209H\u0002J\b\u0010;\u001a\u00020(H\u0002J\b\u0010<\u001a\u00020 H\u0002J\b\u0010=\u001a\u00020(H\u0002J\b\u0010>\u001a\u00020(H\u0002J\b\u0010?\u001a\u00020(H\u0002J\u0010\u0010@\u001a\u00020(2\u0006\u0010A\u001a\u00020BH\u0002J\u0010\u0010C\u001a\u00020(2\u0006\u0010D\u001a\u00020 H\u0002J\u0010\u0010E\u001a\u00020(2\u0006\u0010A\u001a\u00020BH\u0002J\u0018\u0010F\u001a\u00020(2\u0006\u0010G\u001a\u00020 2\u0006\u0010H\u001a\u00020\u001dH\u0002J\u0010\u0010I\u001a\u00020(2\u0006\u0010G\u001a\u00020 H\u0002J\b\u0010J\u001a\u00020(H\u0002J\u0010\u0010K\u001a\u00020 2\u0006\u0010G\u001a\u00020 H\u0002J\u0010\u0010L\u001a\u00020\u001d2\u0006\u0010M\u001a\u00020NH\u0002J\b\u0010O\u001a\u00020(H\u0002J\b\u0010P\u001a\u00020(H\u0002J\b\u0010Q\u001a\u00020(H\u0002J\u0010\u0010R\u001a\u00020(2\u0006\u0010D\u001a\u00020 H\u0002J\u0016\u0010S\u001a\u00020(2\f\u0010T\u001a\b\u0012\u0004\u0012\u00020(0UH\u0002J\b\u0010V\u001a\u00020(H\u0002J\b\u0010W\u001a\u00020XH\u0002J\b\u0010Y\u001a\u00020(H\u0002J\b\u0010Z\u001a\u00020(H\u0002J\b\u0010[\u001a\u00020(H\u0002J@\u0010\\\u001a\u00020(2\u0006\u0010]\u001a\u00020\u00122\u0006\u00108\u001a\u0002092\u000e\u0010^\u001a\n\u0012\u0004\u0012\u00020(\u0018\u00010U2\u0016\b\u0002\u0010_\u001a\u0010\u0012\u0004\u0012\u000209\u0012\u0004\u0012\u00020(\u0018\u00010`H\u0002J\u0010\u0010a\u001a\u00020\r2\u0006\u00108\u001a\u000209H\u0002J\u001e\u0010b\u001a\u00020\r2\u0006\u0010c\u001a\u00020 2\f\u0010^\u001a\b\u0012\u0004\u0012\u00020(0UH\u0002J(\u0010d\u001a\u00020\r2\u0006\u0010c\u001a\u00020 2\b\b\u0002\u0010e\u001a\u00020\u001d2\f\u0010^\u001a\b\u0012\u0004\u0012\u00020(0UH\u0002J\u0010\u0010f\u001a\u00020\r2\u0006\u0010c\u001a\u00020 H\u0002J\u0018\u0010g\u001a\u00020\r2\u0006\u0010c\u001a\u00020 2\u0006\u0010h\u001a\u00020 H\u0002J\u001e\u0010i\u001a\u00020\r2\u0006\u0010c\u001a\u00020 2\f\u0010^\u001a\b\u0012\u0004\u0012\u00020(0UH\u0002J)\u0010j\u001a\u00020k2\u0006\u0010l\u001a\u00020*2\u0006\u0010m\u001a\u00020*2\n\b\u0002\u0010n\u001a\u0004\u0018\u00010*H\u0002¢\u0006\u0002\u0010oJ\u0012\u0010p\u001a\u00020(2\b\u0010]\u001a\u0004\u0018\u00010\u0012H\u0002J\b\u0010q\u001a\u00020*H\u0002J\u0010\u0010r\u001a\u00020*2\u0006\u0010h\u001a\u00020*H\u0002J\u0010\u0010s\u001a\u00020(2\u0006\u00108\u001a\u000209H\u0002J\u0018\u0010t\u001a\u00020*2\u0006\u0010u\u001a\u00020*2\u0006\u0010v\u001a\u00020*H\u0002J\u0018\u0010w\u001a\u00020*2\u0006\u0010x\u001a\u00020*2\u0006\u0010y\u001a\u00020*H\u0002J\b\u0010z\u001a\u00020*H\u0002J\b\u0010{\u001a\u00020*H\u0002J\u0010\u0010|\u001a\u00020*2\u0006\u0010v\u001a\u00020*H\u0002J\u0010\u0010}\u001a\u00020*2\u0006\u0010y\u001a\u00020*H\u0002R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082.¢\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082.¢\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004¢\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u0004¢\u0006\u0002\n\u0000R\u0010\u0010\f\u001a\u0004\u0018\u00010\rX\u0082\u000e¢\u0006\u0002\n\u0000R\u0010\u0010\u000e\u001a\u0004\u0018\u00010\u000fX\u0082\u000e¢\u0006\u0002\n\u0000R\u0010\u0010\u0010\u001a\u0004\u0018\u00010\u000fX\u0082\u000e¢\u0006\u0002\n\u0000R\u0010\u0010\u0011\u001a\u0004\u0018\u00010\u0012X\u0082\u000e¢\u0006\u0002\n\u0000R\u0010\u0010\u0013\u001a\u0004\u0018\u00010\rX\u0082\u000e¢\u0006\u0002\n\u0000R\u0010\u0010\u0014\u001a\u0004\u0018\u00010\rX\u0082\u000e¢\u0006\u0002\n\u0000R\u0010\u0010\u0015\u001a\u0004\u0018\u00010\rX\u0082\u000e¢\u0006\u0002\n\u0000R\u0010\u0010\u0016\u001a\u0004\u0018\u00010\u0017X\u0082\u000e¢\u0006\u0002\n\u0000R\u0010\u0010\u0018\u001a\u0004\u0018\u00010\u0019X\u0082\u000e¢\u0006\u0002\n\u0000R\u0010\u0010\u001a\u001a\u0004\u0018\u00010\u001bX\u0082\u000e¢\u0006\u0002\n\u0000R\u000e\u0010\u001c\u001a\u00020\u001dX\u0082\u000e¢\u0006\u0002\n\u0000R\u000e\u0010\u001e\u001a\u00020\u001dX\u0082\u000e¢\u0006\u0002\n\u0000R\u000e\u0010\u001f\u001a\u00020 X\u0082\u000e¢\u0006\u0002\n\u0000R\u0010\u0010!\u001a\u0004\u0018\u00010\"X\u0082\u000e¢\u0006\u0002\n\u0000R\u0010\u0010#\u001a\u0004\u0018\u00010$X\u0082\u000e¢\u0006\u0002\n\u0000R\u0010\u0010%\u001a\u0004\u0018\u00010&X\u0082\u000e¢\u0006\u0002\n\u0000¨\u0006\u0080\u0001"}, d2 = {"Lcom/poozh/translator/FloatingTranslatorService;", "Landroid/app/Service;", "<init>", "()V", "windowManager", "Landroid/view/WindowManager;", "settings", "Lcom/poozh/translator/data/AppSettings;", "recognizer", "Lcom/poozh/translator/ocr/ScreenTextRecognizer;", "deepSeekClient", "Lcom/poozh/translator/data/DeepSeekClient;", "bubbleView", "Landroid/widget/TextView;", "panelView", "Landroid/widget/LinearLayout;", "contentHost", "selectionOverlay", "Landroid/view/View;", "readingText", "statusText", "titleText", "mediaProjection", "Landroid/media/projection/MediaProjection;", "captureController", "Lcom/poozh/translator/capture/ScreenCaptureController;", "selectionRect", "Landroid/graphics/Rect;", "ocrBusy", "", "translating", "lastStableText", "", "lastResult", "Lcom/poozh/translator/model/AnalysisResult;", "currentCall", "Lokhttp3/Call;", "hiddenOverlayState", "Lcom/poozh/translator/FloatingTranslatorService$HiddenOverlayState;", "onCreate", "", "onStartCommand", "", "intent", "Landroid/content/Intent;", "flags", "startId", "onBind", "Landroid/os/IBinder;", "onDestroy", "handleCaptureResult", "addBubble", "togglePanel", "collapsePanel", "showPanel", "buildPanelHeader", "params", "Landroid/view/WindowManager$LayoutParams;", "buildToolbar", "showReadingPage", "currentReadingText", "showMorePage", "showSelectionOverlay", "refreshOnce", "handleCapturedFrame", "bitmap", "Landroid/graphics/Bitmap;", "handleCaptureError", "message", "handleFrame", "handleRecognizedText", "text", "forceTranslate", "requestTranslation", "retranslateLastText", "normalizeOcrText", "canUseNetwork", "snapshot", "Lcom/poozh/translator/data/SettingsSnapshot;", "requestCapturePermission", "openSettings", "copyResult", "showStatus", "runOnMain", "block", "Lkotlin/Function0;", "ensureForeground", "buildNotification", "Landroid/app/Notification;", "createNotificationChannel", "hideOwnWindowsForCapture", "restoreOwnWindowsAfterCapture", "attachDragAndClick", "view", "onClick", "onDragFinished", "Lkotlin/Function1;", "resizeHandle", "toolbarButton", "label", "textButton", "compact", "sectionTitle", "statusLine", "value", "menuAction", "rounded", "Landroid/graphics/drawable/GradientDrawable;", "color", "radius", "strokeColor", "(IILjava/lang/Integer;)Landroid/graphics/drawable/GradientDrawable;", "removeOverlay", "overlayType", "dp", "savePanelWindowState", "clampWindowX", "x", "width", "clampWindowY", "y", "height", "minPanelWidth", "minPanelHeight", "clampPanelWidth", "clampPanelHeight", "HiddenOverlayState", "Companion", "app_debug"}, k = 1, mv = {2, 0, 0}, xi = 48)
/* loaded from: classes3.dex */
public final class FloatingTranslatorService extends Service {
    public static final String ACTION_CAPTURE_RESULT = "com.poozh.translator.action.CAPTURE_RESULT";
    public static final String ACTION_REQUEST_CAPTURE = "com.poozh.translator.action.REQUEST_CAPTURE";
    public static final String ACTION_SHOW = "com.poozh.translator.action.SHOW";
    public static final String ACTION_STOP = "com.poozh.translator.action.STOP";
    private static final String CHANNEL_ID = "screen_translator";
    public static final String EXTRA_RESULT_CODE = "extra_result_code";
    public static final String EXTRA_RESULT_DATA = "extra_result_data";
    private static final int NOTIFICATION_ID = 3108;
    private TextView bubbleView;
    private ScreenCaptureController captureController;
    private LinearLayout contentHost;
    private Call currentCall;
    private HiddenOverlayState hiddenOverlayState;
    private AnalysisResult lastResult;
    private MediaProjection mediaProjection;
    private boolean ocrBusy;
    private LinearLayout panelView;
    private TextView readingText;
    private View selectionOverlay;
    private Rect selectionRect;
    private AppSettings settings;
    private TextView statusText;
    private TextView titleText;
    private boolean translating;
    private WindowManager windowManager;
    private final ScreenTextRecognizer recognizer = new ScreenTextRecognizer();
    private final DeepSeekClient deepSeekClient = new DeepSeekClient(null, 1, 0 == true ? 1 : 0);
    private String lastStableText = "";

    /* compiled from: FloatingTranslatorService.kt */
    @Metadata(k = 3, mv = {2, 0, 0}, xi = 48)
    public /* synthetic */ class WhenMappings {
        public static final /* synthetic */ int[] $EnumSwitchMapping$0;

        static {
            int[] iArr = new int[RefreshAction.values().length];
            try {
                iArr[RefreshAction.IGNORE_EMPTY.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                iArr[RefreshAction.REUSE_CACHED_RESULT.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                iArr[RefreshAction.REQUEST_TRANSLATION.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            $EnumSwitchMapping$0 = iArr;
        }
    }

    /* compiled from: FloatingTranslatorService.kt */
    @Metadata(d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u000f\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0000\b\u0082\b\u0018\u00002\u00020\u0001B%\u0012\b\u0010\u0002\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\u0004\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\u0005\u001a\u0004\u0018\u00010\u0003¢\u0006\u0004\b\u0006\u0010\u0007J\u0010\u0010\r\u001a\u0004\u0018\u00010\u0003HÆ\u0003¢\u0006\u0002\u0010\tJ\u0010\u0010\u000e\u001a\u0004\u0018\u00010\u0003HÆ\u0003¢\u0006\u0002\u0010\tJ\u0010\u0010\u000f\u001a\u0004\u0018\u00010\u0003HÆ\u0003¢\u0006\u0002\u0010\tJ2\u0010\u0010\u001a\u00020\u00002\n\b\u0002\u0010\u0002\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u0004\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u0005\u001a\u0004\u0018\u00010\u0003HÆ\u0001¢\u0006\u0002\u0010\u0011J\u0013\u0010\u0012\u001a\u00020\u00132\b\u0010\u0014\u001a\u0004\u0018\u00010\u0001HÖ\u0003J\t\u0010\u0015\u001a\u00020\u0003HÖ\u0001J\t\u0010\u0016\u001a\u00020\u0017HÖ\u0001R\u0015\u0010\u0002\u001a\u0004\u0018\u00010\u0003¢\u0006\n\n\u0002\u0010\n\u001a\u0004\b\b\u0010\tR\u0015\u0010\u0004\u001a\u0004\u0018\u00010\u0003¢\u0006\n\n\u0002\u0010\n\u001a\u0004\b\u000b\u0010\tR\u0015\u0010\u0005\u001a\u0004\u0018\u00010\u0003¢\u0006\n\n\u0002\u0010\n\u001a\u0004\b\f\u0010\t¨\u0006\u0018"}, d2 = {"Lcom/poozh/translator/FloatingTranslatorService$HiddenOverlayState;", "", "bubbleVisibility", "", "panelVisibility", "selectionVisibility", "<init>", "(Ljava/lang/Integer;Ljava/lang/Integer;Ljava/lang/Integer;)V", "getBubbleVisibility", "()Ljava/lang/Integer;", "Ljava/lang/Integer;", "getPanelVisibility", "getSelectionVisibility", "component1", "component2", "component3", "copy", "(Ljava/lang/Integer;Ljava/lang/Integer;Ljava/lang/Integer;)Lcom/poozh/translator/FloatingTranslatorService$HiddenOverlayState;", "equals", "", "other", "hashCode", "toString", "", "app_debug"}, k = 1, mv = {2, 0, 0}, xi = 48)
    private static final /* data */ class HiddenOverlayState {
        private final Integer bubbleVisibility;
        private final Integer panelVisibility;
        private final Integer selectionVisibility;

        public static /* synthetic */ HiddenOverlayState copy$default(HiddenOverlayState hiddenOverlayState, Integer num, Integer num2, Integer num3, int i, Object obj) {
            if ((i & 1) != 0) {
                num = hiddenOverlayState.bubbleVisibility;
            }
            if ((i & 2) != 0) {
                num2 = hiddenOverlayState.panelVisibility;
            }
            if ((i & 4) != 0) {
                num3 = hiddenOverlayState.selectionVisibility;
            }
            return hiddenOverlayState.copy(num, num2, num3);
        }

        /* renamed from: component1, reason: from getter */
        public final Integer getBubbleVisibility() {
            return this.bubbleVisibility;
        }

        /* renamed from: component2, reason: from getter */
        public final Integer getPanelVisibility() {
            return this.panelVisibility;
        }

        /* renamed from: component3, reason: from getter */
        public final Integer getSelectionVisibility() {
            return this.selectionVisibility;
        }

        public final HiddenOverlayState copy(Integer bubbleVisibility, Integer panelVisibility, Integer selectionVisibility) {
            return new HiddenOverlayState(bubbleVisibility, panelVisibility, selectionVisibility);
        }

        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof HiddenOverlayState)) {
                return false;
            }
            HiddenOverlayState hiddenOverlayState = (HiddenOverlayState) other;
            return Intrinsics.areEqual(this.bubbleVisibility, hiddenOverlayState.bubbleVisibility) && Intrinsics.areEqual(this.panelVisibility, hiddenOverlayState.panelVisibility) && Intrinsics.areEqual(this.selectionVisibility, hiddenOverlayState.selectionVisibility);
        }

        public int hashCode() {
            return ((((this.bubbleVisibility == null ? 0 : this.bubbleVisibility.hashCode()) * 31) + (this.panelVisibility == null ? 0 : this.panelVisibility.hashCode())) * 31) + (this.selectionVisibility != null ? this.selectionVisibility.hashCode() : 0);
        }

        public String toString() {
            return "HiddenOverlayState(bubbleVisibility=" + this.bubbleVisibility + ", panelVisibility=" + this.panelVisibility + ", selectionVisibility=" + this.selectionVisibility + ")";
        }

        public HiddenOverlayState(Integer bubbleVisibility, Integer panelVisibility, Integer selectionVisibility) {
            this.bubbleVisibility = bubbleVisibility;
            this.panelVisibility = panelVisibility;
            this.selectionVisibility = selectionVisibility;
        }

        public final Integer getBubbleVisibility() {
            return this.bubbleVisibility;
        }

        public final Integer getPanelVisibility() {
            return this.panelVisibility;
        }

        public final Integer getSelectionVisibility() {
            return this.selectionVisibility;
        }
    }

    @Override // android.app.Service
    public void onCreate() {
        super.onCreate();
        Object systemService = getSystemService("window");
        Intrinsics.checkNotNull(systemService, "null cannot be cast to non-null type android.view.WindowManager");
        this.windowManager = (WindowManager) systemService;
        this.settings = new AppSettings(this);
        ensureForeground();
        if (Settings.canDrawOverlays(this)) {
            addBubble();
        }
    }

    @Override // android.app.Service
    public int onStartCommand(Intent intent, int flags, int startId) {
        ensureForeground();
        String action = intent != null ? intent.getAction() : null;
        if (action != null) {
            switch (action.hashCode()) {
                case 1158746526:
                    if (!action.equals(ACTION_SHOW)) {
                        return 1;
                    }
                    break;
                case 1158758051:
                    if (action.equals(ACTION_STOP)) {
                        stopSelf();
                        return 1;
                    }
                    return 1;
                case 1773656247:
                    if (action.equals(ACTION_CAPTURE_RESULT)) {
                        handleCaptureResult(intent);
                        return 1;
                    }
                    return 1;
                default:
                    return 1;
            }
        }
        if (Settings.canDrawOverlays(this)) {
            addBubble();
            return 1;
        }
        return 1;
    }

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override // android.app.Service
    public void onDestroy() {
        Call call = this.currentCall;
        if (call != null) {
            call.cancel();
        }
        ScreenCaptureController screenCaptureController = this.captureController;
        if (screenCaptureController != null) {
            screenCaptureController.release();
        }
        this.captureController = null;
        this.recognizer.close();
        removeOverlay(this.panelView);
        removeOverlay(this.bubbleView);
        removeOverlay(this.selectionOverlay);
        super.onDestroy();
    }

    private final void handleCaptureResult(Intent intent) {
        int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
        Intent data = (Intent) intent.getParcelableExtra(EXTRA_RESULT_DATA);
        if (resultCode == 0 || data == null) {
            showStatus("屏幕捕获授权无效");
            return;
        }
        ScreenCaptureController screenCaptureController = this.captureController;
        if (screenCaptureController != null) {
            screenCaptureController.release();
        }
        this.captureController = null;
        Object systemService = getSystemService("media_projection");
        Intrinsics.checkNotNull(systemService, "null cannot be cast to non-null type android.media.projection.MediaProjectionManager");
        MediaProjectionManager manager = (MediaProjectionManager) systemService;
        this.mediaProjection = manager.getMediaProjection(resultCode, data);
        showStatus("屏幕捕获已就绪");
        if (this.selectionRect == null) {
            showSelectionOverlay();
        }
    }

    private final void addBubble() {
        if (this.bubbleView != null) {
            return;
        }
        TextView bubble = new TextView(this);
        bubble.setText("译");
        bubble.setTextSize(18.0f);
        bubble.setTypeface(Typeface.DEFAULT_BOLD);
        bubble.setGravity(17);
        bubble.setTextColor(-1);
        bubble.setBackground(rounded(Color.rgb(20, 184, 166), dp(28), Integer.valueOf(Color.argb(90, 255, 255, 255))));
        bubble.setElevation(dp(8));
        AppSettings appSettings = this.settings;
        WindowManager windowManager = null;
        if (appSettings == null) {
            Intrinsics.throwUninitializedPropertyAccessException("settings");
            appSettings = null;
        }
        Pair<Integer, Integer> loadBubblePosition = appSettings.loadBubblePosition(dp(18), dp(120));
        int savedX = loadBubblePosition.component1().intValue();
        int savedY = loadBubblePosition.component2().intValue();
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(dp(56), dp(56), overlayType(), 520, -3);
        params.gravity = 8388659;
        params.x = clampWindowX(savedX, dp(56));
        params.y = clampWindowY(savedY, dp(56));
        attachDragAndClick(bubble, params, new Function0() { // from class: com.poozh.translator.FloatingTranslatorService$$ExternalSyntheticLambda10
            @Override // kotlin.jvm.functions.Function0
            public final Object invoke() {
                Unit addBubble$lambda$2;
                addBubble$lambda$2 = FloatingTranslatorService.addBubble$lambda$2(FloatingTranslatorService.this);
                return addBubble$lambda$2;
            }
        }, new Function1() { // from class: com.poozh.translator.FloatingTranslatorService$$ExternalSyntheticLambda12
            @Override // kotlin.jvm.functions.Function1
            public final Object invoke(Object obj) {
                Unit addBubble$lambda$3;
                addBubble$lambda$3 = FloatingTranslatorService.addBubble$lambda$3(FloatingTranslatorService.this, (WindowManager.LayoutParams) obj);
                return addBubble$lambda$3;
            }
        });
        WindowManager windowManager2 = this.windowManager;
        if (windowManager2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("windowManager");
        } else {
            windowManager = windowManager2;
        }
        windowManager.addView(bubble, params);
        this.bubbleView = bubble;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit addBubble$lambda$2(FloatingTranslatorService this$0) {
        this$0.togglePanel();
        return Unit.INSTANCE;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit addBubble$lambda$3(FloatingTranslatorService this$0, WindowManager.LayoutParams it) {
        Intrinsics.checkNotNullParameter(it, "it");
        AppSettings appSettings = this$0.settings;
        if (appSettings == null) {
            Intrinsics.throwUninitializedPropertyAccessException("settings");
            appSettings = null;
        }
        appSettings.saveBubblePosition(it.x, it.y);
        return Unit.INSTANCE;
    }

    private final void togglePanel() {
        if (this.panelView == null) {
            showPanel();
        } else {
            collapsePanel();
        }
    }

    private final void collapsePanel() {
        removeOverlay(this.panelView);
        this.panelView = null;
        this.contentHost = null;
        this.readingText = null;
        this.statusText = null;
        this.titleText = null;
    }

    private final void showPanel() {
        WindowManager windowManager;
        if (this.panelView != null) {
            return;
        }
        int displayWidth = getResources().getDisplayMetrics().widthPixels;
        int displayHeight = getResources().getDisplayMetrics().heightPixels;
        int defaultWidth = RangesKt.coerceAtLeast(Math.min(dp(360), displayWidth - dp(24)), minPanelWidth());
        int defaultHeight = RangesKt.coerceAtLeast(Math.min(dp(560), displayHeight - dp(88)), minPanelHeight());
        AppSettings appSettings = this.settings;
        if (appSettings == null) {
            Intrinsics.throwUninitializedPropertyAccessException("settings");
            appSettings = null;
        }
        OverlayWindowState saved = appSettings.loadPanelState(dp(56), dp(86), defaultWidth, defaultHeight);
        int panelWidth = clampPanelWidth(saved.getWidth());
        int panelHeight = clampPanelHeight(saved.getHeight());
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(panelWidth, panelHeight, overlayType(), 520, -3);
        params.gravity = 8388659;
        params.x = clampWindowX(saved.getX(), panelWidth);
        params.y = clampWindowY(saved.getY(), panelHeight);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(1);
        int dp = dp(12);
        int dp2 = dp(12);
        int dp3 = dp(12);
        int displayWidth2 = dp(10);
        root.setPadding(dp, dp2, dp3, displayWidth2);
        root.setBackground(rounded(Color.argb(238, 15, 18, 24), dp(18), Integer.valueOf(Color.argb(80, 148, 163, 184))));
        root.setElevation(dp(12));
        LinearLayout header = buildPanelHeader(params);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, 0, 1.0f);
        layoutParams.topMargin = dp(10);
        layoutParams.bottomMargin = dp(10);
        linearLayout.setLayoutParams(layoutParams);
        this.contentHost = linearLayout;
        LinearLayout toolbar = buildToolbar(params);
        root.addView(header);
        root.addView(this.contentHost);
        root.addView(toolbar);
        WindowManager windowManager2 = this.windowManager;
        if (windowManager2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("windowManager");
            windowManager = null;
        } else {
            windowManager = windowManager2;
        }
        windowManager.addView(root, params);
        this.panelView = root;
        showReadingPage();
        showStatus(this.lastResult == null ? "等待刷新" : "已加载上次结果");
    }

    private final LinearLayout buildPanelHeader(WindowManager.LayoutParams params) {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(0);
        header.setGravity(16);
        header.setPadding(dp(2), 0, dp(2), 0);
        TextView textView = new TextView(this);
        textView.setText("阅读翻译");
        textView.setTextSize(16.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setTextColor(Color.rgb(241, 245, 249));
        textView.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        this.titleText = textView;
        TextView textView2 = new TextView(this);
        textView2.setText("等待刷新");
        textView2.setTextSize(12.0f);
        textView2.setTextColor(Color.rgb(MlKitException.CODE_SCANNER_TASK_IN_PROGRESS, 251, 241));
        textView2.setPadding(dp(10), dp(5), dp(10), dp(5));
        textView2.setBackground(rounded$default(this, Color.argb(70, 20, 184, 166), dp(12), null, 4, null));
        this.statusText = textView2;
        TextView close = textButton("×", true, new Function0() { // from class: com.poozh.translator.FloatingTranslatorService$$ExternalSyntheticLambda7
            @Override // kotlin.jvm.functions.Function0
            public final Object invoke() {
                Unit buildPanelHeader$lambda$11;
                buildPanelHeader$lambda$11 = FloatingTranslatorService.buildPanelHeader$lambda$11(FloatingTranslatorService.this);
                return buildPanelHeader$lambda$11;
            }
        });
        header.addView(this.titleText);
        header.addView(this.statusText);
        header.addView(close);
        attachDragAndClick(header, params, null, new FloatingTranslatorService$buildPanelHeader$3(this));
        return header;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit buildPanelHeader$lambda$11(FloatingTranslatorService this$0) {
        this$0.collapsePanel();
        return Unit.INSTANCE;
    }

    private final LinearLayout buildToolbar(WindowManager.LayoutParams params) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(17);
        linearLayout.addView(toolbarButton("刷新", new Function0() { // from class: com.poozh.translator.FloatingTranslatorService$$ExternalSyntheticLambda3
            @Override // kotlin.jvm.functions.Function0
            public final Object invoke() {
                Unit buildToolbar$lambda$16$lambda$12;
                buildToolbar$lambda$16$lambda$12 = FloatingTranslatorService.buildToolbar$lambda$16$lambda$12(FloatingTranslatorService.this);
                return buildToolbar$lambda$16$lambda$12;
            }
        }));
        linearLayout.addView(toolbarButton("选区", new Function0() { // from class: com.poozh.translator.FloatingTranslatorService$$ExternalSyntheticLambda4
            @Override // kotlin.jvm.functions.Function0
            public final Object invoke() {
                Unit buildToolbar$lambda$16$lambda$13;
                buildToolbar$lambda$16$lambda$13 = FloatingTranslatorService.buildToolbar$lambda$16$lambda$13(FloatingTranslatorService.this);
                return buildToolbar$lambda$16$lambda$13;
            }
        }));
        linearLayout.addView(toolbarButton("更多", new Function0() { // from class: com.poozh.translator.FloatingTranslatorService$$ExternalSyntheticLambda5
            @Override // kotlin.jvm.functions.Function0
            public final Object invoke() {
                Unit buildToolbar$lambda$16$lambda$14;
                buildToolbar$lambda$16$lambda$14 = FloatingTranslatorService.buildToolbar$lambda$16$lambda$14(FloatingTranslatorService.this);
                return buildToolbar$lambda$16$lambda$14;
            }
        }));
        linearLayout.addView(toolbarButton("收起", new Function0() { // from class: com.poozh.translator.FloatingTranslatorService$$ExternalSyntheticLambda6
            @Override // kotlin.jvm.functions.Function0
            public final Object invoke() {
                Unit buildToolbar$lambda$16$lambda$15;
                buildToolbar$lambda$16$lambda$15 = FloatingTranslatorService.buildToolbar$lambda$16$lambda$15(FloatingTranslatorService.this);
                return buildToolbar$lambda$16$lambda$15;
            }
        }));
        linearLayout.addView(resizeHandle(params));
        return linearLayout;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit buildToolbar$lambda$16$lambda$12(FloatingTranslatorService this$0) {
        this$0.refreshOnce();
        return Unit.INSTANCE;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit buildToolbar$lambda$16$lambda$13(FloatingTranslatorService this$0) {
        this$0.showSelectionOverlay();
        return Unit.INSTANCE;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit buildToolbar$lambda$16$lambda$14(FloatingTranslatorService this$0) {
        this$0.showMorePage();
        return Unit.INSTANCE;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit buildToolbar$lambda$16$lambda$15(FloatingTranslatorService this$0) {
        this$0.collapsePanel();
        return Unit.INSTANCE;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void showReadingPage() {
        TextView textView = this.titleText;
        if (textView != null) {
            textView.setText("阅读翻译");
        }
        LinearLayout host = this.contentHost;
        if (host == null) {
            return;
        }
        host.removeAllViews();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setOverScrollMode(1);
        TextView textView2 = new TextView(this);
        textView2.setText(currentReadingText());
        textView2.setTextSize(16.0f);
        textView2.setLineSpacing(dp(4), 1.0f);
        textView2.setTextColor(Color.rgb(226, 232, 240));
        textView2.setPadding(dp(2), dp(2), dp(2), dp(20));
        this.readingText = textView2;
        scroll.addView(this.readingText);
        host.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
    }

    private final String currentReadingText() {
        AnalysisResult analysisResult = this.lastResult;
        if (analysisResult != null) {
            return analysisResult.toDisplayText();
        }
        if (!StringsKt.isBlank(this.lastStableText)) {
            return "原文\n" + this.lastStableText + "\n\n暂无译文。点“更多 > 重新翻译”再次请求。";
        }
        return "选择屏幕区域后，点“刷新”识别当前画面。\n\n这里会只显示原文、中文翻译和日语解析，不混入设置项。";
    }

    /* JADX WARN: Code restructure failed: missing block: B:32:0x00fa, code lost:
    
        if (r3 == null) goto L38;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private final void showMorePage() {
        /*
            Method dump skipped, instructions count: 385
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.poozh.translator.FloatingTranslatorService.showMorePage():void");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit showMorePage$lambda$22(FloatingTranslatorService this$0) {
        this$0.showReadingPage();
        return Unit.INSTANCE;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit showMorePage$lambda$23(FloatingTranslatorService this$0) {
        this$0.retranslateLastText();
        return Unit.INSTANCE;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit showMorePage$lambda$24(FloatingTranslatorService this$0) {
        this$0.copyResult();
        return Unit.INSTANCE;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit showMorePage$lambda$25(FloatingTranslatorService this$0) {
        this$0.openSettings();
        return Unit.INSTANCE;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit showMorePage$lambda$26(FloatingTranslatorService this$0) {
        this$0.stopSelf();
        return Unit.INSTANCE;
    }

    private final void showSelectionOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            showStatus("请先授权悬浮窗");
            return;
        }
        removeOverlay(this.selectionOverlay);
        SelectionOverlayView overlay = new SelectionOverlayView(this, new Function1() { // from class: com.poozh.translator.FloatingTranslatorService$$ExternalSyntheticLambda21
            @Override // kotlin.jvm.functions.Function1
            public final Object invoke(Object obj) {
                Unit showSelectionOverlay$lambda$27;
                showSelectionOverlay$lambda$27 = FloatingTranslatorService.showSelectionOverlay$lambda$27(FloatingTranslatorService.this, (Rect) obj);
                return showSelectionOverlay$lambda$27;
            }
        }, new Function0() { // from class: com.poozh.translator.FloatingTranslatorService$$ExternalSyntheticLambda1
            @Override // kotlin.jvm.functions.Function0
            public final Object invoke() {
                Unit showSelectionOverlay$lambda$28;
                showSelectionOverlay$lambda$28 = FloatingTranslatorService.showSelectionOverlay$lambda$28(FloatingTranslatorService.this);
                return showSelectionOverlay$lambda$28;
            }
        });
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(-1, -1, overlayType(), 8, -3);
        layoutParams.gravity = 8388659;
        WindowManager windowManager = this.windowManager;
        if (windowManager == null) {
            Intrinsics.throwUninitializedPropertyAccessException("windowManager");
            windowManager = null;
        }
        windowManager.addView(overlay, layoutParams);
        this.selectionOverlay = overlay;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit showSelectionOverlay$lambda$27(FloatingTranslatorService this$0, Rect rect) {
        Intrinsics.checkNotNullParameter(rect, "rect");
        this$0.selectionRect = rect;
        this$0.removeOverlay(this$0.selectionOverlay);
        this$0.selectionOverlay = null;
        this$0.showStatus("选区已更新");
        return Unit.INSTANCE;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit showSelectionOverlay$lambda$28(FloatingTranslatorService this$0) {
        this$0.removeOverlay(this$0.selectionOverlay);
        this$0.selectionOverlay = null;
        this$0.showStatus("已取消选区");
        return Unit.INSTANCE;
    }

    private final void refreshOnce() {
        if (this.ocrBusy || this.translating) {
            showStatus("正在处理当前文本");
            return;
        }
        if (this.panelView == null) {
            showPanel();
        } else {
            showReadingPage();
        }
        MediaProjection projection = this.mediaProjection;
        if (projection == null) {
            requestCapturePermission();
            return;
        }
        if (this.selectionRect == null) {
            showSelectionOverlay();
            return;
        }
        ScreenCaptureController controller = this.captureController;
        if (controller == null) {
            controller = new ScreenCaptureController(this, projection, new Function0() { // from class: com.poozh.translator.FloatingTranslatorService$$ExternalSyntheticLambda11
                @Override // kotlin.jvm.functions.Function0
                public final Object invoke() {
                    Rect rect;
                    rect = FloatingTranslatorService.this.selectionRect;
                    return rect;
                }
            }, new FloatingTranslatorService$refreshOnce$controller$2(this), new FloatingTranslatorService$refreshOnce$controller$3(this));
            this.captureController = controller;
        }
        showStatus("正在截取屏幕");
        hideOwnWindowsForCapture();
        controller.captureOnce();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void handleCapturedFrame(Bitmap bitmap) {
        restoreOwnWindowsAfterCapture();
        handleFrame(bitmap);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void handleCaptureError(String message) {
        restoreOwnWindowsAfterCapture();
        showStatus(message);
    }

    private final void handleFrame(Bitmap bitmap) {
        if (this.ocrBusy || this.translating) {
            bitmap.recycle();
            showStatus("正在处理当前文本");
        } else {
            this.ocrBusy = true;
            showStatus("正在识别文字");
            this.recognizer.recognize(bitmap, new ScreenTextRecognizer.Callback() { // from class: com.poozh.translator.FloatingTranslatorService$handleFrame$1
                @Override // com.poozh.translator.ocr.ScreenTextRecognizer.Callback
                public void onSuccess(String text) {
                    Intrinsics.checkNotNullParameter(text, "text");
                    FloatingTranslatorService.this.ocrBusy = false;
                    FloatingTranslatorService.this.handleRecognizedText(text, false);
                }

                @Override // com.poozh.translator.ocr.ScreenTextRecognizer.Callback
                public void onFailure(String message) {
                    Intrinsics.checkNotNullParameter(message, "message");
                    FloatingTranslatorService.this.ocrBusy = false;
                    FloatingTranslatorService.this.showStatus(message);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void handleRecognizedText(String text, boolean forceTranslate) {
        String normalized = normalizeOcrText(text);
        switch (WhenMappings.$EnumSwitchMapping$0[TranslationRefreshPolicy.INSTANCE.decide(normalized, this.lastStableText, this.lastResult != null, forceTranslate).ordinal()]) {
            case 1:
                showStatus("选区内未识别到文本");
                return;
            case 2:
                showReadingPage();
                showStatus("内容未变化，已复用");
                return;
            case 3:
                requestTranslation(normalized);
                return;
            default:
                throw new NoWhenBranchMatchedException();
        }
    }

    private final void requestTranslation(String text) {
        if (this.translating) {
            showStatus("正在翻译");
            return;
        }
        AppSettings appSettings = this.settings;
        if (appSettings == null) {
            Intrinsics.throwUninitializedPropertyAccessException("settings");
            appSettings = null;
        }
        SettingsSnapshot snapshot = appSettings.load();
        this.lastStableText = text;
        this.lastResult = null;
        showReadingPage();
        TextView textView = this.readingText;
        if (textView != null) {
            textView.setText("原文\n" + text + "\n\n正在翻译...");
        }
        if (!canUseNetwork(snapshot)) {
            TextView textView2 = this.readingText;
            if (textView2 != null) {
                textView2.setText("原文\n" + text + "\n\n当前设置为仅 Wi-Fi 请求，网络不满足条件。");
            }
            showStatus("等待 Wi-Fi");
            return;
        }
        this.translating = true;
        showStatus("正在翻译");
        Call call = this.currentCall;
        if (call != null) {
            call.cancel();
        }
        this.currentCall = this.deepSeekClient.analyze(text, LanguageDetector.INSTANCE.detect(text), snapshot, new FloatingTranslatorService$requestTranslation$1(this, text));
    }

    private final void retranslateLastText() {
        if (this.ocrBusy || this.translating) {
            showStatus("正在处理当前文本");
        } else if (StringsKt.isBlank(this.lastStableText)) {
            showStatus("还没有可重新翻译的文本");
        } else {
            showReadingPage();
            handleRecognizedText(this.lastStableText, true);
        }
    }

    private final String normalizeOcrText(String text) {
        Iterable lines = StringsKt.lines(text);
        Collection arrayList = new ArrayList(CollectionsKt.collectionSizeOrDefault(lines, 10));
        Iterator it = lines.iterator();
        while (it.hasNext()) {
            arrayList.add(StringsKt.trim((CharSequence) it.next()).toString());
        }
        Collection arrayList2 = new ArrayList();
        for (Object obj : (List) arrayList) {
            if (!StringsKt.isBlank((String) obj)) {
                arrayList2.add(obj);
            }
        }
        return CollectionsKt.joinToString$default((List) arrayList2, "\n", null, null, 0, null, null, 62, null);
    }

    private final boolean canUseNetwork(SettingsSnapshot snapshot) {
        NetworkCapabilities capabilities;
        if (!snapshot.getWifiOnly()) {
            return true;
        }
        Object systemService = getSystemService("connectivity");
        Intrinsics.checkNotNull(systemService, "null cannot be cast to non-null type android.net.ConnectivityManager");
        ConnectivityManager manager = (ConnectivityManager) systemService;
        Network network = manager.getActiveNetwork();
        if (network == null || (capabilities = manager.getNetworkCapabilities(network)) == null) {
            return false;
        }
        return capabilities.hasTransport(1);
    }

    private final void requestCapturePermission() {
        Intent intent = new Intent(this, (Class<?>) MainActivity.class).setAction(ACTION_REQUEST_CAPTURE).addFlags(268435456);
        Intrinsics.checkNotNullExpressionValue(intent, "addFlags(...)");
        startActivity(intent);
        showStatus("请授权屏幕捕获");
    }

    private final void openSettings() {
        startActivity(new Intent(this, (Class<?>) MainActivity.class).addFlags(268435456));
    }

    private final void copyResult() {
        String text;
        CharSequence text2;
        AnalysisResult analysisResult = this.lastResult;
        if (analysisResult == null || (text = analysisResult.toDisplayText()) == null) {
            TextView textView = this.readingText;
            text = (textView == null || (text2 = textView.getText()) == null) ? null : text2.toString();
            if (text == null) {
                text = "";
            }
        }
        if (StringsKt.isBlank(text)) {
            return;
        }
        Object systemService = getSystemService("clipboard");
        Intrinsics.checkNotNull(systemService, "null cannot be cast to non-null type android.content.ClipboardManager");
        ClipboardManager clipboard = (ClipboardManager) systemService;
        clipboard.setPrimaryClip(ClipData.newPlainText("translation", text));
        Toast.makeText(this, "已复制", 0).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void showStatus(final String message) {
        runOnMain(new Function0() { // from class: com.poozh.translator.FloatingTranslatorService$$ExternalSyntheticLambda13
            @Override // kotlin.jvm.functions.Function0
            public final Object invoke() {
                Unit showStatus$lambda$34;
                showStatus$lambda$34 = FloatingTranslatorService.showStatus$lambda$34(FloatingTranslatorService.this, message);
                return showStatus$lambda$34;
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit showStatus$lambda$34(FloatingTranslatorService this$0, String $message) {
        TextView textView = this$0.statusText;
        if (textView != null) {
            textView.setText($message);
        }
        return Unit.INSTANCE;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void runOnMain(final Function0<Unit> block) {
        new Handler(getMainLooper()).post(new Runnable() { // from class: com.poozh.translator.FloatingTranslatorService$$ExternalSyntheticLambda14
            @Override // java.lang.Runnable
            public final void run() {
                Function0.this.invoke();
            }
        });
    }

    private final void ensureForeground() {
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());
    }

    private final Notification buildNotification() {
        PendingIntent stopIntent = PendingIntent.getService(this, 0, new Intent(this, (Class<?>) FloatingTranslatorService.class).setAction(ACTION_STOP), 201326592);
        Notification.Builder builder = new Notification.Builder(this, CHANNEL_ID);
        Notification build = builder.setSmallIcon(android.R.drawable.ic_menu_view).setContentTitle("屏幕翻译运行中").setContentText("点击悬浮窗手动刷新识别").setOngoing(true).addAction(android.R.drawable.ic_menu_close_clear_cancel, "停止", stopIntent).build();
        Intrinsics.checkNotNullExpressionValue(build, "build(...)");
        return build;
    }

    private final void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "屏幕翻译", 2);
        ((NotificationManager) getSystemService(NotificationManager.class)).createNotificationChannel(channel);
    }

    private final void hideOwnWindowsForCapture() {
        if (this.hiddenOverlayState != null) {
            return;
        }
        TextView textView = this.bubbleView;
        Integer valueOf = textView != null ? Integer.valueOf(textView.getVisibility()) : null;
        LinearLayout linearLayout = this.panelView;
        Integer valueOf2 = linearLayout != null ? Integer.valueOf(linearLayout.getVisibility()) : null;
        View view = this.selectionOverlay;
        this.hiddenOverlayState = new HiddenOverlayState(valueOf, valueOf2, view != null ? Integer.valueOf(view.getVisibility()) : null);
        TextView textView2 = this.bubbleView;
        if (textView2 != null) {
            textView2.setVisibility(4);
        }
        LinearLayout linearLayout2 = this.panelView;
        if (linearLayout2 != null) {
            linearLayout2.setVisibility(4);
        }
        View view2 = this.selectionOverlay;
        if (view2 != null) {
            view2.setVisibility(4);
        }
    }

    private final void restoreOwnWindowsAfterCapture() {
        HiddenOverlayState state = this.hiddenOverlayState;
        if (state == null) {
            return;
        }
        Integer bubbleVisibility = state.getBubbleVisibility();
        if (bubbleVisibility != null) {
            int intValue = bubbleVisibility.intValue();
            TextView textView = this.bubbleView;
            if (textView != null) {
                textView.setVisibility(intValue);
            }
        }
        Integer panelVisibility = state.getPanelVisibility();
        if (panelVisibility != null) {
            int intValue2 = panelVisibility.intValue();
            LinearLayout linearLayout = this.panelView;
            if (linearLayout != null) {
                linearLayout.setVisibility(intValue2);
            }
        }
        Integer selectionVisibility = state.getSelectionVisibility();
        if (selectionVisibility != null) {
            int intValue3 = selectionVisibility.intValue();
            View view = this.selectionOverlay;
            if (view != null) {
                view.setVisibility(intValue3);
            }
        }
        this.hiddenOverlayState = null;
    }

    /* JADX WARN: Multi-variable type inference failed */
    static /* synthetic */ void attachDragAndClick$default(FloatingTranslatorService floatingTranslatorService, View view, WindowManager.LayoutParams layoutParams, Function0 function0, Function1 function1, int i, Object obj) {
        if ((i & 8) != 0) {
            function1 = null;
        }
        floatingTranslatorService.attachDragAndClick(view, layoutParams, function0, function1);
    }

    private final void attachDragAndClick(final View view, final WindowManager.LayoutParams params, final Function0<Unit> onClick, final Function1<? super WindowManager.LayoutParams, Unit> onDragFinished) {
        final Ref.FloatRef downRawX = new Ref.FloatRef();
        final Ref.FloatRef downRawY = new Ref.FloatRef();
        final Ref.IntRef startX = new Ref.IntRef();
        final Ref.IntRef startY = new Ref.IntRef();
        view.setOnTouchListener(new View.OnTouchListener() { // from class: com.poozh.translator.FloatingTranslatorService$$ExternalSyntheticLambda8
            @Override // android.view.View.OnTouchListener
            public final boolean onTouch(View view2, MotionEvent motionEvent) {
                boolean attachDragAndClick$lambda$40;
                attachDragAndClick$lambda$40 = FloatingTranslatorService.attachDragAndClick$lambda$40(Ref.FloatRef.this, downRawY, startX, params, startY, this, onClick, onDragFinished, view, view2, motionEvent);
                return attachDragAndClick$lambda$40;
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final boolean attachDragAndClick$lambda$40(Ref.FloatRef $downRawX, Ref.FloatRef $downRawY, Ref.IntRef $startX, WindowManager.LayoutParams $params, Ref.IntRef $startY, FloatingTranslatorService this$0, Function0 $onClick, Function1 $onDragFinished, View $view, View view, MotionEvent event) {
        switch (event.getActionMasked()) {
            case 0:
                $downRawX.element = event.getRawX();
                $downRawY.element = event.getRawY();
                $startX.element = $params.x;
                $startY.element = $params.y;
                break;
            case 1:
                float moved = Math.abs(event.getRawX() - $downRawX.element) + Math.abs(event.getRawY() - $downRawY.element);
                if (moved < this$0.dp(8) && $onClick != null) {
                    $onClick.invoke();
                }
                if ($onDragFinished != null) {
                    $onDragFinished.invoke($params);
                    break;
                }
                break;
            case 2:
                $params.x = this$0.clampWindowX($startX.element + ((int) (event.getRawX() - $downRawX.element)), $params.width);
                $params.y = this$0.clampWindowY($startY.element + ((int) (event.getRawY() - $downRawY.element)), $params.height);
                try {
                    Result.Companion companion = Result.INSTANCE;
                    WindowManager windowManager = this$0.windowManager;
                    if (windowManager == null) {
                        Intrinsics.throwUninitializedPropertyAccessException("windowManager");
                        windowManager = null;
                    }
                    windowManager.updateViewLayout($view.getRootView(), $params);
                    Result.m17constructorimpl(Unit.INSTANCE);
                    break;
                } catch (Throwable th) {
                    Result.Companion companion2 = Result.INSTANCE;
                    Result.m17constructorimpl(ResultKt.createFailure(th));
                    return true;
                }
        }
        return true;
    }

    private final TextView resizeHandle(final WindowManager.LayoutParams params) {
        TextView handle = new TextView(this);
        handle.setText("↘");
        handle.setTextSize(18.0f);
        handle.setGravity(17);
        handle.setTypeface(Typeface.DEFAULT_BOLD);
        handle.setTextColor(Color.rgb(MlKitException.CODE_SCANNER_TASK_IN_PROGRESS, 251, 241));
        handle.setPadding(0, dp(10), 0, dp(10));
        handle.setBackground(rounded$default(this, Color.argb(70, 20, 184, 166), dp(12), null, 4, null));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(dp(42), -2);
        layoutParams.leftMargin = dp(3);
        layoutParams.rightMargin = dp(3);
        handle.setLayoutParams(layoutParams);
        final Ref.FloatRef downRawX = new Ref.FloatRef();
        final Ref.FloatRef downRawY = new Ref.FloatRef();
        final Ref.IntRef startWidth = new Ref.IntRef();
        final Ref.IntRef startHeight = new Ref.IntRef();
        handle.setOnTouchListener(new View.OnTouchListener() { // from class: com.poozh.translator.FloatingTranslatorService$$ExternalSyntheticLambda2
            @Override // android.view.View.OnTouchListener
            public final boolean onTouch(View view, MotionEvent motionEvent) {
                boolean resizeHandle$lambda$45;
                resizeHandle$lambda$45 = FloatingTranslatorService.resizeHandle$lambda$45(Ref.FloatRef.this, downRawY, startWidth, params, startHeight, this, view, motionEvent);
                return resizeHandle$lambda$45;
            }
        });
        return handle;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final boolean resizeHandle$lambda$45(Ref.FloatRef $downRawX, Ref.FloatRef $downRawY, Ref.IntRef $startWidth, WindowManager.LayoutParams $params, Ref.IntRef $startHeight, FloatingTranslatorService this$0, View view, MotionEvent event) {
        Object m17constructorimpl;
        switch (event.getActionMasked()) {
            case 0:
                $downRawX.element = event.getRawX();
                $downRawY.element = event.getRawY();
                $startWidth.element = $params.width;
                $startHeight.element = $params.height;
                break;
            case 1:
                this$0.savePanelWindowState($params);
                break;
            case 2:
                $params.width = this$0.clampPanelWidth($startWidth.element + ((int) (event.getRawX() - $downRawX.element)));
                $params.height = this$0.clampPanelHeight($startHeight.element + ((int) (event.getRawY() - $downRawY.element)));
                $params.x = this$0.clampWindowX($params.x, $params.width);
                $params.y = this$0.clampWindowY($params.y, $params.height);
                LinearLayout linearLayout = this$0.panelView;
                if (linearLayout != null) {
                    try {
                        Result.Companion companion = Result.INSTANCE;
                        WindowManager windowManager = this$0.windowManager;
                        if (windowManager == null) {
                            Intrinsics.throwUninitializedPropertyAccessException("windowManager");
                            windowManager = null;
                        }
                        windowManager.updateViewLayout(linearLayout, $params);
                        m17constructorimpl = Result.m17constructorimpl(Unit.INSTANCE);
                    } catch (Throwable th) {
                        Result.Companion companion2 = Result.INSTANCE;
                        m17constructorimpl = Result.m17constructorimpl(ResultKt.createFailure(th));
                    }
                    Result.m16boximpl(m17constructorimpl);
                    break;
                }
                break;
        }
        return true;
    }

    private final TextView toolbarButton(String label, final Function0<Unit> onClick) {
        TextView textView = new TextView(this);
        textView.setText(label);
        textView.setTextSize(13.0f);
        textView.setGravity(17);
        textView.setTextColor(Color.rgb(226, 232, 240));
        textView.setPadding(0, dp(10), 0, dp(10));
        textView.setBackground(rounded$default(this, Color.argb(55, 148, 163, 184), dp(12), null, 4, null));
        textView.setOnClickListener(new View.OnClickListener() { // from class: com.poozh.translator.FloatingTranslatorService$$ExternalSyntheticLambda15
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                Function0.this.invoke();
            }
        });
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, -2, 1.0f);
        layoutParams.leftMargin = dp(3);
        layoutParams.rightMargin = dp(3);
        textView.setLayoutParams(layoutParams);
        return textView;
    }

    static /* synthetic */ TextView textButton$default(FloatingTranslatorService floatingTranslatorService, String str, boolean z, Function0 function0, int i, Object obj) {
        if ((i & 2) != 0) {
            z = false;
        }
        return floatingTranslatorService.textButton(str, z, function0);
    }

    private final TextView textButton(String label, boolean compact, final Function0<Unit> onClick) {
        TextView textView = new TextView(this);
        textView.setText(label);
        textView.setTextSize(compact ? 20.0f : 14.0f);
        textView.setGravity(17);
        textView.setTextColor(Color.rgb(226, 232, 240));
        textView.setPadding(dp(10), dp(4), dp(10), dp(4));
        textView.setBackground(rounded$default(this, Color.argb(45, 148, 163, 184), dp(12), null, 4, null));
        textView.setOnClickListener(new View.OnClickListener() { // from class: com.poozh.translator.FloatingTranslatorService$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                Function0.this.invoke();
            }
        });
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(compact ? dp(38) : -2, compact ? dp(34) : -2);
        layoutParams.leftMargin = dp(8);
        textView.setLayoutParams(layoutParams);
        return textView;
    }

    private final TextView sectionTitle(String label) {
        TextView textView = new TextView(this);
        textView.setText(label);
        textView.setTextSize(13.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setTextColor(Color.rgb(153, 246, 228));
        textView.setPadding(0, dp(12), 0, dp(6));
        return textView;
    }

    private final TextView statusLine(String label, String value) {
        TextView textView = new TextView(this);
        textView.setText(label + "：" + value);
        textView.setTextSize(14.0f);
        textView.setTextColor(Color.rgb(226, 232, 240));
        textView.setPadding(dp(12), dp(9), dp(12), dp(9));
        textView.setBackground(rounded$default(this, Color.argb(36, 51, 65, 85), dp(10), null, 4, null));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.bottomMargin = dp(6);
        textView.setLayoutParams(layoutParams);
        return textView;
    }

    private final TextView menuAction(String label, final Function0<Unit> onClick) {
        TextView textView = new TextView(this);
        textView.setText(label);
        textView.setTextSize(15.0f);
        textView.setGravity(16);
        textView.setTextColor(Color.rgb(241, 245, 249));
        textView.setPadding(dp(14), dp(12), dp(14), dp(12));
        textView.setBackground(rounded$default(this, Color.argb(55, 20, 184, 166), dp(12), null, 4, null));
        textView.setOnClickListener(new View.OnClickListener() { // from class: com.poozh.translator.FloatingTranslatorService$$ExternalSyntheticLambda9
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                Function0.this.invoke();
            }
        });
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.bottomMargin = dp(8);
        textView.setLayoutParams(layoutParams);
        return textView;
    }

    static /* synthetic */ GradientDrawable rounded$default(FloatingTranslatorService floatingTranslatorService, int i, int i2, Integer num, int i3, Object obj) {
        if ((i3 & 4) != 0) {
            num = null;
        }
        return floatingTranslatorService.rounded(i, i2, num);
    }

    private final GradientDrawable rounded(int color, int radius, Integer strokeColor) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(color);
        gradientDrawable.setCornerRadius(radius);
        if (strokeColor != null) {
            gradientDrawable.setStroke(dp(1), strokeColor.intValue());
        }
        return gradientDrawable;
    }

    private final void removeOverlay(View view) {
        if (view == null) {
            return;
        }
        try {
            Result.Companion companion = Result.INSTANCE;
            WindowManager windowManager = this.windowManager;
            if (windowManager == null) {
                Intrinsics.throwUninitializedPropertyAccessException("windowManager");
                windowManager = null;
            }
            windowManager.removeView(view);
            Result.m17constructorimpl(Unit.INSTANCE);
        } catch (Throwable th) {
            Result.Companion companion2 = Result.INSTANCE;
            Result.m17constructorimpl(ResultKt.createFailure(th));
        }
    }

    private final int overlayType() {
        return 2038;
    }

    private final int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void savePanelWindowState(WindowManager.LayoutParams params) {
        AppSettings appSettings = this.settings;
        if (appSettings == null) {
            Intrinsics.throwUninitializedPropertyAccessException("settings");
            appSettings = null;
        }
        appSettings.savePanelState(params.x, params.y, params.width, params.height);
    }

    private final int clampWindowX(int x, int width) {
        int effectiveWidth = Math.max(width, dp(48));
        int maxX = Math.max(0, getResources().getDisplayMetrics().widthPixels - effectiveWidth);
        return RangesKt.coerceIn(x, 0, maxX);
    }

    private final int clampWindowY(int y, int height) {
        int effectiveHeight = Math.max(height, dp(48));
        int maxY = Math.max(0, getResources().getDisplayMetrics().heightPixels - effectiveHeight);
        return RangesKt.coerceIn(y, 0, maxY);
    }

    private final int minPanelWidth() {
        return dp(MlKitException.LOW_LIGHT_AUTO_EXPOSURE_COMPUTATION_FAILURE);
    }

    private final int minPanelHeight() {
        return dp(320);
    }

    private final int clampPanelWidth(int width) {
        int maxWidth = Math.max(minPanelWidth(), getResources().getDisplayMetrics().widthPixels - dp(24));
        return RangesKt.coerceIn(width, minPanelWidth(), maxWidth);
    }

    private final int clampPanelHeight(int height) {
        int maxHeight = Math.max(minPanelHeight(), getResources().getDisplayMetrics().heightPixels - dp(72));
        return RangesKt.coerceIn(height, minPanelHeight(), maxHeight);
    }
}
