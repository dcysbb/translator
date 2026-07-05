package com.example.translator

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.delay

// ─────────────────────────── Design Tokens ───────────────────────────

private object OverlayColors {
    val Accent = GlassPalette.Accent
    val AccentSecondary = GlassPalette.AccentSecondary
    // Glass surfaces: translucent so the screen behind refracts through.
    val PillBgTop = Color.White.copy(alpha = 0.10f)
    val PillBgBottom = Color.White.copy(alpha = 0.04f)
    val CardBg = Color.White.copy(alpha = 0.10f)
    val CardBgError = GlassPalette.ErrorText.copy(alpha = 0.10f)
    val BorderSubtle = GlassPalette.BorderSubtle
    val BorderFocus = GlassPalette.BorderGlass
    val TextPrimary = GlassPalette.TextPrimary
    val TextSecondary = GlassPalette.TextSecondary
    val TextMuted = GlassPalette.TextMuted
    val ErrorText = GlassPalette.ErrorText
    val TopHighlight = GlassPalette.HighlightTopSoft
}

// ─────────────────────────── Overlay State ────────────────────────────

/** Overlay UI state. */
sealed class OverlayState {
    data object Idle : OverlayState()
    data object Loading : OverlayState()
    data class Error(val message: String) : OverlayState()
}

/** Which page the expanded panel is showing. */
enum class PanelSubPage { Home, Settings, History }

// ─────────────────────────── OverlayManager ──────────────────────────
//
// Two independent WindowManager windows — a fixed-size bubble and a
// fixed-width panel — of which exactly one is mounted at a time. Splitting
// them (instead of one WRAP_CONTENT window whose content animates in place)
// is what kills the flicker: each window's physical size is decided once at
// addView time, so expanding/collapsing becomes a cheap alpha cross-fade
// rather than a per-frame WindowManager re-layout.
//
// Position is tracked in [overlayX]/[overlayY] (top-left gravity) and shared
// by both windows, so a drag on either carries over to the other when the
// open state switches.

class OverlayManager(private val context: Context) {

    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val lifecycleOwner = OverlayLifecycleOwner()

    private var bubbleView: View? = null
    private var panelView: View? = null
    private var selectionView: View? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var panelParams: WindowManager.LayoutParams? = null

    /** Fired with the user-drawn selection Rect (screen px), or null if canceled. */
    var onSelectionResult: ((android.graphics.Rect?) -> Unit)? = null

    private var currentAnimator: Animator? = null

    // Shared top-left position (pixels) for both windows.
    private var overlayX = 80
    private var overlayY = 160

    /** Which window is currently mounted. */
    private var panelOpenState: Boolean = false

    private var resultText by mutableStateOf("")
    private var statusText by mutableStateOf("")
    private var overlayState by mutableStateOf<OverlayState>(OverlayState.Idle)

    /** Which sub-page the expanded panel shows. */
    private var panelSubPage by mutableStateOf(PanelSubPage.Home)
    /** Translation history, most-recent first. */
    private var history by mutableStateOf<List<HistoryItem>>(emptyList())
    private val prefs = PreferencesManager(context)

    var onPauseToggle: (() -> Unit)? = null
    var onClose: (() -> Unit)? = null
    var onRefresh: (() -> Unit)? = null
    /** Re-open the drag-to-select overlay so the user can pick a new region. */
    var onSelectRegion: (() -> Unit)? = null
    /** Invoked after the user edits API settings so the client is rebuilt. */
    var onRequestRebuildClient: (() -> Unit)? = null

    init {
        lifecycleOwner.performRestore(null)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        history = prefs.loadHistory()
    }

    /** Prepend a translation to history, trim to the limit, persist, and update
     *  the in-memory state so the History page reflects it live. */
    fun addToHistory(original: String, translation: String) {
        if (translation.isBlank()) return
        val item = HistoryItem(original, translation, System.currentTimeMillis())
        val updated = (listOf(item) + history).take(PreferencesManager.MAX_HISTORY)
        history = updated
        prefs.saveHistory(updated)
    }

    // ───────────────────────── Window management ─────────────────────────

    fun showControlOverlay() {
        // Initial mount is always the collapsed bubble.
        if (bubbleView == null && panelView == null) {
            applyOpenState(open = false)
        }
    }

