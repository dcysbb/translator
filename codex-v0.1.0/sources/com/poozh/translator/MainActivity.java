package com.poozh.translator;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.exifinterface.media.ExifInterface;
import com.google.mlkit.common.MlKitException;
import com.poozh.translator.data.AppSettings;
import com.poozh.translator.data.SettingsSnapshot;
import kotlin.Deprecated;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import kotlin.text.StringsKt;

/* compiled from: MainActivity.kt */
@Metadata(d1 = {"\u0000l\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0007\u0018\u0000 62\u00020\u0001:\u00016B\u0007¢\u0006\u0004\b\u0002\u0010\u0003J\u0012\u0010\u000e\u001a\u00020\u000f2\b\u0010\u0010\u001a\u0004\u0018\u00010\u0011H\u0014J\b\u0010\u0012\u001a\u00020\u000fH\u0014J\"\u0010\u0013\u001a\u00020\u000f2\u0006\u0010\u0014\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u00152\b\u0010\u0017\u001a\u0004\u0018\u00010\u0018H\u0015J\b\u0010\u0019\u001a\u00020\u000fH\u0002J\b\u0010\u001a\u001a\u00020\u000fH\u0002J\b\u0010\u001b\u001a\u00020\u000fH\u0002J\b\u0010\u001c\u001a\u00020\u000fH\u0002J\b\u0010\u001d\u001a\u00020\u000fH\u0002J\u0010\u0010\u001e\u001a\u00020\u000f2\u0006\u0010\u001f\u001a\u00020\u0018H\u0002J\u0010\u0010 \u001a\u00020!2\u0006\u0010\"\u001a\u00020#H\u0002J.\u0010$\u001a\u00020\u00072\u0006\u0010%\u001a\u00020#2\b\b\u0002\u0010&\u001a\u00020'2\u0012\u0010(\u001a\u000e\u0012\u0004\u0012\u00020*\u0012\u0004\u0012\u00020\u000f0)H\u0002J \u0010+\u001a\u00020\t2\u0006\u0010,\u001a\u00020#2\u0006\u0010-\u001a\u00020#2\u0006\u0010.\u001a\u00020'H\u0002J)\u0010/\u001a\u0002002\u0006\u00101\u001a\u00020\u00152\u0006\u00102\u001a\u00020\u00152\n\b\u0002\u00103\u001a\u0004\u0018\u00010\u0015H\u0002¢\u0006\u0002\u00104J\u0010\u00105\u001a\u00020\u00152\u0006\u0010-\u001a\u00020\u0015H\u0002R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082.¢\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082.¢\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082.¢\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\tX\u0082.¢\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\tX\u0082.¢\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\rX\u0082.¢\u0006\u0002\n\u0000¨\u00067"}, d2 = {"Lcom/poozh/translator/MainActivity;", "Landroid/app/Activity;", "<init>", "()V", "settings", "Lcom/poozh/translator/data/AppSettings;", "statusText", "Landroid/widget/TextView;", "apiKeyInput", "Landroid/widget/EditText;", "baseUrlInput", "modelInput", "wifiOnlyInput", "Landroid/widget/CheckBox;", "onCreate", "", "savedInstanceState", "Landroid/os/Bundle;", "onResume", "onActivityResult", "requestCode", "", "resultCode", "data", "Landroid/content/Intent;", "buildContentView", "saveSettings", "refreshStatus", "requestScreenCapture", "requestNotificationPermissionIfNeeded", "startTranslatorService", "intent", "section", "Landroid/widget/LinearLayout;", "title", "", "actionButton", "label", "primary", "", "action", "Lkotlin/Function1;", "Landroid/view/View;", "input", "hint", "value", "password", "rounded", "Landroid/graphics/drawable/GradientDrawable;", "color", "radius", "strokeColor", "(IILjava/lang/Integer;)Landroid/graphics/drawable/GradientDrawable;", "dp", "Companion", "app_debug"}, k = 1, mv = {2, 0, 0}, xi = 48)
/* loaded from: classes3.dex */
public final class MainActivity extends Activity {
    private static final int REQUEST_MEDIA_PROJECTION = 1001;
    private static final int REQUEST_POST_NOTIFICATIONS = 1002;
    private EditText apiKeyInput;
    private EditText baseUrlInput;
    private EditText modelInput;
    private AppSettings settings;
    private TextView statusText;
    private CheckBox wifiOnlyInput;

