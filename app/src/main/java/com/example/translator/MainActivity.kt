package com.example.translator

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ──────────────────────────────────────────────────────────────────────────────
//  Color Palette
// ──────────────────────────────────────────────────────────────────────────────
private val BgDark1 = Color(0xFF0D0D1A)
private val BgDark2 = Color(0xFF1A1A2E)
private val BgDark3 = Color(0xFF16213E)
private val AccentTeal = Color(0xFF00D4AA)
private val AccentCyan = Color(0xFF00B4D8)
private val AccentPurple = Color(0xFF7B2FBE)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFFA0AEC0)
private val ErrorRed = Color(0xFFFF6B6B)
private val SuccessGreen = Color(0xFF00D4AA)
private val CardBg = Color(0xFF111128)
private val CardBorder = Color.White.copy(alpha = 0.08f)

private val BgGradient = Brush.verticalGradient(listOf(BgDark1, BgDark2, BgDark3))
private val AccentGradient = Brush.horizontalGradient(listOf(AccentTeal, AccentCyan))
private val AccentGradientVertical = Brush.verticalGradient(listOf(AccentTeal, AccentCyan))

// ──────────────────────────────────────────────────────────────────────────────
//  MainActivity  (all original logic preserved)
// ──────────────────────────────────────────────────────────────────────────────
class MainActivity : ComponentActivity() {

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Check permission again after returning
        checkPermissions()
    }

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val intent = Intent(this, CaptureService::class.java).apply {
                action = CaptureService.ACTION_START
                putExtra(CaptureService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(CaptureService.EXTRA_RESULT_DATA, result.data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }

    private var hasOverlayPermission by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkPermissions()
        val prefs = PreferencesManager(this)

        setContent {
            var currentScreen by remember { mutableStateOf("main") }

            MaterialTheme {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        if (targetState == "settings") {
                            (slideInHorizontally { it } + fadeIn()) togetherWith
                                    (slideOutHorizontally { -it / 3 } + fadeOut())
                        } else {
                            (slideInHorizontally { -it } + fadeIn()) togetherWith
                                    (slideOutHorizontally { it / 3 } + fadeOut())
                        }
                    },
                    label = "screenTransition"
                ) { screen ->
                    if (screen == "main") {
                        MainScreen(
                            hasOverlayPermission = hasOverlayPermission,
                            onRequestOverlay = { requestOverlayPermission() },
                            onStartCapture = { requestMediaProjection() },
                            onStopCapture = { stopCaptureService() },
                            onRefresh = { refreshTranslation() },
                            onOpenSettings = { currentScreen = "settings" }
                        )
                    } else {
                        SettingsScreen(
                            prefs = prefs,
                            onBack = { currentScreen = "main" }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    private fun checkPermissions() {
        hasOverlayPermission = Settings.canDrawOverlays(this)
    }

    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        }
    }

    private fun requestMediaProjection() {
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission()
            return
        }
        val mediaProjectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjectionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
    }

    private fun stopCaptureService() {
        val intent = Intent(this, CaptureService::class.java).apply {
            action = CaptureService.ACTION_STOP
        }
        startService(intent)
    }

    /** Ask the running capture service to re-translate the current selection. */
    private fun refreshTranslation() {
        val intent = Intent(this, CaptureService::class.java).apply {
            action = CaptureService.ACTION_REFRESH
        }
        startService(intent)
    }
}

// ──────────────────────────────────────────────────────────────────────────────
//  Main Screen
// ──────────────────────────────────────────────────────────────────────────────
@Composable
fun MainScreen(
    hasOverlayPermission: Boolean,
    onRequestOverlay: () -> Unit,
    onStartCapture: () -> Unit,
    onStopCapture: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Hero Section ─────────────────────────────────────────────
            Spacer(Modifier.height(24.dp))
            HeroGlowIcon()
            Spacer(Modifier.height(20.dp))

            Text(
                "屏幕翻译",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "悬浮窗 · 实时 OCR · 日英译中",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )

            Spacer(Modifier.height(32.dp))

            // ── Status Card ──────────────────────────────────────────────
            StatusCard(hasOverlayPermission)

            Spacer(Modifier.height(28.dp))

            // ── Action Buttons ───────────────────────────────────────────
            if (!hasOverlayPermission) {
                PrimaryActionButton(
                    text = "授予悬浮窗权限",
                    icon = Icons.Rounded.TouchApp,
                    onClick = onRequestOverlay
                )
            } else {
                PrimaryActionButton(
                    text = "开始翻译服务",
                    icon = Icons.Rounded.PlayArrow,
                    onClick = onStartCapture
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SecondaryActionButton(
                        text = "刷新翻译",
                        icon = Icons.Rounded.Refresh,
                        onClick = onRefresh,
                        modifier = Modifier.weight(1f)
                    )
                    SecondaryActionButton(
                        text = "停止服务",
                        icon = Icons.Rounded.Stop,
                        onClick = onStopCapture,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Settings Entry ───────────────────────────────────────────
            SecondaryActionButton(
                text = "设置",
                icon = Icons.Rounded.Settings,
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
//  Hero Glow Icon – animated pulse ring
// ──────────────────────────────────────────────────────────────────────────────
@Composable
private fun HeroGlowIcon() {
    val infiniteTransition = rememberInfiniteTransition(label = "heroGlow")

    val ringScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringScale"
    )

    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringAlpha"
    )

    val innerGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "innerGlow"
    )

    Box(contentAlignment = Alignment.Center) {
        // Outer pulse ring
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(ringScale)
                .border(
                    width = 2.dp,
                    brush = Brush.sweepGradient(
                        listOf(
                            AccentTeal.copy(alpha = ringAlpha),
                            AccentPurple.copy(alpha = ringAlpha * 0.5f),
                            AccentCyan.copy(alpha = ringAlpha),
                            AccentTeal.copy(alpha = ringAlpha)
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Inner icon circle
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            AccentTeal.copy(alpha = innerGlow * 0.4f),
                            AccentPurple.copy(alpha = 0.15f),
                            CardBg
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.sweepGradient(
                        listOf(
                            AccentTeal.copy(alpha = 0.5f),
                            AccentCyan.copy(alpha = 0.3f),
                            AccentPurple.copy(alpha = 0.4f),
                            AccentTeal.copy(alpha = 0.5f)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Translate,
                contentDescription = "Translate",
                modifier = Modifier.size(36.dp),
                tint = AccentTeal
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
//  Status Card
// ──────────────────────────────────────────────────────────────────────────────
@Composable
private fun StatusCard(hasOverlayPermission: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "statusPulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )

    val statusColor = if (hasOverlayPermission) SuccessGreen else Color(0xFFFFC107)
    val statusText = if (hasOverlayPermission) "已授予" else "未授予"

    DarkGlassCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                "系统状态",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = AccentTeal,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Animated indicator dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .graphicsLayer { alpha = dotAlpha }
                        .background(statusColor, CircleShape)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "悬浮窗权限",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    statusText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
//  Dark Glass Card
// ──────────────────────────────────────────────────────────────────────────────
@Composable
fun DarkGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.06f),
                        Color.White.copy(alpha = 0.02f)
                    )
                )
            )
            .drawBehind {
                // subtle inner bg
                drawRect(CardBg.copy(alpha = 0.6f))
            }
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.04f),
                        Color.Transparent
                    )
                )
            )
            .border(1.dp, CardBorder, shape),
        content = content
    )
}

// ──────────────────────────────────────────────────────────────────────────────
//  Action Buttons
// ──────────────────────────────────────────────────────────────────────────────
@Composable
fun PrimaryActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(100),
        label = "primaryBtnScale"
    )

    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(scale)
            .clip(shape)
            .background(AccentGradient, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BgDark1,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = text,
                color = BgDark1,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun SecondaryActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(100),
        label = "secondaryBtnScale"
    )

    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .height(48.dp)
            .scale(scale)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.04f), shape)
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(AccentTeal.copy(alpha = 0.4f), AccentCyan.copy(alpha = 0.25f))
                ),
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AccentTeal,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                color = AccentTeal,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
//  Settings Screen
// ──────────────────────────────────────────────────────────────────────────────
@Composable
fun SettingsScreen(prefs: PreferencesManager, onBack: () -> Unit) {
    var apiKey by remember { mutableStateOf(prefs.apiKey) }
    var modelName by remember { mutableStateOf(prefs.modelName) }
    var baseUrl by remember { mutableStateOf(prefs.baseUrl) }
    var intervalMs by remember { mutableStateOf(prefs.recognitionIntervalMs.toString()) }
    var wifiOnly by remember { mutableStateOf(prefs.wifiOnly) }
    var apiKeyVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(48.dp))

            // ── Top Bar ──────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "返回",
                        tint = TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "设置",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── API 配置 Group ────────────────────────────────────────────
            SettingsSectionHeader("API 配置")
            Spacer(Modifier.height(10.dp))

            DarkGlassCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // API Key with visibility toggle
                    DarkTextField(
                        label = "DeepSeek API Key",
                        value = apiKey,
                        onValueChange = {
                            apiKey = it
                            prefs.apiKey = it
                        },
                        visualTransformation = if (apiKeyVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                Icon(
                                    imageVector = if (apiKeyVisible) Icons.Rounded.VisibilityOff
                                    else Icons.Rounded.Visibility,
                                    contentDescription = "切换可见性",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    )

                    DarkTextField(
                        label = "模型名称",
                        value = modelName,
                        onValueChange = {
                            modelName = it
                            prefs.modelName = it
                        }
                    )

                    DarkTextField(
                        label = "Base URL",
                        value = baseUrl,
                        onValueChange = {
                            baseUrl = it
                            prefs.baseUrl = it
                        }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── 识别设置 Group ────────────────────────────────────────────
            SettingsSectionHeader("识别设置")
            Spacer(Modifier.height(10.dp))

            DarkGlassCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DarkTextField(
                        label = "识别间隔（ms，300-5000）",
                        value = intervalMs,
                        onValueChange = {
                            intervalMs = it
                            it.toLongOrNull()?.let { ms -> prefs.recognitionIntervalMs = ms }
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "仅在 Wi-Fi 下请求",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "开启后，移动网络下将跳过翻译请求。",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = wifiOnly,
                            onCheckedChange = {
                                wifiOnly = it
                                prefs.wifiOnly = it
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BgDark1,
                                checkedTrackColor = AccentTeal,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = Color.White.copy(alpha = 0.08f),
                                uncheckedBorderColor = Color.White.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
//  Settings Helpers
// ──────────────────────────────────────────────────────────────────────────────
@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = AccentTeal,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun DarkTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(14.dp)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextSecondary, fontSize = 13.sp) },
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        singleLine = true,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        colors = TextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = AccentTeal,
            focusedContainerColor = Color.White.copy(alpha = 0.05f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
            focusedIndicatorColor = AccentTeal.copy(alpha = 0.7f),
            unfocusedIndicatorColor = Color.White.copy(alpha = 0.1f),
            focusedLabelColor = AccentTeal,
            unfocusedLabelColor = TextSecondary
        )
    )
}