    /**
     * Mount exactly one window. The other is removed first. Each window is
     * cross-faded in via a View alpha animator so the transition reads as a
     * smooth fade rather than a hard cut, without touching the window's size.
     */
    private fun applyOpenState(open: Boolean) {
        if (open == panelOpenState && (bubbleView != null || panelView != null)) return
        panelOpenState = open

        // Cancel any in-flight transition so rapid taps don't stack animators.
        currentAnimator?.cancel()
        currentAnimator = null

        if (open) {
            // Bubble shrinks/fades out first, then panel springs in.
            val bubble = bubbleView
            if (bubble != null) {
                animateOut(bubble, toScale = 0.7f, duration = 120) {
                    removeViewSafe(bubbleView)
                    bubbleView = null
                    ensurePanelWindow()
                    animateIn(panelView!!, fromScale = 0.88f, duration = 220, overshoot = true)
                }
            } else {
                ensurePanelWindow()
                animateIn(panelView!!, fromScale = 0.88f, duration = 220, overshoot = true)
            }
        } else {
            // Panel shrinks/fades out first, then bubble springs in.
            val panel = panelView
            if (panel != null) {
                animateOut(panel, toScale = 0.85f, duration = 140) {
                    removeViewSafe(panelView)
                    panelView = null
                    ensureBubbleWindow()
                    animateIn(bubbleView!!, fromScale = 0.6f, duration = 240, overshoot = true)
                }
            } else {
                ensureBubbleWindow()
                animateIn(bubbleView!!, fromScale = 0.6f, duration = 240, overshoot = true)
            }
        }
    }

