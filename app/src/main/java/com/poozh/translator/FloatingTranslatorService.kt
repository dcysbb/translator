package com.poozh.translator

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Outline
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.poozh.translator.capture.ScreenCaptureController
import com.poozh.translator.data.AppSettings
import com.poozh.translator.data.DeepSeekClient
import com.poozh.translator.data.FavoritesManager
import com.poozh.translator.data.ModelProviders
import com.poozh.translator.data.SettingsSnapshot
import com.poozh.translator.data.HistoryManager
import com.poozh.translator.model.AnalysisProgress
import com.poozh.translator.model.AnalysisResult
import com.poozh.translator.model.LanguageDetector
import com.poozh.translator.model.RefreshAction
import com.poozh.translator.model.TermNote
import com.poozh.translator.model.TranslationRefreshPolicy
import com.poozh.translator.model.TranslationUiState
import com.poozh.translator.ocr.ScreenTextRecognizer
import com.poozh.translator.ui.Md3
import com.poozh.translator.ui.Md3Motion
import com.poozh.translator.ui.Md3TextStyle
import com.poozh.translator.ui.SelectionOverlayView
import java.lang.ref.WeakReference
import kotlin.math.max
import kotlin.math.min

class FloatingTranslatorService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var settings: AppSettings
    private val recognizer = ScreenTextRecognizer()
    private val deepSeekClient = DeepSeekClient()

    private var bubbleView: ImageView? = null
    private var panelView: LinearLayout? = null
    private var contentHost: LinearLayout? = null
    private var selectionOverlay: View? = null
    private var readingText: TextView? = null
    private var statusText: TextView? = null
    private var titleText: TextView? = null

    private var mediaProjection: MediaProjection? = null
    private var captureController: ScreenCaptureController? = null
    private var selectionRect: android.graphics.Rect? = null
    private var ocrBusy = false
    private var translating = false
    /** Re-capture attempts made when OCR returned a blank frame (transient
     *  frame from mid-transition). Reset on a non-blank result or failure. */
    private var blankFrameRetries = 0
    private var lastStableText = ""
    private var lastResult: AnalysisResult? = null
    private var currentTranslation: DeepSeekClient.TranslationHandle? = null
    private var translationUiState: TranslationUiState = TranslationUiState.Idle
    private var activeTranslationRequestId = 0L
    private var bubbleStateAnimator: AnimatorSet? = null
    private var bubbleFeedbackState: BubbleFeedbackState? = null
    /** Last progress snapshot shown, used to throttle UI updates (~80ms). */
    private var lastPartialShownAt = 0L
    private var lastProgressShown: AnalysisProgress? = null
    private var hiddenOverlayState: HiddenOverlayState? = null

    private data class HiddenOverlayState(
        val bubbleVisibility: Int?,
        val panelVisibility: Int?,
        val selectionVisibility: Int?
    )

    private enum class BubbleFeedbackState { IDLE, IN_PROGRESS, COMPLETE, FAILED }

    override fun onCreate() {
        super.onCreate()
        instanceRef = WeakReference(this)
        isRunning = true
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        settings = AppSettings(this)
        ensureForeground()
        if (Settings.canDrawOverlays(this)) addBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureForeground()
        when (intent?.action) {
            ACTION_SHOW, null -> if (Settings.canDrawOverlays(this)) addBubble()
            ACTION_CAPTURE_RESULT -> handleCaptureResult(intent)
            ACTION_REFRESH_SETTINGS -> {
                // Settings changed in the UI — force the next refresh to re-read
                // the latest snapshot (cheap; the client already reads on demand).
                lastStableText = ""
            }
            ACTION_STOP -> stopSelf()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        instanceRef.clear()
        isRunning = false
        activeTranslationRequestId++
        bubbleStateAnimator?.cancel()
        currentTranslation?.cancel()
        currentTranslation = null
        captureController?.release()
        captureController = null
        recognizer.close()
        removeOverlay(panelView)
        removeOverlay(bubbleView)
        removeOverlay(selectionOverlay)
        super.onDestroy()
    }

    private fun handleCaptureResult(intent: Intent) {
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
        val data: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
        }
        if (resultCode == 0 || data == null) {
            showStatus("屏幕捕获授权无效")
            return
        }

        captureController?.release()
        captureController = null
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        // getMediaProjection can throw on Android 14+ when the projection token
        // has expired or the FGS type isn't allowed. Catch + null-check so a
        // denied/expired projection surfaces a friendly status instead of a crash.
        val projection = try {
            manager.getMediaProjection(resultCode, data)
        } catch (e: Throwable) {
            android.util.Log.e("FloatingTranslatorService", "getMediaProjection failed", e)
            showStatus("屏幕捕获授权已失效，请重新授权")
            return
        }
        if (projection == null) {
            showStatus("无法获取屏幕捕获授权，请重试")
            return
        }
        mediaProjection = projection
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                startForeground(
                    NOTIFICATION_ID,
                    buildNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                )
            } catch (e: Throwable) {
                android.util.Log.e("FloatingTranslatorService", "Failed to promote FGS to mediaProjection", e)
            }
        }
        showStatus("屏幕捕获已就绪")
        if (selectionRect == null) showSelectionOverlay()
    }

    private fun addBubble() {
        if (bubbleView != null) return
        val bubble = ImageView(this).apply {
            setImageResource(R.drawable.translator_icon_source)
            scaleType = ImageView.ScaleType.CENTER_CROP
            setPadding(0, 0, 0, 0)
            background = Md3.ripple(
                context = this@FloatingTranslatorService,
                fillColor = Md3.light.surfaceContainerLowest,
                radiusDp = 999f,
                strokeColor = Md3.light.outlineVariant,
                rippleColor = Md3.withAlpha(Md3.light.primary, 0.14f)
            )
            elevation = dp(6).toFloat()
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
            clipToOutline = true
            contentDescription = "屏幕翻译"
        }
        val defaultBubbleX = resources.displayMetrics.widthPixels - dp(82)
        val defaultBubbleY = dp(112)
        val (savedX, savedY) = settings.loadBubblePosition(defaultBubbleX, defaultBubbleY)
        val params = WindowManager.LayoutParams(
            dp(56),
            dp(56),
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = clampWindowX(savedX, dp(56))
            y = clampWindowY(savedY, dp(56))
        }
        attachDragAndClick(
            view = bubble,
            dragTarget = bubble,
            params = params,
            onClick = { togglePanel() },
            onDragFinished = { settings.saveBubblePosition(it.x, it.y) }
        )
        windowManager.addView(bubble, params)
        bubbleView = bubble
        Md3Motion.enter(bubble, dp(8).toFloat(), endAlpha = settings.overlayOpacity)
        applyOverlayOpacity()
        updateBubbleFeedback()
    }

    private fun togglePanel() {
        if (panelView == null) showPanel() else collapsePanel()
    }

    private fun collapsePanel() {
        val panel = panelView ?: return
        panelView = null
        contentHost = null
        readingText = null
        statusText = null
        titleText = null
        // Shrink the panel back into the bubble (non-linear), then remove it.
        val params = panel.layoutParams as? WindowManager.LayoutParams
        val (pivotX, pivotY) = if (params != null) bubblePivotRelativeToPanel(params) else 0f to 0f
        Md3Motion.scaleOutTo(panel, pivotX, pivotY) {
            // Hide before removal: removing a window can trigger one last
            // full-size draw of the view (its scale/alpha reset), which shows
            // up as a "flash" the size of the expanded panel. INVISIBLE first
            // guarantees nothing is drawn on top while the window tears down.
            panel.visibility = View.INVISIBLE
            panel.alpha = 0f
            removeOverlay(panel)
        }
        setBubbleDocked(false)
        updateBubbleFeedback()
    }

    private fun showPanel() {
        if (panelView != null) return
        val displayWidth = resources.displayMetrics.widthPixels
        val displayHeight = resources.displayMetrics.heightPixels
        val defaultWidth = min(dp(360), displayWidth - dp(24)).coerceAtLeast(minPanelWidth())
        val defaultHeight = min(dp(560), displayHeight - dp(88)).coerceAtLeast(minPanelHeight())
        val saved = settings.loadPanelState(dp(56), dp(86), defaultWidth, defaultHeight)
        val panelWidth = clampPanelWidth(saved.width)
        val panelHeight = clampPanelHeight(saved.height)
        val params = WindowManager.LayoutParams(
            panelWidth,
            panelHeight,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = clampWindowX(saved.x, panelWidth)
            y = clampWindowY(saved.y, panelHeight)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(12))
            background = Md3.surface(
                context = this@FloatingTranslatorService,
                color = Md3.dark.surfaceContainerHigh,
                radiusDp = 28f
            )
            elevation = dp(6).toFloat()
            clipToOutline = true
        }

        val header = buildPanelHeader(root, params)
        contentHost = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                topMargin = dp(10)
                bottomMargin = dp(10)
            }
        }
        val toolbar = buildToolbar(params)

        root.addView(header)
        root.addView(contentHost)
        root.addView(toolbar)

        windowManager.addView(root, params)
        panelView = root
        setBubbleDocked(true)
        // Non-linear scale-in from the bubble's position — the panel grows out
        // of the bubble instead of a plain fade. Apply opacity as the end alpha
        // so it composes with the user's transparency setting.
        val (pivotX, pivotY) = bubblePivotRelativeToPanel(params)
        Md3Motion.scaleInFrom(root, pivotX, pivotY, endAlpha = settings.overlayOpacity)
        showReadingPage()
        showStatus(translationUiState.statusText())
    }

    /**
     * Bubble center in panel-local coordinates (so it can be used as a scale
     * pivot on the panel view). Falls back to the panel's top-start corner if
     * the bubble isn't mounted.
     */
    private fun bubblePivotRelativeToPanel(panelParams: WindowManager.LayoutParams): Pair<Float, Float> {
        val bubble = bubbleView ?: return 0f to 0f
        val bubbleLoc = IntArray(2)
        bubble.getLocationOnScreen(bubbleLoc)
        val bubbleCenterX = bubbleLoc[0] + bubble.width / 2f
        val bubbleCenterY = bubbleLoc[1] + bubble.height / 2f
        // panel top-left in screen coords is (panelParams.x, panelParams.y).
        val localX = bubbleCenterX - panelParams.x
        val localY = bubbleCenterY - panelParams.y
        return localX to localY
    }

    private fun buildPanelHeader(root: View, params: WindowManager.LayoutParams): LinearLayout {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(2), 0, dp(2), 0)
        }
        titleText = TextView(this).apply {
            text = "阅读翻译"
            Md3.applyTextStyle(this, Md3TextStyle.TitleMedium, Md3.dark.onSurface)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        statusText = TextView(this).apply {
            text = "等待刷新"
            Md3.applyTextStyle(this, Md3TextStyle.LabelMedium, Md3.dark.onSecondaryContainer)
            setPadding(dp(11), dp(6), dp(11), dp(6))
            background = Md3.ripple(this@FloatingTranslatorService, Md3.dark.secondaryContainer, 999f)
            clipToOutline = true
        }
        val close = textButton("×", compact = true) { collapsePanel() }

        header.addView(titleText)
        header.addView(statusText)
        header.addView(close)
        attachDragAndClick(
            view = header,
            dragTarget = root,
            params = params,
            onClick = null,
            onDragFinished = ::savePanelWindowState
        )
        return header
    }

    private fun buildToolbar(params: WindowManager.LayoutParams): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            addView(toolbarButton("刷新", primary = true) { refreshOnce() })
            // Re-translate the last OCR text — handy when a translation fails
            // (timeout/server error) so the user can retry in one tap without
            // re-capturing the screen.
            addView(toolbarButton("重译") { retranslateLastText() })
            addView(toolbarButton("选区") { showSelectionOverlay() })
            addView(toolbarButton("更多") { showMorePage() })
            addView(toolbarButton("收起") { collapsePanel() })
            addView(resizeHandle(params))
        }
    }

    private fun showReadingPage() {
        titleText?.text = "阅读翻译"
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            clipToPadding = false
        }
        readingText = TextView(this).apply {
            Md3.applyTextStyle(this, Md3TextStyle.BodyLarge, Md3.dark.onSurface)
            setLineSpacing(dp(5).toFloat(), 1.0f)
            setPadding(dp(4), dp(3), dp(4), dp(22))
        }
        val tv = readingText!!
        when (val state = translationUiState) {
            is TranslationUiState.Complete -> {
                tv.text = buildSpannableReadingText(state.result)
                if (state.result.words.isNotEmpty()) attachWordTapDetector(tv)
            }
            is TranslationUiState.InProgress -> {
                tv.text = buildSpannableProgressText(
                    AnalysisProgress(
                        sourceText = state.sourceText,
                        translation = state.partialTranslation,
                        words = state.words,
                        grammar = state.grammar,
                        isThinking = state.isThinking
                    )
                )
                if (state.words.isNotEmpty()) attachWordTapDetector(tv)
            }
            is TranslationUiState.Interrupted -> {
                tv.text = SpannableStringBuilder(buildSpannableProgressText(state.progress))
                    .append("\n\n⚠ ${state.message}")
                if (state.progress.words.isNotEmpty()) attachWordTapDetector(tv)
            }
            else -> tv.text = currentReadingText()
        }
        scroll.addView(readingText)
        swapContent(scroll)
    }

    /**
     * Build a SpannableStringBuilder for the reading page. Section headers get
     * bold style, and each word's surface text is wrapped in a ClickableSpan
     * whose word surfaces open the in-overlay detail page for favourites.
     */
    private fun buildSpannableReadingText(result: AnalysisResult): CharSequence {
        return buildSpannableAnalysisText(
            sourceText = result.sourceText,
            translation = result.translation,
            words = result.words,
            grammar = result.grammar
        )
    }

    private fun buildSpannableProgressText(progress: AnalysisProgress): CharSequence {
        val content = buildSpannableAnalysisText(
            sourceText = progress.sourceText,
            translation = progress.translation,
            words = progress.words,
            grammar = progress.grammar
        )
        if (progress.translation.isBlank() && progress.words.isEmpty() && progress.grammar.isEmpty()) {
            return SpannableStringBuilder().apply {
                append("原文\n").append(progress.sourceText)
                append(if (progress.isThinking) "\n\n模型正在思考…" else "\n\n正在连接模型服务…")
            }
        }
        val footer = if (progress.grammar.isNotEmpty()) "\n正在收尾解析…" else "\n正在解析详细内容…"
        return SpannableStringBuilder(content).append(footer)
    }

    private fun buildSpannableAnalysisText(
        sourceText: String,
        translation: String,
        words: List<TermNote>,
        grammar: List<String>
    ): CharSequence {
        val sb = SpannableStringBuilder()
        // 原文
        sb.appendBoldLine("━━ 原文 ━━")
        sb.append(sourceText.ifBlank { "未识别到文本" }).append("\n\n")
        // 译文
        sb.appendBoldLine("━━ 译文 ━━")
        sb.append(translation.ifBlank { "暂无翻译" })
        // 单词释义
        if (words.isNotEmpty()) {
            sb.append("\n\n")
            sb.appendBoldLine("━━ 单词释义 ━━")
            for (w in words) {
                sb.append("• ")
                // The clickable surface
                val surfaceStart = sb.length
                sb.append(w.surface)
                sb.setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        showWordDetailsPage(w)
                    }
                    override fun updateDrawState(tp: TextPaint) {
                        tp.isUnderlineText = false
                        tp.color = Md3.dark.primary
                    }
                }, surfaceStart, sb.length, 0)
                // Reading + meaning + note
                w.reading.takeIf { it.isNotBlank() }?.let { sb.append("【$it】") }
                sb.append("　")
                w.meaning.takeIf { it.isNotBlank() }?.let { sb.append(it) }
                w.note.takeIf { it.isNotBlank() }?.let { sb.append("（$it）") }
                sb.append("\n")
            }
        }
        // 语法解析
        if (grammar.isNotEmpty()) {
            sb.append("\n")
            sb.appendBoldLine("━━ 语法解析 ━━")
            for (g in grammar) sb.append("• $g\n")
        }
        return sb.trim()
    }

    private fun SpannableStringBuilder.appendBoldLine(text: String) {
        val start = this.length
        this.append(text).append("\n")
        this.setSpan(StyleSpan(android.graphics.Typeface.BOLD), start, this.length - 1, 0)
    }

    /**
     * Detect taps on ClickableSpan word entries inside the TextView without
     * using LinkMovementMethod (which conflicts with ScrollView scrolling).
     * Uses ACTION_UP + touch-slop check so a scroll/drag doesn't trigger a
     * word tap.
     */
    private fun attachWordTapDetector(tv: TextView) {
        val slop = android.view.ViewConfiguration.get(tv.context).scaledTouchSlop
        var downX = 0f
        var downY = 0f
        var isPotentialTap = false
        tv.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX; downY = event.rawY; isPotentialTap = true
                    // Claim the gesture so we get UP; ScrollView won't scroll
                    // unless we release on MOVE.
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                    true
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    if (isPotentialTap &&
                        (Math.abs(event.rawX - downX) > slop || Math.abs(event.rawY - downY) > slop)) {
                        isPotentialTap = false
                        // Release so the parent ScrollView can scroll.
                        v.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                    true
                }
                android.view.MotionEvent.ACTION_UP -> {
                    val wasTap = isPotentialTap
                    isPotentialTap = false
                    v.parent?.requestDisallowInterceptTouchEvent(false)
                    if (!wasTap) return@setOnTouchListener true
                    // Find the character offset at the touch point.
                    val layout = tv.layout ?: return@setOnTouchListener true
                    val x = event.x.toInt() - tv.totalPaddingLeft + tv.scrollX
                    val y = event.y.toInt() - tv.totalPaddingTop + tv.scrollY
                    val line = layout.getLineForVertical(y)
                    if (line < 0 || line > layout.lineCount - 1) return@setOnTouchListener true
                    val off = layout.getOffsetForHorizontal(line, x.toFloat())
                    val spanned = tv.text as? android.text.Spanned ?: return@setOnTouchListener true
                    if (off < 0 || off >= spanned.length) return@setOnTouchListener true
                    val spans = spanned.getSpans(off, (off + 1).coerceAtMost(spanned.length), ClickableSpan::class.java)
                    if (spans.isNotEmpty()) {
                        spans[0].onClick(v)
                    }
                    true
                }
                android.view.MotionEvent.ACTION_CANCEL -> {
                    isPotentialTap = false
                    v.parent?.requestDisallowInterceptTouchEvent(false)
                    true
                }
                else -> false
            }
        }
    }

    /** Show word details inside the existing overlay window. */
    private fun showWordDetailsPage(word: TermNote) {
        readingText = null
        titleText?.text = "单词详情"
        val scroll = ScrollView(this).apply { clipToPadding = false }
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(4), dp(4), dp(14))
        }
        page.addView(TextView(this).apply {
            text = word.surface
            Md3.applyTextStyle(this, Md3TextStyle.HeadlineSmall, Md3.dark.primary)
            setPadding(dp(13), dp(13), dp(13), dp(5))
        })
        word.reading.takeIf { it.isNotBlank() }?.let {
            page.addView(statusLine("读音", it))
        }
        word.meaning.takeIf { it.isNotBlank() }?.let {
            page.addView(statusLine("释义", it))
        }
        word.note.takeIf { it.isNotBlank() }?.let {
            page.addView(statusLine("备注", it))
        }
        val alreadyFav = FavoritesManager.isFavorite(this, word.surface)
        page.addView(menuAction(if (alreadyFav) "取消收藏" else "收藏") {
            if (alreadyFav) {
                FavoritesManager.removeFavoriteByWord(this, word.surface)
                Toast.makeText(this, "已取消收藏", Toast.LENGTH_SHORT).show()
            } else {
                val added = FavoritesManager.addFavorite(
                    context = this,
                    word = word.surface,
                    reading = word.reading,
                    meaning = word.meaning,
                    note = word.note,
                    sourceContext = lastStableText.take(200)
                )
                Toast.makeText(this, if (added) "已加入收藏" else "已在收藏中", Toast.LENGTH_SHORT).show()
            }
            showWordDetailsPage(word)
        })
        page.addView(menuAction("返回阅读") { showReadingPage() })
        scroll.addView(page)
        swapContent(scroll)
        showStatus(if (alreadyFav) "已收藏" else "单词详情")
    }

    private fun showFavoritesPage() {
        readingText = null
        titleText?.text = "收藏夹"
        val scroll = ScrollView(this).apply { clipToPadding = false }
        val menu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(12))
        }
        val favorites = FavoritesManager.getFavorites(this)
        if (favorites.isEmpty()) {
            menu.addView(TextView(this).apply {
                text = "暂无收藏"
                Md3.applyTextStyle(this, Md3TextStyle.BodyMedium, Md3.dark.onSurfaceVariant)
                setPadding(dp(16), dp(24), dp(16), dp(24))
            })
        } else {
            favorites.forEach { item ->
                val row = TextView(this).apply {
                    val parts = listOf(
                        item.reading.takeIf { it.isNotBlank() }?.let { "【$it】" },
                        item.meaning.takeIf { it.isNotBlank() }
                    ).filterNotNull().joinToString(" ")
                    text = "• ${item.word}  $parts"
                    Md3.applyTextStyle(this, Md3TextStyle.BodyMedium, Md3.dark.onSurface)
                    setPadding(dp(16), dp(10), dp(16), dp(10))
                    background = Md3.ripple(this@FloatingTranslatorService, Md3.dark.surfaceContainer, 8f)
                    clipToOutline = true
                    setOnClickListener {
                        FavoritesManager.removeFavorite(this@FloatingTranslatorService, item.id)
                        Toast.makeText(this@FloatingTranslatorService, "已移除「${item.word}」", Toast.LENGTH_SHORT).show()
                        showFavoritesPage() // refresh
                    }
                    Md3.bindStateLayer(this)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = dp(4) }
                }
                menu.addView(row)
            }
            menu.addView(menuAction("清空收藏") {
                FavoritesManager.clearFavorites(this)
                Toast.makeText(this, "收藏已清空", Toast.LENGTH_SHORT).show()
                showFavoritesPage()
            })
        }
        menu.addView(menuAction("返回阅读") { showReadingPage() })
        scroll.addView(menu)
        swapContent(scroll)
        showStatus("收藏夹（${favorites.size}）")
    }

    private fun currentReadingText(): String {
        return translationUiState.displayText()
    }

    private fun showMorePage() {
        readingText = null
        titleText?.text = "更多"
        val scroll = ScrollView(this).apply { clipToPadding = false }
        val menu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(12))
        }
        menu.addView(sectionTitle("状态"))
        val snapshot = settings.load()
        menu.addView(statusLine("模型服务", ModelProviders.displayName(snapshot.baseUrl)))
        menu.addView(statusLine("API Key", if (snapshot.apiKey.isNotBlank()) "已保存" else "未设置"))
        menu.addView(statusLine("模型", snapshot.model))
        menu.addView(statusLine("网络限制", if (snapshot.wifiOnly) "仅 Wi-Fi" else "不限"))
        menu.addView(statusLine("悬浮窗", if (Settings.canDrawOverlays(this)) "已授权" else "未授权"))
        menu.addView(statusLine("屏幕捕获", if (mediaProjection != null) "已授权" else "未授权"))
        menu.addView(statusLine("选区", selectionRect?.let { "${it.width()} × ${it.height()}" } ?: "未选择"))
        menu.addView(sectionTitle("显示"))
        menu.addView(opacityRow())

        menu.addView(sectionTitle("操作"))
        menu.addView(menuAction("返回阅读") { showReadingPage() })
        menu.addView(menuAction("收藏夹") { showFavoritesPage() })
        menu.addView(menuAction("重新翻译") { retranslateLastText() })
        menu.addView(menuAction("复制阅读内容") { copyResult() })
        menu.addView(menuAction("打开主设置") { openSettings() })
        menu.addView(menuAction("停止悬浮服务") { stopSelf() })

        scroll.addView(menu)
        swapContent(scroll)
        showStatus("状态与操作")
    }

    private fun showSelectionOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            showStatus("请先授权悬浮窗")
            return
        }
        removeOverlay(selectionOverlay)
        val overlay = SelectionOverlayView(
            this,
            onSelected = { rect ->
                selectionRect = rect
                removeOverlay(selectionOverlay)
                selectionOverlay = null
                showStatus("选区已更新")
            },
            onCanceled = {
                removeOverlay(selectionOverlay)
                selectionOverlay = null
                showStatus("已取消选区")
            }
        )
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }
        windowManager.addView(overlay, params)
        selectionOverlay = overlay
    }

    private fun refreshOnce() {
        if (ocrBusy) {
            showStatus("正在处理当前文本")
            return
        }
        if (translating) {
            cancelActiveTranslation()
            showStatus("已中断旧翻译，正在刷新")
        }
        if (panelView == null) showPanel() else showReadingPage()

        val projection = mediaProjection
        if (projection == null) {
            requestCapturePermission()
            return
        }
        if (selectionRect == null) {
            showSelectionOverlay()
            return
        }

        val controller = captureController ?: ScreenCaptureController(
            context = this,
            mediaProjection = projection,
            selectionProvider = { selectionRect },
            frameCallback = ::handleCapturedFrame,
            errorCallback = ::handleCaptureError,
            onProjectionStopped = ::handleProjectionStopped
        ).also { captureController = it }
        showStatus("正在截取屏幕")
        hideOwnWindowsForCapture()
        controller.captureOnce()
    }

    private fun handleCapturedFrame(bitmap: android.graphics.Bitmap) {
        restoreOwnWindowsAfterCapture()
        handleFrame(bitmap)
    }

    private fun handleCaptureError(message: String) {
        restoreOwnWindowsAfterCapture()
        showStatus(message)
    }

    /**
     * The MediaProjection token died (revoked by the user, expired, or rejected
     * by Android 14+ single-use rules). Clear our references so the next refresh
     * re-requests permission instead of looping on a dead projection. Then
     * automatically re-request so the user doesn't have to tap anything.
     */
    private fun handleProjectionStopped() {
        runCatching { mediaProjection?.stop() }
        mediaProjection = null
        captureController?.release()
        captureController = null
        ocrBusy = false
        restoreOwnWindowsAfterCapture()
        // Auto re-request permission so the overlay keeps working after a token
        // expiry without the user having to dig into the main UI.
        requestCapturePermission()
    }

    private fun handleFrame(bitmap: android.graphics.Bitmap) {
        if (ocrBusy || translating) {
            bitmap.recycle()
            showStatus("正在处理当前文本")
            return
        }
        ocrBusy = true
        showStatus("正在识别文字")
        recognizer.recognize(bitmap, object : ScreenTextRecognizer.Callback {
            override fun onSuccess(text: String) {
                ocrBusy = false
                android.util.Log.d("OcrTrace", "OCR len=${text.length} blank=${text.isBlank()} retry=$blankFrameRetries")
                if (text.isBlank() && blankFrameRetries < MAX_BLANK_FRAME_RETRIES) {
                    // The captured frame was likely a transient/blank frame from
                    // mid-transition (app switch, overlay hide). Wait briefly for
                    // the screen to settle and re-capture before declaring "no
                    // text" — the next frame almost always has real content.
                    blankFrameRetries++
                    Handler(mainLooper).postDelayed({ captureController?.captureOnce() }, BLANK_RETRY_DELAY_MS)
                    showStatus("正在重新截取画面…")
                    return
                }
                blankFrameRetries = 0
                handleRecognizedText(text, forceTranslate = false)
            }

            override fun onFailure(message: String) {
                ocrBusy = false
                android.util.Log.w("OcrTrace", "OCR onFailure: $message")
                blankFrameRetries = 0
                showStatus(message)
            }
        })
    }

    private fun handleRecognizedText(text: String, forceTranslate: Boolean) {
        val normalized = normalizeOcrText(text)
        when (
            TranslationRefreshPolicy.decide(
                currentText = normalized,
                lastText = lastStableText,
                hasCachedResult = lastResult != null,
                forceTranslate = forceTranslate
            )
        ) {
            RefreshAction.IGNORE_EMPTY -> showStatus("选区内未识别到文本")
            RefreshAction.REUSE_CACHED_RESULT -> {
                lastResult?.let { translationUiState = TranslationUiState.Complete(normalized, it) }
                showReadingPage()
                showStatus("内容未变化，已复用")
            }
            RefreshAction.REQUEST_TRANSLATION -> requestTranslation(normalized)
        }
    }

    private fun requestTranslation(text: String) {
        if (translating) {
            showStatus("正在翻译")
            return
        }
        val snapshot = settings.load()
        lastStableText = text
        lastResult = null
        translationUiState = TranslationUiState.InProgress(sourceText = text)
        showReadingPage()
        showStatus(translationUiState.statusText())
        updateBubbleFeedback()

        if (!canUseNetwork(snapshot)) {
            translationUiState = TranslationUiState.Failed(
                sourceText = text,
                message = "当前设置为仅 Wi-Fi 请求，网络不满足条件。"
            )
            readingText?.text = translationUiState.displayText()
            showStatus("等待 Wi-Fi")
            updateBubbleFeedback()
            return
        }

        translating = true
        val requestId = ++activeTranslationRequestId
        lastPartialShownAt = 0L
        lastProgressShown = null
        showStatus("正在翻译")
        currentTranslation?.cancel()
        currentTranslation = deepSeekClient.analyze(
            text = text,
            language = LanguageDetector.detect(text),
            settings = snapshot,
            callback = object : DeepSeekClient.ResultCallback {
                override fun onAnalysisProgress(progress: AnalysisProgress) {
                    runOnMain {
                        if (!translating || requestId != activeTranslationRequestId) return@runOnMain
                        translationUiState = TranslationUiState.InProgress(
                            sourceText = text,
                            partialTranslation = progress.translation,
                            isThinking = progress.isThinking,
                            words = progress.words,
                            grammar = progress.grammar
                        )
                        updateBubbleFeedback()
                        // The panel may be collapsed. State is still retained so
                        // reopening it immediately renders the latest partial.
                        if (panelView == null) return@runOnMain
                        // Throttle live updates to ~every 80ms so we don't flood
                        // the UI thread / WindowManager on every token.
                        val now = System.currentTimeMillis()
                        if (now - lastPartialShownAt < 80L) return@runOnMain
                        lastPartialShownAt = now
                        if (progress == lastProgressShown) return@runOnMain
                        lastProgressShown = progress
                        renderProgressInPlace(progress)
                        showStatus(translationUiState.statusText())
                    }
                }

                override fun onPartialFailure(progress: AnalysisProgress, message: String) {
                    runOnMain {
                        if (requestId != activeTranslationRequestId) return@runOnMain
                        translating = false
                        currentTranslation = null
                        lastResult = null
                        translationUiState = TranslationUiState.Interrupted(text, progress, message)
                        readingText?.text = SpannableStringBuilder(buildSpannableProgressText(progress))
                            .append("\n\n⚠ $message")
                        if (panelView != null) {
                            showStatus("解析不完整")
                        } else {
                            updateBubbleFeedback()
                            Toast.makeText(this@FloatingTranslatorService, "解析不完整，点悬浮球查看", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onSuccess(result: AnalysisResult) {
                    runOnMain {
                        if (requestId != activeTranslationRequestId) return@runOnMain
                        translating = false
                        currentTranslation = null
                        lastResult = result
                        translationUiState = TranslationUiState.Complete(text, result)
                        HistoryManager.addHistory(
                            context = this@FloatingTranslatorService,
                            original = text,
                            translated = result.toDisplayText(),
                            providerId = snapshot.providerId
                        )
                        if (panelView != null) {
                            showReadingPage()
                            showStatus("翻译完成")
                        } else {
                            updateBubbleFeedback()
                            Toast.makeText(this@FloatingTranslatorService, "翻译完成，点悬浮球查看", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onFailure(message: String) {
                    runOnMain {
                        if (requestId != activeTranslationRequestId) return@runOnMain
                        translating = false
                        currentTranslation = null
                        lastResult = null
                        translationUiState = TranslationUiState.Failed(text, message)
                        if (panelView != null) {
                            readingText?.text = translationUiState.displayText()
                            showStatus("翻译失败")
                        } else {
                            updateBubbleFeedback()
                            Toast.makeText(this@FloatingTranslatorService, "翻译失败，点悬浮球查看", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
    }

    private fun renderProgressInPlace(progress: AnalysisProgress) {
        val textView = readingText ?: return
        textView.text = buildSpannableProgressText(progress)
        if (progress.words.isNotEmpty()) attachWordTapDetector(textView)
    }

    private fun cancelActiveTranslation() {
        if (!translating && currentTranslation == null) return
        activeTranslationRequestId++
        currentTranslation?.cancel()
        currentTranslation = null
        translating = false
    }

    private fun retranslateLastText() {
        if (ocrBusy || translating) {
            showStatus("正在处理当前文本")
            return
        }
        if (lastStableText.isBlank()) {
            showStatus("还没有可重新翻译的文本")
            return
        }
        showReadingPage()
        handleRecognizedText(lastStableText, forceTranslate = true)
    }

    private fun normalizeOcrText(text: String): String {
        return text.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n")
    }

    private fun canUseNetwork(snapshot: SettingsSnapshot): Boolean {
        if (!snapshot.wifiOnly) return true
        val manager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun requestCapturePermission() {
        val intent = Intent(this, CapturePermissionActivity::class.java)
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
            )
        startActivity(intent)
        showStatus("请授权屏幕捕获")
    }

    private fun openSettings() {
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun copyResult() {
        val text = lastResult?.toDisplayText()
            ?: readingText?.text?.toString()
            ?: translationUiState.displayText()
        if (text.isBlank()) return
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("translation", text))
        Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show()
    }

    private fun showStatus(message: String) {
        runOnMain {
            val target = statusText ?: return@runOnMain
            if (target.text == message) return@runOnMain
            Md3Motion.updateText(target) { target.text = message }
        }
    }

    private fun setBubbleDocked(docked: Boolean) {
        val bubble = bubbleView ?: return
        bubbleStateAnimator?.cancel()
        bubbleStateAnimator = null
        bubble.scaleX = 1f
        bubble.scaleY = 1f
        bubble.animate().cancel()
        if (docked) {
            bubble.isEnabled = false
            bubble.animate()
                .alpha(0f)
                .scaleX(0.72f)
                .scaleY(0.72f)
                .setDuration(160L)
                .withEndAction {
                    if (panelView != null) bubble.visibility = View.INVISIBLE
                }
                .start()
        } else {
            bubble.visibility = View.VISIBLE
            bubble.isEnabled = true
            Md3Motion.enter(bubble, dp(8).toFloat(), endAlpha = settings.overlayOpacity)
            updateBubbleFeedback()
        }
    }

    /** Apply token-backed feedback without forcing the collapsed panel open. */
    private fun updateBubbleFeedback() {
        val bubble = bubbleView ?: return
        val nextFeedbackState = when (translationUiState) {
            TranslationUiState.Idle -> BubbleFeedbackState.IDLE
            is TranslationUiState.InProgress -> BubbleFeedbackState.IN_PROGRESS
            is TranslationUiState.Complete -> BubbleFeedbackState.COMPLETE
            is TranslationUiState.Failed -> BubbleFeedbackState.FAILED
            is TranslationUiState.Interrupted -> BubbleFeedbackState.FAILED
        }
        // Partial text can arrive many times per second. Once the correct
        // collapsed-state animation is running, do not restart it per token.
        if (bubbleFeedbackState == nextFeedbackState) {
            if (panelView != null || bubbleStateAnimator?.isRunning == true) return
        }
        bubbleFeedbackState = nextFeedbackState
        bubbleStateAnimator?.cancel()
        bubbleStateAnimator = null
        bubble.scaleX = 1f
        bubble.scaleY = 1f

        val (fill, stroke, description) = when (translationUiState) {
            TranslationUiState.Idle -> Triple(
                Md3.light.surfaceContainerLowest,
                Md3.light.outlineVariant,
                "屏幕翻译"
            )
            is TranslationUiState.InProgress -> Triple(
                Md3.light.primaryContainer,
                Md3.light.primary,
                "屏幕翻译，正在翻译"
            )
            is TranslationUiState.Complete -> Triple(
                Md3.light.secondaryContainer,
                Md3.light.secondary,
                "屏幕翻译，翻译完成"
            )
            is TranslationUiState.Failed -> Triple(
                Md3.light.errorContainer,
                Md3.light.error,
                "屏幕翻译，翻译失败"
            )
            is TranslationUiState.Interrupted -> Triple(
                Md3.light.errorContainer,
                Md3.light.error,
                "屏幕翻译，解析不完整"
            )
        }
        bubble.background = Md3.ripple(
            context = this,
            fillColor = fill,
            radiusDp = 999f,
            strokeColor = stroke,
            rippleColor = Md3.withAlpha(stroke, 0.16f)
        )
        bubble.contentDescription = description

        // The panel owns the visible status while expanded. Animate only the
        // collapsed bubble so background work remains apparent but unobtrusive.
        if (panelView != null || bubble.visibility != View.VISIBLE) return
        val values = when (translationUiState) {
            is TranslationUiState.InProgress -> floatArrayOf(1f, 0.90f, 1f)
            is TranslationUiState.Complete -> floatArrayOf(1f, 1.12f, 1f)
            is TranslationUiState.Failed -> floatArrayOf(1f, 0.88f, 1f)
            is TranslationUiState.Interrupted -> floatArrayOf(1f, 0.88f, 1f)
            TranslationUiState.Idle -> return
        }
        val scaleX = ObjectAnimator.ofFloat(bubble, View.SCALE_X, *values)
        val scaleY = ObjectAnimator.ofFloat(bubble, View.SCALE_Y, *values)
        if (translationUiState is TranslationUiState.InProgress) {
            scaleX.repeatCount = ValueAnimator.INFINITE
            scaleY.repeatCount = ValueAnimator.INFINITE
        }
        bubbleStateAnimator = AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = if (translationUiState is TranslationUiState.InProgress) 1100L else 420L
            start()
        }
    }

    private fun swapContent(view: View) {
        val host = contentHost ?: return
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        if (host.childCount == 0) {
            host.addView(view, params)
            Md3Motion.enter(view, dp(10).toFloat())
            return
        }

        val old = host.getChildAt(0)
        old.animate()
            .alpha(0f)
            .translationY(-dp(6).toFloat())
            .setDuration(110L)
            .withEndAction {
                host.removeAllViews()
                host.addView(view, params)
                Md3Motion.enter(view, dp(12).toFloat())
            }
            .start()
    }

    private fun runOnMain(block: () -> Unit) {
        Handler(mainLooper).post(block)
    }

    private fun ensureForeground() {
        createNotificationChannel()
        try {
            // Start as standard foreground service first.
            startForeground(NOTIFICATION_ID, buildNotification())
        } catch (e: Throwable) {
            android.util.Log.e("FloatingTranslatorService", "startForeground failed", e)
            Toast.makeText(this, "无法启动翻译服务：${e.localizedMessage ?: e::class.simpleName}", Toast.LENGTH_LONG).show()
            stopSelf()
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, FloatingTranslatorService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            Notification.Builder(this)
        }
        return builder
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("屏幕翻译运行中")
            .setContentText("点击悬浮窗手动刷新识别")
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "停止", stopIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "屏幕翻译",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun hideOwnWindowsForCapture() {
        if (hiddenOverlayState != null) return
        // Remember the target alpha to restore to (respects overlay opacity).
        val targetAlpha = settings.overlayOpacity
        hiddenOverlayState = HiddenOverlayState(
            bubbleVisibility = bubbleView?.visibility,
            panelVisibility = panelView?.visibility,
            selectionVisibility = selectionOverlay?.visibility
        )
        // Fade out (not instant hide) so the refresh reads as a smooth pulse.
        bubbleView?.animate()?.alpha(0f)?.setDuration(140L)?.start()
        panelView?.animate()?.alpha(0f)?.setDuration(140L)?.start()
        // The selection overlay is fullscreen and not opacity-controlled; hide
        // it instantly so it never appears in the captured frame.
        selectionOverlay?.visibility = View.INVISIBLE
    }

    private fun restoreOwnWindowsAfterCapture() {
        val state = hiddenOverlayState ?: return
        val targetAlpha = settings.overlayOpacity
        bubbleView?.animate()?.alpha(targetAlpha)?.setDuration(180L)?.start()
        panelView?.animate()?.alpha(targetAlpha)?.setDuration(180L)?.start()
        state.selectionVisibility?.let { selectionOverlay?.visibility = it }
        hiddenOverlayState = null
    }

    private fun attachDragAndClick(
        view: View,
        dragTarget: View,
        params: WindowManager.LayoutParams,
        onClick: (() -> Unit)?,
        onDragFinished: ((WindowManager.LayoutParams) -> Unit)? = null
    ) {
        var downRawX = 0f
        var downRawY = 0f
        var startX = 0
        var startY = 0
        view.isClickable = true
        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = params.x
                    startY = params.y
                    // No press scale on the drag target — scaling the bubble
                    // mid-drag makes it visibly change size and shifts the touch
                    // anchor, which feels broken.
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    params.x = clampWindowX(startX + (event.rawX - downRawX).toInt(), params.width)
                    params.y = clampWindowY(startY + (event.rawY - downRawY).toInt(), params.height)
                    runCatching { windowManager.updateViewLayout(dragTarget, params) }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    val moved = kotlin.math.abs(event.rawX - downRawX) + kotlin.math.abs(event.rawY - downRawY)
                    if (moved < dp(8)) {
                        view.performClick()
                        onClick?.invoke()
                        onDragFinished?.invoke(params)
                    } else {
                        if (dragTarget == bubbleView) {
                            val screenWidth = resources.displayMetrics.widthPixels
                            val bubbleWidth = params.width
                            val targetX = if (params.x + bubbleWidth / 2 < screenWidth / 2) {
                                0
                            } else {
                                screenWidth - bubbleWidth
                            }
                            animateBubbleToEdge(dragTarget, params, targetX) {
                                onDragFinished?.invoke(params)
                            }
                        } else {
                            onDragFinished?.invoke(params)
                        }
                    }
                    true
                }

                MotionEvent.ACTION_CANCEL -> {
                    Md3Motion.release(dragTarget)
                    true
                }

                else -> false
            }
        }
    }

    private fun animateBubbleToEdge(
        dragTarget: View,
        params: WindowManager.LayoutParams,
        targetX: Int,
        onFinished: () -> Unit
    ) {
        val animator = android.animation.ValueAnimator.ofInt(params.x, targetX).apply {
            duration = 200L
            interpolator = android.view.animation.DecelerateInterpolator()
            addUpdateListener { animation ->
                params.x = animation.animatedValue as Int
                runCatching { windowManager.updateViewLayout(dragTarget, params) }
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    onFinished()
                }
            })
        }
        animator.start()
    }

    private fun resizeHandle(params: WindowManager.LayoutParams): TextView {
        val handle = TextView(this).apply {
            text = "↘"
            Md3.applyTextStyle(this, Md3TextStyle.TitleMedium, Md3.dark.onSecondaryContainer)
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(10))
            background = Md3.ripple(this@FloatingTranslatorService, Md3.dark.secondaryContainer, 16f)
            clipToOutline = true
            layoutParams = LinearLayout.LayoutParams(dp(42), LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                leftMargin = dp(3)
                rightMargin = dp(3)
            }
        }

        var downRawX = 0f
        var downRawY = 0f
        var startWidth = 0
        var startHeight = 0
        handle.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startWidth = params.width
                    startHeight = params.height
                    Md3Motion.press(handle, scale = 0.94f)
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    params.width = clampPanelWidth(startWidth + (event.rawX - downRawX).toInt())
                    params.height = clampPanelHeight(startHeight + (event.rawY - downRawY).toInt())
                    params.x = clampWindowX(params.x, params.width)
                    params.y = clampWindowY(params.y, params.height)
                    panelView?.let { runCatching { windowManager.updateViewLayout(it, params) } }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    Md3Motion.release(handle)
                    savePanelWindowState(params)
                    true
                }

                MotionEvent.ACTION_CANCEL -> {
                    Md3Motion.release(handle)
                    true
                }

                else -> false
            }
        }
        return handle
    }

    private fun toolbarButton(label: String, primary: Boolean = false, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            gravity = Gravity.CENTER
            Md3.applyTextStyle(
                this,
                Md3TextStyle.LabelLarge,
                if (primary) Md3.dark.onPrimary else Md3.dark.onSecondaryContainer
            )
            setPadding(0, dp(10), 0, dp(10))
            background = Md3.ripple(
                context = this@FloatingTranslatorService,
                fillColor = if (primary) Md3.dark.primary else Md3.dark.secondaryContainer,
                radiusDp = 999f,
                rippleColor = Md3.withAlpha(if (primary) Md3.dark.onPrimary else Md3.dark.onSecondaryContainer, 0.16f)
            )
            clipToOutline = true
            setOnClickListener { onClick() }
            Md3.bindStateLayer(this, pressedScale = if (primary) 0.96f else 0.98f)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dp(3)
                rightMargin = dp(3)
            }
        }
    }

    private fun textButton(label: String, compact: Boolean = false, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            gravity = Gravity.CENTER
            Md3.applyTextStyle(this, if (compact) Md3TextStyle.TitleMedium else Md3TextStyle.LabelLarge, Md3.dark.onSurface)
            setPadding(dp(10), dp(4), dp(10), dp(4))
            background = Md3.ripple(this@FloatingTranslatorService, Md3.dark.surfaceContainerHighest, 999f)
            clipToOutline = true
            setOnClickListener { onClick() }
            Md3.bindStateLayer(this, pressedScale = 0.94f)
            layoutParams = LinearLayout.LayoutParams(
                if (compact) dp(38) else LinearLayout.LayoutParams.WRAP_CONTENT,
                if (compact) dp(34) else LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { leftMargin = dp(8) }
        }
    }

    private fun sectionTitle(label: String): TextView {
        return TextView(this).apply {
            text = label
            Md3.applyTextStyle(this, Md3TextStyle.LabelMedium, Md3.dark.primary)
            setPadding(0, dp(12), 0, dp(6))
        }
    }

    private fun statusLine(label: String, value: String): TextView {
        return TextView(this).apply {
            text = "$label：$value"
            Md3.applyTextStyle(this, Md3TextStyle.BodyMedium, Md3.dark.onSurfaceVariant)
            setPadding(dp(13), dp(10), dp(13), dp(10))
            background = Md3.surface(
                context = this@FloatingTranslatorService,
                color = Md3.dark.surfaceContainer,
                radiusDp = 12f
            )
            clipToOutline = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(6) }
        }
    }

    private fun menuAction(label: String, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            gravity = Gravity.CENTER_VERTICAL
            Md3.applyTextStyle(this, Md3TextStyle.LabelLarge, Md3.dark.onSecondaryContainer)
            setPadding(dp(15), dp(13), dp(15), dp(13))
            background = Md3.ripple(this@FloatingTranslatorService, Md3.dark.secondaryContainer, 999f)
            clipToOutline = true
            setOnClickListener { onClick() }
            Md3.bindStateLayer(this, pressedScale = 0.98f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
        }
    }

    /** "悬浮窗透明度" row with a SeekBar (30%–100%). Live-applies to bubble+panel. */
    private fun opacityRow(): LinearLayout {
        val opacity = settings.overlayOpacity
        val label = TextView(this).apply {
            text = "悬浮窗透明度 ${(opacity * 100).toInt()}%"
            Md3.applyTextStyle(this, Md3TextStyle.BodyMedium, Md3.dark.onSurfaceVariant)
        }
        val seek = SeekBar(this).apply {
            // Map 0.3..1.0 → 0..100 progress.
            val pct = ((opacity - 0.3f) / 0.7f * 100f).toInt().coerceIn(0, 100)
            progress = pct
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val value = 0.3f + (progress / 100f) * 0.7f
                    label.text = "悬浮窗透明度 ${(value * 100).toInt()}%"
                    if (fromUser) {
                        settings.overlayOpacity = value
                        applyOverlayOpacity()
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(10), dp(15), dp(10))
            addView(label)
            addView(seek)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
        }
    }

    /** Apply the saved overlay opacity to the bubble and panel (rendering-layer
     *  alpha; doesn't affect touch hit-testing). */
    private fun applyOverlayOpacity() {
        val alpha = settings.overlayOpacity
        runOnMain {
            bubbleView?.alpha = alpha
            panelView?.alpha = alpha
        }
    }

    private fun removeOverlay(view: View?) {
        if (view == null) return
        runCatching { windowManager.removeView(view) }
    }

    private fun overlayType(): Int = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun savePanelWindowState(params: WindowManager.LayoutParams) {
        settings.savePanelState(
            x = params.x,
            y = params.y,
            width = params.width,
            height = params.height
        )
    }

    private fun clampWindowX(x: Int, width: Int): Int {
        val effectiveWidth = max(width, dp(48))
        val maxX = max(0, resources.displayMetrics.widthPixels - effectiveWidth)
        return x.coerceIn(0, maxX)
    }

    private fun clampWindowY(y: Int, height: Int): Int {
        val effectiveHeight = max(height, dp(48))
        val maxY = max(0, resources.displayMetrics.heightPixels - effectiveHeight)
        return y.coerceIn(0, maxY)
    }

    private fun minPanelWidth(): Int = dp(300)

    private fun minPanelHeight(): Int = dp(320)

    private fun clampPanelWidth(width: Int): Int {
        val maxWidth = max(minPanelWidth(), resources.displayMetrics.widthPixels - dp(24))
        return width.coerceIn(minPanelWidth(), maxWidth)
    }

    private fun clampPanelHeight(height: Int): Int {
        val maxHeight = max(minPanelHeight(), resources.displayMetrics.heightPixels - dp(72))
        return height.coerceIn(minPanelHeight(), maxHeight)
    }

    companion object {
        var isRunning = false
        val isCaptureActive: Boolean
            get() = instanceRef.get()?.mediaProjection != null

        private var instanceRef = WeakReference<FloatingTranslatorService>(null)

        const val ACTION_SHOW = "com.poozh.translator.action.SHOW"
        const val ACTION_STOP = "com.poozh.translator.action.STOP"
        const val ACTION_REQUEST_CAPTURE = "com.poozh.translator.action.REQUEST_CAPTURE"
        const val ACTION_CAPTURE_RESULT = "com.poozh.translator.action.CAPTURE_RESULT"
        /** Settings changed in the UI — tell the running service to pick them up. */
        const val ACTION_REFRESH_SETTINGS = "com.poozh.translator.action.REFRESH_SETTINGS"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"

        private const val CHANNEL_ID = "screen_translator"
        private const val NOTIFICATION_ID = 3108

        /** When OCR returns a blank frame, retry the capture this many times
         *  before reporting "no text". A transient blank frame is common right
         *  after an app switch or overlay transition; the next frame usually
         *  has real content. */
        private const val MAX_BLANK_FRAME_RETRIES = 2
        private const val BLANK_RETRY_DELAY_MS = 250L
    }
}
