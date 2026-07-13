package com.poozh.translator

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
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
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.drawerlayout.widget.DrawerLayout
import androidx.core.view.GravityCompat
import com.poozh.translator.data.AppSettings
import com.poozh.translator.data.HistoryItem
import com.poozh.translator.data.HistoryManager
import com.poozh.translator.data.ModelProviderPreset
import com.poozh.translator.data.ModelProviders
import com.poozh.translator.data.SettingsSnapshot
import com.poozh.translator.ui.Md3
import com.poozh.translator.ui.Md3ButtonStyle
import com.poozh.translator.ui.Md3Motion
import com.poozh.translator.ui.Md3TextStyle

enum class AppTab {
    Console,
    Settings,
    History,
    Guide
}

class MainActivity : Activity() {
    private lateinit var settings: AppSettings
    private lateinit var apiKeyInput: EditText
    private lateinit var baseUrlInput: EditText
    private lateinit var modelInput: EditText
    private lateinit var wifiOnlyInput: Switch
    private lateinit var thinkingInput: Switch
    private lateinit var opacitySeekBar: SeekBar

    private lateinit var controlButton: TextView
    
    private lateinit var overlayDot: View
    private lateinit var overlayText: TextView
    private lateinit var overlayAction: View
    
    private lateinit var captureDot: View
    private lateinit var captureText: TextView
    private lateinit var captureAction: View
    
    private lateinit var serviceDot: View
    private lateinit var serviceText: TextView

    private lateinit var pageContainer: LinearLayout
    private lateinit var mainTitle: TextView
    private var drawerMenuLayout: LinearLayout? = null
    private var drawerRef: DrawerLayout? = null
    private var currentTab: AppTab = AppTab.Console

    private var selectedProviderId: String = ModelProviders.DEFAULT_PROVIDER_ID
    private var providerChipRow: LinearLayout? = null
    private var currentServiceLine: TextView? = null

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
        if (currentTab == AppTab.Console) {
            refreshStatus()
            // Returning from the screen-capture permission dialog, the service
            // receives ACTION_CAPTURE_RESULT and assigns mediaProjection
            // asynchronously — refresh again after it has had time to process,
            // so the "屏幕捕获就绪" indicator matches reality instead of
            // lagging one cycle behind.
            window.decorView.postDelayed({ if (currentTab == AppTab.Console) refreshStatus() }, 700)
        } else if (currentTab == AppTab.History) {
            switchTab(AppTab.History)
        }
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

    @Deprecated("Deprecated by Android framework")
    override fun onBackPressed() {
        val drawer = drawerRef
        if (drawer != null && drawer.isDrawerOpen(Gravity.LEFT)) {
            drawer.closeDrawer(Gravity.LEFT)
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
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

        // ── Main Content Area ──
        val main = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Md3.light.surface)
            setPadding(dp(screenMargin()), dp(20), dp(screenMargin()), dp(32))
        }

        main.addView(mainTopBar(drawer))

        val scrollRoot = ScrollView(this).apply {
            clipToPadding = false
            isFillViewport = true
        }
        
        pageContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scrollRoot.addView(pageContainer)
        main.addView(scrollRoot, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))

        // ── Left Navigation Drawer ──
        val drawerScroll = ScrollView(this).apply {
            clipToPadding = false
            isFillViewport = true
            
            // MD3 Nav Drawer has 16dp rounded corners on the right edge
            val radius = dp(16).toFloat()
            val bg = GradientDrawable().apply {
                setColor(Md3.light.surfaceContainerLow)
                cornerRadii = floatArrayOf(
                    0f, 0f,           // Top-Left
                    radius, radius,   // Top-Right
                    radius, radius,   // Bottom-Right
                    0f, 0f            // Bottom-Left
                )
            }
            background = bg
        }
        
        val drawerContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(28), dp(12), dp(32))
        }
        
        // Drawer Header
        drawerContent.addView(TextView(this).apply {
            text = "屏幕翻译"
            Md3.applyTextStyle(this, Md3TextStyle.HeadlineSmall, Md3.light.primary)
            setPadding(dp(12), 0, 0, dp(4))
        })
        drawerContent.addView(TextView(this).apply {
            text = "实时 OCR 选区翻译工具"
            Md3.applyTextStyle(this, Md3TextStyle.BodySmall, Md3.light.onSurfaceVariant)
            setPadding(dp(12), 0, 0, dp(24))
        })

        // Drawer Menu Items
        drawerMenuLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val tabActions = listOf(
            Triple(AppTab.Console, "\uD83C\uDFAE", "翻译控制台"),
            Triple(AppTab.Settings, "\u2699\uFE0F", "连接与配置"),
            Triple(AppTab.History, "\uD83D\uDCCB", "历史记录"),
            Triple(AppTab.Guide, "\uD83D\uDCD6", "使用指南")
        )

        for ((tab, icon, label) in tabActions) {
            drawerMenuLayout!!.addView(drawerMenuItem(tab, icon, label) {
                switchTab(tab)
            })
        }
        drawerContent.addView(drawerMenuLayout!!)

        drawerScroll.addView(drawerContent)
        
        // Add main content (must be first in DrawerLayout)
        val mainParams = DrawerLayout.LayoutParams(
            DrawerLayout.LayoutParams.MATCH_PARENT,
            DrawerLayout.LayoutParams.MATCH_PARENT
        )
        drawer.addView(main, mainParams)

        // Add drawer menu (must be after main content)
        val drawerWidth = (resources.displayMetrics.widthPixels * 0.82f).toInt()
        val drawerParams = DrawerLayout.LayoutParams(
            drawerWidth,
            DrawerLayout.LayoutParams.MATCH_PARENT
        ).apply { gravity = Gravity.LEFT }
        drawer.addView(drawerScroll, drawerParams)

        setContentView(drawer)
        drawerRef = drawer

        switchTab(AppTab.Console)
    }

    private fun mainTopBar(drawer: DrawerLayout): LinearLayout {
        val hamburger = TextView(this).apply {
            text = "☰"
            gravity = Gravity.CENTER
            minWidth = dp(48)
            minHeight = dp(48)
            isClickable = true
            isFocusable = true
            Md3.applyTextStyle(this, Md3TextStyle.TitleLarge, Md3.light.onSurfaceVariant)
            background = Md3.ripple(this@MainActivity, Color.TRANSPARENT, 999f)
            setOnClickListener {
                drawer.openDrawer(Gravity.LEFT)
            }
        }
        
        mainTitle = TextView(this).apply {
            text = "翻译控制台"
            // M3 Top App Bar: TitleLarge, onSurface color
            Md3.applyTextStyle(this, Md3TextStyle.TitleLarge, Md3.light.onSurface)
            setPadding(dp(4), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        
        // M3 Small Top App Bar: 64dp height, center-aligned vertically
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(64)
            setPadding(dp(4), 0, dp(4), 0)
            addView(hamburger)
            addView(mainTitle)
        }
    }

    private fun switchTab(tab: AppTab) {
        currentTab = tab
        drawerRef?.closeDrawer(Gravity.LEFT)
        
        mainTitle.text = when (tab) {
            AppTab.Console -> "翻译控制台"
            AppTab.Settings -> "连接与配置"
            AppTab.History -> "历史记录"
            AppTab.Guide -> "使用指南"
        }
        
        updateDrawerSelection()
        
        pageContainer.removeAllViews()
        when (tab) {
            AppTab.Console -> {
                pageContainer.addView(buildStatusCard())
                pageContainer.addView(buildConsoleCard(), blockParams(top = 16))
                refreshStatus()
            }
            AppTab.Settings -> {
                val snapshot = settings.load()
                pageContainer.addView(buildSettingsCard(snapshot))
            }
            AppTab.History -> {
                pageContainer.addView(buildHistoryPage())
            }
            AppTab.Guide -> {
                pageContainer.addView(buildGuideCard())
            }
        }
        
        Md3Motion.enter(pageContainer, dp(8).toFloat())
    }

    private fun drawerMenuItem(tab: AppTab, icon: String, label: String, action: () -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(56) // M3 navigation drawer: 56dp item height
            setPadding(dp(16), 0, dp(24), 0)
            tag = tab
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56)
            ).apply {
                bottomMargin = dp(2)
                topMargin = dp(2)
            }

            // M3 Leading icon in drawer item
            val iconView = TextView(this@MainActivity).apply {
                text = icon
                textSize = 20f
                gravity = Gravity.CENTER
                minWidth = dp(24)
                setPadding(0, 0, dp(12), 0)
            }
            addView(iconView)

            val labelView = TextView(this@MainActivity).apply {
                text = label
                tag = "label"
            }
            addView(labelView)

            setOnClickListener {
                action()
            }
        }
    }

    private fun updateDrawerSelection() {
        val menuLayout = drawerMenuLayout ?: return
        val activeTab = currentTab
        for (i in 0 until menuLayout.childCount) {
            val item = menuLayout.getChildAt(i) as? LinearLayout ?: continue
            val itemTab = item.tag as? AppTab ?: continue
            val selected = itemTab == activeTab
            // Apply style to the label child
            val labelView = item.findViewWithTag<TextView>("label")
            if (labelView != null) {
                Md3.applyTextStyle(
                    labelView,
                    Md3TextStyle.LabelLarge,
                    if (selected) Md3.light.onSecondaryContainer else Md3.light.onSurfaceVariant
                )
            }
            // M3 Nav Drawer: active indicator is pill-shaped secondaryContainer fill
            item.background = Md3.ripple(
                context = this,
                fillColor = if (selected) Md3.light.secondaryContainer else Color.TRANSPARENT,
                radiusDp = 999f, // Full pill shape per M3 spec
                rippleColor = Md3.withAlpha(Md3.light.primary, 0.12f)
            )
        }
    }

    private fun buildStatusCard(): LinearLayout {
        return card("服务状态").apply {
            val overlayRow = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(6), 0, dp(6))

                overlayDot = View(this@MainActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(8), dp(8)).apply { rightMargin = dp(12) }
                }
                addView(overlayDot)

                overlayText = TextView(this@MainActivity).apply {
                    Md3.applyTextStyle(this, Md3TextStyle.BodyMedium, Md3.light.onSurface)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                addView(overlayText)

                overlayAction = TextView(this@MainActivity).apply {
                    text = "去授权"
                    gravity = Gravity.CENTER
                    setPadding(dp(12), dp(4), dp(12), dp(4))
                    Md3.applyTextStyle(this, Md3TextStyle.LabelMedium, Md3.light.primary)
                    background = Md3.ripple(this@MainActivity, Color.TRANSPARENT, 8f, Md3.light.outline)
                    setOnClickListener {
                        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                    }
                }
                addView(overlayAction)
            }
            addView(overlayRow)

            val captureRow = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(6), 0, dp(6))

                captureDot = View(this@MainActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(8), dp(8)).apply { rightMargin = dp(12) }
                }
                addView(captureDot)

                captureText = TextView(this@MainActivity).apply {
                    Md3.applyTextStyle(this, Md3TextStyle.BodyMedium, Md3.light.onSurface)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                addView(captureText)

                captureAction = TextView(this@MainActivity).apply {
                    text = "去授权"
                    gravity = Gravity.CENTER
                    setPadding(dp(12), dp(4), dp(12), dp(4))
                    Md3.applyTextStyle(this, Md3TextStyle.LabelMedium, Md3.light.primary)
                    background = Md3.ripple(this@MainActivity, Color.TRANSPARENT, 8f, Md3.light.outline)
                    setOnClickListener { requestScreenCapture() }
                }
                addView(captureAction)
            }
            addView(captureRow)

            val serviceRow = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(6), 0, dp(6))

                serviceDot = View(this@MainActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(8), dp(8)).apply { rightMargin = dp(12) }
                }
                addView(serviceDot)

                serviceText = TextView(this@MainActivity).apply {
                    Md3.applyTextStyle(this, Md3TextStyle.BodyMedium, Md3.light.onSurface)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                addView(serviceText)
            }
            addView(serviceRow)
        }
    }

    private fun buildConsoleCard(): LinearLayout {
        return card("翻译控制台").apply {
            controlButton = actionButton("启动翻译服务", Md3ButtonStyle.Filled) {
                if (FloatingTranslatorService.isRunning) {
                    startTranslatorService(
                        Intent(this@MainActivity, FloatingTranslatorService::class.java)
                            .setAction(FloatingTranslatorService.ACTION_STOP)
                    )
                    Toast.makeText(this@MainActivity, "悬浮服务已停止", Toast.LENGTH_SHORT).show()
                } else {
                    if (!Settings.canDrawOverlays(this@MainActivity)) {
                        Toast.makeText(this@MainActivity, "请先授权悬浮窗", Toast.LENGTH_SHORT).show()
                    } else {
                        startTranslatorService(
                            Intent(this@MainActivity, FloatingTranslatorService::class.java)
                                .setAction(FloatingTranslatorService.ACTION_SHOW)
                        )
                        Toast.makeText(this@MainActivity, "悬浮窗已启动", Toast.LENGTH_SHORT).show()
                    }
                }
                controlButton.postDelayed({ refreshStatus() }, 180)
                    // Service start/stop is asynchronous — isRunning flips in
                    // onCreate/onDestroy, which may not have run yet at 180ms.
                    // Re-check after the service has had time to fully start or
                    // tear down so the indicator matches reality.
                    controlButton.postDelayed({ refreshStatus() }, 600)
            }
            addView(controlButton)
        }
    }

    private fun buildSettingsCard(snapshot: SettingsSnapshot): LinearLayout {
        return card("连接与配置").apply {
            currentServiceLine = TextView(this@MainActivity).apply {
                Md3.applyTextStyle(this, Md3TextStyle.TitleSmall, Md3.light.onSurface)
                setPadding(0, 0, 0, dp(8))
            }
            addView(currentServiceLine!!)
            addView(providerPresetScroller(snapshot))

            val preset = ModelProviders.byId(snapshot.providerId)
            apiKeyInput = inputRow(this, if (preset.requiresApiKey) "${preset.name} API Key" else "本地服务通常可留空", snapshot.apiKey, password = true)
            baseUrlInput = inputRow(this, "Chat Completions URL", snapshot.baseUrl, password = false)
            modelInput = inputRow(this, "模型名称", snapshot.model, password = false)

            addView(switchRow(snapshot))
            addView(thinkingSwitchRow(snapshot))
            opacitySeekBar = opacityRow(this, snapshot)

            val buttonsRow = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(12), 0, 0)

                val btnSave = actionButton("保存设置", Md3ButtonStyle.Filled) { saveSettings() }.apply {
                    layoutParams = LinearLayout.LayoutParams(0, dp(48), 1.2f).apply { rightMargin = dp(8) }
                }
                
                // M3 destructive action: Outlined red style matching error palette
                val btnClear = TextView(this@MainActivity).apply {
                    text = "清除 Key"
                    gravity = Gravity.CENTER
                    minHeight = dp(48)
                    Md3.applyTextStyle(this, Md3TextStyle.LabelLarge, Md3.light.error)
                    background = Md3.ripple(
                        context = this@MainActivity,
                        fillColor = Color.TRANSPARENT,
                        radiusDp = 999f,
                        strokeColor = Md3.light.error
                    )
                    setOnClickListener {
                        settings.clearApiKey()
                        apiKeyInput.setText("")
                        Toast.makeText(this@MainActivity, "API Key 已清除", Toast.LENGTH_SHORT).show()
                        refreshStatus()
                    }
                    Md3.bindStateLayer(this)
                    layoutParams = LinearLayout.LayoutParams(0, dp(48), 0.8f)
                }

                addView(btnSave)
                addView(btnClear)
            }
            addView(buttonsRow)
        }
    }

    private fun buildGuideCard(): LinearLayout {
        return card("使用指南").apply {
            addView(listLine("1. 基础授权", "请先通过上方服务状态中的“去授权”开启相关权限。"))
            addView(listLine("2. 启动服务", "点击翻译控制台中的“启动翻译服务”，即可显示桌面悬浮球。"))
            addView(listLine("3. 选区划范围", "点击悬浮球，拖动框选需要识别的区域。"))
            addView(listLine("4. 识别与翻译", "选区完成后，点击悬浮窗“刷新”即可进行实时翻译。"))
        }
    }

    private fun buildHistoryPage(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            
            val historyList = HistoryManager.getHistory(this@MainActivity)
            if (historyList.isNotEmpty()) {
                val clearBtn = actionButton("清空历史记录", Md3ButtonStyle.Outlined) {
                    HistoryManager.clearHistory(this@MainActivity)
                    Toast.makeText(this@MainActivity, "历史记录已清空", Toast.LENGTH_SHORT).show()
                    switchTab(AppTab.History)
                }.apply {
                    setTextColor(Md3.light.error)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(44)
                    ).apply { bottomMargin = dp(16) }
                }
                addView(clearBtn)
            }

            if (historyList.isEmpty()) {
                addView(TextView(this@MainActivity).apply {
                    text = "暂无历史记录\n在悬浮球中成功翻译的内容会自动保存在这里。"
                    gravity = Gravity.CENTER
                    Md3.applyTextStyle(this, Md3TextStyle.BodyMedium, Md3.light.onSurfaceVariant)
                    setPadding(0, dp(64), 0, dp(64))
                })
            } else {
                for (item in historyList) {
                    addView(buildHistoryCard(item), blockParams(top = 8))
                }
            }
        }
    }

    private fun buildHistoryCard(item: HistoryItem): LinearLayout {
        val dateFormat = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
        val dateStr = dateFormat.format(java.util.Date(item.timestamp))
        val providerName = ModelProviders.byId(item.providerId).name

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = Md3.surface(
                context = this@MainActivity,
                color = Md3.light.surfaceContainerLow,
                radiusDp = 12f,
                strokeColor = Md3.light.outlineVariant
            )

            val headerRow = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                
                addView(TextView(this@MainActivity).apply {
                    text = providerName
                    setPadding(dp(8), dp(2), dp(8), dp(2))
                    Md3.applyTextStyle(this, Md3TextStyle.LabelMedium, Md3.light.onSecondaryContainer)
                    background = Md3.surface(this@MainActivity, Md3.light.secondaryContainer, 4f)
                })

                addView(TextView(this@MainActivity).apply {
                    text = "  $dateStr"
                    Md3.applyTextStyle(this, Md3TextStyle.BodySmall, Md3.light.onSurfaceVariant)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })

                addView(TextView(this@MainActivity).apply {
                    text = "删除"
                    setPadding(dp(8), dp(4), dp(8), dp(4))
                    Md3.applyTextStyle(this, Md3TextStyle.LabelMedium, Md3.light.error)
                    background = Md3.ripple(this@MainActivity, Color.TRANSPARENT, 4f)
                    setOnClickListener {
                        HistoryManager.deleteHistoryItem(this@MainActivity, item.id)
                        Toast.makeText(this@MainActivity, "已删除历史项", Toast.LENGTH_SHORT).show()
                        switchTab(AppTab.History)
                    }
                })
            }
            addView(headerRow)

            addView(TextView(this@MainActivity).apply {
                text = item.originalText
                Md3.applyTextStyle(this, Md3TextStyle.BodyMedium, Md3.light.onSurfaceVariant)
                setPadding(0, dp(8), 0, dp(4))
            })

            addView(View(this@MainActivity).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply {
                    topMargin = dp(6)
                    bottomMargin = dp(6)
                }
                setBackgroundColor(Md3.light.outlineVariant)
            })

            addView(TextView(this@MainActivity).apply {
                text = item.translatedText
                Md3.applyTextStyle(this, Md3TextStyle.BodyLarge, Md3.light.onSurface)
                setPadding(0, dp(4), 0, dp(8))
            })

            addView(TextView(this@MainActivity).apply {
                text = "复制译文"
                gravity = Gravity.CENTER
                minHeight = dp(36)
                Md3.applyTextStyle(this, Md3TextStyle.LabelMedium, Md3.light.primary)
                background = Md3.ripple(
                    context = this@MainActivity,
                    fillColor = Md3.light.surfaceContainerHigh,
                    radiusDp = 8f
                )
                setOnClickListener {
                    val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("translation", item.translatedText))
                    Toast.makeText(this@MainActivity, "译文已复制到剪贴板", Toast.LENGTH_SHORT).show()
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(36)
                )
            })
        }
    }

    private fun saveSettings() {
        val current = settings.load()
        val providerId = selectedProviderId
        val opacityProgress = opacitySeekBar.progress
        val opacityValue = 0.3f + (opacityProgress / 100f) * 0.7f

        settings.save(
            providerId = providerId,
            baseUrl = baseUrlInput.text.toString().trim(),
            model = modelInput.text.toString().trim(),
            intervalMs = current.intervalMs,
            wifiOnly = wifiOnlyInput.isChecked,
            thinkingEnabled = thinkingInput.isChecked
        )
        settings.saveApiKey(providerId, apiKeyInput.text.toString().trim())
        settings.overlayOpacity = opacityValue

        Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show()
        refreshStatus()
        startTranslatorService(
            Intent(this, FloatingTranslatorService::class.java)
                .setAction(FloatingTranslatorService.ACTION_REFRESH_SETTINGS)
        )
    }

    private fun refreshStatus() {
        val snapshot = settings.load()
        selectedProviderId = snapshot.providerId

        currentServiceLine?.text = "当前服务：${ModelProviders.byId(snapshot.providerId).name}"

        if (::overlayDot.isInitialized) {
            val hasOverlay = Settings.canDrawOverlays(this)
            
            // Clean M3 color mapping: Success uses primary teal, failure uses error red
            overlayDot.background = Md3.surface(
                context = this,
                color = if (hasOverlay) Md3.light.primary else Md3.light.error,
                radiusDp = 999f
            )
            overlayText.text = if (hasOverlay) "悬浮窗权限：已授权" else "悬浮窗权限：未授权"
            overlayAction.visibility = if (hasOverlay) View.GONE else View.VISIBLE

            val hasCapture = FloatingTranslatorService.isCaptureActive
            captureDot.background = Md3.surface(
                context = this,
                color = if (hasCapture) Md3.light.primary else Md3.light.error,
                radiusDp = 999f
            )
            captureText.text = if (hasCapture) "屏幕截图捕获：已就绪" else "屏幕截图捕获：未授权"
            captureAction.visibility = if (hasCapture) View.GONE else View.VISIBLE

            val running = FloatingTranslatorService.isRunning
            serviceDot.background = Md3.surface(
                context = this,
                color = if (running) Md3.light.primary else Color.rgb(180, 180, 180),
                radiusDp = 999f
            )
            serviceText.text = if (running) "服务运行状态：正在运行" else "服务运行状态：已停止"

            if (running) {
                controlButton.text = "停止翻译服务"
                controlButton.background = Md3.buttonBackground(this, Md3ButtonStyle.Outlined)
                controlButton.setTextColor(Md3.light.error)
            } else {
                controlButton.text = "启动翻译服务"
                controlButton.background = Md3.buttonBackground(this, Md3ButtonStyle.Filled)
                controlButton.setTextColor(Md3.light.onPrimary)
            }
        }
    }

    private fun requestScreenCapture() {
        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
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
            setPadding(dp(16), dp(16), dp(16), dp(16))
            // M3 Outlined Card: medium (12dp) corner radius, outlineVariant border
            background = Md3.surface(
                context = this@MainActivity,
                color = Md3.light.surfaceContainerLow,
                radiusDp = 12f,
                strokeColor = Md3.light.outlineVariant
            )
            addView(TextView(this@MainActivity).apply {
                text = title
                // M3: card titles use onSurface, not primary
                Md3.applyTextStyle(this, Md3TextStyle.TitleMedium, Md3.light.onSurface)
                setPadding(0, 0, 0, dp(12))
            })
        }
    }

    private fun listLine(label: String, value: String): TextView {
        return TextView(this).apply {
            text = "$label：$value"
            Md3.applyTextStyle(this, Md3TextStyle.BodyMedium, Md3.light.onSurfaceVariant)
            setPadding(0, dp(4), 0, dp(4))
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
        updateProviderChipStyles()
        return HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            isFillViewport = true
            addView(row)
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
                radiusDp = 999f,
                strokeColor = if (selected) null else Md3.light.outline
            )
        }
        currentServiceLine?.text = "当前服务：${ModelProviders.byId(selectedId).name}"
    }

    private fun applyProviderPreset(preset: ModelProviderPreset) {
        selectedProviderId = preset.id
        settings.selectProvider(preset.id)
        val savedKey = settings.getApiKey(preset.id)
        val savedModel = settings.getModel(preset.id).ifBlank { preset.defaultModel }
        baseUrlInput.setText(preset.baseUrl)
        modelInput.setText(savedModel)
        apiKeyInput.setText(savedKey)
        apiKeyInput.hint = if (preset.requiresApiKey) "${preset.name} API Key" else "本地服务通常可留空"
        updateProviderChipStyles()
        Toast.makeText(this, "已切换到 ${preset.name}", Toast.LENGTH_SHORT).show()
        refreshStatus()
    }

    private fun inputRow(
        parent: ViewGroup,
        hint: String,
        value: String,
        password: Boolean = false
    ): EditText {
        val edit = EditText(this).apply {
            this.hint = hint
            setText(value)
            Md3.applyTextStyle(this, Md3TextStyle.BodyLarge, Md3.light.onSurface)
            setHintTextColor(Md3.light.onSurfaceVariant)
            setSingleLine(true)
            background = null
            inputType = if (password) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL
            }
        }

        // Compliant MD3 Outlined Text Field container layout
        val container = FrameLayout(this).apply {
            background = Md3.surface(
                context = this@MainActivity,
                color = Color.TRANSPARENT, // Clean transparent background
                radiusDp = 8f,
                strokeColor = Md3.light.outline, // Inactive outline state
                strokeWidthDp = 1f
            )

            val editParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).apply {
                leftMargin = dp(16)
                rightMargin = if (password) dp(56) else dp(16)
            }
            addView(edit, editParams)

            if (password) {
                val toggle = TextView(this@MainActivity).apply {
                    text = "显示"
                    gravity = Gravity.CENTER
                    setPadding(dp(12), 0, dp(12), 0)
                    Md3.applyTextStyle(this, Md3TextStyle.LabelLarge, Md3.light.primary)
                    isClickable = true
                    isFocusable = true
                    background = Md3.ripple(this@MainActivity, Color.TRANSPARENT, 4f)
                    setOnClickListener {
                        val isPass = edit.inputType == (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
                        if (isPass) {
                            edit.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                            text = "隐藏"
                        } else {
                            edit.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                            text = "显示"
                        }
                        edit.setSelection(edit.text.length)
                    }
                }
                val toggleParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                ).apply { gravity = Gravity.END }
                addView(toggle, toggleParams)
            }

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56)
            ).apply { bottomMargin = dp(12) }
        }

        // Active/inactive state outline transition animation matching MD3 specs
        edit.setOnFocusChangeListener { _, hasFocus ->
            container.background = if (hasFocus) {
                Md3.surface(
                    context = this@MainActivity,
                    color = Color.TRANSPARENT,
                    radiusDp = 8f,
                    strokeColor = Md3.light.primary, // Focused active stroke (2dp thick primary color)
                    strokeWidthDp = 2f
                )
            } else {
                Md3.surface(
                    context = this@MainActivity,
                    color = Color.TRANSPARENT,
                    radiusDp = 8f,
                    strokeColor = Md3.light.outline, // Unfocused standard stroke (1dp thick outline color)
                    strokeWidthDp = 1f
                )
            }
        }

        parent.addView(container)
        return edit
    }

    private fun switchRow(snapshot: SettingsSnapshot): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, dp(8))
            addView(TextView(this@MainActivity).apply {
                text = "仅 Wi-Fi 下使用"
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

    /** Toggle for the reasoning model's thinking phase. Off = faster, no
     *  chain-of-thought; On = better grammar analysis but slower. */
    private fun thinkingSwitchRow(snapshot: SettingsSnapshot): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, dp(8))
            addView(TextView(this@MainActivity).apply {
                text = "深度思考"
                Md3.applyTextStyle(this, Md3TextStyle.BodyLarge, Md3.light.onSurface)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            thinkingInput = Switch(this@MainActivity).apply {
                isChecked = snapshot.thinkingEnabled
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    thumbTintList = ColorStateList.valueOf(Md3.light.primary)
                    trackTintList = ColorStateList.valueOf(Md3.withAlpha(Md3.light.primary, 0.32f))
                }
            }
            addView(thinkingInput)
        }
    }

    private fun opacityRow(parent: ViewGroup, snapshot: SettingsSnapshot): SeekBar {
        val opacity = settings.overlayOpacity
        val label = TextView(this).apply {
            text = "悬浮窗透明度 ${(opacity * 100).toInt()}%"
            Md3.applyTextStyle(this, Md3TextStyle.BodyMedium, Md3.light.onSurfaceVariant)
            setPadding(0, dp(4), 0, dp(4))
        }
        val seek = SeekBar(this).apply {
            val pct = ((opacity - 0.3f) / 0.7f * 100f).toInt().coerceIn(0, 100)
            progress = pct
            
            // Set M3 progress track and thumb colors
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                progressTintList = ColorStateList.valueOf(Md3.light.primary)
                thumbTintList = ColorStateList.valueOf(Md3.light.primary)
                progressBackgroundTintList = ColorStateList.valueOf(Md3.light.outlineVariant)
            }
            
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val value = 0.3f + (progress / 100f) * 0.7f
                    label.text = "悬浮窗透明度 ${(value * 100).toInt()}%"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(8))
            addView(label)
            addView(seek)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        parent.addView(container)
        return seek
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
