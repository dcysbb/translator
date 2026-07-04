package com.example.translator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.IntOffset
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
import kotlin.math.roundToInt

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

class OverlayManager(private val context: Context) {

    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var controlView: View? = null

    private val lifecycleOwner = OverlayLifecycleOwner()

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

    fun showControlOverlay() {
        if (controlView != null) return

        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                OverlayTheme {
                    FloatingOverlay(
                        resultText = resultText,
                        statusText = statusText,
                        state = overlayState,
                        onPauseToggle = { onPauseToggle?.invoke() },
                        onClose = { onClose?.invoke() },
                        onRefresh = { onRefresh?.invoke() }
                    )
                }
            }
        }

        val layoutParams = WindowManager.LayoutParams(
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
            x = 80
            y = 160
        }

        windowManager.addView(composeView, layoutParams)
        controlView = composeView
    }

    fun updateText(text: String) {
        resultText = text
        statusText = ""
        overlayState = OverlayState.Idle
    }

    fun updateStatus(status: String) {
        statusText = status
    }

    fun showLoading() {
        statusText = ""
        overlayState = OverlayState.Loading
    }

    fun showError(message: String) {
        statusText = message
        overlayState = OverlayState.Error(message)
    }

    fun reset() {
        resultText = ""
        statusText = ""
        overlayState = OverlayState.Idle
    }

    fun hideAll() {
        controlView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
            controlView = null
        }
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    // ────────────────────────── Composable UI ─────────────────────────

    @Composable
    private fun FloatingOverlay(
        resultText: String,
        statusText: String,
        state: OverlayState,
        onPauseToggle: () -> Unit,
        onClose: () -> Unit,
        onRefresh: () -> Unit
    ) {
        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }

        // Collapsed = single bubble; Expanded = pill + result panel.
        // Starts collapsed. Refresh forces collapse (bubble shows loading),
        // completion forces expand so the result/error is visible.
        var panelOpen by remember { mutableStateOf(false) }
        var userCardExpanded by remember { mutableStateOf(true) }

        val showCard by remember(panelOpen, userCardExpanded, state, resultText, statusText) {
            derivedStateOf {
                panelOpen &&
                    userCardExpanded &&
                    state !is OverlayState.Loading &&
                    (resultText.isNotEmpty() || statusText.isNotEmpty())
            }
        }

        // Drive panel open/close from processing state:
        //  - Loading  -> collapse to bubble (show spinner on the ball)
        //  - Idle/Error with content -> auto-expand panel
        LaunchedEffect(state) {
            when (state) {
                is OverlayState.Loading -> panelOpen = false
                is OverlayState.Idle, is OverlayState.Error ->
                    if (resultText.isNotEmpty() || statusText.isNotEmpty()) panelOpen = true
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .pointerInput(panelOpen) {
                    // Whole overlay is draggable; tap is handled inside each surface.
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                }
        ) {
            // Bubble ↔ Panel cross-fade. Bubble is always composed so its
            // position/drag is preserved while the panel slides in.
            Bubble(
                state = state,
                visible = !panelOpen,
                onClick = { panelOpen = true },
                onLongClick = { onClose() }   // long-press the ball to stop the service
            )

            AnimatedVisibility(
                visible = panelOpen,
                enter = fadeIn(animationSpec = tween(180)) +
                    expandVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = 320f)),
                exit = fadeOut(animationSpec = tween(140)) +
                    shrinkVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
            ) {
                Column(modifier = Modifier.widthIn(max = 280.dp)) {
                    ControlPill(
                        state = state,
                        showCard = showCard,
                        onRefresh = onRefresh,
                        onPauseToggle = onPauseToggle,
                        onExpandToggle = { userCardExpanded = !userCardExpanded },
                        onClose = { panelOpen = false }
                    )

                    AnimatedVisibility(
                        visible = showCard,
                        enter = expandVertically(
                            animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f)
                        ) + fadeIn(animationSpec = tween(220)),
                        exit = shrinkVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        ) + fadeOut(animationSpec = tween(150))
                    ) {
                        Column {
                            Spacer(Modifier.height(6.dp))
                            ResultCard(
                                text = resultText,
                                status = statusText,
                                isError = state is OverlayState.Error,
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
                }
            }
        }
    }

    // ── Collapsed bubble (single ball) ──

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun Bubble(
        state: OverlayState,
        visible: Boolean,
        onClick: () -> Unit,
        onLongClick: () -> Unit
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(160)) + scaleIn(animationSpec = spring(stiffness = 360f)),
            exit = fadeOut(animationSpec = tween(120)) + scaleOut(animationSpec = tween(120))
        ) {
            val interactionSource = remember { MutableInteractionSource() }
            val pressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (pressed) 0.9f else 1f,
                animationSpec = tween(90),
                label = "bubblePress"
            )

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .scale(scale)
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
                    )
                    .combinedClickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                        onLongClick = onLongClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (state) {
                    is OverlayState.Loading -> LoadingRing()
                    else -> Box(
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

    // ── Control pill ──

    @Composable
    private fun ControlPill(
        state: OverlayState,
        showCard: Boolean,
        onRefresh: () -> Unit,
        onPauseToggle: () -> Unit,
        onExpandToggle: () -> Unit,
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
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Leading icon: pulsing dot when loading, translate icon otherwise
                when (state) {
                    is OverlayState.Loading -> PulsingDot()
                    else -> Icon(
                        imageVector = Icons.Filled.Translate,
                        contentDescription = "Translate",
                        tint = OverlayColors.Accent,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(Modifier.width(4.dp))

                PillIconButton(Icons.Filled.Refresh, "Refresh") { onRefresh() }
                PillIconButton(
                    if (state is OverlayState.Loading) Icons.Filled.Pause
                    else Icons.Filled.PlayArrow,
                    "Pause/Resume"
                ) { onPauseToggle() }
                ExpandChevron(expanded = showCard) { onExpandToggle() }
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
    private fun ExpandChevron(expanded: Boolean, onClick: () -> Unit) {
        val rotation by animateFloatAsState(
            targetValue = if (expanded) 180f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "chevron"
        )
        IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = if (expanded) "折叠" else "展开",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier
                    .size(16.dp)
                    .rotate(rotation)
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
