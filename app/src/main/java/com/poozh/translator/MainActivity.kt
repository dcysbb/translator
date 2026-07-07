package com.poozh.translator

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.drawerlayout.widget.DrawerLayout
import androidx.core.view.GravityCompat
import com.poozh.translator.data.AppSettings
import com.poozh.translator.data.ModelProviderPreset
import com.poozh.translator.data.ModelProviders
import com.poozh.translator.data.SettingsSnapshot
import com.poozh.translator.ui.Md3
import com.poozh.translator.ui.Md3ButtonStyle
import com.poozh.translator.ui.Md3Motion
import com.poozh.translator.ui.Md3TextStyle

class MainActivity : Activity() {
    private lateinit var settings: AppSettings
    private lateinit var statusText: TextView
    private lateinit var apiKeyInput: EditText
    private lateinit var baseUrlInput: EditText
    private lateinit var modelInput: EditText
    private lateinit var wifiOnlyInput: Switch
    /** Provider currently selected in the UI chip row. Drives save() + chips. */
    private var selectedProviderId: String = ModelProviders.DEFAULT_PROVIDER_ID
    /** Chip row container — kept so we can restyle chips when the selection
     *  changes, instead of rebuilding the whole view tree. */
    private var providerChipRow: LinearLayout? = null
    /** "当前服务" line — refreshed when the provider changes. */
    private var currentServiceLine: TextView? = null
    /** The drawer layout — kept so [onBackPressed] can close it when open. */
    private var drawerRef: DrawerLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = AppSettings(this)
        buildContentView()
        requestNotificationPermissionIfNeeded()

        if (intent?.action == FloatingTranslatorService.ACTION_REQUEST_CAPTURE) {
            requestScreenCapture()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    @Deprecated("Deprecated by Android framework")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == RESULT_OK && data != null) {
                val serviceIntent = Intent(this, FloatingTranslatorService::class.java)
                    .setAction(FloatingTranslatorService.ACTION_CAPTURE_RESULT)
                    .putExtra(FloatingTranslatorService.EXTRA_RESULT_CODE, resultCode)
                    .putExtra(FloatingTranslatorService.EXTRA_RESULT_DATA, data)
                startTranslatorService(serviceIntent)
                Toast.makeText(this, "屏幕捕获已授权", Toast.LENGTH_SHORT).show()
                if (intent?.action == FloatingTranslatorService.ACTION_REQUEST_CAPTURE) finish()
            } else {
                Toast.makeText(this, "未获得屏幕捕获权限", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun buildContentView() {
        val snapshot = settings.load()
        selectedProviderId = snapshot.providerId
        window.statusBarColor = Md3.light.surface
        window.navigationBarColor = Md3.light.surfaceContainer
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        val drawer = DrawerLayout(this)

        // ── Main content area ──────────────────────────────────────────────
        val main = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Md3.light.surface)
            setPadding(dp(screenMargin()), dp(20), dp(screenMargin()), dp(32))
        }
        // Top bar: hamburger + title.
        main.addView(mainTopBar(drawer))
        // Status card.
        statusText = TextView(this).apply {
            Md3.applyTextStyle(this, Md3TextStyle.BodyMedium, Md3.light.onSurface)
            setLineSpacing(dp(3).toFloat(), 1.0f)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = Md3.surface(
                context = this@MainActivity,
                color = Md3.light.surfaceContainer,
                radiusDp = 12f
            )
        }
        main.addView(statusText, blockParams(top = 16))
        // Primary CTA: start the overlay service.
        main.addView(card("翻译控制台").apply {
            addView(actionButton("启动悬浮翻译", Md3ButtonStyle.Filled) {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    Toast.makeText(this@MainActivity, "请先授权悬浮窗", Toast.LENGTH_SHORT).show()
                } else {
                    startTranslatorService(
                        Intent(this@MainActivity, FloatingTranslatorService::class.java)
                            .setAction(FloatingTranslatorService.ACTION_SHOW)
                    )
                    Toast.makeText(this@MainActivity, "悬浮窗已启动", Toast.LENGTH_SHORT).show()
                }
            })
            addView(actionButton("授权屏幕捕获", Md3ButtonStyle.FilledTonal) { requestScreenCapture() })
        }, blockParams(top = 16))