    /**
     * Spring/scale a freshly mounted window into view. Uses View.scaleX/Y +
     * alpha (pure render-layer transforms) so WindowManager never re-lays out
     * the window — this is what keeps the transition flicker-free.
     */
    private fun animateIn(view: View, fromScale: Float, duration: Long, overshoot: Boolean) {
        view.scaleX = fromScale
        view.scaleY = fromScale
        view.alpha = 0f
        view.pivotX = view.width / 2f
        view.pivotY = view.height / 2f
        val set = AnimatorSet()
        val sx = ObjectAnimator.ofFloat(view, View.SCALE_X, fromScale, 1f)
        val sy = ObjectAnimator.ofFloat(view, View.SCALE_Y, fromScale, 1f)
        val al = ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f)
        // Smooth decelerate — eases to a stop with NO overshoot/bounce.
        val interp = android.view.animation.DecelerateInterpolator(1.6f)
        sx.interpolator = interp
        sy.interpolator = interp
        al.interpolator = interp
        set.playTogether(sx, sy, al)
        set.duration = duration
        set.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (currentAnimator === animation) currentAnimator = null
            }
        })
        currentAnimator = set
        set.start()
    }

    /**
     * Shrink + fade a window out, then invoke [onEnd] (which typically removes
     * the window and mounts the next one).
     */
    private fun animateOut(view: View, toScale: Float, duration: Long, onEnd: () -> Unit) {
        view.pivotX = view.width / 2f
        view.pivotY = view.height / 2f
        val set = AnimatorSet()
        val sx = ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, toScale)
        val sy = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, toScale)
        val al = ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0f)
        set.playTogether(sx, sy, al)
        set.duration = duration
        set.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (currentAnimator === animation) currentAnimator = null
                onEnd()
            }
        })
        currentAnimator = set
        set.start()
    }

    private fun removeViewSafe(view: View?) {
        if (view == null) return
        try {
            windowManager.removeView(view)
        } catch (_: Exception) {
        }
    }

    // ── Bubble window ──

    private fun ensureBubbleWindow() {
        if (bubbleView != null) return
        val view = newComposeView { BubbleContent() }

        val params = baseParams().apply {
            // 56dp fixed square — size never changes, so no per-frame re-layout.
            width = dp(56)
            height = dp(56)
            x = overlayX
            y = overlayY
        }
        view.setOnTouchListener(
            OverlayTouchListener(
                context = context,
                onTap = { handleOverlayTap() },
                onDragBy = ::handleDrag,
                onLongPress = { onClose?.invoke() }
            )
        )
        addViewSafe(view, params)
        bubbleView = view
        bubbleParams = params
    }

    // ── Panel window ──

    private fun ensurePanelWindow() {
        if (panelView != null) return
        val view = newComposeView { PanelContent() }

        val params = baseParams().apply {
            // Fixed width, WRAP_CONTENT height: the panel's height only changes
            // when the result text genuinely changes length, which is fine —
            // the flicker came from the *expand/collapse* animation changing
            // size every frame, not from text-driven reflow.
            width = dp(280)
            height = WindowManager.LayoutParams.WRAP_CONTENT
            x = overlayX
            y = overlayY
            // Real liquid-glass refraction: blur whatever is on screen behind
            // the overlay. Android 12+; silently ignored on older devices or
            // compositors that don't support it (falls back to the frosted fill).
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    blurBehindRadius = 24
                    flags = flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                } catch (_: Throwable) {
                }
            }
        }
        // No view-level OnTouchListener here: that would swallow the Compose
        // button taps. The panel drags via a dedicated handle in PanelContent.
        addViewSafe(view, params)
        panelView = view
        panelParams = params
    }

    private fun newComposeView(content: @Composable () -> Unit): ComposeView =
        ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                OverlayTheme { content() }
            }
        }

    private fun baseParams() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
    }

    private fun addViewSafe(view: View, params: WindowManager.LayoutParams) {
        try {
            windowManager.addView(view, params)
        } catch (_: Exception) {
        }
    }

    private fun dp(value: Int): Int {
        val density = context.resources.displayMetrics.density
        return (value * density).toInt()
    }

    // ───────────────────────── Drag handling ─────────────────────────

    /**
     * Apply a drag delta to both windows' stored position, and live-update
     * whichever window is currently mounted. Called from the bubble's native
     * listener and the panel's Compose drag handle.
     */
    private fun handleDrag(dx: Int, dy: Int) {
        overlayX += dx
        overlayY += dy
        if (panelOpenState) {
            panelParams?.let { p ->
                p.x = overlayX
                p.y = overlayY
                updateLayoutSafe(panelView, p)
            }
        } else {
            bubbleParams?.let { p ->
                p.x = overlayX
                p.y = overlayY
                updateLayoutSafe(bubbleView, p)
            }
        }
    }

    private fun updateLayoutSafe(view: View?, params: WindowManager.LayoutParams) {
        if (view == null) return
        try {
            windowManager.updateViewLayout(view, params)
        } catch (_: Exception) {
        }
    }

    // ───────────────────────── State transitions ─────────────────────────

    private fun handleOverlayTap() {
        applyOpenState(open = !panelOpenState)
    }

    fun updateText(text: String) {
        resultText = text
        statusText = ""
        overlayState = OverlayState.Idle
        panelSubPage = PanelSubPage.Home   // show the result on the home page
        applyOpenState(open = true)   // auto-expand to show the result
    }

    fun updateStatus(status: String) {
        statusText = status
    }

    fun showLoading() {
        statusText = ""
        overlayState = OverlayState.Loading
        applyOpenState(open = false)  // collapse to bubble, show spinner
    }

    fun showError(message: String) {
        statusText = message
        overlayState = OverlayState.Error(message)
        applyOpenState(open = true)   // auto-expand to show the error
    }

    fun reset() {
        resultText = ""
        statusText = ""
        overlayState = OverlayState.Idle
        applyOpenState(open = false)
    }

    /**
     * Show a full-screen drag-to-select overlay. The user drags out a rectangle;
     * [onSelectionResult] is invoked with that Rect (or null on cancel). Removes
     * itself when done.
     */
    fun showSelectionOverlay() {
        hideSelectionOverlay()
        val overlay = SelectionOverlayView(
            context = context,
            onSelected = { rect ->
                hideSelectionOverlay()
                onSelectionResult?.invoke(rect)
            },
            onCanceled = {
                hideSelectionOverlay()
                onSelectionResult?.invoke(null)
            }
        )
        // Full-screen, focusable so it receives the drag.
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        addViewSafe(overlay, params)
        selectionView = overlay
    }

    fun hideSelectionOverlay() {
        selectionView?.let {
            removeViewSafe(it)
            selectionView = null
        }
    }

    fun hideAll() {
        currentAnimator?.cancel()
        currentAnimator = null
        hideSelectionOverlay()
        removeViewSafe(panelView)
        removeViewSafe(bubbleView)
        panelView = null
        bubbleView = null
        panelParams = null
        bubbleParams = null
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    /**
     * Get the current absolute screen bounds (in pixels) of the visible overlay window.
     */
    fun getOverlayBounds(): android.graphics.Rect? {
        val view = if (panelOpenState) panelView else bubbleView
        if (view == null || !view.isAttachedToWindow) {
            android.util.Log.d("OverlayManager", "getOverlayBounds: view is null or not attached. panelOpenState=$panelOpenState")
            return null
        }
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val rect = android.graphics.Rect(
            location[0],
            location[1],
            location[0] + view.width,
            location[1] + view.height
        )
        android.util.Log.d("OverlayManager", "getOverlayBounds: panelOpenState=$panelOpenState, rect=$rect, view.width=${view.width}, view.height=${view.height}")
        return rect
    }

    // ────────────────────────── Composable UI ─────────────────────────

    /** Collapsed bubble content. While loading, the whole bubble softly pulses
     *  (alpha breathing) so it reads as "working"; the inner content crossfades
     *  between the idle "译" mark and the loading ring. All fixed-size — the
     *  pulse is a pure alpha animation, so no re-layout. */
    @Composable
    private fun BubbleContent() {
        val loading = overlayState is OverlayState.Loading

        // Slow alpha breathing applied to the whole bubble while loading.
        val pulseTransition = rememberInfiniteTransition(label = "bubblePulse")
        val pulseAlpha by pulseTransition.animateFloat(
            initialValue = 0.62f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bubblePulseAlpha"
        )

        Box(
            modifier = Modifier
                .size(56.dp)
                .alpha(if (loading) pulseAlpha else 1f)
                .clip(CircleShape)
                // Borderless glass — no hard rim, melts into the screen.
                .then(Modifier.liquidGlassOverlay(CircleShape, fillAlpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Crossfade(
                targetState = loading,
                animationSpec = tween(180),
                label = "bubbleContent"
            ) { isLoading ->
                if (isLoading) {
                    LoadingRing()
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        OverlayColors.Accent.copy(alpha = 0.35f),
                                        Color.Transparent
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "译",
                            color = OverlayColors.Accent,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    /** A spinning arc with a gradient tail that fades out, drawn on a Canvas.
     *  More refined than a uniform CircularProgressIndicator. */
    @Composable
    private fun LoadingRing() {
        val transition = rememberInfiniteTransition(label = "ring")
        val angle by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "ringSpin"
        )
        Canvas(modifier = Modifier.size(26.dp).rotate(angle)) {
            val stroke = 2.8.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)
            // Sweep gradient: solid accent at the head, fading to transparent at
            // the tail — gives the classic "comet" spinner look.
            val brush = Brush.sweepGradient(
                colors = listOf(
                    OverlayColors.Accent,
                    OverlayColors.Accent.copy(alpha = 0.55f),
                    Color.Transparent
                ),
                center = Offset(size.width / 2f, size.height / 2f)
            )
            drawArc(
                brush = brush,
                startAngle = 0f,
                sweepAngle = 300f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
    }

    /** Expanded panel content. The control pill (with a drag handle on its
     *  leading edge) plus the result card on the home page; settings and
     *  history are alternate sub-pages switched via [panelSubPage]. */
    @Composable
    private fun PanelContent() {
        Column(modifier = Modifier.widthIn(max = 280.dp)) {
            AnimatedContent(
                targetState = panelSubPage,
                transitionSpec = {
                    (slideInHorizontally { it / 3 } + fadeIn(tween(180))) togetherWith
                        (slideOutHorizontally { -it / 3 } + fadeOut(tween(140)))
                },
                label = "subPage"
            ) { page ->
                when (page) {
                    PanelSubPage.Home -> HomePage()
                    PanelSubPage.Settings -> SettingsPage(onBack = { panelSubPage = PanelSubPage.Home })
                    PanelSubPage.History -> HistoryPage(onBack = { panelSubPage = PanelSubPage.Home })
                }
            }
        }
    }

    @Composable
    private fun HomePage() {
        Column {
            ControlPill(
                state = overlayState,
                onRefresh = { onRefresh?.invoke() },
                onSelectRegion = { onSelectRegion?.invoke() },
                onOpenHistory = { panelSubPage = PanelSubPage.History },
                onOpenSettings = { panelSubPage = PanelSubPage.Settings },
                // ✕ collapses back to the bubble (expand/collapse loop). The
                // service itself is stopped by long-pressing the bubble.
                onClose = { applyOpenState(open = false) }
            )

            // Result card is always laid out when the panel is open (no inner
            // AnimatedVisibility) — that's the whole point: the panel's size
            // only changes when text length genuinely changes.
            Spacer(Modifier.height(6.dp))
            ResultCard(
                text = resultText,
                status = statusText,
                isError = overlayState is OverlayState.Error,
                onCopy = {
                    val clipboard = context.getSystemService(
                        Context.CLIPBOARD_SERVICE
                    ) as ClipboardManager
                    clipboard.setPrimaryClip(
                        ClipData.newPlainText(
                            "translation",
                            resultText.ifEmpty { statusText }
                        )
                    )
                }
            )
        }
    }

    // ── Settings sub-page ──

    @Composable
    private fun SettingsPage(onBack: () -> Unit) {
        val cardShape = RoundedCornerShape(16.dp)
        Column {
            SubPageHeader(title = "设置", onBack = onBack)
            Spacer(Modifier.height(6.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .then(Modifier.liquidGlassOverlay(cardShape))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                var providerId by remember { mutableStateOf(prefs.currentProviderId) }
                var apiKey by remember(providerId) { mutableStateOf(prefs.getApiKey(providerId)) }
                var model by remember(providerId) {
                    mutableStateOf(prefs.getModel(providerId).ifBlank { ModelProviders.byId(providerId).defaultModel })
                }
                var customUrl by remember(providerId) { mutableStateOf(prefs.customBaseUrl) }
                var keyVisible by remember { mutableStateOf(false) }
                val provider = ModelProviders.byId(providerId)

                // Provider chips (wrap; overlay is narrow).
                Text("供应商", color = OverlayColors.TextSecondary, fontSize = 11.sp)
                ProviderChipRow(
                    providers = ModelProviders.all,
                    selectedId = providerId,
                    onSelect = { providerId = it; prefs.currentProviderId = it; onRequestRebuildClient?.invoke() }
                )

                if (provider.needsApiKey) {
                    OverlayTextField(
                        label = "API Key",
                        value = apiKey,
                        onValueChange = {
                            apiKey = it
                            prefs.setApiKey(providerId, it)
                            onRequestRebuildClient?.invoke()
                        },
                        obscured = !keyVisible,
                        onToggleObscure = { keyVisible = !keyVisible }
                    )
                }
                OverlayTextField(
                    label = "模型名称",
                    value = model,
                    onValueChange = {
                        model = it
                        prefs.setModel(providerId, it)
                        onRequestRebuildClient?.invoke()
                    }
                )
                if (providerId == ModelProviders.CUSTOM_ID) {
                    OverlayTextField(
                        label = "Base URL",
                        value = customUrl,
                        onValueChange = {
                            customUrl = it
                            prefs.customBaseUrl = it
                            onRequestRebuildClient?.invoke()
                        }
                    )
                }
            }
        }
    }

    /** Wrapping chip row of providers for the narrow overlay panel. */
    @Composable
    private fun ProviderChipRow(
        providers: List<ModelProvider>,
        selectedId: String,
        onSelect: (String) -> Unit
    ) {
        // Simple flow layout via Column of Rows is overkill; use a horizontal
        // scroll since the overlay is narrow.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            providers.forEach { p ->
                val selected = p.id == selectedId
                val chipShape = RoundedCornerShape(50)
                Box(
                    modifier = Modifier
                        .clip(chipShape)
                        .background(
                            if (selected) OverlayColors.Accent.copy(alpha = 0.20f)
                            else Color.White.copy(alpha = 0.05f)
                        )
                        .border(
                            0.8.dp,
                            if (selected) OverlayColors.Accent else OverlayColors.BorderSubtle,
                            chipShape
                        )
                        .clickable { onSelect(p.id) }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        p.displayName,
                        fontSize = 11.sp,
                        color = if (selected) OverlayColors.Accent else OverlayColors.TextSecondary,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }

    // ── History sub-page ──

    @Composable
    private fun HistoryPage(onBack: () -> Unit) {
        Column {
            SubPageHeader(title = "历史", onBack = onBack)
            Spacer(Modifier.height(6.dp))
            if (history.isEmpty()) {
                EmptyHistory()
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    history.forEach { item -> HistoryRow(item) }
                    Spacer(Modifier.height(4.dp))
                    ClearHistoryButton()
                }
            }
        }
    }

    @Composable
    private fun EmptyHistory() {
        val cardShape = RoundedCornerShape(16.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .then(Modifier.liquidGlassOverlay(cardShape))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "暂无翻译历史",
                color = OverlayColors.TextSecondary.copy(alpha = 0.7f),
                fontSize = 13.sp
            )
        }
    }

    @Composable
    private fun HistoryRow(item: HistoryItem) {
        val cardShape = RoundedCornerShape(14.dp)
        var copied by remember { mutableStateOf(false) }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .then(Modifier.liquidGlassOverlay(cardShape))
                .clickable {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                        as ClipboardManager
                    clipboard.setPrimaryClip(
                        ClipData.newPlainText("translation", item.translation)
                    )
                    copied = true
                }
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = formatTime(item.timestamp),
                color = OverlayColors.TextSecondary.copy(alpha = 0.6f),
                fontSize = 10.sp
            )
            if (item.original.isNotBlank()) {
                Text(
                    text = item.original,
                    color = OverlayColors.TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = item.translation,
                color = OverlayColors.TextPrimary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 8,
                overflow = TextOverflow.Ellipsis
            )
            Crossfade(targetState = copied, animationSpec = tween(160), label = "histCopied") {
                if (it) {
                    Text(
                        "已复制译文",
                        color = OverlayColors.Accent,
                        fontSize = 10.sp
                    )
                }
            }
            LaunchedEffect(copied) {
                if (copied) { delay(1500); copied = false }
            }
        }
    }

    @Composable
    private fun ClearHistoryButton() {
        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (pressed) 0.96f else 1f,
            animationSpec = spring(stiffness = Spring.StiffnessMedium),
            label = "clearScale"
        )
        val shape = RoundedCornerShape(12.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale)
                .clip(shape)
                .background(OverlayColors.CardBgError)
                .border(1.dp, OverlayColors.ErrorText.copy(alpha = 0.3f), shape)
                .clickable(interactionSource = interaction, indication = null) {
                    history = emptyList()
                    prefs.saveHistory(emptyList())
                }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "清空历史",
                color = OverlayColors.ErrorText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }

    /** Shared header for Settings/History: back arrow + title. */
    @Composable
    private fun SubPageHeader(title: String, onBack: () -> Unit) {
        val pillShape = RoundedCornerShape(20.dp)
        Column(
            modifier = Modifier
                .clip(pillShape)
                .then(Modifier.liquidGlassOverlay(pillShape))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PillIconButton(Icons.AutoMirrored.Filled.ArrowBack, "返回") { onBack() }
                Spacer(Modifier.width(6.dp))
                Text(
                    title,
                    color = OverlayColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                OverlayColors.Accent.copy(alpha = 0.6f),
                                OverlayColors.AccentSecondary.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }

    @Composable
    private fun OverlayTextField(
        label: String,
        value: String,
        onValueChange: (String) -> Unit,
        obscured: Boolean = false,
        onToggleObscure: () -> Unit = {}
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                label,
                color = OverlayColors.TextSecondary,
                fontSize = 11.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = OverlayColors.TextPrimary,
                        fontSize = 13.sp
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(OverlayColors.Accent),
                    visualTransformation = if (obscured) PasswordVisualTransformation()
                    else VisualTransformation.None,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, OverlayColors.BorderSubtle, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                )
                if (obscured || value.isNotEmpty()) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .clickable { onToggleObscure() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (obscured) Icons.Filled.PlayArrow
                            else Icons.Filled.Pause,
                            contentDescription = "切换可见",
                            tint = OverlayColors.TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }

    private fun formatTime(ts: Long): String {
        val fmt = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
        return fmt.format(java.util.Date(ts))
    }

    @Composable
    private fun ControlPill(
        state: OverlayState,
        onRefresh: () -> Unit,
        onSelectRegion: () -> Unit,
        onOpenHistory: () -> Unit,
        onOpenSettings: () -> Unit,
        onClose: () -> Unit
    ) {
        val pillShape = RoundedCornerShape(20.dp)

        Column(
            modifier = Modifier
                .clip(pillShape)
                .then(Modifier.liquidGlassOverlay(pillShape))
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Drag handle: the leading icon area. Dragging here moves the
                // panel window (and keeps the bubble's stored position in sync).
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                // dragAmount is already in px; handleDrag wants px.
                                handleDrag(dragAmount.x.toInt(), dragAmount.y.toInt())
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    when (state) {
                        is OverlayState.Loading -> PulsingDot()
                        else -> Icon(
                            imageVector = Icons.Filled.Translate,
                            contentDescription = "拖动",
                            tint = OverlayColors.Accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(Modifier.width(2.dp))

                PillIconButton(Icons.Filled.Refresh, "刷新") { onRefresh() }
                PillIconButton(Icons.Filled.Crop, "选区") { onSelectRegion() }
                PillIconButton(Icons.Filled.History, "历史") { onOpenHistory() }
                PillIconButton(Icons.Filled.Settings, "设置") { onOpenSettings() }
                PillIconButton(Icons.Filled.Close, "关闭") { onClose() }
            }

            // Thin accent indicator bar at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                OverlayColors.Accent.copy(alpha = 0.6f),
                                OverlayColors.AccentSecondary.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }

    /** Pill icon button with a tactile press animation: on press the icon
     *  scales down, a circular accent highlight fades in behind it, and the
     *  tint brightens. Springs back on release. */
    @Composable
    private fun PillIconButton(
        icon: ImageVector,
        desc: String,
        onClick: () -> Unit
    ) {
        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (pressed) 0.82f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "pillScale"
        )
        val highlight by animateFloatAsState(
            targetValue = if (pressed) 1f else 0f,
            animationSpec = spring(stiffness = Spring.StiffnessMedium),
            label = "pillHighlight"
        )

        Box(
            modifier = Modifier
                .size(32.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(OverlayColors.Accent.copy(alpha = 0.18f * highlight))
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = desc,
                tint = lerpWhiteAccent(pressed),
                modifier = Modifier.size(16.dp)
            )
        }
    }

    /** Blend the icon tint from idle white-ish to accent when pressed. */
    private fun lerpWhiteAccent(pressed: Boolean): Color =
        if (pressed) OverlayColors.Accent else Color.White.copy(alpha = 0.85f)

    @Composable
    private fun PulsingDot() {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(600),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )
        Box(
            modifier = Modifier
                .size(18.dp)
                .scale(scale)
                .padding(3.dp)
                .clip(CircleShape)
                .background(OverlayColors.Accent)
        )
    }

    // ── Result Card ──

    @Composable
    private fun ResultCard(
        text: String,
        status: String,
        isError: Boolean,
        onCopy: () -> Unit
    ) {
        val cardShape = RoundedCornerShape(16.dp)
        val isErrorState = isError

        // Copy feedback: flips to "已复制" briefly after a copy.
        var copied by remember { mutableStateOf(false) }

        val baseModifier = if (isErrorState) {
            // Soft red tint over the glass for error cards.
            Modifier.drawBehind { drawRect(OverlayColors.CardBgError) }
        } else Modifier

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .then(Modifier.liquidGlassOverlay(cardShape))
                .then(baseModifier)
        ) {
            // Subtle top highlight line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(OverlayColors.TopHighlight)
            )

            Column(modifier = Modifier.padding(12.dp)) {
                // Status line animates on change (识别中… / 翻译中… / errors).
                if (status.isNotEmpty()) {
                    AnimatedContent(
                        targetState = status,
                        transitionSpec = {
                            (slideInVertically { it / 3 } + fadeIn(tween(220))) togetherWith
                                (slideOutVertically { -it / 3 } + fadeOut(tween(160)))
                        },
                        label = "statusAnim",
                        modifier = Modifier.fillMaxWidth()
                    ) { s ->
                        Text(
                            text = s,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isError) OverlayColors.ErrorText
                            else OverlayColors.TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    if (text.isNotEmpty()) Spacer(Modifier.height(6.dp))
                }

                if (text.isNotEmpty()) {
                    // Body text "floats in" on each refresh.
                    AnimatedContent(
                        targetState = text,
                        transitionSpec = {
                            (slideInVertically { it / 4 } + fadeIn(tween(240))) togetherWith
                                (slideOutVertically { -it / 4 } + fadeOut(tween(160)))
                        },
                        label = "textAnim"
                    ) { t ->
                        Text(
                            text = t,
                            color = OverlayColors.TextPrimary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 16,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // Copy chip — toggles label/icon on tap via Crossfade.
                    AssistChip(
                        onClick = {
                            onCopy()
                            copied = true
                        },
                        label = {
                            Crossfade(
                                targetState = copied,
                                animationSpec = tween(160),
                                label = "copyLabel"
                            ) { isCopied ->
                                Text(
                                    if (isCopied) "已复制" else "复制",
                                    fontSize = 11.sp,
                                    color = OverlayColors.Accent,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        },
                        leadingIcon = {
                            Crossfade(
                                targetState = copied,
                                animationSpec = tween(160),
                                label = "copyIcon"
                            ) { isCopied ->
                                Icon(
                                    if (isCopied) Icons.Filled.Check
                                    else Icons.Filled.ContentCopy,
                                    contentDescription = "Copy",
                                    modifier = Modifier.size(14.dp),
                                    tint = OverlayColors.Accent
                                )
                            }
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = OverlayColors.Accent.copy(alpha = 0.35f)
                        ),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = OverlayColors.Accent.copy(alpha = 0.08f)
                        )
                    )

                    // Reset the "已复制" hint back to "复制" after 2s.
                    LaunchedEffect(copied) {
                        if (copied) {
                            delay(2000)
                            copied = false
                        }
                    }
                } else {
                    // Empty state hint so the card is never blank.
                    Text(
                        text = "点刷新开始翻译",
                        color = OverlayColors.TextSecondary.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// ─────────────────────────── Theme Wrapper ────────────────────────────

@Composable
private fun OverlayTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme()) {
        content()
    }
}

// ─────────────────────────── Lifecycle Owner ──────────────────────────

class OverlayLifecycleOwner : SavedStateRegistryOwner, ViewModelStoreOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry
    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore
        get() = store

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        lifecycleRegistry.handleLifecycleEvent(event)
    }

    fun performRestore(savedState: android.os.Bundle?) {
        savedStateRegistryController.performRestore(savedState)
    }
}

// ─────────────────────────── Overlay Touch Listener ──────────────────────
//
// Native touch handling for the BUBBLE window only (the panel drags via a
// Compose drag handle so its buttons keep working). Distinguishes tap (toggle
// open state) from drag (move both windows' stored position) from long-press
// (close the service). Reports drag deltas to the manager via [onDragBy] so
// the manager keeps bubble + panel positions in sync.

private class OverlayTouchListener(
    private val context: Context,
    private val onTap: () -> Unit,
    private val onDragBy: (dx: Int, dy: Int) -> Unit,
    private val onLongPress: () -> Unit
) : View.OnTouchListener {

    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    private var longPressHandled = false

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val longPressTimeout = ViewConfiguration.getLongPressTimeout().toLong()
    private val handler = Handler(Looper.getMainLooper())

    private val longPressRunnable = Runnable {
        if (!isDragging && !longPressHandled) {
            longPressHandled = true
            onLongPress()
        }
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                longPressHandled = false
                handler.postDelayed(longPressRunnable, longPressTimeout)
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - initialTouchX
                val dy = event.rawY - initialTouchY
                if (!isDragging && (Math.abs(dx) > touchSlop || Math.abs(dy) > touchSlop)) {
                    isDragging = true
                    handler.removeCallbacks(longPressRunnable)
                }
                if (isDragging) {
                    // Move one MOVE-step at a time using the delta since the
                    // previous MOVE, so the window tracks the finger 1:1.
                    onDragBy(dx.toInt() - lastDragX, dy.toInt() - lastDragY)
                    lastDragX = dx.toInt()
                    lastDragY = dy.toInt()
                }
            }
            MotionEvent.ACTION_UP -> {
                handler.removeCallbacks(longPressRunnable)
                if (!isDragging && !longPressHandled) onTap()
                resetDrag()
            }
            MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(longPressRunnable)
                resetDrag()
            }
        }
        return true
    }

    private var lastDragX = 0
    private var lastDragY = 0
    private fun resetDrag() {
        isDragging = false
        lastDragX = 0
        lastDragY = 0
    }
}
