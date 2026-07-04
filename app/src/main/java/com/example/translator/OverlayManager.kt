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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
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
import androidx.lifecycle.LifecycleOwner
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

/** Overlay UI state. */
sealed class OverlayState {
    /** Idle, showing last result (or nothing). */
    data object Idle : OverlayState()
    /** A translation request is in flight; result card collapses and a spinner shows. */
    data object Loading : OverlayState()
    /** Recoverable error; show the message instead of a result. */
    data class Error(val message: String) : OverlayState()
}

/**
 * Manages the overlay window shown above other apps while a capture session is
 * running. Renders a small draggable "liquid-glass" control pill, a loading
 * spinner, and a collapsible result card.
 */
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
                AppGlassTheme {
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

    /** Show a new translation result; flips state to Idle and auto-expands. */
    fun updateText(text: String) {
        resultText = text
        statusText = ""
        overlayState = OverlayState.Idle
    }

    /** Show a transient status line (e.g. "Loading…"). */
    fun updateStatus(status: String) {
        statusText = status
    }

    /** Begin the loading flow: collapse the card and show the spinner. */
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
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
            controlView = null
        }
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    // ------------------------------------------------------------------ UI

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
        // User-controlled expansion. While loading the card is forced hidden
        // regardless of this flag; when results arrive we auto-expand.
        var userExpanded by remember { mutableStateOf(true) }
        val showCard by remember(state, resultText, statusText) {
            derivedStateOf {
                state !is OverlayState.Loading &&
                    userExpanded &&
                    (resultText.isNotEmpty() || statusText.isNotEmpty())
            }
        }

        // When leaving the Loading state, auto-expand so the new result shows.
        LaunchedEffect(state) {
            if (state is OverlayState.Idle || state is OverlayState.Error) {
                userExpanded = true
            }
        }

        Column(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                }
                .widthIn(max = 240.dp)
        ) {
            // Control pill
            GlassPill(shape = RoundedCornerShape(22.dp)) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    when (state) {
                        is OverlayState.Loading -> LoadingSpinner()
                        else -> Icon(
                            imageVector = Icons.Filled.Translate,
                            contentDescription = "Translate",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(2.dp))
                    GlassIconButton(Icons.Filled.Refresh, "Refresh") { onRefresh() }
                    GlassIconButton(
                        if (state is OverlayState.Loading) Icons.Filled.Pause
                        else Icons.Filled.PlayArrow,
                        "Pause/Resume"
                    ) { onPauseToggle() }
                    ExpandToggleButton(expanded = showCard) { userExpanded = !userExpanded }
                    GlassIconButton(Icons.Filled.Close, "Close") { onClose() }
                }
            }

            // Animated expand/collapse for the result card.
            AnimatedVisibility(
                visible = showCard,
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(animationSpec = tween(200)),
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
                            val clipboard =
                                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(
                                ClipData.newPlainText("translation", resultText.ifEmpty { statusText })
                            )
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun GlassIconButton(
        icon: ImageVector,
        desc: String,
        onClick: () -> Unit
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(30.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = desc,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }

    /** Expand/collapse chevron that smoothly rotates between states. */
    @Composable
    private fun ExpandToggleButton(expanded: Boolean, onClick: () -> Unit) {
        val rotation by animateFloatAsState(
            targetValue = if (expanded) 180f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "chevron"
        )
        IconButton(onClick = onClick, modifier = Modifier.size(30.dp)) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = if (expanded) "折叠" else "展开",
                tint = Color.White,
                modifier = Modifier
                    .size(16.dp)
                    .rotate(rotation)
            )
        }
    }

    @Composable
    private fun LoadingSpinner() {
        val transition = rememberInfiniteTransition(label = "spinner")
        val angle by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "spin"
        )
        CircularProgressIndicator(
            modifier = Modifier
                .size(18.dp)
                .rotate(angle),
            strokeWidth = 2.dp,
            color = Color.White
        )
    }

    @Composable
    private fun ResultCard(
        text: String,
        status: String,
        isError: Boolean,
        onCopy: () -> Unit
    ) {
        GlassCard(
            shape = RoundedCornerShape(18.dp),
            isError = isError,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (status.isNotEmpty()) {
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isError) Color(0xFFff8a80) else Color.White.copy(alpha = 0.7f)
                    )
                    if (text.isNotEmpty()) Spacer(Modifier.height(4.dp))
                }
                if (text.isNotEmpty()) {
                    Text(
                        text = text,
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        maxLines = 14,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AssistChip(
                            onClick = onCopy,
                            label = {
                                Text(
                                    "复制",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color.White.copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------- Glass atoms

/**
 * A "liquid glass" surface: translucent vertical gradient body with a brighter
 * top highlight, a thin semi-transparent border, soft shadow and rounded
 * corners. Works on all API levels (no RenderEffect/blur required).
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.22f),
                        Color.White.copy(alpha = 0.08f)
                    )
                )
            )
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)), shape)
            .shadow(elevation = 8.dp, shape = shape, clip = false),
    ) {
        content()
    }
}

@Composable
private fun GlassPill(
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(22.dp),
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.28f),
                        Color.Black.copy(alpha = 0.18f)
                    )
                )
            )
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.30f)), shape)
            .shadow(elevation = 10.dp, shape = shape, clip = false),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
private fun GlassCard(
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(18.dp),
    isError: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = if (isError) listOf(
                        Color(0xFFb00020).copy(alpha = 0.45f),
                        Color(0xFF7f0000).copy(alpha = 0.55f)
                    ) else listOf(
                        Color.White.copy(alpha = 0.20f),
                        Color.Black.copy(alpha = 0.22f)
                    )
                )
            )
            .border(
                BorderStroke(1.dp, Color.White.copy(alpha = 0.26f)),
                shape
            )
            .shadow(elevation = 12.dp, shape = shape, clip = false),
        content = content
    )
}

/**
 * Minimal theme wrapper so overlay content does not depend on the system
 * MaterialTheme colors (which we override with white-on-glass).
 */
@Composable
private fun AppGlassTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme()) {
        content()
    }
}

// -------------------------------------------------------------- Lifecycle owner

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