        // ── Drawer (left slide-out) ────────────────────────────────────────
        val drawerScroll = ScrollView(this).apply { clipToPadding = false }
        val drawerContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Md3.light.surfaceContainerLow)
            setPadding(dp(screenMargin()), dp(28), dp(screenMargin()), dp(32))
        }
        drawerContent.addView(TextView(this).apply {
            text = "设置"
            Md3.applyTextStyle(this, Md3TextStyle.HeadlineSmall, Md3.light.onSurface)
            setPadding(0, 0, 0, dp(8))
        })

        // Group: 连接配置 (the connection-settings card, including provider chips).
        drawerContent.addView(card("连接配置").apply {
            currentServiceLine = listLine("当前服务", ModelProviders.byId(snapshot.providerId).name)
            addView(currentServiceLine!!)
            addView(providerPresetScroller(snapshot))
            val preset = ModelProviders.byId(snapshot.providerId)
            apiKeyInput = input(
                hint = if (preset.requiresApiKey) "${preset.name} API Key" else "本地服务通常可留空",
                value = snapshot.apiKey,
                password = true
            )
            baseUrlInput = input("Chat Completions URL", snapshot.baseUrl, password = false)
            modelInput = input("模型名称", snapshot.model, password = false)
            addView(apiKeyInput)
            addView(baseUrlInput)
            addView(modelInput)
            addView(switchRow(snapshot))
            addView(actionButton("保存设置", Md3ButtonStyle.Filled) { saveSettings() })
            addView(actionButton("清除 API Key", Md3ButtonStyle.Text) {
                settings.clearApiKey()
                apiKeyInput.setText("")
                Toast.makeText(this@MainActivity, "API Key 已清除", Toast.LENGTH_SHORT).show()
                refreshStatus()
            })
        }, blockParams(top = 16))

        // Group: 屏幕捕获与权限.
        drawerContent.addView(card("屏幕捕获与权限").apply {
            addView(actionButton("授权屏幕捕获", Md3ButtonStyle.FilledTonal) { requestScreenCapture() })
            addView(actionButton("悬浮窗权限", Md3ButtonStyle.Outlined) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            })
            addView(actionButton("停止悬浮服务", Md3ButtonStyle.Text) {
                startTranslatorService(
                    Intent(this@MainActivity, FloatingTranslatorService::class.java)
                        .setAction(FloatingTranslatorService.ACTION_STOP)
                )
                Toast.makeText(this@MainActivity, "悬浮服务已停止", Toast.LENGTH_SHORT).show()
            })
        }, blockParams(top = 16))

        // Group: 使用说明.
        drawerContent.addView(card("使用说明").apply {
            addView(listLine("悬浮窗", "屏幕边缘"))
            addView(listLine("选区", "OCR 区域"))
            addView(listLine("刷新", "识别并翻译"))
            addView(listLine("复制", "阅读内容"))
        }, blockParams(top = 16))

        drawerScroll.addView(drawerContent)
        val drawerWidth = (resources.displayMetrics.widthPixels * 0.84f).toInt()
        val drawerParams = DrawerLayout.LayoutParams(
            drawerWidth,
            DrawerLayout.LayoutParams.MATCH_PARENT
        ).apply { gravity = GravityCompat.START }
        drawer.addView(drawerScroll, drawerParams)

        // Main content fills the rest.
        val mainParams = DrawerLayout.LayoutParams(
            DrawerLayout.LayoutParams.MATCH_PARENT,
            DrawerLayout.LayoutParams.MATCH_PARENT
        )
        drawer.addView(main, mainParams)

        setContentView(drawer)
        drawerRef = drawer
        Md3Motion.enter(main, dp(16).toFloat())
    }

    @Deprecated("Deprecated by Android framework")
    override fun onBackPressed() {
        // Close the drawer first instead of finishing the activity immediately.
        val drawer = drawerRef
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    /** Top bar with a hamburger button that opens the drawer. */
    private fun mainTopBar(drawer: DrawerLayout): LinearLayout {
        val hamburger = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_sort_by_size)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { drawer.openDrawer(GravityCompat.START) }
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        val title = TextView(this).apply {
            text = "屏幕翻译"
            Md3.applyTextStyle(this, Md3TextStyle.HeadlineMedium, Md3.light.onSurface)
            setPadding(dp(8), 0, 0, 0)
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(4))
            addView(hamburger)
            addView(title)
        }
    }

    private fun saveSettings() {
        val current = settings.load()
        val providerId = selectedProviderId
        settings.save(
            providerId = providerId,
            baseUrl = baseUrlInput.text.toString().trim(),
            model = modelInput.text.toString().trim(),
            intervalMs = current.intervalMs,
            wifiOnly = wifiOnlyInput.isChecked
        )
        // Per-provider key: persist what's in the field (may be empty = cleared).
        settings.saveApiKey(providerId, apiKeyInput.text.toString().trim())
        Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show()
        refreshStatus()
        // Tell the running service to pick up the new settings on its next pass.
        startTranslatorService(
            Intent(this, FloatingTranslatorService::class.java)
                .setAction(FloatingTranslatorService.ACTION_REFRESH_SETTINGS)
        )
    }

    private fun refreshStatus() {
        val snapshot = settings.load()
        selectedProviderId = snapshot.providerId
        val overlay = if (Settings.canDrawOverlays(this)) "已授权" else "未授权"
        val key = if (snapshot.apiKey.isNotBlank()) "已保存" else "未设置"
        val network = if (snapshot.wifiOnly) "仅 Wi-Fi" else "不限网络"
        statusText.text = "悬浮窗：$overlay\n模型服务：${ModelProviders.byId(snapshot.providerId).name}\nAPI Key：$key\n模型：${snapshot.model}\n网络：$network"
    }

    private fun requestScreenCapture() {
        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        // On Android 14+ the no-arg createScreenCaptureIntent() defaults to the
        // "single app" (partial) consent flow, which only captures the chosen
        // app's window. That breaks OCR: when the user returns to this app to
        // draw the selection / tap refresh, the captured app goes to the
        // background and stops rendering, so OCR gets a blank frame.
        //
        // createConfigForDefaultDisplay() forces a full-screen (whole display)
        // capture session and skips the app chooser, so the projection keeps
        // producing frames regardless of which app is in the foreground — which
        // is what an always-on-top selection overlay needs.
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            projectionManager.createScreenCaptureIntent(
                android.media.projection.MediaProjectionConfig.createConfigForDefaultDisplay()
            )
        } else {
            projectionManager.createScreenCaptureIntent()
        }
        startActivityForResult(intent, REQUEST_MEDIA_PROJECTION)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_POST_NOTIFICATIONS)
        }
    }

    private fun startTranslatorService(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
    }

    private fun card(title: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(14))
            background = Md3.surface(
                context = this@MainActivity,
                color = Md3.light.surfaceContainerLow,
                radiusDp = 12f,
                strokeColor = Md3.light.outlineVariant
            )
            addView(TextView(this@MainActivity).apply {
                text = title
                Md3.applyTextStyle(this, Md3TextStyle.TitleMedium, Md3.light.onSurface)
                setPadding(0, 0, 0, dp(12))
            })
        }
    }

    private fun listLine(label: String, value: String): TextView {
        return TextView(this).apply {
            text = "$label：$value"
            Md3.applyTextStyle(this, Md3TextStyle.BodyMedium, Md3.light.onSurfaceVariant)
            setPadding(0, dp(10), 0, dp(10))
            background = Md3.ripple(this@MainActivity, Color.TRANSPARENT, 8f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun providerPresetScroller(snapshot: SettingsSnapshot): HorizontalScrollView {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, dp(4), 0)
        }
        providerChipRow = row
        ModelProviders.presets.forEach { preset ->
            row.addView(presetChip(preset))
        }
        // Apply the initial selection styling (which chip is highlighted).
        updateProviderChipStyles()
        return HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            isFillViewport = true
            addView(row)
            // Don't steal taps from chips when there's nothing to scroll —
            // without this the parent ScrollView/DrawerLayout can swallow the
            // ACTION_UP and the chip's click listener never fires.
            setOnTouchListener { v, event ->
                v.parent?.requestDisallowInterceptTouchEvent(true)
                false
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
        }
    }

    private fun presetChip(preset: ModelProviderPreset): TextView {
        return TextView(this).apply {
            text = preset.name
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            minHeight = dp(40)
            minWidth = dp(64)
            setPadding(dp(16), dp(8), dp(16), dp(8))
            tag = preset.id
            setOnClickListener { applyProviderPreset(preset) }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { rightMargin = dp(8) }
        }
    }

    /**
     * Restyle every chip in [providerChipRow] to reflect [selectedProviderId].
     * Called both at build time and after [applyProviderPreset] so the highlight
     * actually follows the user's tap (the original bug: styles were baked in
     * once and never updated).
     */
    private fun updateProviderChipStyles() {
        val row = providerChipRow ?: return
        val selectedId = selectedProviderId
        for (i in 0 until row.childCount) {
            val chip = row.getChildAt(i) as? TextView ?: continue
            val selected = chip.tag == selectedId
            Md3.applyTextStyle(
                chip,
                Md3TextStyle.LabelLarge,
                if (selected) Md3.light.onSecondaryContainer else Md3.light.onSurfaceVariant
            )
            chip.background = Md3.ripple(
                context = this,
                fillColor = if (selected) Md3.light.secondaryContainer else Color.TRANSPARENT,
                radiusDp = 8f,
                strokeColor = if (selected) null else Md3.light.outline
            )
        }
        currentServiceLine?.text = "当前服务：${ModelProviders.byId(selectedId).name}"
    }

    private fun applyProviderPreset(preset: ModelProviderPreset) {
        // Switch the active provider immediately and load its saved (or default)
        // model/url + saved key into the fields. baseUrl/model/key persist
        // independently per provider, so toggling between chips never loses config.
        selectedProviderId = preset.id
        settings.selectProvider(preset.id)
        val savedKey = settings.getApiKey(preset.id)
        val savedModel = settings.getModel(preset.id).ifBlank { preset.defaultModel }
        baseUrlInput.setText(preset.baseUrl)
        modelInput.setText(savedModel)
        apiKeyInput.setText(savedKey)
        apiKeyInput.hint = if (preset.requiresApiKey) "${preset.name} API Key" else "本地服务通常可留空"
        // Refresh chip highlights + the "当前服务" line so the tap is visible.
        updateProviderChipStyles()
        Toast.makeText(this, "已切换到 ${preset.name}", Toast.LENGTH_SHORT).show()
        refreshStatus()
    }

    private fun input(hint: String, value: String, password: Boolean): EditText {
        return EditText(this).apply {
            this.hint = hint
            setText(value)
            Md3.applyTextStyle(this, Md3TextStyle.BodyLarge, Md3.light.onSurface)
            setHintTextColor(Md3.light.onSurfaceVariant)
            setSingleLine(true)
            setPadding(dp(16), 0, dp(16), 0)
            background = Md3.ripple(
                context = this@MainActivity,
                fillColor = Md3.light.surfaceContainerHighest,
                radiusDp = 8f,
                strokeColor = Md3.light.outline
            )
            inputType = if (password) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56)
            ).apply { bottomMargin = dp(12) }
        }
    }

    private fun switchRow(snapshot: SettingsSnapshot): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, dp(12))
            addView(TextView(this@MainActivity).apply {
                text = "仅 Wi-Fi 时请求模型服务"
                Md3.applyTextStyle(this, Md3TextStyle.BodyLarge, Md3.light.onSurface)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            wifiOnlyInput = Switch(this@MainActivity).apply {
                isChecked = snapshot.wifiOnly
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    thumbTintList = ColorStateList.valueOf(Md3.light.primary)
                    trackTintList = ColorStateList.valueOf(Md3.withAlpha(Md3.light.primary, 0.32f))
                }
            }
            addView(wifiOnlyInput)
        }
    }

    private fun actionButton(label: String, style: Md3ButtonStyle, action: (View) -> Unit): TextView {
        return TextView(this).apply {
            text = label
            gravity = Gravity.CENTER
            minHeight = dp(48)
            Md3.applyTextStyle(this, Md3TextStyle.LabelLarge, Md3.buttonTextColor(style))
            setPadding(dp(24), 0, dp(24), 0)
            background = Md3.buttonBackground(this@MainActivity, style)
            setOnClickListener(action)
            Md3.bindStateLayer(this)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
            ).apply { bottomMargin = dp(8) }
        }
    }

    private fun blockParams(top: Int): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(top) }
    }

    private fun screenMargin(): Int = if (resources.displayMetrics.widthPixels / resources.displayMetrics.density >= 600f) 24 else 16

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val REQUEST_MEDIA_PROJECTION = 1001
        private const val REQUEST_POST_NOTIFICATIONS = 1002
    }
}