    @Override // android.app.Activity
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.settings = new AppSettings(this);
        buildContentView();
        requestNotificationPermissionIfNeeded();
        Intent intent = getIntent();
        if (Intrinsics.areEqual(intent != null ? intent.getAction() : null, FloatingTranslatorService.ACTION_REQUEST_CAPTURE)) {
            requestScreenCapture();
        }
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    @Override // android.app.Activity
    @Deprecated(message = "Deprecated by Android framework")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001) {
            if (resultCode == -1 && data != null) {
                Intent serviceIntent = new Intent(this, (Class<?>) FloatingTranslatorService.class).setAction(FloatingTranslatorService.ACTION_CAPTURE_RESULT).putExtra(FloatingTranslatorService.EXTRA_RESULT_CODE, resultCode).putExtra(FloatingTranslatorService.EXTRA_RESULT_DATA, data);
                Intrinsics.checkNotNullExpressionValue(serviceIntent, "putExtra(...)");
                startTranslatorService(serviceIntent);
                Toast.makeText(this, "屏幕捕获已授权", 0).show();
                Intent intent = getIntent();
                if (Intrinsics.areEqual(intent != null ? intent.getAction() : null, FloatingTranslatorService.ACTION_REQUEST_CAPTURE)) {
                    finish();
                    return;
                }
                return;
            }
            Toast.makeText(this, "未获得屏幕捕获权限", 0).show();
        }
    }

    private final void buildContentView() {
        CheckBox checkBox;
        AppSettings appSettings = this.settings;
        if (appSettings == null) {
            Intrinsics.throwUninitializedPropertyAccessException("settings");
            appSettings = null;
        }
        SettingsSnapshot snapshot = appSettings.load();
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setPadding(dp(18), dp(22), dp(18), dp(28));
        linearLayout.setBackgroundColor(Color.rgb(244, 247, 249));
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(Color.rgb(244, 247, 249));
        scrollView.addView(linearLayout);
        TextView textView = new TextView(this);
        textView.setText("屏幕翻译");
        textView.setTextSize(28.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setTextColor(Color.rgb(15, 23, 42));
        linearLayout.addView(textView);
        TextView textView2 = new TextView(this);
        textView2.setText("把悬浮窗当成阅读工具：在其他 App 中框选文字，手动刷新识别并阅读译文。");
        textView2.setTextSize(15.0f);
        textView2.setTextColor(Color.rgb(71, 85, 105));
        textView2.setPadding(0, dp(8), 0, dp(18));
        linearLayout.addView(textView2);
        TextView textView3 = new TextView(this);
        textView3.setTextSize(14.0f);
        textView3.setTextColor(Color.rgb(15, 23, 42));
        textView3.setPadding(dp(14), dp(12), dp(14), dp(12));
        textView3.setBackground(rounded(-1, dp(14), Integer.valueOf(Color.rgb(226, 232, 240))));
        this.statusText = textView3;
        TextView textView4 = this.statusText;
        if (textView4 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("statusText");
            textView4 = null;
        }
        linearLayout.addView(textView4);
        LinearLayout section = section("权限与悬浮窗");
        section.addView(actionButton$default(this, "授权悬浮窗", false, new Function1() { // from class: com.poozh.translator.MainActivity$$ExternalSyntheticLambda1
            @Override // kotlin.jvm.functions.Function1
            public final Object invoke(Object obj) {
                Unit buildContentView$lambda$8$lambda$5;
                buildContentView$lambda$8$lambda$5 = MainActivity.buildContentView$lambda$8$lambda$5(MainActivity.this, (View) obj);
                return buildContentView$lambda$8$lambda$5;
            }
        }, 2, null));
        section.addView(actionButton$default(this, "启动悬浮窗", false, new Function1() { // from class: com.poozh.translator.MainActivity$$ExternalSyntheticLambda2
            @Override // kotlin.jvm.functions.Function1
            public final Object invoke(Object obj) {
                Unit buildContentView$lambda$8$lambda$6;
                buildContentView$lambda$8$lambda$6 = MainActivity.buildContentView$lambda$8$lambda$6(MainActivity.this, (View) obj);
                return buildContentView$lambda$8$lambda$6;
            }
        }, 2, null));
        section.addView(actionButton$default(this, "授权屏幕捕获", false, new Function1() { // from class: com.poozh.translator.MainActivity$$ExternalSyntheticLambda3
            @Override // kotlin.jvm.functions.Function1
            public final Object invoke(Object obj) {
                Unit buildContentView$lambda$8$lambda$7;
                buildContentView$lambda$8$lambda$7 = MainActivity.buildContentView$lambda$8$lambda$7(MainActivity.this, (View) obj);
                return buildContentView$lambda$8$lambda$7;
            }
        }, 2, null));
        linearLayout.addView(section);
        LinearLayout section2 = section("DeepSeek");
        this.apiKeyInput = input("API Key 留空则不修改", "", true);
        this.baseUrlInput = input("Base URL", snapshot.getBaseUrl(), false);
        this.modelInput = input(ExifInterface.TAG_MODEL, snapshot.getModel(), false);
        CheckBox checkBox2 = new CheckBox(this);
        checkBox2.setText("仅 Wi-Fi 时请求 DeepSeek");
        checkBox2.setTextSize(14.0f);
        checkBox2.setTextColor(Color.rgb(51, 65, 85));
        checkBox2.setChecked(snapshot.getWifiOnly());
        checkBox2.setPadding(0, dp(4), 0, dp(8));
        this.wifiOnlyInput = checkBox2;
        EditText editText = this.apiKeyInput;
        if (editText == null) {
            Intrinsics.throwUninitializedPropertyAccessException("apiKeyInput");
            editText = null;
        }
        section2.addView(editText);
        EditText editText2 = this.baseUrlInput;
        if (editText2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("baseUrlInput");
            editText2 = null;
        }
        section2.addView(editText2);
        EditText editText3 = this.modelInput;
        if (editText3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("modelInput");
            editText3 = null;
        }
        section2.addView(editText3);
        CheckBox checkBox3 = this.wifiOnlyInput;
        if (checkBox3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("wifiOnlyInput");
            checkBox = null;
        } else {
            checkBox = checkBox3;
        }
        section2.addView(checkBox);
        section2.addView(actionButton("保存设置", true, new Function1() { // from class: com.poozh.translator.MainActivity$$ExternalSyntheticLambda4
            @Override // kotlin.jvm.functions.Function1
            public final Object invoke(Object obj) {
                Unit buildContentView$lambda$12$lambda$10;
                buildContentView$lambda$12$lambda$10 = MainActivity.buildContentView$lambda$12$lambda$10(MainActivity.this, (View) obj);
                return buildContentView$lambda$12$lambda$10;
            }
        }));
        section2.addView(actionButton$default(this, "清除 API Key", false, new Function1() { // from class: com.poozh.translator.MainActivity$$ExternalSyntheticLambda5
            @Override // kotlin.jvm.functions.Function1
            public final Object invoke(Object obj) {
                Unit buildContentView$lambda$12$lambda$11;
                buildContentView$lambda$12$lambda$11 = MainActivity.buildContentView$lambda$12$lambda$11(MainActivity.this, (View) obj);
                return buildContentView$lambda$12$lambda$11;
            }
        }, 2, null));
        linearLayout.addView(section2);
        setContentView(scrollView);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit buildContentView$lambda$8$lambda$5(MainActivity this$0, View it) {
        Intrinsics.checkNotNullParameter(it, "it");
        this$0.startActivity(new Intent("android.settings.action.MANAGE_OVERLAY_PERMISSION", Uri.parse("package:" + this$0.getPackageName())));
        return Unit.INSTANCE;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit buildContentView$lambda$8$lambda$6(MainActivity this$0, View it) {
        Intrinsics.checkNotNullParameter(it, "it");
        if (!Settings.canDrawOverlays(this$0)) {
            Toast.makeText(this$0, "请先授权悬浮窗", 0).show();
        } else {
            Intent action = new Intent(this$0, (Class<?>) FloatingTranslatorService.class).setAction(FloatingTranslatorService.ACTION_SHOW);
            Intrinsics.checkNotNullExpressionValue(action, "setAction(...)");
            this$0.startTranslatorService(action);
            Toast.makeText(this$0, "悬浮窗已启动", 0).show();
        }
        return Unit.INSTANCE;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit buildContentView$lambda$8$lambda$7(MainActivity this$0, View it) {
        Intrinsics.checkNotNullParameter(it, "it");
        this$0.requestScreenCapture();
        return Unit.INSTANCE;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit buildContentView$lambda$12$lambda$10(MainActivity this$0, View it) {
        Intrinsics.checkNotNullParameter(it, "it");
        this$0.saveSettings();
        return Unit.INSTANCE;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit buildContentView$lambda$12$lambda$11(MainActivity this$0, View it) {
        Intrinsics.checkNotNullParameter(it, "it");
        AppSettings appSettings = this$0.settings;
        if (appSettings == null) {
            Intrinsics.throwUninitializedPropertyAccessException("settings");
            appSettings = null;
        }
        appSettings.clearApiKey();
        Toast.makeText(this$0, "API Key 已清除", 0).show();
        this$0.refreshStatus();
        return Unit.INSTANCE;
    }

    private final void saveSettings() {
        AppSettings appSettings;
        AppSettings appSettings2 = this.settings;
        EditText editText = null;
        if (appSettings2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("settings");
            appSettings2 = null;
        }
        SettingsSnapshot current = appSettings2.load();
        AppSettings appSettings3 = this.settings;
        if (appSettings3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("settings");
            appSettings = null;
        } else {
            appSettings = appSettings3;
        }
        EditText editText2 = this.baseUrlInput;
        if (editText2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("baseUrlInput");
            editText2 = null;
        }
        String obj = StringsKt.trim((CharSequence) editText2.getText().toString()).toString();
        EditText editText3 = this.modelInput;
        if (editText3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("modelInput");
            editText3 = null;
        }
        String obj2 = StringsKt.trim((CharSequence) editText3.getText().toString()).toString();
        long intervalMs = current.getIntervalMs();
        CheckBox checkBox = this.wifiOnlyInput;
        if (checkBox == null) {
            Intrinsics.throwUninitializedPropertyAccessException("wifiOnlyInput");
            checkBox = null;
        }
        appSettings.save(obj, obj2, intervalMs, checkBox.isChecked());
        EditText editText4 = this.apiKeyInput;
        if (editText4 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("apiKeyInput");
            editText4 = null;
        }
        String apiKey = StringsKt.trim((CharSequence) editText4.getText().toString()).toString();
        if (!StringsKt.isBlank(apiKey)) {
            AppSettings appSettings4 = this.settings;
            if (appSettings4 == null) {
                Intrinsics.throwUninitializedPropertyAccessException("settings");
                appSettings4 = null;
            }
            appSettings4.saveApiKey(apiKey);
            EditText editText5 = this.apiKeyInput;
            if (editText5 == null) {
                Intrinsics.throwUninitializedPropertyAccessException("apiKeyInput");
            } else {
                editText = editText5;
            }
            editText.setText("");
        }
        Toast.makeText(this, "设置已保存", 0).show();
        refreshStatus();
    }

    private final void refreshStatus() {
        AppSettings appSettings = this.settings;
        TextView textView = null;
        if (appSettings == null) {
            Intrinsics.throwUninitializedPropertyAccessException("settings");
            appSettings = null;
        }
        SettingsSnapshot snapshot = appSettings.load();
        String overlay = Settings.canDrawOverlays(this) ? "已授权" : "未授权";
        String key = !StringsKt.isBlank(snapshot.getApiKey()) ? "已保存" : "未设置";
        String network = snapshot.getWifiOnly() ? "仅 Wi-Fi" : "不限网络";
        TextView textView2 = this.statusText;
        if (textView2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("statusText");
        } else {
            textView = textView2;
        }
        textView.setText("悬浮窗：" + overlay + "\nDeepSeek Key：" + key + "\n模型：" + snapshot.getModel() + "\n网络：" + network);
    }

    private final void requestScreenCapture() {
        Object systemService = getSystemService("media_projection");
        Intrinsics.checkNotNull(systemService, "null cannot be cast to non-null type android.media.projection.MediaProjectionManager");
        MediaProjectionManager projectionManager = (MediaProjectionManager) systemService;
        startActivityForResult(projectionManager.createScreenCaptureIntent(), 1001);
    }

    private final void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1002);
        }
    }

    private final void startTranslatorService(Intent intent) {
        startForegroundService(intent);
    }

    private final LinearLayout section(String title) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setPadding(dp(14), dp(14), dp(14), dp(12));
        linearLayout.setBackground(rounded(-1, dp(16), Integer.valueOf(Color.rgb(226, 232, 240))));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = dp(14);
        linearLayout.setLayoutParams(layoutParams);
        TextView textView = new TextView(this);
        textView.setText(title);
        textView.setTextSize(18.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setTextColor(Color.rgb(15, 23, 42));
        textView.setPadding(0, 0, 0, dp(10));
        linearLayout.addView(textView);
        return linearLayout;
    }

    static /* synthetic */ TextView actionButton$default(MainActivity mainActivity, String str, boolean z, Function1 function1, int i, Object obj) {
        if ((i & 2) != 0) {
            z = false;
        }
        return mainActivity.actionButton(str, z, function1);
    }

    private final TextView actionButton(String label, boolean primary, final Function1<? super View, Unit> action) {
        TextView textView = new TextView(this);
        textView.setText(label);
        textView.setTextSize(15.0f);
        textView.setGravity(17);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setTextColor(primary ? -1 : Color.rgb(15, 118, 110));
        textView.setPadding(dp(12), dp(12), dp(12), dp(12));
        textView.setBackground(rounded(primary ? Color.rgb(15, 118, 110) : Color.rgb(240, 253, 250), dp(12), primary ? null : Integer.valueOf(Color.rgb(153, 246, 228))));
        textView.setOnClickListener(new View.OnClickListener() { // from class: com.poozh.translator.MainActivity$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                Function1.this.invoke(view);
            }
        });
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.bottomMargin = dp(8);
        textView.setLayoutParams(layoutParams);
        return textView;
    }

    private final EditText input(String hint, String value, boolean password) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setText(value);
        editText.setTextSize(14.0f);
        int i = 1;
        editText.setSingleLine(true);
        editText.setPadding(dp(12), 0, dp(12), 0);
        editText.setTextColor(Color.rgb(15, 23, 42));
        editText.setHintTextColor(Color.rgb(100, 116, 139));
        editText.setBackground(rounded(Color.rgb(248, 250, 252), dp(12), Integer.valueOf(Color.rgb(MlKitException.CODE_SCANNER_APP_NAME_UNAVAILABLE, 213, 225))));
        if (password) {
            i = 129;
        }
        editText.setInputType(i);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, dp(48));
        layoutParams.bottomMargin = dp(10);
        editText.setLayoutParams(layoutParams);
        return editText;
    }

    static /* synthetic */ GradientDrawable rounded$default(MainActivity mainActivity, int i, int i2, Integer num, int i3, Object obj) {
        if ((i3 & 4) != 0) {
            num = null;
        }
        return mainActivity.rounded(i, i2, num);
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

    private final int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
