package com.example.translator

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
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
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

// ─────────────────────────── Design Tokens ───────────────────────────

private object OverlayColors {
    val Accent = Color(0xFF00D4AA)
    val AccentSecondary = Color(0xFF00B4D8)
    val PillBgTop = Color(0x000000).copy(alpha = 0.78f)
    val PillBgBottom = Color(0xFF111122).copy(alpha = 0.88f)
    val CardBg = Color(0xFF0A0A14).copy(alpha = 0.92f)
    val CardBgError = Color(0xFFFF6B6B).copy(alpha = 0.08f)
    val BorderSubtle = Color.White.copy(alpha = 0.10f)
    val BorderFocus = Color.White.copy(alpha = 0.18f)
    val TextPrimary = Color.White
    val TextSecondary = Color(0xFFA0AEC0)
    val ErrorText = Color(0xFFFF6B6B)
    val TopHighlight = Color.White.copy(alpha = 0.06f)
}

// ─────────────────────────── Overlay State ────────────────────────────

/** Overlay UI state. */
sealed class OverlayState {
    data object Idle : OverlayState()
    data object Loading : OverlayState()
    data class Error(val message: String) : OverlayState()
}

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
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var panelParams: WindowManager.LayoutParams? = null

    private var fadeAnimator: ObjectAnimator? = null

    // Shared top-left position (pixels) for both windows.
    private var overlayX = 80
    private var overlayY = 160

    /** Which window is currently mounted. */
    private var panelOpenState: Boolean = false

    private var resultText by mutableStateOf("")
    private var statusText by mutableStateOf("")
    private var overlayState by mutableStateOf<OverlayState>(OverlayState.Idle)

    var onPauseToggle: (() -> Unit)? = null
    var onClose: (() -> Unit)? = null
    var onRefresh: (() -> Unit)? = null

    init {
        lifecycleOwner.performRestore(null)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
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

        // Cancel any in-flight fade so rapid taps don't stack animators.
        fadeAnimator?.cancel()
        fadeAnimator = null

        if (open) {
            removeViewSafe(bubbleView)
            bubbleView = null
            ensurePanelWindow()
            fadeIn(panelView!!)
        } else {
            removeViewSafe(panelView)
            panelView = null
            ensureBubbleWindow()
            fadeIn(bubbleView!!)
        }
    }

    private fun fadeIn(view: View) {
        view.alpha = 0f
        val anim = ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f).apply {
            duration = 180
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (fadeAnimator === animation) fadeAnimator = null
                }
            })
        }
        fadeAnimator = anim
        anim.start()
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

    fun hideAll() {
        fadeAnimator?.cancel()
        fadeAnimator = null
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

    // ────────────────────────── Composable UI ─────────────────────────

    /** Collapsed bubble content. Crossfades (fixed-size, alpha only) between
     *  the idle "译" mark and the loading ring. */
    @Composable
    private fun BubbleContent() {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(OverlayColors.PillBgTop, OverlayColors.PillBgBottom)
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.sweepGradient(
                        listOf(
                            OverlayColors.Accent.copy(alpha = 0.7f),
                            OverlayColors.AccentSecondary.copy(alpha = 0.4f),
                            OverlayColors.Accent.copy(alpha = 0.7f)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Crossfade(
                targetState = overlayState is OverlayState.Loading,
                animationSpec = tween(180),
                label = "bubbleContent"
            ) { loading ->
                if (loading) {
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
        CircularProgressIndicator(
            modifier = Modifier.size(26.dp).rotate(angle),
            strokeWidth = 2.5.dp,
            color = OverlayColors.Accent
        )
    }

    /** Expanded panel content: a control pill (with a drag handle on its
     *  leading edge) plus the result card. Buttons are real Compose clicks;
     *  dragging is scoped to the leading handle so it never fights the buttons. */
    @Composable
    private fun PanelContent() {
        Column(modifier = Modifier.widthIn(max = 280.dp)) {
            ControlPill(
                state = overlayState,
                onRefresh = { onRefresh?.invoke() },
                onPauseToggle = { onPauseToggle?.invoke() },
                onClose = { onClose?.invoke() }
            )

            // Result card is always laid out when the panel is open (no inner
            // AnimatedVisibility) — that's the whole point: the panel's size
            // only changes when text length actually changes.
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

    @Composable
    private fun ControlPill(
        state: OverlayState,
        onRefresh: () -> Unit,
        onPauseToggle: () -> Unit,
        onClose: () -> Unit
    ) {
        val pillShape = RoundedCornerShape(20.dp)

        Column(
            modifier = Modifier
                .clip(pillShape)
                .background(
                    Brush.verticalGradient(
                        listOf(OverlayColors.PillBgTop, OverlayColors.PillBgBottom)
                    )
                )
                .border(1.dp, OverlayColors.BorderSubtle, pillShape)
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

                PillIconButton(Icons.Filled.Refresh, "Refresh") { onRefresh() }
                PillIconButton(
                    if (state is OverlayState.Loading) Icons.Filled.Pause
                    else Icons.Filled.PlayArrow,
                    "Pause/Resume"
                ) { onPauseToggle() }
                PillIconButton(Icons.Filled.Close, "Close") { onClose() }
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

    @Composable
    private fun PillIconButton(
        icon: ImageVector,
        desc: String,
        onClick: () -> Unit
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = desc,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(16.dp)
            )
        }
    }

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
        val cardBg = if (isError) OverlayColors.CardBgError else OverlayColors.CardBg

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(cardBg)
                .border(1.dp, OverlayColors.BorderSubtle, cardShape)
        ) {
            // Subtle top highlight line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(OverlayColors.TopHighlight)
            )

            Column(modifier = Modifier.padding(12.dp)) {
                if (status.isNotEmpty()) {
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isError) OverlayColors.ErrorText
                        else OverlayColors.TextSecondary,
                        fontSize = 12.sp
                    )
                    if (text.isNotEmpty()) Spacer(Modifier.height(6.dp))
                }

                if (text.isNotEmpty()) {
                    Text(
                        text = text,
                        color = OverlayColors.TextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 16,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(10.dp))

                    // Copy chip
                    AssistChip(
                        onClick = onCopy,
                        label = {
                            Text(
                                "复制",
                                fontSize = 11.sp,
                                color = OverlayColors.Accent,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.ContentCopy,
                                contentDescription = "Copy",
                                modifier = Modifier.size(14.dp),
                                tint = OverlayColors.Accent
                            )
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = OverlayColors.Accent.copy(alpha = 0.35f)
                        ),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = OverlayColors.Accent.copy(alpha = 0.08f)
                        )
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
